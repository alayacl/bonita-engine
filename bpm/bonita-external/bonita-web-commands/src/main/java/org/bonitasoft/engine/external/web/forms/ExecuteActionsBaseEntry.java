/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.engine.external.web.forms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bonitasoft.engine.api.impl.transaction.activity.GetActivityInstance;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessDefinition;
import org.bonitasoft.engine.api.impl.transaction.process.GetProcessInstance;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceNotFoundException;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceNotFoundException;
import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.command.system.CommandWithParameters;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.core.operation.model.SLeftOperand;
import org.bonitasoft.engine.core.operation.model.SOperation;
import org.bonitasoft.engine.core.operation.model.SOperatorType;
import org.bonitasoft.engine.core.operation.model.builder.SOperationBuilders;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.SProcessDefinitionNotFoundException;
import org.bonitasoft.engine.core.process.definition.exception.SProcessDefinitionReadException;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SActivityInstance;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.ClassLoaderException;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.engine.expression.exception.SInvalidExpressionException;
import org.bonitasoft.engine.expression.model.SExpression;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilders;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.operation.LeftOperand;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.search.descriptor.SearchProcessInstanceDescriptor;
import org.bonitasoft.engine.service.PlatformServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.sessionaccessor.TenantIdNotSetException;

/**
 * @author Ruiheng Fan
 * @author Matthieu Chaffotte
 */
public abstract class ExecuteActionsBaseEntry extends CommandWithParameters {

    protected static final String ACTIVITY_INSTANCE_ID_KEY = "ACTIVITY_INSTANCE_ID_KEY";

    protected static final String PROCESS_DEFINITION_ID_KEY = "PROCESS_DEFINITION_ID_KEY";

    protected static final String OPERATIONS_LIST_KEY = "OPERATIONS_LIST_KEY";

    protected static final String OPERATIONS_INPUT_KEY = "OPERATIONS_INPUT_KEY";

    protected static final String CONNECTORS_LIST_KEY = "CONNECTORS_LIST_KEY";

    protected static final String USER_ID_KEY = "USER_ID_KEY";

    protected static TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    protected void log(final TenantServiceAccessor tenantAccessor, final Exception e) {
        final TechnicalLoggerService logger = tenantAccessor.getTechnicalLoggerService();
        logger.log(this.getClass(), TechnicalLogSeverity.DEBUG, e);
    }

