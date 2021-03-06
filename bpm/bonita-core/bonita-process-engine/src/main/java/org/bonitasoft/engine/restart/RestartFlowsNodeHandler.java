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
package org.bonitasoft.engine.restart;

import java.util.List;

import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeReadException;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.execution.ProcessExecutor;
import org.bonitasoft.engine.execution.work.ExecuteFlowNodeWork;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.service.PlatformServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.transaction.STransactionException;
import org.bonitasoft.engine.work.WorkRegisterException;
import org.bonitasoft.engine.work.WorkService;

/**
 * @author Baptiste Mesta
 */
public class RestartFlowsNodeHandler implements TenantRestartHandler {

    @Override
    public void handleRestart(final PlatformServiceAccessor platformServiceAccessor, final TenantServiceAccessor tenantServiceAccessor) throws RestartException {
        final TransactionExecutor transactionExecutor = tenantServiceAccessor.getTransactionExecutor();
        final ActivityInstanceService activityInstanceService = tenantServiceAccessor.getActivityInstanceService();
        QueryOptions queryOptions = QueryOptions.defaultQueryOptions();
        List<SFlowNodeInstance> flowNodes;
        final WorkService workService = platformServiceAccessor.getWorkService();
        final ProcessExecutor processExecutor = tenantServiceAccessor.getProcessExecutor();
        try {
            boolean txOpened = transactionExecutor.openTransaction();
            try {
                final BPMInstanceBuilders bpmInstanceBuilders = tenantServiceAccessor.getBPMInstanceBuilders();
                final int processInstanceIndex = bpmInstanceBuilders.getSUserTaskInstanceBuilder().getParentProcessInstanceIndex();
                do {
                    flowNodes = activityInstanceService.getFlowNodeInstancesToRestart(queryOptions);
                    queryOptions = QueryOptions.getNextPage(queryOptions);
                    for (final SFlowNodeInstance flowNodeInstance : flowNodes) {
                        workService.registerWork(new ExecuteFlowNodeWork(processExecutor, flowNodeInstance.getId(), null, null, flowNodeInstance
                                .getLogicalGroup(processInstanceIndex)));
                    }
                } while (flowNodes.size() == queryOptions.getNumberOfResults());
            } catch (final WorkRegisterException e) {
                handleException(transactionExecutor, e, "Unable to restart flowNodes: can't register work");
            } catch (final SFlowNodeReadException e) {
                handleException(transactionExecutor, e, "Unable to restart flowNodes: can't read flow nodes");
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new RestartException("Unable to restart transitions: issue with transaction", e);
        }

    }

    private void handleException(final TransactionExecutor transactionExecutor, final Exception e, final String message) throws STransactionException,
            RestartException {
        transactionExecutor.setTransactionRollback();
        throw new RestartException(message, e);
    }
}
