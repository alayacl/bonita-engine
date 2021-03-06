/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.actor.mapping.model.SActorBuilders;
import org.bonitasoft.engine.actor.xml.ActorBinding;
import org.bonitasoft.engine.actor.xml.ActorMappingBinding;
import org.bonitasoft.engine.actor.xml.ActorMembershipBinding;
import org.bonitasoft.engine.actor.xml.GroupPathsBinding;
import org.bonitasoft.engine.actor.xml.RoleNamesBinding;
import org.bonitasoft.engine.actor.xml.UserNamesBinding;
import org.bonitasoft.engine.api.impl.resolver.DependencyResolver;
import org.bonitasoft.engine.api.impl.transaction.actor.ImportActorMapping;
import org.bonitasoft.engine.archive.ArchiveService;
import org.bonitasoft.engine.cache.CacheService;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.command.CommandService;
import org.bonitasoft.engine.command.DefaultCommandProvider;
import org.bonitasoft.engine.command.model.SCommandBuilderAccessor;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.connector.ConnectorExecutor;
import org.bonitasoft.engine.core.category.CategoryService;
import org.bonitasoft.engine.core.category.model.builder.SCategoryBuilderAccessor;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.connector.ConnectorService;
import org.bonitasoft.engine.core.expression.control.api.ExpressionResolverService;
import org.bonitasoft.engine.core.filter.UserFilterService;
import org.bonitasoft.engine.core.login.LoginService;
import org.bonitasoft.engine.core.operation.OperationService;
import org.bonitasoft.engine.core.operation.model.builder.SOperationBuilders;
import org.bonitasoft.engine.core.process.comment.api.SCommentService;
import org.bonitasoft.engine.core.process.comment.model.builder.SCommentBuilders;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.builder.BPMDefinitionBuilders;
import org.bonitasoft.engine.core.process.document.api.ProcessDocumentService;
import org.bonitasoft.engine.core.process.document.mapping.DocumentMappingService;
import org.bonitasoft.engine.core.process.document.mapping.model.builder.SDocumentMappingBuilderAccessor;
import org.bonitasoft.engine.core.process.document.model.builder.SProcessDocumentBuilder;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.TokenService;
import org.bonitasoft.engine.core.process.instance.api.TransitionService;
import org.bonitasoft.engine.core.process.instance.api.event.EventInstanceService;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.data.DataService;
import org.bonitasoft.engine.data.definition.model.builder.SDataDefinitionBuilders;
import org.bonitasoft.engine.data.instance.api.DataInstanceService;
import org.bonitasoft.engine.data.model.builder.SDataSourceModelBuilder;
import org.bonitasoft.engine.dependency.DependencyService;
import org.bonitasoft.engine.dependency.model.builder.DependencyBuilderAccessor;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.execution.ContainerRegistry;
import org.bonitasoft.engine.execution.FlowNodeExecutor;
import org.bonitasoft.engine.execution.ProcessExecutor;
import org.bonitasoft.engine.execution.event.EventsHandler;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.expression.ExpressionService;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilders;
import org.bonitasoft.engine.external.identity.mapping.ExternalIdentityMappingService;
import org.bonitasoft.engine.external.identity.mapping.model.SExternalIdentityMappingBuilders;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.model.builder.IdentityModelBuilder;
import org.bonitasoft.engine.lock.LockService;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.platform.model.builder.STenantBuilder;
import org.bonitasoft.engine.profile.ProfileService;
import org.bonitasoft.engine.profile.builder.SProfileBuilderAccessor;
import org.bonitasoft.engine.profile.xml.ChildrenEntriesBinding;
import org.bonitasoft.engine.profile.xml.MembershipBinding;
import org.bonitasoft.engine.profile.xml.MembershipsBinding;
import org.bonitasoft.engine.profile.xml.ParentProfileEntryBinding;
import org.bonitasoft.engine.profile.xml.ProfileBinding;
import org.bonitasoft.engine.profile.xml.ProfileEntriesBinding;
import org.bonitasoft.engine.profile.xml.ProfileEntryBinding;
import org.bonitasoft.engine.profile.xml.ProfileMappingBinding;
import org.bonitasoft.engine.profile.xml.ProfilesBinding;
import org.bonitasoft.engine.queriablelogger.model.builder.SQueriableLogModelBuilder;
import org.bonitasoft.engine.search.descriptor.SearchEntitiesDescriptor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.bonitasoft.engine.supervisor.mapping.SupervisorMappingService;
import org.bonitasoft.engine.supervisor.mapping.model.SProcessSupervisorBuilders;
import org.bonitasoft.engine.transaction.TransactionService;
import org.bonitasoft.engine.xml.ElementBinding;
import org.bonitasoft.engine.xml.Parser;
import org.bonitasoft.engine.xml.ParserFactory;
import org.bonitasoft.engine.xml.SInvalidSchemaException;
import org.bonitasoft.engine.xml.XMLWriter;

