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
package org.bonitasoft.engine.bpm.bar.xml;

import java.util.Map;

import org.bonitasoft.engine.bpm.actor.ActorDefinition;
import org.bonitasoft.engine.bpm.actor.impl.ActorDefinitionImpl;
import org.bonitasoft.engine.io.xml.XMLParseException;

/**
 * @author Baptiste Mesta
 * @author Matthieu Chaffotte
 */
public class ActorDefinitionBinding extends NamedElementBinding {

    private String description;

    @Override
    public void setChildElement(final String name, final String value, final Map<String, String> attributes) throws XMLParseException {
        if (XMLProcessDefinition.DESCRIPTION.equals(name)) {
            description = value;
        }
    }

    @Override
    public void setChildObject(final String name, final Object value) throws XMLParseException {
    }

    @Override
    public ActorDefinition getObject() {
        final ActorDefinitionImpl actorDefintionImpl = new ActorDefinitionImpl(name);
        actorDefintionImpl.setDescription(description);
        actorDefintionImpl.setInitiator(false);
        return actorDefintionImpl;
    }

    @Override
    public String getElementTag() {
        return XMLProcessDefinition.ACTOR_NODE;
    }

}
