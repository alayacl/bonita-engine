/**
 * Copyright (C) 2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.instance.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.core.process.instance.api.FlowNodeInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeModificationException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeReadException;
import org.bonitasoft.engine.core.process.instance.api.states.FlowNodeState;
import org.bonitasoft.engine.core.process.instance.model.SFlowElementInstance;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SStateCategory;
import org.bonitasoft.engine.core.process.instance.model.STaskPriority;
import org.bonitasoft.engine.core.process.instance.model.archive.SAFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.core.process.instance.model.builder.SFlowNodeInstanceLogBuilder;
import org.bonitasoft.engine.core.process.instance.model.builder.SUserTaskInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.recorder.SelectDescriptorBuilder;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.PersistentObject;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.queriablelogger.model.builder.SPersistenceLogBuilder;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;

/**
 * @author Elias Ricken de Medeiros
 * @author Frederic Bouquet
 */
public abstract class FlowNodeInstanceServiceImpl implements FlowNodeInstanceService {

    private final SUserTaskInstanceBuilder activityInstanceKeyProvider;

    protected final BPMInstanceBuilders instanceBuilders;

    private final EventService eventService;

    private final Recorder recorder;

    private final ReadPersistenceService persistenceRead;

    private final QueriableLoggerService queriableLoggerService;

    private final TechnicalLoggerService logger;

    public FlowNodeInstanceServiceImpl(final Recorder recorder, final ReadPersistenceService persistenceRead, final EventService eventService,
            final BPMInstanceBuilders instanceBuilders, final QueriableLoggerService queriableLoggerService, final TechnicalLoggerService logger) {
        this.recorder = recorder;
        this.persistenceRead = persistenceRead;
        this.instanceBuilders = instanceBuilders;
        this.logger = logger;
        activityInstanceKeyProvider = instanceBuilders.getSUserTaskInstanceBuilder();
        this.eventService = eventService;
        this.queriableLoggerService = queriableLoggerService;
    }