/**
 * @author Matthieu Chaffotte
 * @author Yanyan Liu
 * @author Elias Ricken de Medeiros
 */
public class SpringTenantServiceAccessor implements TenantServiceAccessor {

    private final SpringTenantFileSystemBeanAccessor beanAccessor;

    private IdentityService identityService;

    private IdentityModelBuilder identityModelBuilder;

    private LoginService loginService;

    private QueriableLoggerService queriableLoggerService;

    private SQueriableLogModelBuilder logModelBuilder;

    private TechnicalLoggerService technicalLoggerService;

    private STenantBuilder tenantBuilder;

    private TransactionService transactionService;

    private ProcessDefinitionService processDefinitionService;

    private ActivityInstanceService activityInstanceService;

    private ProcessInstanceService processInstanceService;

    private TokenService tokenCountService;

    private FlowNodeExecutor flowNodeExecutor;

    private ProcessExecutor processExecutor;

    private FlowNodeStateManager flowNodeStateManager;

    private TransactionExecutor transactionExecutor;

    private BPMDefinitionBuilders bpmDefinitionBuilders;

    private BPMInstanceBuilders bpmInstanceBuilders;

    private ActorMappingService actorMappingService;

    private SActorBuilders sActorBuilders;

    private ArchiveService archiveService;

    private SCategoryBuilderAccessor categoryBuilderAccessor;

    private CategoryService categoryService;

    private SExpressionBuilders sExpressionBuilders;

    private ExpressionService expressionService;

    private CommandService commandService;

    private SCommandBuilderAccessor commandBuilderAccessor;

    private TransitionService transitionService;

    private ClassLoaderService classLoaderService;

    private DependencyService dependencyService;

    private DependencyBuilderAccessor dependencyBuilderAccessor;

    private EventInstanceService eventInstanceService;

    private final long tenantId;

    private ConnectorService connectorService;

    private ConnectorInstanceService connectorInstanceService;

    private SProcessDocumentBuilder processDocumentBuilder;

    private ProcessDocumentService processDocumentService;

    private DocumentMappingService documentMappingService;

    private SDocumentMappingBuilderAccessor documentMappingBuilderAccessor;

    private ProfileService profileService;

    private SProfileBuilderAccessor sProfileBuilderAccessor;

    private DataInstanceService dataInstanceService;

    private SDataDefinitionBuilders sDataDefinitionBuilders;

    private DataService dataService;

    private SDataSourceModelBuilder sDataSourceModelBuilder;

    private ParserFactory parserFactory;

    private OperationService operationService;

    private ExpressionResolverService expressionResolverService;

    private SupervisorMappingService supervisorService;

    private SProcessSupervisorBuilders supervisorBuilders;

    private SOperationBuilders sOperationBuilders;

    private UserFilterService userFilterService;

    private SearchEntitiesDescriptor searchEntitiesDescriptor;

    private SCommentService commentService;

    private SCommentBuilders sCommentBuilders;

    private ContainerRegistry containerRegistry;

    private ExternalIdentityMappingService externalIdentityMappingService;

    private SExternalIdentityMappingBuilders sExternalIdentityMappingBuilders;

    private LockService lockService;

    private EventsHandler eventsHandler;

    private EventService eventService;

    private ConnectorExecutor connectorExecutor;

    private CacheService cacheService;

    private DependencyResolver dependencyResolver;

    private DefaultCommandProvider commandProvider;

    public SpringTenantServiceAccessor(final Long tenantId) {
        beanAccessor = new SpringTenantFileSystemBeanAccessor(tenantId);
        this.tenantId = tenantId;
    }