    protected long getTenantId() {
        SessionAccessor sessionAccessor = null;
        try {
            sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
        try {
            return sessionAccessor.getTenantId();
        } catch (final TenantIdNotSetException e) {
            throw new BonitaRuntimeException(e);
        }
    }

    protected SOperation toSOperation(final Operation operation, final SOperationBuilders sOperationBuilders, final SExpressionBuilders sExpressionBuilders)
            throws SInvalidExpressionException {
        final SExpression rightOperand = toSExpression(operation.getRightOperand(), sExpressionBuilders);
        final SOperatorType operatorType = SOperatorType.valueOf(operation.getType().name());
        final SLeftOperand sLeftOperand = toSLeftOperand(operation.getLeftOperand(), sOperationBuilders);
        final SOperation sOperation = sOperationBuilders.getSOperationBuilder().createNewInstance().setOperator(operation.getOperator())
                .setRightOperand(rightOperand).setType(operatorType).setLeftOperand(sLeftOperand).done();
        return sOperation;
    }

    protected SLeftOperand toSLeftOperand(final LeftOperand variableToSet, final SOperationBuilders sOperationBuilders) {
        return sOperationBuilders.getSLeftOperandBuilder().createNewInstance().setName(variableToSet.getName()).done();
    }

    protected SExpression toSExpression(final Expression exp, final SExpressionBuilders sExpressionBuilders) throws SInvalidExpressionException {
        final List<SExpression> dependencies = new ArrayList<SExpression>(exp.getDependencies().size());
        if (!exp.getDependencies().isEmpty()) {
            for (final Expression dependency : exp.getDependencies()) {
                dependencies.add(toSExpression(dependency, sExpressionBuilders));
            }
        }
        final SExpression sExpression = sExpressionBuilders.getExpressionBuilder().createNewInstance().setName(exp.getName()).setContent(exp.getContent())
                .setExpressionType(exp.getExpressionType()).setInterpreter(exp.getInterpreter()).setReturnType(exp.getReturnType())
                .setDependencies(dependencies).done();
        return sExpression;
    }

    protected List<SOperation> toSOperation(final List<Operation> operations, final SOperationBuilders sOperationBuilders,
            final SExpressionBuilders sExpressionBuilders) throws SInvalidExpressionException {
        if (operations == null) {
            return null;
        }
        if (operations.isEmpty()) {
            return Collections.emptyList();
        }
        final List<SOperation> sOperations = new ArrayList<SOperation>(operations.size());
        for (final Operation operation : operations) {
            final SOperation sOperation = toSOperation(operation, sOperationBuilders, sExpressionBuilders);
            sOperations.add(sOperation);
        }
        return sOperations;
    }

    protected SActivityInstance getActivityInstance(final TenantServiceAccessor tenantAccessor, final long activityInstanceId)
            throws ActivityInstanceNotFoundException {
        final ActivityInstanceService activityInstanceService = tenantAccessor.getActivityInstanceService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final GetActivityInstance getActivityInstance = new GetActivityInstance(activityInstanceService, activityInstanceId);
        try {
            transactionExecutor.execute(getActivityInstance);
        } catch (final SBonitaException e) {
            throw new ActivityInstanceNotFoundException(activityInstanceId);
        }
        return getActivityInstance.getResult();
    }

    protected ProcessInstance getProcessInstance(final TenantServiceAccessor tenantAccessor, final long processInstanceId)
            throws ProcessInstanceNotFoundException {
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final SearchProcessInstanceDescriptor searchProcessInstanceDescriptor = tenantAccessor.getSearchEntitiesDescriptor().getProcessInstanceDescriptor();

        final GetProcessInstance getProcessInstance = new GetProcessInstance(processInstanceService, processDefinitionService, searchProcessInstanceDescriptor,
                processInstanceId);
        try {
            transactionExecutor.execute(getProcessInstance);
        } catch (final SBonitaException e) {
            throw new ProcessInstanceNotFoundException(processInstanceId);
        }
        return getProcessInstance.getResult();
    }

    protected ClassLoader getLocalClassLoader(final TenantServiceAccessor tenantAccessor, final long processDefinitionId) throws ClassLoaderException {
        final ClassLoaderService classLoaderService = tenantAccessor.getClassLoaderService();
        try {
            return classLoaderService.getLocalClassLoader("process", processDefinitionId);
        } catch (org.bonitasoft.engine.classloader.ClassLoaderException e) {
            throw new ClassLoaderException(e);
        }
    }

    protected SProcessDefinition getServerProcessDefinition(final TransactionExecutor transactionExecutor, final long processDefinitionUUID,
            final ProcessDefinitionService processDefinitionService) throws SProcessDefinitionNotFoundException, SProcessDefinitionReadException {
        final TransactionContentWithResult<SProcessDefinition> transactionContentWithResult = new GetProcessDefinition(processDefinitionUUID,
                processDefinitionService);
        try {
            transactionExecutor.execute(transactionContentWithResult);
            return transactionContentWithResult.getResult();
        } catch (final SProcessDefinitionNotFoundException e) {
            throw e;
        } catch (final SProcessDefinitionReadException e) {
            throw e;
        } catch (final SBonitaException e) {
            throw new SProcessDefinitionNotFoundException(e);
        }
    }

    protected long getUserIdFromSession() {
        SessionAccessor sessionAccessor = null;
        long userId;
        try {
            sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long sessionId = sessionAccessor.getSessionId();
            final PlatformServiceAccessor platformServiceAccessor = ServiceAccessorFactory.getInstance().createPlatformServiceAccessor();
            userId = platformServiceAccessor.getSessionService().getSession(sessionId).getUserId();
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
        return userId;
    }

    protected String getUserNameFromSession() {
        SessionAccessor sessionAccessor = null;
        String userName;
        try {
            sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long sessionId = sessionAccessor.getSessionId();
            final PlatformServiceAccessor platformServiceAccessor = ServiceAccessorFactory.getInstance().createPlatformServiceAccessor();
            userName = platformServiceAccessor.getSessionService().getSession(sessionId).getUserName();
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
        return userName;
    }

    protected SProcessDefinition getProcessDefinition(final TenantServiceAccessor tenantAccessor, final long processDefinitionId)
            throws InvalidProcessDefinitionException {
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final GetProcessDefinition getProcessDefinition = new GetProcessDefinition(processDefinitionId, processDefinitionService);
        try {
            transactionExecutor.execute(getProcessDefinition);
        } catch (final SBonitaException e) {
            throw new InvalidProcessDefinitionException("invalid processDefinition with id:" + processDefinitionId);
        }
        return getProcessDefinition.getResult();
    }

}