    protected <T extends SLogBuilder> void initializeLogBuilder(final T logBuilder, final String message) {
        logBuilder.createNewInstance().actionStatus(SQueriableLog.STATUS_FAIL).severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    protected <T extends HasCRUDEAction> void updateLog(final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    protected SFlowNodeInstanceLogBuilder getQueriableLog(final ActionType actionType, final String message, final SFlowElementInstance flowElementInstance) {
        final SFlowNodeInstanceLogBuilder logBuilder = instanceBuilders.getActivityInstanceLogBuilder();
        initializeLogBuilder(logBuilder, message);
        updateLog(actionType, logBuilder);
        logBuilder.processInstanceId(flowElementInstance.getRootContainerId());
        return logBuilder;
    }

    @Override
    public void setState(final SFlowNodeInstance flowNodeInstance, final FlowNodeState state) throws SFlowNodeModificationException {
        final SFlowNodeInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Updating flow node instance state", flowNodeInstance);
        final long now = System.currentTimeMillis();
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getPreviousStateIdKey(), flowNodeInstance.getStateId());
        descriptor.addField(activityInstanceKeyProvider.getStateIdKey(), state.getId());
        descriptor.addField(activityInstanceKeyProvider.getStateNameKey(), state.getName());
        descriptor.addField(activityInstanceKeyProvider.getStableKey(), state.isStable());
        descriptor.addField(activityInstanceKeyProvider.getTerminalKey(), state.isTerminal());
        descriptor.addField(activityInstanceKeyProvider.getReachStateDateKey(), now);
        descriptor.addField(activityInstanceKeyProvider.getLastUpdateDateKey(), now);
        descriptor.addField(activityInstanceKeyProvider.getStateExecutingKey(), false);
        if (logger.isLoggable(getClass(), TechnicalLogSeverity.DEBUG)) {
            logger.log(
                    getClass(),
                    TechnicalLogSeverity.DEBUG,
                    MessageFormat.format("[{0} with id {1}] changed state {2}->{3}(new={4})", flowNodeInstance.getClass().getSimpleName(),
                            flowNodeInstance.getId(), flowNodeInstance.getStateId(), state.getId(), state.getClass().getSimpleName()));
        }

        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(flowNodeInstance, descriptor);
        final SUpdateEvent updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(ACTIVITYINSTANCE_STATE)
                .setObject(flowNodeInstance)
                .done();
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "setState");
        } catch (final SRecorderException e) {
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "setState");
            throw new SFlowNodeModificationException(e);
        }
    }

    @Override
    public void setExecuting(final SFlowNodeInstance flowNodeInstance) throws SFlowNodeModificationException {
        final SFlowNodeInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Updating flow node instance state", flowNodeInstance);
        final long now = System.currentTimeMillis();
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getStateExecutingKey(), true);
        descriptor.addField(activityInstanceKeyProvider.getLastUpdateDateKey(), now);
        if (logger.isLoggable(getClass(), TechnicalLogSeverity.DEBUG)) {
            logger.log(
                    getClass(),
                    TechnicalLogSeverity.DEBUG,
                    MessageFormat.format("[{0} with id {1}] have executing flag set to true", flowNodeInstance.getClass().getSimpleName(),
                            flowNodeInstance.getId()));
        }

        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(flowNodeInstance, descriptor);
        final SUpdateEvent updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(ACTIVITYINSTANCE_STATE)
                .setObject(flowNodeInstance)
                .done();
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "setState");
        } catch (final SRecorderException e) {
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "setState");
            throw new SFlowNodeModificationException(e);
        }
    }

    @Override
    public void updateDisplayName(final SFlowNodeInstance flowNodeInstance, final String displayName) throws SFlowNodeModificationException {
        if (displayName != null && !displayName.equals(flowNodeInstance.getDisplayName())) {
            final String event = ACTIVITYINSTANCE_DISPLAY_NAME;
            final String key = activityInstanceKeyProvider.getDisplayNameKey();
            updateOneField(flowNodeInstance, displayName, event, key);
        }
    }

    private void updateOneField(final SFlowNodeInstance flowNodeInstance, final String displayDescription, final String event, final String lastUpdateKey)
            throws SFlowNodeModificationException {
        final SFlowNodeInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, event, flowNodeInstance);
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(lastUpdateKey, displayDescription);

        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(flowNodeInstance, descriptor);
        final SUpdateEvent updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(event).setObject(flowNodeInstance).done();
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateOneField");

        } catch (final SRecorderException e) {
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateOneField");

            throw new SFlowNodeModificationException(e);
        }
    }

    @Override
    public void updateDisplayDescription(final SFlowNodeInstance flowNodeInstance, final String displayDescription) throws SFlowNodeModificationException {
        if (displayDescription != null && !displayDescription.equals(flowNodeInstance.getDisplayDescription())) {
            final String event = ACTIVITYINSTANCE_DISPLAY_DESCRIPTION;
            final String key = activityInstanceKeyProvider.getDisplayDescriptionKey();
            updateOneField(flowNodeInstance, displayDescription, event, key);
        }
    }

    @Override
    public void setTaskPriority(final SFlowNodeInstance flowNodeInstance, final STaskPriority priority) throws SFlowNodeModificationException {
        final SFlowNodeInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Updating flow node instance state", flowNodeInstance);
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getPriorityKey(), priority);

        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(flowNodeInstance, descriptor);
        final SUpdateEvent updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(ACTIVITYINSTANCE_STATE)
                .setObject(flowNodeInstance)
                .done();
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "setTaskPriority");

        } catch (final SRecorderException e) {
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "setTaskPriority");

            throw new SFlowNodeModificationException(e);
        }
    }

    @Override
    public List<SFlowNodeInstance> getActiveFlowNodes(final long rootContainerId) throws SFlowNodeReadException {
        try {
            final SelectListDescriptor<SFlowNodeInstance> selectListDescriptor = SelectDescriptorBuilder.getActiveFlowNodes(rootContainerId);
            return persistenceRead.selectList(selectListDescriptor);
        } catch (final SBonitaReadException bre) {
            // TODO log the exception
            throw new SFlowNodeReadException(bre);
        }
    }

    @Override
    public SFlowNodeInstance getFlowNodeInstance(final long flowNodeInstanceId) throws SFlowNodeNotFoundException, SFlowNodeReadException {
        SFlowNodeInstance selectOne;
        try {
            selectOne = persistenceRead.selectById(SelectDescriptorBuilder
                    .getElementById(SFlowNodeInstance.class, "SFlowNodeInstance", flowNodeInstanceId));
        } catch (final SBonitaReadException e) {
            throw new SFlowNodeReadException(e);
        }
        if (selectOne == null) {
            throw new SFlowNodeNotFoundException(flowNodeInstanceId);
        }
        return selectOne;
    }

    @Override
    public List<SFlowNodeInstance> getFlowNodeInstances(final long rootContainerId, final int fromIndex, final int maxResults) throws SFlowNodeReadException {
        List<SFlowNodeInstance> selectList;
        try {
            selectList = getPersistenceRead().selectList(SelectDescriptorBuilder.getFlowNodesFromProcessInstance(rootContainerId, fromIndex, maxResults));
        } catch (final SBonitaReadException e) {
            throw new SFlowNodeReadException(e);
        }
        return getUnmodifiableList(selectList);
    }

    @Override
    public List<SAFlowNodeInstance> getArchivedFlowNodeInstances(final long rootContainerId, final int fromIndex, final int maxResults)
            throws SFlowNodeReadException {
        List<SAFlowNodeInstance> selectList;
        try {
            selectList = getPersistenceRead().selectList(
                    SelectDescriptorBuilder.getArchivedFlowNodesFromProcessInstance(rootContainerId, fromIndex, maxResults));
        } catch (final SBonitaReadException e) {
            throw new SFlowNodeReadException(e);
        }
        return getUnmodifiableList(selectList);
    }

    @Override
    public SAFlowNodeInstance getArchivedFlowNodeInstance(final long archivedFlowNodeInstanceId, final ReadPersistenceService persistenceService)
            throws SFlowNodeReadException, SFlowNodeNotFoundException {
        SAFlowNodeInstance selectOne;
        try {
            selectOne = persistenceService.selectById(SelectDescriptorBuilder.getElementById(SAFlowNodeInstance.class, "SArchivedFlowNodeInstance",
                    archivedFlowNodeInstanceId));
        } catch (final SBonitaReadException e) {
            throw new SFlowNodeReadException(e);
        }
        if (selectOne == null) {
            throw new SFlowNodeNotFoundException(archivedFlowNodeInstanceId);
        }
        return selectOne;
    }

    @Override
    public void setStateCategory(final SFlowElementInstance flowElementInstance, final SStateCategory stateCategory) throws SFlowNodeModificationException {
        final SFlowNodeInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "update stateCategory", flowElementInstance);
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getStateCategoryKey(), stateCategory);

        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(flowElementInstance, descriptor);
        final SUpdateEvent updateEvent = (SUpdateEvent) getEventService().getEventBuilder().createUpdateEvent(STATE_CATEGORY).setObject(flowElementInstance)
                .done();
        try {
            getRecorder().recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(flowElementInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "setStateCategory");

        } catch (final SRecorderException sre) {
            initiateLogBuilder(flowElementInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "setStateCategory");
            throw new SFlowNodeModificationException(sre);
        }

    }

    @Override
    public void setExecutedBy(final SFlowNodeInstance flowNodeInstance, final long userId) throws SFlowNodeModificationException {
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getExecutedBy(), userId);
        updateFlowNode(flowNodeInstance, EXECUTED_BY_MODIFIED, descriptor);
    }

    @Override
    public void setExecutedByDelegate(final SFlowNodeInstance flowNodeInstance, final long executerDelegateId) throws SFlowNodeModificationException {
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getExecutedByDelegate(), executerDelegateId);
        updateFlowNode(flowNodeInstance, EXECUTED_BY_DELEGATE_MODIFIED, descriptor);
    }

    @Override
    public void setExpectedEndDate(final SFlowNodeInstance flowNodeInstance, final long dueDate) throws SFlowNodeModificationException {
        final EntityUpdateDescriptor descriptor = new EntityUpdateDescriptor();
        descriptor.addField(activityInstanceKeyProvider.getExpectedEndDateKey(), dueDate);
        updateFlowNode(flowNodeInstance, EXPECTED_END_DATE_MODIFIED, descriptor);
    }

    protected void updateFlowNode(final SFlowNodeInstance flowNodeInstance, final String eventName, final EntityUpdateDescriptor descriptor)
            throws SFlowNodeModificationException {
        final SFlowNodeInstanceLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, eventName, flowNodeInstance);
        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(flowNodeInstance, descriptor);
        final SUpdateEvent updateEvent = (SUpdateEvent) getEventService().getEventBuilder().createUpdateEvent(eventName).setObject(flowNodeInstance).done();
        try {
            getRecorder().recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateActivity");
        } catch (final SRecorderException sre) {
            initiateLogBuilder(flowNodeInstance.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateActivity");
            throw new SFlowNodeModificationException(sre);
        }
    }

    protected <T> List<T> getUnmodifiableList(List<T> selectList) {
        if (selectList == null) {
            selectList = new ArrayList<T>();
        }
        return Collections.unmodifiableList(selectList);
    }

    @Override
    public long getNumberOfFlowNodeInstances(final Class<? extends PersistentObject> entityClass, final QueryOptions countOptions)
            throws SBonitaSearchException {
        try {
            return getPersistenceRead().getNumberOfEntities(entityClass, countOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SFlowNodeInstance> searchFlowNodeInstances(final Class<? extends PersistentObject> entityClass, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            return (List<SFlowNodeInstance>) getPersistenceRead().searchEntity(entityClass, searchOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public long getNumberOfArchivedFlowNodeInstances(final Class<? extends SAFlowNodeInstance> entityClass, final QueryOptions countOptions)
            throws SBonitaSearchException {
        try {
            return getPersistenceRead().getNumberOfEntities(entityClass, countOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SAFlowNodeInstance> searchArchivedFlowNodeInstances(final Class<? extends SAFlowNodeInstance> entityClass, final QueryOptions searchOptions)
            throws SBonitaSearchException {
        try {
            return (List<SAFlowNodeInstance>) getPersistenceRead().searchEntity(entityClass, searchOptions, null);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    protected EventService getEventService() {
        return eventService;
    }

    protected Recorder getRecorder() {
        return recorder;
    }

    protected ReadPersistenceService getPersistenceRead() {
        return persistenceRead;
    }

    protected BPMInstanceBuilders getInstanceBuilders() {
        return instanceBuilders;
    }

    protected TechnicalLoggerService getLogger() {
        return logger;
    }

    private void initiateLogBuilder(final long objectId, final int sQueriableLogStatus, final SPersistenceLogBuilder logBuilder, final String callerMethodName) {
        logBuilder.actionScope(String.valueOf(objectId));
        logBuilder.actionStatus(sQueriableLogStatus);
        logBuilder.objectId(objectId);
        final SQueriableLog log = logBuilder.done();
        if (queriableLoggerService.isLoggable(log.getActionType(), log.getSeverity())) {
            queriableLoggerService.log(getClass().getName(), callerMethodName, log);
        }
    }

    @Override
    public List<SFlowNodeInstance> getFlowNodeInstancesToRestart(final QueryOptions queryOptions) throws SFlowNodeReadException {
        List<SFlowNodeInstance> selectList;
        try {
            final Map<String, Object> parameters = new HashMap<String, Object>(1);
            selectList = getPersistenceRead().selectList(
                    new SelectListDescriptor<SFlowNodeInstance>("getFlowNodeInstancesToRestart", parameters, SFlowNodeInstance.class, queryOptions));
        } catch (final SBonitaReadException e) {
            throw new SFlowNodeReadException(e);
        }
        return getUnmodifiableList(selectList);
    }
}
