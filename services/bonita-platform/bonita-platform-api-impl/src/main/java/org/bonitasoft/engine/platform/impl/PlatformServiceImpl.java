/**
 * Copyright (C) 2011 BonitaSoft S.A.
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
package org.bonitasoft.engine.platform.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.commons.CollectionUtil;
import org.bonitasoft.engine.commons.LogUtil;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.platform.PlatformService;
import org.bonitasoft.engine.platform.SDeletingActivatedTenantException;
import org.bonitasoft.engine.platform.SPlatformAlreadyExistException;
import org.bonitasoft.engine.platform.SPlatformCreationException;
import org.bonitasoft.engine.platform.SPlatformDeletionException;
import org.bonitasoft.engine.platform.SPlatformNotFoundException;
import org.bonitasoft.engine.platform.SPlatformUpdateException;
import org.bonitasoft.engine.platform.STenantActivationException;
import org.bonitasoft.engine.platform.STenantAlreadyExistException;
import org.bonitasoft.engine.platform.STenantCreationException;
import org.bonitasoft.engine.platform.STenantDeactivationException;
import org.bonitasoft.engine.platform.STenantDeletionException;
import org.bonitasoft.engine.platform.STenantException;
import org.bonitasoft.engine.platform.STenantNotFoundException;
import org.bonitasoft.engine.platform.STenantUpdateException;
import org.bonitasoft.engine.platform.model.SPlatform;
import org.bonitasoft.engine.platform.model.STenant;
import org.bonitasoft.engine.platform.model.builder.STenantBuilder;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.services.PersistenceService;
import org.bonitasoft.engine.services.SPersistenceException;
import org.bonitasoft.engine.services.TenantPersistenceService;
import org.bonitasoft.engine.services.UpdateDescriptor;

/**
 * @author Charles Souillard
 * @author Celine Souchet
 */
public class PlatformServiceImpl implements PlatformService {

    private final PersistenceService platformPersistenceService;

    private final List<TenantPersistenceService> tenantPersistenceServices;

    private final STenantBuilder tenantBuilder;

    private final TechnicalLoggerService logger;

    private final boolean trace;

    private final boolean error;

