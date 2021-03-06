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
package org.bonitasoft.engine.core.process.instance.model.builder.impl;

import org.bonitasoft.engine.core.process.instance.model.SPendingActivityMapping;
import org.bonitasoft.engine.core.process.instance.model.builder.SPendingActivityMappingBuilder;
import org.bonitasoft.engine.core.process.instance.model.impl.SPendingActivityMappingImpl;

/**
 * @author Baptiste Mesta
 */
public class SPendingActivityMappingBuilderImpl implements SPendingActivityMappingBuilder {

    private SPendingActivityMappingImpl entity;

    @Override
    public SPendingActivityMappingBuilder createNewInstanceForUser(final long activityId, final long userId) {
        entity = new SPendingActivityMappingImpl(activityId);
        entity.setUserId(userId);
        return this;
    }

    @Override
    public SPendingActivityMappingBuilder createNewInstanceForActor(final long activityId, final long actorId) {
        entity = new SPendingActivityMappingImpl(activityId);
        entity.setActorId(actorId);
        return this;
    }

    @Override
    public SPendingActivityMapping done() {
        return entity;
    }

}
