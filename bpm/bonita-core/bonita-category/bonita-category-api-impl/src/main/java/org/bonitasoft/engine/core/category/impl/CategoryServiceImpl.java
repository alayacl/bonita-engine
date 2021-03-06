/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.category.impl;

import java.util.Collections;
import java.util.List;

import org.bonitasoft.engine.core.category.CategoryService;
import org.bonitasoft.engine.core.category.exception.SCategoryAlreadyExistsException;
import org.bonitasoft.engine.core.category.exception.SCategoryCreationException;
import org.bonitasoft.engine.core.category.exception.SCategoryDeletionException;
import org.bonitasoft.engine.core.category.exception.SCategoryException;
import org.bonitasoft.engine.core.category.exception.SCategoryInProcessAlreadyExistsException;
import org.bonitasoft.engine.core.category.exception.SCategoryNotFoundException;
import org.bonitasoft.engine.core.category.model.SCategory;
import org.bonitasoft.engine.core.category.model.SProcessCategoryMapping;
import org.bonitasoft.engine.core.category.model.builder.SCategoryBuilder;
import org.bonitasoft.engine.core.category.model.builder.SCategoryBuilderAccessor;
import org.bonitasoft.engine.core.category.model.builder.SCategoryLogBuilder;
import org.bonitasoft.engine.core.category.model.builder.SProcessCategoryMappingBuilder;
import org.bonitasoft.engine.core.category.persistence.SelectDescriptorBuilder;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SDeleteEvent;
import org.bonitasoft.engine.events.model.SInsertEvent;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilder;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.queriablelogger.model.builder.SPersistenceLogBuilder;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;
import org.bonitasoft.engine.session.SSessionNotFoundException;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.session.model.SSession;
import org.bonitasoft.engine.sessionaccessor.ReadSessionAccessor;
import org.bonitasoft.engine.sessionaccessor.SessionIdNotSetException;