    @Override
    public IdentityModelBuilder getIdentityModelBuilder() {
        if (identityModelBuilder == null) {
            identityModelBuilder = beanAccessor.getService(IdentityModelBuilder.class);
        }
        return identityModelBuilder;
    }

    @Override
    public IdentityService getIdentityService() {
        if (identityService == null) {
            identityService = beanAccessor.getService(IdentityService.class);
        }
        return identityService;
    }

    @Override
    public LoginService getLoginService() {
        if (loginService == null) {
            loginService = beanAccessor.getService(LoginService.class);
        }
        return loginService;
    }

    @Override
    public QueriableLoggerService getQueriableLoggerService() {
        if (queriableLoggerService == null) {
            queriableLoggerService = beanAccessor.getService("syncQueriableLoggerService", QueriableLoggerService.class);
        }
        return queriableLoggerService;
    }

    @Override
    public TechnicalLoggerService getTechnicalLoggerService() {
        if (technicalLoggerService == null) {
            technicalLoggerService = beanAccessor.getService(TechnicalLoggerService.class);
        }
        return technicalLoggerService;
    }

    @Override
    public STenantBuilder getSTenantBuilder() {
        if (tenantBuilder == null) {
            tenantBuilder = beanAccessor.getService(STenantBuilder.class);
        }
        return tenantBuilder;
    }

    @Override
    public TransactionService getTransactionService() {
        if (transactionService == null) {
            transactionService = beanAccessor.getService(TransactionService.class);
        }
        return transactionService;
    }

    @Override
    public ProcessDefinitionService getProcessDefinitionService() {
        if (processDefinitionService == null) {
            processDefinitionService = beanAccessor.getService(ProcessDefinitionService.class);
        }
        return processDefinitionService;
    }

    @Override
    public ProcessInstanceService getProcessInstanceService() {
        if (processInstanceService == null) {
            processInstanceService = beanAccessor.getService(ProcessInstanceService.class);
        }
        return processInstanceService;
    }

    @Override
    public TokenService getTokenService() {
        if (tokenCountService == null) {
            tokenCountService = beanAccessor.getService(TokenService.class);
        }
        return tokenCountService;
    }

    @Override
    public ActivityInstanceService getActivityInstanceService() {
        if (activityInstanceService == null) {
            activityInstanceService = beanAccessor.getService(ActivityInstanceService.class);
        }
        return activityInstanceService;
    }

    @Override
    public BPMInstanceBuilders getBPMInstanceBuilders() {
        if (bpmInstanceBuilders == null) {
            bpmInstanceBuilders = beanAccessor.getService(BPMInstanceBuilders.class);
        }
        return bpmInstanceBuilders;
    }

    @Override
    public BPMDefinitionBuilders getBPMDefinitionBuilders() {
        if (bpmDefinitionBuilders == null) {
            bpmDefinitionBuilders = beanAccessor.getService(BPMDefinitionBuilders.class);
        }
        return bpmDefinitionBuilders;
    }

    @Override
    public FlowNodeExecutor getFlowNodeExecutor() {
        if (flowNodeExecutor == null) {
            flowNodeExecutor = beanAccessor.getService(FlowNodeExecutor.class);
        }
        return flowNodeExecutor;
    }

    @Override
    public ProcessExecutor getProcessExecutor() {
        if (processExecutor == null) {
            processExecutor = beanAccessor.getService(ProcessExecutor.class);
        }
        return processExecutor;
    }

    @Override
    public FlowNodeStateManager getFlowNodeStateManager() {
        if (flowNodeStateManager == null) {
            flowNodeStateManager = beanAccessor.getService(FlowNodeStateManager.class);
        }
        return flowNodeStateManager;
    }

    @Override
    public TransactionExecutor getTransactionExecutor() {
        if (transactionExecutor == null) {
            transactionExecutor = beanAccessor.getService(TransactionExecutor.class);
        }
        return transactionExecutor;
    }

    @Override
    public SQueriableLogModelBuilder getSQueriableLogModelBuilder() {
        if (logModelBuilder == null) {
            logModelBuilder = beanAccessor.getService(SQueriableLogModelBuilder.class);
        }
        return logModelBuilder;
    }

    @Override
    public ActorMappingService getActorMappingService() {
        if (actorMappingService == null) {
            actorMappingService = beanAccessor.getService(ActorMappingService.class);
        }
        return actorMappingService;
    }

