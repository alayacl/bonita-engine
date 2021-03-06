/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.process.definition.model.impl;

import java.util.Map;

import org.bonitasoft.engine.bpm.flownode.impl.SendTaskDefinitionImpl;
import org.bonitasoft.engine.core.operation.model.builder.SOperationBuilders;
import org.bonitasoft.engine.core.process.definition.model.SFlowNodeType;
import org.bonitasoft.engine.core.process.definition.model.SSendTaskDefinition;
import org.bonitasoft.engine.core.process.definition.model.STransitionDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.trigger.SThrowMessageEventTriggerDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.trigger.impl.SThrowMessageEventTriggerDefinitionImpl;
import org.bonitasoft.engine.data.definition.model.builder.SDataDefinitionBuilders;
import org.bonitasoft.engine.expression.model.builder.SExpressionBuilders;

/**
 * @author Baptiste Mesta
 */
public class SSendTaskDefinitionImpl extends SActivityDefinitionImpl implements SSendTaskDefinition {

    private static final long serialVersionUID = 8112705930442175231L;

    private final SThrowMessageEventTriggerDefinition trigger;

    public SSendTaskDefinitionImpl(final SFlowElementContainerDefinitionImpl parentContainer, final SendTaskDefinitionImpl activityDefinition,
            final SExpressionBuilders sExpressionBuilders, final Map<String, STransitionDefinition> transitionsMap,
            final SDataDefinitionBuilders sDataDefinitionBuilders, final SOperationBuilders sOperationBuilders) {
        super(parentContainer, activityDefinition, sExpressionBuilders, transitionsMap, sDataDefinitionBuilders, sOperationBuilders);
        trigger = new SThrowMessageEventTriggerDefinitionImpl(activityDefinition.getMessageTrigger(), sDataDefinitionBuilders, sExpressionBuilders);
    }

    public SSendTaskDefinitionImpl(final long id, final String name, final SThrowMessageEventTriggerDefinition throwMessageEventTriggerDefinition) {
        super(id, name);
        trigger = throwMessageEventTriggerDefinition;
    }

    @Override
    public SFlowNodeType getType() {
        return SFlowNodeType.SEND_TASK;
    }

    @Override
    public SThrowMessageEventTriggerDefinition getMessageTrigger() {
        return trigger;
    }

}