    public PlatformServiceImpl(final PersistenceService platformPersistenceService, final List<TenantPersistenceService> tenantPersistenceServices,
            final STenantBuilder tenantBuilder, final TechnicalLoggerService logger) {
        this.platformPersistenceService = platformPersistenceService;
        this.tenantPersistenceServices = tenantPersistenceServices;
        this.tenantBuilder = tenantBuilder;
        this.logger = logger;
        trace = logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE);
        error = logger.isLoggable(this.getClass(), TechnicalLogSeverity.ERROR);
    }

    @Override
    public void createPlatformTables() throws SPlatformCreationException, SPlatformAlreadyExistException {
        // create platform tables
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "createPlatformTables"));
        }
        try {
            platformPersistenceService.createStructure();
            platformPersistenceService.postCreateStructure();
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "createPlatformTables"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createPlatformTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformCreationException("Unable to create platform tables: " + e.getMessage(), e);
        } catch (final IOException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createPlatformTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformCreationException("Unable to create platform tables: " + e.getMessage(), e);
        }
    }

    @Override
    public void createPlatform(final SPlatform platform) throws SPlatformCreationException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "createPlatform"));
        }
        // if platform doesn't exist, insert it
        try {
            platformPersistenceService.insert(platform);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "createPlatform"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createPlatform", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformCreationException("Unable to insert the platform row: " + e.getMessage(), e);
        }
    }

    @Override
    public long createTenant(final STenant tenant) throws STenantCreationException, STenantAlreadyExistException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "createTenant"));
        }
        // check if the tenant already exists. If yes, throws
        // TenantAlreadyExistException
        STenant existingTenant = null;
        try {
            existingTenant = getTenantByName(tenant.getName());
        } catch (final STenantNotFoundException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createTenant", e));
            }
            // OK
        }
        if (existingTenant != null) {
            throw new STenantAlreadyExistException("Unable to create the tenant " + tenant.getName() + " : it already exists.");
        }

        // create the tenant
        try {
            platformPersistenceService.insert(tenant);
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createTenant", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantCreationException("Unable to insert the tenant row: " + e.getMessage(), e);
        }

        // initialize tenant
        try {
            final Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("tenantid", Long.toString(tenant.getId()));
            for (final PersistenceService tenantPersistenceService : tenantPersistenceServices) {
                tenantPersistenceService.initializeStructure(replacements);
            }
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "createTenant"));
            }
            return tenant.getId();
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createTenant", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantCreationException("Unable to create tenant tables: " + e.getMessage(), e);
        } catch (final IOException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createTenant", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantCreationException("Unable to create tenant tables: " + e.getMessage(), e);
        }
    }

    @Override
    public void createTenantTables() throws STenantCreationException {
        // this is the first tenant, we should create tenant tables
        try {
            for (final PersistenceService tenantPersistenceService : tenantPersistenceServices) {
                tenantPersistenceService.createStructure();
                tenantPersistenceService.postCreateStructure();
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createTenantTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantCreationException("Unable to create tenant tables: " + e.getMessage(), e);
        } catch (final IOException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createTenantTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantCreationException("Unable to create tenant tables: " + e.getMessage(), e);
        }
    }

    @Override
    public void initializePlatformStructure() throws SPlatformCreationException {
        try {
            platformPersistenceService.initializeStructure();
        } catch (final SPersistenceException e) {
            throw new SPlatformCreationException("unable to initialize platform structure", e);
        } catch (final IOException e) {
            throw new SPlatformCreationException("unable to initialize platform structure", e);
        }
    }

    @Override
    public void deletePlatform() throws SPlatformDeletionException, SPlatformNotFoundException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deletePlatform"));
        }
        final SPlatform platform = getPlatform();
        List<STenant> existingTenants;
        try {
            existingTenants = getTenants(QueryOptions.defaultQueryOptions());
        } catch (final STenantException e) {
            throw new SPlatformDeletionException(e);
        }

        if (existingTenants.size() > 0) {
            throw new SPlatformDeletionException("Some tenants still are in the system. Can not delete platform");
        }

        try {
            platformPersistenceService.delete(platform);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deletePlatform"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deletePlatform", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformDeletionException("Unable to delete the platform row: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePlatformTables() throws SPlatformDeletionException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deletePlatformTables"));
        }
        // delete platform tables
        try {
            platformPersistenceService.preDropStructure();
            platformPersistenceService.deleteStructure();
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deletePlatformTables"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deletePlatformTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformDeletionException("Unable to delete platform tables: " + e.getMessage(), e);
        } catch (final IOException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deletePlatformTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformDeletionException("Unable to delete platform tables: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTenant(final long tenantId) throws STenantDeletionException, STenantNotFoundException, SDeletingActivatedTenantException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteTenant"));
        }
        final STenant tenant = getTenant(tenantId);
        if (tenant.getStatus().equals(ACTIVATED)) {
            throw new SDeletingActivatedTenantException();
        }
        try {
            platformPersistenceService.delete(tenant);
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteTenant", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantDeletionException("Unable to delete the tenant: " + e.getMessage(), e);
        }

    }

    @Override
    public void deleteTenantTables() throws STenantDeletionException {
        try {
            for (final PersistenceService tenantPersistenceService : tenantPersistenceServices) {
                tenantPersistenceService.preDropStructure();
                tenantPersistenceService.deleteStructure();
            }
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteTenantTables"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteTenantTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantDeletionException("Unable to delete tenant tables: " + e.getMessage(), e);
        } catch (final IOException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteTenantTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantDeletionException("Unable to delete tenant tables: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteTenantObjects(final long tenantId) throws STenantDeletionException, STenantNotFoundException, SDeletingActivatedTenantException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteTenantObjects"));
        }
        final STenant tenant = getTenant(tenantId);
        if (tenant.getStatus().equals(ACTIVATED)) {
            throw new SDeletingActivatedTenantException();
        }
        try {
            for (final TenantPersistenceService tenantPersistenceService : tenantPersistenceServices) {
                tenantPersistenceService.deleteTenant(tenantId);
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteTenantObjects", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantDeletionException("Unable to delete the tenant object: " + e.getMessage(), e);
        } catch (final IOException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteTenantObjects", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantDeletionException("Unable to delete the tenant object: " + e.getMessage(), e);
        }
    }

    @Override
    public SPlatform getPlatform() throws SPlatformNotFoundException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getPlatform"));
        }
        SPlatform platform = null;
        try {
            platform = platformPersistenceService.selectOne(new SelectOneDescriptor<SPlatform>("getPlatform", null, SPlatform.class));
            if (platform == null) {
                throw new SPlatformNotFoundException("No platform found");
            }
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getPlatform"));
            }
            return platform;
        } catch (final Throwable e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE,
                        LogUtil.getLogOnExceptionMethod(this.getClass(), "getPlatform", "Unable to check if a platform already exists: " + e.getMessage()));
            }
            throw new SPlatformNotFoundException("Unable to check if a platform already exists: " + e.getMessage(), e);
        }
    }

    @Override
    public STenant getTenant(final long id) throws STenantNotFoundException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getTenant"));
        }
        STenant tenant;
        try {
            tenant = platformPersistenceService.selectById(new SelectByIdDescriptor<STenant>("getTenantById", STenant.class, id));
            if (tenant == null) {
                throw new STenantNotFoundException("No tenant found with id: " + id);
            }
        } catch (final Throwable e) {
            throw new STenantNotFoundException("Unable to get the tenant: " + e.getMessage(), e);
        }
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getTenant"));
        }
        return tenant;
    }

    @Override
    public boolean isPlatformCreated() {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "isPlatformCreated"));
        }
        boolean flag = false;
        try {
            getPlatform();
            flag = true;
        } catch (final SPlatformNotFoundException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "isPlatformCreated", e));
            }
            return false;
        }
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "isPlatformCreated"));
        }
        return flag;
    }

    @Override
    public STenant getTenantByName(final String name) throws STenantNotFoundException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getTenantByName"));
        }
        STenant tenant;
        try {
            final Map<String, Object> parameters = CollectionUtil.buildSimpleMap(tenantBuilder.getNameKey(), name);
            tenant = platformPersistenceService.selectOne(new SelectOneDescriptor<STenant>("getTenantByName", parameters, STenant.class));
            if (tenant == null) {
                throw new STenantNotFoundException("No tenant found with name: " + name);
            }
        } catch (final SBonitaReadException e) {
            throw new STenantNotFoundException("Unable to check if a tenant already exists: " + e.getMessage(), e);
        }
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getTenantByName"));
        }
        return tenant;
    }

    @Override
    public STenant getDefaultTenant() throws STenantNotFoundException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getDefaultTenant"));
        }
        STenant tenant = null;
        try {
            tenant = platformPersistenceService.selectOne(new SelectOneDescriptor<STenant>("getDefaultTenant", null, STenant.class));
            if (tenant == null) {
                throw new STenantNotFoundException("No default tenant found");
            }
        } catch (final SBonitaReadException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getDefaultTenant", e));
            }
            throw new STenantNotFoundException("Unable to check if a default tenant already exists: " + e.getMessage(), e);
        }
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getDefaultTenant"));
        }
        return tenant;
    }

    @Override
    public List<STenant> getTenants(final Collection<Long> ids, final QueryOptions queryOptions) throws STenantNotFoundException, STenantException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getTenants"));
        }
        List<STenant> tenants;
        try {
            final Map<String, Object> parameters = CollectionUtil.buildSimpleMap("ids", ids);
            tenants = platformPersistenceService.selectList(new SelectListDescriptor<STenant>("getTenantsByIds", parameters, STenant.class, queryOptions));
            if (tenants.size() != ids.size()) {
                throw new STenantNotFoundException("Unable to retrieve all tenants by ids. Expected: " + ids + ", retrieved: " + tenants);
            }
        } catch (final SBonitaReadException e) {
            throw new STenantException("Problem getting list of tenants: " + e.getMessage(), e);
        }
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getTenants"));
        }
        return tenants;
    }

    @Override
    public void updatePlatform(final SPlatform platform, final EntityUpdateDescriptor descriptor) throws SPlatformUpdateException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "updatePlatform"));
        }
        final UpdateDescriptor desc = new UpdateDescriptor(platform);
        desc.addFields(descriptor.getFields());
        try {
            platformPersistenceService.update(desc);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "updatePlatform"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "updatePlatform", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new SPlatformUpdateException("Problem while updating platform: " + platform, e);
        }
    }

    @Override
    public void updateTenant(final STenant tenant, final EntityUpdateDescriptor descriptor) throws STenantUpdateException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "updateTenant"));
        }
        final UpdateDescriptor desc = new UpdateDescriptor(tenant);
        desc.addFields(descriptor.getFields());
        try {
            platformPersistenceService.update(desc);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "updateTenant"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "updateTenant", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantUpdateException("Problem while updating tenant: " + tenant, e);
        }
    }

    @Override
    public boolean activateTenant(final long tenantId) throws STenantNotFoundException, STenantActivationException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "activateTenant"));
        }
        final STenant tenant = getTenant(tenantId);
        if (!isTenantActivated(tenant)) {
            final UpdateDescriptor desc = new UpdateDescriptor(tenant);
            desc.addField(tenantBuilder.getStatusKey(), ACTIVATED);
            try {
                platformPersistenceService.update(desc);
                if (trace) {
                    logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "activateTenant"));
                }
            } catch (final SPersistenceException e) {
                if (trace) {
                    logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "activateTenant", e));
                }
                e.printStackTrace();
                if (error) {
                    logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
                }
                throw new STenantActivationException("Problem while activating tenant: " + tenant, e);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void deactiveTenant(final long tenantId) throws STenantNotFoundException, STenantDeactivationException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deactiveTenant"));
        }
        final STenant tenant = getTenant(tenantId);
        final UpdateDescriptor desc = new UpdateDescriptor(tenant);
        desc.addField(tenantBuilder.getStatusKey(), DEACTIVATED);
        try {
            platformPersistenceService.update(desc);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deactiveTenant"));
            }
        } catch (final SPersistenceException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deactiveTenant", e));
            }
            e.printStackTrace();
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantDeactivationException("Problem while deactivating tenant: " + tenant, e);
        }
    }

    @Override
    public List<STenant> getTenants(final QueryOptions queryOptions) throws STenantException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getTenants"));
        }
        List<STenant> tenants;
        try {
            tenants = platformPersistenceService.selectList(new SelectListDescriptor<STenant>("getTenants", null, STenant.class, queryOptions));
        } catch (final SBonitaReadException e) {
            throw new STenantException("Problem getting list of tenants: " + e.getMessage(), e);
        }
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getTenants"));
        }
        return tenants;
    }

    @Override
    public int getNumberOfTenants() throws STenantException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getNumberOfTenants"));
        }
        final Map<String, Object> emptyMap = Collections.emptyMap();
        try {
            final Long read = platformPersistenceService.selectOne(new SelectOneDescriptor<Long>("getNumberOfTenants", emptyMap, STenant.class, Long.class));
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getNumberOfTenants"));
            }
            return read.intValue();
        } catch (final SBonitaReadException e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getNumberOfTenants", e));
            }
            throw new STenantException(e);
        }
    }

    @Override
    public boolean isTenantActivated(final STenant sTenant) {
        return ACTIVATED.equals(sTenant.getStatus());
    }

    @Override
    public List<STenant> searchTenants(final QueryOptions options) throws SBonitaSearchException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "searchSTenants"));
        }
        try {
            final List<STenant> listsSTenants = platformPersistenceService.searchEntity(STenant.class, options, null);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "searchSTenants"));
            }
            return listsSTenants;
        } catch (final SBonitaReadException bre) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "searchSTenants", bre));
            }
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public long getNumberOfTenants(final QueryOptions options) throws SBonitaSearchException {
        if (trace) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getNumberOfSTenant"));
        }
        try {
            final long number = platformPersistenceService.getNumberOfEntities(STenant.class, options, null);
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getNumberOfSTenant"));
            }
            return number;
        } catch (final SBonitaReadException bre) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getNumberOfSTenant", bre));
            }
            throw new SBonitaSearchException(bre);
        }
    }

    @Override
    public void cleanTenantTables() throws STenantUpdateException {
        try {
            for (final PersistenceService tenantPersistenceService : tenantPersistenceServices) {
                tenantPersistenceService.cleanStructure();
            }
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "cleanTenantTables"));
            }
        } catch (final Exception e) {
            if (trace) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "cleanTenantTables", e));
            }
            if (error) {
                logger.log(this.getClass(), TechnicalLogSeverity.ERROR, e);
            }
            throw new STenantUpdateException("Unable to clean tenant tables: " + e.getMessage(), e);
        }
    }

}
