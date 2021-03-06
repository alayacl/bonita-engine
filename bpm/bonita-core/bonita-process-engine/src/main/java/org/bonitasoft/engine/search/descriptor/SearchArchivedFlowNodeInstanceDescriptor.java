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
package org.bonitasoft.engine.search.descriptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.engine.bpm.flownode.ArchivedFlowNodeInstanceSearchDescriptor;
import org.bonitasoft.engine.core.process.instance.model.archive.SAFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.archive.builder.SAFlowNodeInstanceBuilder;
import org.bonitasoft.engine.core.process.instance.model.builder.BPMInstanceBuilders;
import org.bonitasoft.engine.execution.state.FlowNodeStateManager;
import org.bonitasoft.engine.persistence.PersistentObject;

/**
 * @author Emmanuel Duchastenier
 */
public class SearchArchivedFlowNodeInstanceDescriptor extends SearchEntityDescriptor {

    private final Map<String, FieldDescriptor> archFlowNodeDescriptorKeys;

    private final Map<Class<? extends PersistentObject>, Set<String>> flowNodeInstanceDescriptorAllFields;

    public SearchArchivedFlowNodeInstanceDescriptor(final BPMInstanceBuilders bpmInstanceBuilders, final FlowNodeStateManager flowNodeStateManager) {
        final SAFlowNodeInstanceBuilder flowNodeKeyProvider = bpmInstanceBuilders.getSAUserTaskInstanceBuilder();
        archFlowNodeDescriptorKeys = new HashMap<String, FieldDescriptor>(9);
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.NAME,
                new FieldDescriptor(SAFlowNodeInstance.class, flowNodeKeyProvider.getNameKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.STATE_NAME,
                new FieldDescriptor(SAFlowNodeInstance.class, flowNodeKeyProvider.getStateNameKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.PROCESS_DEFINITION_ID, new FieldDescriptor(SAFlowNodeInstance.class,
                flowNodeKeyProvider.getProcessDefinitionKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, new FieldDescriptor(SAFlowNodeInstance.class,
                flowNodeKeyProvider.getParentProcessInstanceKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.PARENT_ACTIVITY_INSTANCE_ID, new FieldDescriptor(SAFlowNodeInstance.class,
                flowNodeKeyProvider.getParentActivityInstanceKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.ROOT_PROCESS_INSTANCE_ID, new FieldDescriptor(SAFlowNodeInstance.class,
                flowNodeKeyProvider.getRootProcessInstanceKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.DISPLAY_NAME,
                new FieldDescriptor(SAFlowNodeInstance.class, flowNodeKeyProvider.getDisplayNameKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.FLOW_NODE_TYPE, new FieldDescriptor(SAFlowNodeInstance.class,
                flowNodeKeyProvider.getKindKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.ORIGINAL_FLOW_NODE_ID, new FieldDescriptor(SAFlowNodeInstance.class,
                flowNodeKeyProvider.getSourceObjectIdKey()));
        archFlowNodeDescriptorKeys.put(ArchivedFlowNodeInstanceSearchDescriptor.TERMINAL,
                new FieldDescriptor(SAFlowNodeInstance.class, flowNodeKeyProvider.getTerminalKey()));

        final Set<String> tasksInstanceFields = new HashSet<String>(2);
        tasksInstanceFields.add(flowNodeKeyProvider.getNameKey());
        tasksInstanceFields.add(flowNodeKeyProvider.getDisplayNameKey());
        flowNodeInstanceDescriptorAllFields = new HashMap<Class<? extends PersistentObject>, Set<String>>(1);
        flowNodeInstanceDescriptorAllFields.put(SAFlowNodeInstance.class, tasksInstanceFields);
    }

    @Override
    protected Map<String, FieldDescriptor> getEntityKeys() {
        return archFlowNodeDescriptorKeys;
    }

    @Override
    protected Map<Class<? extends PersistentObject>, Set<String>> getAllFields() {
        return flowNodeInstanceDescriptorAllFields;
    }

}