    @Override
    public ArchiveService getArchiveService() {
        if (archiveService == null) {
            archiveService = beanAccessor.getService(ArchiveService.class);
        }
        return archiveService;
    }

    @Override
    public SActorBuilders getSActorBuilders() {
        if (sActorBuilders == null) {
            sActorBuilders = beanAccessor.getService(SActorBuilders.class);
        }
        return sActorBuilders;
    }

    @Override
    public CategoryService getCategoryService() {
        if (categoryService == null) {
            categoryService = beanAccessor.getService(CategoryService.class);
        }
        return categoryService;
    }

    @Override
    public SCategoryBuilderAccessor getCategoryModelBuilderAccessor() {
        if (categoryBuilderAccessor == null) {
            categoryBuilderAccessor = beanAccessor.getService(SCategoryBuilderAccessor.class);
        }
        return categoryBuilderAccessor;
    }

    @Override
    public SExpressionBuilders getSExpressionBuilders() {
        if (sExpressionBuilders == null) {
            sExpressionBuilders = beanAccessor.getService(SExpressionBuilders.class);
        }
        return sExpressionBuilders;
    }

    @Override
    public CommandService getCommandService() {
        if (commandService == null) {
            commandService = beanAccessor.getService(CommandService.class);
        }
        return commandService;
    }

    @Override
    public SCommandBuilderAccessor getSCommandBuilderAccessor() {
        if (commandBuilderAccessor == null) {
            commandBuilderAccessor = beanAccessor.getService(SCommandBuilderAccessor.class);
        }
        return commandBuilderAccessor;
    }

    @Override
    public TransitionService getTransitionInstanceService() {
        if (transitionService == null) {
            transitionService = beanAccessor.getService(TransitionService.class);
        }
        return transitionService;
    }

    @Override
    public ClassLoaderService getClassLoaderService() {
        if (classLoaderService == null) {
            classLoaderService = beanAccessor.getService("classLoaderService", ClassLoaderService.class);
        }
        return classLoaderService;
    }

    @Override
    public DependencyService getDependencyService() {
        if (dependencyService == null) {
            dependencyService = beanAccessor.getService("dependencyService", DependencyService.class);
        }
        return dependencyService;
    }

    @Override
    public DependencyBuilderAccessor getDependencyBuilderAccessor() {
        if (dependencyBuilderAccessor == null) {
            dependencyBuilderAccessor = beanAccessor.getService("dependencyBuilderAccessor", DependencyBuilderAccessor.class);
        }
        return dependencyBuilderAccessor;
    }

    @Override
    public DocumentMappingService getDocumentMappingService() {
        if (documentMappingService == null) {
            documentMappingService = beanAccessor.getService(DocumentMappingService.class);
        }
        return documentMappingService;
    }

    @Override
    public SDocumentMappingBuilderAccessor getDocumentMappingBuilderAccessor() {
        if (documentMappingBuilderAccessor == null) {
            documentMappingBuilderAccessor = beanAccessor.getService(SDocumentMappingBuilderAccessor.class);
        }
        return documentMappingBuilderAccessor;
    }

    @Override
    public long getTenantId() {
        return tenantId;
    }

    @Override
    public EventInstanceService getEventInstanceService() {
        if (eventInstanceService == null) {
            eventInstanceService = beanAccessor.getService(EventInstanceService.class);
        }
        return eventInstanceService;
    }

    @Override
    public ConnectorService getConnectorService() {
        if (connectorService == null) {
            connectorService = beanAccessor.getService("connectorService", ConnectorService.class);
        }
        return connectorService;
    }

    @Override
    public ConnectorInstanceService getConnectorInstanceService() {
        if (connectorInstanceService == null) {
            connectorInstanceService = beanAccessor.getService(ConnectorInstanceService.class);
        }
        return connectorInstanceService;
    }

    @Override
    public ConnectorExecutor getConnectorExecutor() {
        if (connectorExecutor == null) {
            connectorExecutor = beanAccessor.getService(ConnectorExecutor.class);
        }
        return connectorExecutor;
    }

    @Override
    public ExpressionService getExpressionService() {
        if (expressionService == null) {
            expressionService = beanAccessor.getService(ExpressionService.class);
        }
        return expressionService;
    }

