/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.engine.core.process.definition.model.bindings;

import java.util.Map;

import org.bonitasoft.engine.core.process.definition.model.event.trigger.impl.STerminateEventTriggerDefinitionImpl;
import org.bonitasoft.engine.xml.ElementBinding;
import org.bonitasoft.engine.xml.SXMLParseException;

/**
 * @author Matthieu Chaffotte
 */
public class STerminateEventTriggerDefinitionBinding extends ElementBinding {

    @Override
    public void setAttributes(final Map<String, String> attributes) throws SXMLParseException {
    }

    @Override
    public void setChildElement(final String name, final String value, final Map<String, String> attributes) throws SXMLParseException {
    }

    @Override
    public void setChildObject(final String name, final Object value) throws SXMLParseException {
    }

    @Override
    public Object getObject() {
        return new STerminateEventTriggerDefinitionImpl();
    }

    @Override
    public String getElementTag() {
        return XMLSProcessDefinition.TERMINATE_EVENT_TRIGGER_NODE;
    }

}
