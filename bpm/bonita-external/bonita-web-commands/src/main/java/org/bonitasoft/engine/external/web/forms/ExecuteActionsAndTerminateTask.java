/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.classloader.ClassLoaderService;
import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.core.expression.control.model.SExpressionContext;
import org.bonitasoft.engine.core.operation.OperationService;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.data.instance.api.DataInstanceContainer;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.execution.ProcessExecutor;
import org.bonitasoft.engine.operation.Operation;
import org.bonitasoft.engine.operation.OperationExecutionException;
import org.bonitasoft.engine.service.ModelConvertor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;

/**
 * @author Ruiheng Fan
 * @author Baptiste Mesta
 * @author Elias Ricken de Medeiros
 * @author Matthieu Chaffotte
 */
public class ExecuteActionsAndTerminateTask extends ExecuteActionsBaseEntry {

    public static final String ACTIVITY_INSTANCE_ID_KEY = "ACTIVITY_INSTANCE_ID_KEY";

    @Override
    public Serializable execute(final Map<String, Serializable> parameters, final TenantServiceAccessor serviceAccessor)
            throws SCommandParameterizationException, SCommandExecutionException {
        final List<Operation> operations = getOperations(parameters);
        final Map<String, Serializable> operationsContext = getOperationsContext(parameters);
        final long sActivityInstanceID = getActivityInstanceId(parameters);

        try {
            final ClassLoaderService classLoaderService = serviceAccessor.getClassLoaderService();
            final ActivityInstanceService activityInstanceService = serviceAccessor.getActivityInstanceService();
            final TransactionExecutor transactionExecutor = serviceAccessor.getTransactionExecutor();
            final boolean txOpened = transactionExecutor.openTransaction();
            final ClassLoader processClassloader;
            final long processDefinitionID;
            try {
                final SFlowNodeInstance flowNodeInstance = activityInstanceService.getFlowNodeInstance(sActivityInstanceID);
                processDefinitionID = flowNodeInstance.getLogicalGroup(0);
                processClassloader = classLoaderService.getLocalClassLoader("process", processDefinitionID);
                final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(processClassloader);
                    updateActivityInstanceVariables(operations, operationsContext, sActivityInstanceID, processDefinitionID);
                } finally {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                }
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
            executeActivity(sActivityInstanceID);
        } catch (final BonitaException e) {
            throw new SCommandExecutionException(
                    "Error executing command 'Map<String, Serializable> ExecuteActionsAndTerminateTask(List<Operation>, Map<String, Serializable>, long activityInstanceId)'",
                    e);
        } catch (final SBonitaException e) {
            throw new SCommandExecutionException(
                    "Error executing command 'Map<String, Serializable> ExecuteActionsAndTerminateTask(List<Operation>, Map<String, Serializable>, long activityInstanceId)'",
                    e);
        }
        return null;
    }

    protected long getActivityInstanceId(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        final String message = "Mandatory parameter " + ACTIVITY_INSTANCE_ID_KEY + " is missing or not convertible to long.";
        final Long instanceId = getMandatoryParameter(parameters, ACTIVITY_INSTANCE_ID_KEY, message);
        return instanceId;
    }

    protected List<Operation> getOperations(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        final String message = "Mandatory parameter " + OPERATIONS_LIST_KEY + " is missing or not convertible to List.";
        return getParameter(parameters, OPERATIONS_LIST_KEY, message);
    }

    protected Map<String, Serializable> getOperationsContext(final Map<String, Serializable> parameters) throws SCommandParameterizationException {
        final String message = "Mandatory parameter " + OPERATIONS_INPUT_KEY + " is missing or not convertible to Map.";
        return getParameter(parameters, OPERATIONS_INPUT_KEY, message);
    }

    protected void updateActivityInstanceVariables(final List<Operation> operations, final Map<String, Serializable> operationsContext,
            final long activityInstanceId, final Long processDefinitionID) throws BonitaException {
        final TenantServiceAccessor tenantAccessor = TenantServiceSingleton.getInstance(getTenantId());
        final OperationService operationService = tenantAccessor.getOperationService();
        final Iterator<Operation> iterator = operations.iterator();
        final SExpressionContext sExpressionContext = new SExpressionContext();
        sExpressionContext.setSerializableInputValues(operationsContext);
        sExpressionContext.setContainerId(activityInstanceId);
        sExpressionContext.setContainerType(DataInstanceContainer.ACTIVITY_INSTANCE.name());
        sExpressionContext.setProcessDefinitionId(processDefinitionID);
        try {
            while (iterator.hasNext()) {
                final Operation entry = iterator.next();
                operationService.execute(ModelConvertor.constructSOperation(entry, tenantAccessor), activityInstanceId,
                        DataInstanceContainer.ACTIVITY_INSTANCE.name(), sExpressionContext);
            }
        } catch (final SBonitaException e) {
            throw new OperationExecutionException(e);
        }
    }

    protected void executeActivity(final long activityInstanceId) throws BonitaException {
        final TenantServiceAccessor tenantAccessor = TenantServiceSingleton.getInstance(getTenantId());
        final ProcessExecutor processExecutor = tenantAccessor.getProcessExecutor();
        try {
            processExecutor.executeActivity(activityInstanceId, getUserIdFromSession(), getUserIdFromSession());
        } catch (final SBonitaException e) {
            log(tenantAccessor, e);
            throw new BonitaException(e.getMessage());
        }
    }

}