    @Override
    public SProcessDocumentBuilder getProcessDocumentBuilder() {
        if (processDocumentBuilder == null) {
            processDocumentBuilder = beanAccessor.getService(SProcessDocumentBuilder.class);
        }
        return processDocumentBuilder;
    }

    @Override
    public ProcessDocumentService getProcessDocumentService() {
        if (processDocumentService == null) {
            processDocumentService = beanAccessor.getService(ProcessDocumentService.class);
        }
        return processDocumentService;
    }

    @Override
    public ProfileService getProfileService() {
        if (profileService == null) {
            profileService = beanAccessor.getService(ProfileService.class);
        }
        return profileService;
    }

    @Override
    public SProfileBuilderAccessor getSProfileBuilderAccessor() {
        if (sProfileBuilderAccessor == null) {
            sProfileBuilderAccessor = beanAccessor.getService(SProfileBuilderAccessor.class);
        }
        return sProfileBuilderAccessor;
    }

    @Override
    public DataInstanceService getDataInstanceService() {
        if (dataInstanceService == null) {
            dataInstanceService = beanAccessor.getService(DataInstanceService.class);
        }
        return dataInstanceService;
    }

    @Override
    public SDataDefinitionBuilders getSDataDefinitionBuilders() {
        if (sDataDefinitionBuilders == null) {
            sDataDefinitionBuilders = beanAccessor.getService(SDataDefinitionBuilders.class);
        }
        return sDataDefinitionBuilders;
    }

    @Override
    public SDataSourceModelBuilder getSDataSourceModelBuilder() {
        if (sDataSourceModelBuilder == null) {
            sDataSourceModelBuilder = beanAccessor.getService(SDataSourceModelBuilder.class);
        }
        return sDataSourceModelBuilder;
    }

    @Override
    public DataService getDataService() {
        if (dataService == null) {
            dataService = beanAccessor.getService(DataService.class);
        }
        return dataService;
    }

    @Override
    public ParserFactory getParserFactgory() {
        if (parserFactory == null) {
            parserFactory = beanAccessor.getService(ParserFactory.class);
        }
        return parserFactory;
    }

    @Override
    public OperationService getOperationService() {
        if (operationService == null) {
            operationService = beanAccessor.getService(OperationService.class);
        }
        return operationService;
    }

    @Override
    public Parser getActorMappingParser() {
        final List<Class<? extends ElementBinding>> bindings = new ArrayList<Class<? extends ElementBinding>>();
        bindings.add(ActorMappingBinding.class);
        bindings.add(ActorBinding.class);
        bindings.add(UserNamesBinding.class);
        bindings.add(GroupPathsBinding.class);
        bindings.add(RoleNamesBinding.class);
        bindings.add(ActorMembershipBinding.class);
        final Parser parser = getParserFactgory().createParser(bindings);
        final InputStream resource = ImportActorMapping.class.getClassLoader().getResourceAsStream("actorMapping.xsd");
        try {
            parser.setSchema(resource);
            return parser;
        } catch (final SInvalidSchemaException ise) {
            throw new BonitaRuntimeException(ise);
        } finally {
            try {
                resource.close();
            } catch (final IOException ioe) {
                throw new BonitaRuntimeException(ioe);
            }
        }
    }

    @Override
    public XMLWriter getXMLWriter() {
        return beanAccessor.getService(XMLWriter.class);
    }

    @Override
    public ExpressionResolverService getExpressionResolverService() {
        if (expressionResolverService == null) {
            expressionResolverService = beanAccessor.getService(ExpressionResolverService.class);
        }
        return expressionResolverService;
    }

    @Override
    public SupervisorMappingService getSupervisorService() {
        if (supervisorService == null) {
            supervisorService = beanAccessor.getService(SupervisorMappingService.class);
        }
        return supervisorService;
    }

    @Override
    public SProcessSupervisorBuilders getSSupervisorBuilders() {
        if (supervisorBuilders == null) {
            supervisorBuilders = beanAccessor.getService(SProcessSupervisorBuilders.class);
        }
        return supervisorBuilders;
    }

    @Override
    public SOperationBuilders getSOperationBuilders() {
        if (sOperationBuilders == null) {
            sOperationBuilders = beanAccessor.getService(SOperationBuilders.class);
        }
        return sOperationBuilders;
    }

    @Override
    public UserFilterService getUserFilterService() {
        if (userFilterService == null) {
            userFilterService = beanAccessor.getService("userFilterService", UserFilterService.class);
        }
        return userFilterService;
    }

