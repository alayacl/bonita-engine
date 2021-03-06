/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.core.login;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.bonitasoft.engine.authentication.AuthenticationException;
import org.bonitasoft.engine.authentication.AuthenticationService;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.home.BonitaHomeServer;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.io.PropertiesManager;
import org.bonitasoft.engine.session.SSessionException;
import org.bonitasoft.engine.session.SSessionNotFoundException;
import org.bonitasoft.engine.session.SessionService;
import org.bonitasoft.engine.session.model.SSession;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

/**
 * @author Matthieu Chaffotte
 */
public class SecuredLoginServiceImpl implements LoginService {

    private final AuthenticationService authenticationService;

    private final SessionService sessionService;

    private final SessionAccessor sessionAccessor;

    public SecuredLoginServiceImpl(final AuthenticationService authenticationService, final SessionService sessionService, final SessionAccessor sessionAccessor) {
        this.authenticationService = authenticationService;
        this.sessionService = sessionService;
        this.sessionAccessor = sessionAccessor;
    }

    @Override
    public SSession login(final long tenantId, final String userName, final String password) throws SLoginException {
        sessionAccessor.setSessionInfo(-1, tenantId); // necessary to check user credentials
        long userId;
        boolean technicalUser = false;
        try {
            final SUser user = authenticationService.checkUserCredentials(userName, password);
            userId = user.getId();
        } catch (final AuthenticationException ae) {
            // Authentication fails it can be due to the technical user
            final TechnicalUser technicalUser2 = getTechnicalUser(tenantId);
            if (technicalUser2.getUserName().equals(userName) && technicalUser2.getPassword().equals(password)) {
                technicalUser = true;
                userId = -1;
            } else {
                try {
                    Thread.sleep(3000);
                } catch (final InterruptedException e) {
                    throw new SLoginException("User name or password is not valid!");
                }
                throw new SLoginException("User name or password is not valid!");
            }
        } finally {
            // clean session accessor
            sessionAccessor.deleteSessionId();
        }
        try {
            return sessionService.createSession(tenantId, userId, userName, technicalUser);
        } catch (final SSessionException e) {
            throw new SLoginException(e);
        }
    }

    @Override
    public void logout(final long sessionId) throws SLoginException, SSessionNotFoundException {
        sessionService.deleteSession(sessionId);
    }

    @Override
    public boolean isValid(final long sessionId) {
        try {
            return sessionService.isValid(sessionId);
        } catch (final SSessionNotFoundException e) {
            return false;
        }
    }

    private TechnicalUser getTechnicalUser(final long tenantId) throws SLoginException {
        try {
            final String technicalUserPropertiesPath = BonitaHomeServer.getInstance().getTenantConfFolder(tenantId) + File.separator
                    + "bonita-server.properties";
            final Properties properties = PropertiesManager.getProperties(new File(technicalUserPropertiesPath));
            final String userName = (String) properties.get("userName");
            final String password = (String) properties.get("userPassword");
            return new TechnicalUser(userName, password);
        } catch (final BonitaHomeNotSetException e) {
            throw new SLoginException(e);
        } catch (final IOException e) {
            throw new SLoginException(e);
        }
    }

}
