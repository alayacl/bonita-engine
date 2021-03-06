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
package org.bonitasoft.engine.api.impl.transaction.platform;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContent;
import org.bonitasoft.engine.platform.session.PlatformSessionService;
import org.bonitasoft.engine.session.SessionService;

/**
 * @author Matthieu Chaffotte
 */
public class RenewSSession implements TransactionContent {

    private final SessionService sessionService;

    private final PlatformSessionService platformSessionService;

    private final long sessionId;

    public RenewSSession(final SessionService sessionService, final long sessionId) {
        super();
        this.sessionService = sessionService;
        this.sessionId = sessionId;
        platformSessionService = null;
    }

    public RenewSSession(final PlatformSessionService platformSessionService, final long sessionId) {
        super();
        this.platformSessionService = platformSessionService;
        this.sessionId = sessionId;
        sessionService = null;
    }

    @Override
    public void execute() throws SBonitaException {
        if (sessionService != null) {
            sessionService.renewSession(sessionId);
        } else {
            platformSessionService.renewSession(sessionId);
        }
    }

}