    @Override
    public SearchEntitiesDescriptor getSearchEntitiesDescriptor() {
        if (searchEntitiesDescriptor == null) {
            searchEntitiesDescriptor = beanAccessor.getService(SearchEntitiesDescriptor.class);
        }
        return searchEntitiesDescriptor;
    }

    @Override
    public SCommentService getCommentService() {
        if (commentService == null) {
            commentService = beanAccessor.getService(SCommentService.class);
        }
        return commentService;
    }

    @Override
    public SCommentBuilders getSCommentBuilders() {
        if (sCommentBuilders == null) {
            sCommentBuilders = beanAccessor.getService(SCommentBuilders.class);
        }
        return sCommentBuilders;
    }

    @Override
    public ContainerRegistry getContainerRegistry() {
        if (containerRegistry == null) {
            containerRegistry = beanAccessor.getService(ContainerRegistry.class);
        }
        return containerRegistry;
    }

    @Override
    public ExternalIdentityMappingService getExternalIdentityMappingService() {
        if (externalIdentityMappingService == null) {
            externalIdentityMappingService = beanAccessor.getService(ExternalIdentityMappingService.class);
        }
        return externalIdentityMappingService;
    }

    @Override
    public SExternalIdentityMappingBuilders getExternalIdentityMappingBuilders() {
        if (sExternalIdentityMappingBuilders == null) {
            sExternalIdentityMappingBuilders = beanAccessor.getService(SExternalIdentityMappingBuilders.class);
        }
        return sExternalIdentityMappingBuilders;
    }

    @Override
    public LockService getLockService() {
        if (lockService == null) {
            lockService = beanAccessor.getService(LockService.class);
        }
        return lockService;
    }

    @Override
    public Parser getProfileParser() {
        final List<Class<? extends ElementBinding>> bindings = new ArrayList<Class<? extends ElementBinding>>();
        bindings.add(ProfileBinding.class);
        bindings.add(ProfilesBinding.class);
        bindings.add(ProfileEntryBinding.class);
        bindings.add(ParentProfileEntryBinding.class);
        bindings.add(ChildrenEntriesBinding.class);
        bindings.add(ProfileEntriesBinding.class);
        bindings.add(ProfileMappingBinding.class);
        bindings.add(MembershipsBinding.class);
        bindings.add(MembershipBinding.class);
        bindings.add(UserNamesBinding.class);
        bindings.add(RoleNamesBinding.class);
        bindings.add(RoleNamesBinding.class);
        bindings.add(GroupPathsBinding.class);
        final Parser parser = getParserFactgory().createParser(bindings);
        final InputStream resource = ImportActorMapping.class.getClassLoader().getResourceAsStream("profiles.xsd");
        try {
            parser.setSchema(resource);
            return parser;
        } catch (final SInvalidSchemaException ise) {
            throw new BonitaRuntimeException(ise);
        } finally {
            try {
                resource.close();
            } catch (final IOException ioe) {
                throw new BonitaRuntimeException(ioe);
            }
        }
    }

    @Override
    public EventsHandler getEventsHandler() {
        if (eventsHandler == null) {
            eventsHandler = beanAccessor.getService(EventsHandler.class);
        }
        return eventsHandler;
    }

    @Override
    public EventService getEventService() {
        if (eventService == null) {
            eventService = beanAccessor.getService(EventService.class);
        }
        return eventService;
    }

    @Override
    public void initializeServiceAccessor(final ClassLoader classLoader) {
        beanAccessor.initializeContext(classLoader);
    }

    public SpringTenantFileSystemBeanAccessor getBeanAccessor() {
        return beanAccessor;
    }

    @Override
    public CacheService getCacheService() {
        if (cacheService == null) {
            cacheService = beanAccessor.getService("cacheService", CacheService.class);
        }
        return cacheService;
    }

    @Override
    public DependencyResolver getDependencyResolver() {
        if (dependencyResolver == null) {
            dependencyResolver = beanAccessor.getService(DependencyResolver.class);
        }
        return dependencyResolver;
    }

    @Override
    public DefaultCommandProvider getDefaultCommandProvider() {
        if (commandProvider == null) {
            commandProvider = beanAccessor.getService(DefaultCommandProvider.class);
        }
        return commandProvider;
    }

}
