/**
 * Copyright (C) 2011-2012 BonitaSoft S.A.
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
package org.bonitasoft.engine.api.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bonitasoft.engine.api.internal.ServerAPI;
import org.bonitasoft.engine.api.internal.ServerWrappedException;

/**
 * @author Matthieu Chaffotte
 */
public class ClientInterceptor implements InvocationHandler, Serializable {

    /**
     * This interceptor is used only to access the PlatformLoginAPI or the Tenant Login API
     * It is used to (and only to) convert the call into a "serverAPI" call
     * Server API has only one operation
     * This interceptor is used to login as we do not transmit any session to server side.
     * 
     * For other operations, a child of this class is used: ClientSessionInterceptor
     */
    private static final long serialVersionUID = -6284726148297940515L;

    private final ServerAPI api;

    private final String interfaceName;

    private static final Logger LOGGER = Logger.getLogger(ClientInterceptor.class.getName());

    public ClientInterceptor(final String interfaceName, final ServerAPI api) {
        this.api = api;
        this.interfaceName = interfaceName;
    }

    protected Map<String, Serializable> getOptions() {
        return new TreeMap<String, Serializable>();
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        try {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final List<String> classNameParameters = new ArrayList<String>();
            for (final Class<?> parameterType : parameterTypes) {
                classNameParameters.add(parameterType.getName());
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Calling method " + method.getName() + " on API" + api.getClass().getName());
            }
            // invoke ServerAPI unique method
            final Object object = api.invokeMethod(getOptions(), this.interfaceName, method.getName(), classNameParameters, args);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Quitting method " + method.getName() + " on API" + api.getClass().getName());
            }
            return object;
        } catch (final ServerWrappedException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Quitting method " + method.getName() + " on API " + api.getClass().getName() + " with exception " + e.getMessage());
            }
            final Throwable cause = e.getCause();
            throw cause;
        } catch (final RemoteException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Quitting method " + method.getName() + " on API " + api.getClass().getName() + " with exception " + e.getMessage());
            }
            final Throwable cause = e.getCause();
            throw cause;
        }
    }
}