/**
 * @author Yanyan Liu
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public class CategoryServiceImpl implements CategoryService {

    private final ReadPersistenceService persistenceService;

    private final Recorder recorder;

    private final EventService eventService;

    private final SessionService sessionService;

    private final ReadSessionAccessor sessionAccessor;

    private final SCategoryBuilderAccessor categoryBuilderAccessor;

    private final QueriableLoggerService queriableLoggerService;

    public CategoryServiceImpl(final ReadPersistenceService persistenceService, final Recorder recorder, final EventService eventService,
            final SessionService sessionService, final ReadSessionAccessor sessionAccessor, final SCategoryBuilderAccessor categoryBuilderAccessor,
            final QueriableLoggerService queriableLoggerService) {
        super();
        this.persistenceService = persistenceService;
        this.recorder = recorder;
        this.eventService = eventService;
        this.sessionService = sessionService;
        this.sessionAccessor = sessionAccessor;
        this.categoryBuilderAccessor = categoryBuilderAccessor;
        this.queriableLoggerService = queriableLoggerService;
    }

    @Override
    public SCategory createCategory(final String name, final String description) throws SCategoryAlreadyExistsException, SCategoryCreationException {
        if (name == null) {
            throw new SCategoryCreationException("Category name can not be null!");
        }
        try {
            getCategoryByName(name);
            throw new SCategoryAlreadyExistsException("Category with name " + name + " already exists!");
        } catch (final SCategoryNotFoundException scnfe) {
            return addCategory(name, description);
        }
    }

    private SCategory addCategory(final String name, final String description) throws SCategoryCreationException {
        final SCategoryBuilder categoryBuilder = categoryBuilderAccessor.getCategoryBuilder();
        final SCategoryLogBuilder logBuilder = getQueriableLog(ActionType.CREATED, "Creating a new category with name " + name);
        final SEventBuilder eventBuilder = eventService.getEventBuilder();
        final long creator;
        try {
            creator = getCreator();
        } catch (final SSessionNotFoundException e) {
            throw new SCategoryCreationException(e);
        } catch (final SessionIdNotSetException e) {
            throw new SCategoryCreationException(e);
        }
        final SCategory sCategory = categoryBuilder.createNewInstance(name, creator).setDescription(description).done();
        final InsertRecord insertRecord = new InsertRecord(sCategory);
        SInsertEvent insertEvent = null;
        if (eventService.hasHandlers(CATEGORY, EventActionType.CREATED)) {
            insertEvent = (SInsertEvent) eventBuilder.createInsertEvent(CATEGORY).setObject(sCategory).done();
        }
        try {
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(insertRecord.getEntity().getId(), SQueriableLog.STATUS_OK, logBuilder, "addCategory");
            return sCategory;
        } catch (final SRecorderException e) {
            initiateLogBuilder(insertRecord.getEntity().getId(), SQueriableLog.STATUS_FAIL, logBuilder, "addCategory");
            throw new SCategoryCreationException(e);
        }
    }

    private long getCreator() throws SSessionNotFoundException, SessionIdNotSetException {
        final SSession session = sessionService.getSession(sessionAccessor.getSessionId());
        return session.getUserId();
    }

    @Override
    public SCategory getCategory(final long id) throws SCategoryNotFoundException {
        final SelectByIdDescriptor<SCategory> selectByIdDescriptor = SelectDescriptorBuilder.getCategory(id);
        try {
            final SCategory category = persistenceService.selectById(selectByIdDescriptor);
            if (category == null) {
                throw new SCategoryNotFoundException(id + " does not refer to any category");
            }
            return category;
        } catch (final SBonitaReadException bre) {
            throw new SCategoryNotFoundException(bre);
        }
    }

    @Override
    public SCategory getCategoryByName(final String name) throws SCategoryNotFoundException {
        final SelectOneDescriptor<SCategory> descriptor = SelectDescriptorBuilder.getCategory(name);
        try {
            final SCategory category = persistenceService.selectOne(descriptor);
            if (category == null) {
                throw new SCategoryNotFoundException("Category not found with name: " + name);
            }
            return category;
        } catch (final SBonitaReadException bre) {
            throw new SCategoryNotFoundException(bre);
        }
    }

    @Override
    public void updateCategory(final long categoryId, final EntityUpdateDescriptor descriptor) throws SCategoryException {
        final SCategoryLogBuilder logBuilder = getQueriableLog(ActionType.UPDATED, "Updating category");
        final SCategory persistedCategory = getCategory(categoryId);
        try {
            final UpdateRecord updateRecord = UpdateRecord.buildSetFields(persistedCategory, descriptor);
            SUpdateEvent updateEvent = null;
            if (eventService.hasHandlers(CATEGORY, EventActionType.UPDATED)) {
                updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(CATEGORY).setObject(persistedCategory).done();
                updateEvent.setOldObject(persistedCategory);
            }
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(categoryId, SQueriableLog.STATUS_OK, logBuilder, "updateCategory");
        } catch (final SRecorderException e) {
            initiateLogBuilder(categoryId, SQueriableLog.STATUS_FAIL, logBuilder, "updateCategory");
            throw new SCategoryException("Can't update category " + persistedCategory, e);
        } catch (final Exception e) {
            throw new SCategoryException("Can't update category " + persistedCategory, e);
        }
    }

    @Override
    public void deleteCategory(final long categoryId) throws SCategoryNotFoundException, SCategoryDeletionException {
        final SCategory sCategory = getCategory(categoryId);
        final DeleteRecord record = new DeleteRecord(sCategory);
        final SCategoryLogBuilder logBuilder = getQueriableLog(ActionType.DELETED, "Deleting a category");
        SDeleteEvent deleteEvent = null;
        if (eventService.hasHandlers(CATEGORY, EventActionType.DELETED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(CATEGORY).setObject(sCategory).done();
        }
        try {
            recorder.recordDelete(record, deleteEvent);
            initiateLogBuilder(categoryId, SQueriableLog.STATUS_OK, logBuilder, "deleteCategory");
        } catch (final SRecorderException e) {
            initiateLogBuilder(categoryId, SQueriableLog.STATUS_FAIL, logBuilder, "deleteCategory");
            throw new SCategoryDeletionException("Can't delete process category " + sCategory, e);
        }
    }

    @Override
    public long getNumberOfCategories() throws SCategoryException {
        try {
            return persistenceService.selectOne(SelectDescriptorBuilder.getNumberOfElement("Category", SCategory.class));
        } catch (final SBonitaReadException e) {
            throw new SCategoryException("Can't get the number of process category", e);
        }
    }

    @Override
    public List<SCategory> getCategories(final int fromIndex, final int numberOfCategories, final String field, final OrderByType order)
            throws SCategoryException {
        final SelectListDescriptor<SCategory> descriptor = SelectDescriptorBuilder.getCategories(field, order, fromIndex, numberOfCategories);
        try {
            return persistenceService.selectList(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public void addProcessDefinitionToCategory(final long categoryId, final long processDefinitionId) throws SCategoryException {
        getCategory(categoryId);
        if (isCategoryExistsInProcess(categoryId, processDefinitionId)) {
            throw new SCategoryInProcessAlreadyExistsException("The category '" + categoryId + "' is already in process '" + processDefinitionId + "'");
        }
        final SProcessCategoryMappingBuilder mappingBuilder = categoryBuilderAccessor.getSProcessCategoryMappingBuilder();
        final SProcessCategoryMapping mapping = mappingBuilder.createNewInstance(categoryId, processDefinitionId).done();
        final InsertRecord insertRecord = new InsertRecord(mapping);
        final String logMessage = "Creating a new category mapping {categoryId:" + categoryId + " --> processDefinitionId:" + processDefinitionId + "}";
        final SCategoryLogBuilder logBuilder = getQueriableLog(ActionType.CREATED, logMessage);
        SInsertEvent insertEvent = null;
        if (eventService.hasHandlers(CATEGORY, EventActionType.CREATED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            insertEvent = (SInsertEvent) eventBuilder.createInsertEvent(CATEGORY).setObject(mapping).done();
        }
        try {
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(categoryId, SQueriableLog.STATUS_OK, logBuilder, "addProcessDefinitionToCategory");
        } catch (final SRecorderException e) {
            initiateLogBuilder(categoryId, SQueriableLog.STATUS_FAIL, logBuilder, "addProcessDefinitionToCategory");
            throw new SCategoryException(e);
        }
    }

    private boolean isCategoryExistsInProcess(final long categoryId, final long processDefinitionId) throws SCategoryException {
        final SelectOneDescriptor<Long> descriptor = SelectDescriptorBuilder.isCategoryExistsInProcess(categoryId, processDefinitionId);
        try {
            return persistenceService.selectOne(descriptor) == 0 ? false : true;
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public void addProcessDefinitionsToCategory(final long categoryId, final List<Long> processDefinitionIds) throws SCategoryException {
        for (final long processDefinitionId : processDefinitionIds) {
            addProcessDefinitionToCategory(categoryId, processDefinitionId);
        }
    }

    @Override
    public long getNumberOfCategoriesOfProcess(final long processDefinitionId) throws SCategoryException {
        final SelectOneDescriptor<Long> descriptor = SelectDescriptorBuilder.getNumberOfCategoriesOfProcess(processDefinitionId);
        try {
            return persistenceService.selectOne(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public long getNumberOfCategoriesUnrelatedToProcess(final long processDefinitionId) throws SCategoryException {
        final SelectOneDescriptor<Long> descriptor = SelectDescriptorBuilder.getNumberOfCategoriesUnrelatedToProcess(processDefinitionId);
        try {
            return persistenceService.selectOne(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public List<Long> getProcessDefinitionIdsOfCategory(final long categoryId) throws SCategoryException {
        final SelectListDescriptor<Long> descriptor = SelectDescriptorBuilder.getProcessIdsOfCategory(categoryId);
        try {
            return persistenceService.selectList(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public List<SCategory> getCategoriesOfProcessDefinition(final long processDefinitionId, final int fromIndex, final int numberOfCategories,
            final OrderByType order) throws SCategoryException {
        final SelectListDescriptor<SCategory> descriptor = SelectDescriptorBuilder.getCategoriesOfProcess(processDefinitionId, fromIndex, numberOfCategories,
                order);
        try {
            return persistenceService.selectList(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public List<SCategory> getCategoriesUnrelatedToProcessDefinition(final long processDefinitionId, final int fromIndex, final int numberOfCategories,
            final OrderByType order) throws SCategoryException {
        final SelectListDescriptor<SCategory> descriptor = SelectDescriptorBuilder.getCategoriesUnrelatedToProcess(processDefinitionId, fromIndex,
                numberOfCategories, order);
        try {
            return persistenceService.selectList(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public void removeCategoriesFromProcessDefinition(final long processId, final List<Long> categoryIds) throws SCategoryException {
        final SelectListDescriptor<SProcessCategoryMapping> descriptor = SelectDescriptorBuilder.getProcessCategoryMappingsOfProcess(processId);
        try {
            final List<SProcessCategoryMapping> mappings = persistenceService.selectList(descriptor);
            for (final SProcessCategoryMapping mapping : mappings) {
                // Is the current category mapping in the list of the ones we want to remove? :
                if (categoryIds.contains(mapping.getCategoryId())) {
                    deleteProcessCategoryMapping(mapping);
                }
            }
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public void removeProcessDefinitionsOfCategory(final long categoryId) throws SCategoryException {
        final SelectListDescriptor<SProcessCategoryMapping> descriptor = SelectDescriptorBuilder.getProcessCategoryMappingsOfCategory(categoryId);
        try {
            final List<SProcessCategoryMapping> mappings = persistenceService.selectList(descriptor);
            for (final SProcessCategoryMapping mapping : mappings) {
                deleteProcessCategoryMapping(mapping);
            }
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    private void deleteProcessCategoryMapping(final SProcessCategoryMapping mapping) throws SCategoryException {
        final DeleteRecord deleteRecord = new DeleteRecord(mapping);
        final SEventBuilder eventBuilder = eventService.getEventBuilder();
        final String logMessage = "Deleting a category mapping {processDefinitionId:" + mapping.getProcessId() + " --> categoryId" + mapping.getCategoryId()
                + "}";
        final SCategoryLogBuilder logBuilder = getQueriableLog(ActionType.DELETED, logMessage);
        SDeleteEvent deleteEvent = null;
        if (eventService.hasHandlers(CATEGORY, EventActionType.DELETED)) {
            deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(CATEGORY).setObject(mapping).done();
        }
        try {
            // /FIXME change log
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(mapping.getId(), SQueriableLog.STATUS_OK, logBuilder, "deleteProcessCategoryMapping");
        } catch (final SRecorderException e) {
            initiateLogBuilder(mapping.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "deleteProcessCategoryMapping");
            throw new SCategoryException(e);
        }
    }

    @Override
    public void removeProcessIdOfCategories(final long processId) throws SCategoryException {
        final SelectListDescriptor<SProcessCategoryMapping> descriptor = SelectDescriptorBuilder.getProcessCategoryMappingsOfProcess(processId);
        try {
            final List<SProcessCategoryMapping> mappings = persistenceService.selectList(descriptor);
            for (final SProcessCategoryMapping mapping : mappings) {
                deleteProcessCategoryMapping(mapping);
            }
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    private SCategoryLogBuilder getQueriableLog(final ActionType actionType, final String message) {
        final SCategoryLogBuilder logBuilder = categoryBuilderAccessor.getSCategoryLogBuilder();
        this.initializeLogBuilder(logBuilder, message);
        this.updateLog(actionType, logBuilder);
        return logBuilder;
    }

    private <T extends SLogBuilder> void initializeLogBuilder(final T logBuilder, final String message) {
        logBuilder.createNewInstance().actionStatus(SQueriableLog.STATUS_FAIL).severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    private <T extends HasCRUDEAction> void updateLog(final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    @Override
    public long getNumberOfCategorizedProcessIds(final List<Long> processIds) throws SCategoryException {
        if (processIds == null || processIds.size() <= 0) {
            return 0; // Should return 0 or throw Exception?
        }
        final SelectOneDescriptor<Long> descriptor = SelectDescriptorBuilder.getNumberOfCategorizedProcessIds(processIds);
        try {
            return persistenceService.selectOne(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    @Override
    public List<Long> getCategorizedProcessIds(final List<Long> processIds) throws SCategoryException {
        if (processIds == null || processIds.size() <= 0) {
            return Collections.emptyList();
        }
        final SelectListDescriptor<Long> descriptor = SelectDescriptorBuilder.getCategorizedProcessIds(processIds);
        try {
            return persistenceService.selectList(descriptor);
        } catch (final SBonitaReadException e) {
            throw new SCategoryException(e);
        }
    }

    private void initiateLogBuilder(final long objectId, final int sQueriableLogStatus, final SPersistenceLogBuilder logBuilder, final String callerMethodName) {
        logBuilder.actionScope(String.valueOf(objectId));
        logBuilder.actionStatus(sQueriableLogStatus);
        logBuilder.objectId(objectId);
        final SQueriableLog log = logBuilder.done();
        if (queriableLoggerService.isLoggable(log.getActionType(), log.getSeverity())) {
            queriableLoggerService.log(this.getClass().getName(), callerMethodName, log);
        }
    }

}
