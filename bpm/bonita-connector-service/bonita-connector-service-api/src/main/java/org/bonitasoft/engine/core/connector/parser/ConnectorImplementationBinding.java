/**
 * Copyright (C) 2011-2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.engine.core.connector.parser;

import java.util.Map;

import org.bonitasoft.engine.xml.ElementBinding;
import org.bonitasoft.engine.xml.SXMLParseException;

/**
 * @author Yanyan Liu
 */
public class ConnectorImplementationBinding extends ElementBinding {

    private String implementationId;

    private String implementationVersion;

    private String definitionId;

    private String definitionVersion;

    private String implementationClassname;

    private JarDependencies jarDependencies;

    @Override
    public void setAttributes(final Map<String, String> attributes) throws SXMLParseException {
    }

    @Override
    public void setChildElement(final String name, final String value, final Map<String, String> attributes) throws SXMLParseException {
        if (XMLDescriptor.IMPLEMENTATION_ID.equals(name)) {
            implementationId = value;
        }
        if (XMLDescriptor.IMPLEMENTATION_VERSION.equals(name)) {
            implementationVersion = value;
        }
        if (XMLDescriptor.DEFINITION_ID.equals(name)) {
            definitionId = value;
        }
        if (XMLDescriptor.DEFINITION_VERSION.equals(name)) {
            definitionVersion = value;
        }
        if (XMLDescriptor.IMPLEMENTATION_CLASSNAME.equals(name)) {
            implementationClassname = value;
        }

    }

    @Override
    public void setChildObject(final String name, final Object value) throws SXMLParseException {
        if (XMLDescriptor.JAR_DEPENDENCIES.equals(name)) {
            jarDependencies = (JarDependencies) value;
        }
    }

    @Override
    public Object getObject() {
        return new SConnectorImplementationDescriptor(implementationClassname, implementationId, implementationVersion, definitionId, definitionVersion,
                jarDependencies);
    }

    @Override
    public String getElementTag() {
        return XMLDescriptor.CONNECTOR_IMPLEMENTATION;// "connectorImplementation";
    }

}
