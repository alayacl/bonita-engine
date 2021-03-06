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
package org.bonitasoft.engine.data.instance.model.archive.impl;

import java.io.Serializable;

import org.bonitasoft.engine.data.instance.model.SDataInstance;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * @author Matthieu Chaffotte
 */
public final class SAXMLObjectDataInstanceImpl extends SADataInstanceImpl {

    private static final long serialVersionUID = -6828749783941071011L;

    private String value;

    public SAXMLObjectDataInstanceImpl() {
        super();
    }

    public SAXMLObjectDataInstanceImpl(final SDataInstance sDataInstance) {
        super(sDataInstance);
    }

    @Override
    public Serializable getValue() {
        final XStream xstream = new XStream(new StaxDriver());
        return (Serializable) xstream.fromXML(value);
    }

    @Override
    public void setValue(final Serializable value) {
        final XStream xStream = new XStream(new StaxDriver());
        this.value = xStream.toXML(value);
    }

    @Override
    public String getDiscriminator() {
        return SAXMLObjectDataInstanceImpl.class.getSimpleName();
    }

}
