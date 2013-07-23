/**
 * Copyright (C) 2011 BonitaSoft S.A.
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
package org.bonitasoft.engine.api.impl.transaction.flownode;

import java.util.List;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;

/**
 * @author Baptiste Mesta
 */
public final class GetExecutableElements implements TransactionContentWithResult<List<SFlowNodeInstance>> {

    private final long processInstanceId;

    private final ActivityInstanceService activityInstanceService;

    private List<SFlowNodeInstance> executableFlowNode;

    public GetExecutableElements(final long processInstanceId, final ActivityInstanceService activityInstanceService,
            final FlowNodeStateManager flowNodeStateManager) {
        this.processInstanceId = processInstanceId;
        this.activityInstanceService = activityInstanceService;
    }

    @Override
    public void execute() throws SBonitaException {
        executableFlowNode = activityInstanceService.getActiveFlowNodes(processInstanceId);// TODO naming
    }

    @Override
    public List<SFlowNodeInstance> getResult() {
        return executableFlowNode;
    }

}