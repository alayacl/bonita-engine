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
package org.bonitasoft.engine.api.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.actor.mapping.model.SActor;
import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.impl.resolver.ActorProcessDependencyResolver;
import org.bonitasoft.engine.api.impl.transaction.actor.GetActor;
import org.bonitasoft.engine.api.impl.transaction.identity.AddUserMembership;
import org.bonitasoft.engine.api.impl.transaction.identity.AddUserMemberships;
import org.bonitasoft.engine.api.impl.transaction.identity.CreateGroup;
import org.bonitasoft.engine.api.impl.transaction.identity.CreateRole;
import org.bonitasoft.engine.api.impl.transaction.identity.CreateUser;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteGroup;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteGroups;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteRole;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteRoles;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteUser;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteUserMembership;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteUserMemberships;
import org.bonitasoft.engine.api.impl.transaction.identity.DeleteUsers;
import org.bonitasoft.engine.api.impl.transaction.identity.GetGroupByPath;
import org.bonitasoft.engine.api.impl.transaction.identity.GetGroups;
import org.bonitasoft.engine.api.impl.transaction.identity.GetNumberOfInstance;
import org.bonitasoft.engine.api.impl.transaction.identity.GetNumberOfUserMemberships;
import org.bonitasoft.engine.api.impl.transaction.identity.GetNumberOfUsersInType;
import org.bonitasoft.engine.api.impl.transaction.identity.GetRole;
import org.bonitasoft.engine.api.impl.transaction.identity.GetRoleByName;
import org.bonitasoft.engine.api.impl.transaction.identity.GetRoles;
import org.bonitasoft.engine.api.impl.transaction.identity.GetSContactInfo;
import org.bonitasoft.engine.api.impl.transaction.identity.GetSGroup;
import org.bonitasoft.engine.api.impl.transaction.identity.GetSUser;
import org.bonitasoft.engine.api.impl.transaction.identity.GetUserMembership;
import org.bonitasoft.engine.api.impl.transaction.identity.GetUserMembershipsOfGroup;
import org.bonitasoft.engine.api.impl.transaction.identity.GetUserMembershipsOfRole;
import org.bonitasoft.engine.api.impl.transaction.identity.GetUsersInGroup;
import org.bonitasoft.engine.api.impl.transaction.identity.GetUsersInRole;
import org.bonitasoft.engine.api.impl.transaction.identity.UpdateGroup;
import org.bonitasoft.engine.api.impl.transaction.identity.UpdateMembershipByRoleIdAndGroupId;
import org.bonitasoft.engine.api.impl.transaction.identity.UpdateRole;
import org.bonitasoft.engine.api.impl.transaction.identity.UpdateUser;
import org.bonitasoft.engine.commons.NullCheckingUtil;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SObjectAlreadyExistsException;
import org.bonitasoft.engine.commons.transaction.TransactionContent;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.commons.transaction.TransactionExecutor;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.identity.ContactData;
import org.bonitasoft.engine.identity.ContactDataUpdater;
import org.bonitasoft.engine.identity.ContactDataUpdater.ContactDataField;
import org.bonitasoft.engine.identity.Group;
import org.bonitasoft.engine.identity.GroupCreator;
import org.bonitasoft.engine.identity.GroupCriterion;
import org.bonitasoft.engine.identity.GroupNotFoundException;
import org.bonitasoft.engine.identity.GroupUpdater;
import org.bonitasoft.engine.identity.GroupUpdater.GroupField;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.ImportPolicy;
import org.bonitasoft.engine.identity.MembershipNotFoundException;
import org.bonitasoft.engine.identity.OrganizationExportException;
import org.bonitasoft.engine.identity.OrganizationImportException;
import org.bonitasoft.engine.identity.Role;
import org.bonitasoft.engine.identity.RoleCreator;
import org.bonitasoft.engine.identity.RoleCriterion;
import org.bonitasoft.engine.identity.RoleNotFoundException;
import org.bonitasoft.engine.identity.RoleUpdater;
import org.bonitasoft.engine.identity.RoleUpdater.RoleField;
import org.bonitasoft.engine.identity.SGroupNotFoundException;
import org.bonitasoft.engine.identity.SIdentityException;
import org.bonitasoft.engine.identity.SRoleNotFoundException;
import org.bonitasoft.engine.identity.SUserNotFoundException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCreator;
import org.bonitasoft.engine.identity.UserCriterion;
import org.bonitasoft.engine.identity.UserMembership;
import org.bonitasoft.engine.identity.UserMembershipCriterion;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.identity.UserUpdater;
import org.bonitasoft.engine.identity.UserUpdater.UserField;
import org.bonitasoft.engine.identity.model.SContactInfo;
import org.bonitasoft.engine.identity.model.SGroup;
import org.bonitasoft.engine.identity.model.SRole;
import org.bonitasoft.engine.identity.model.SUser;
import org.bonitasoft.engine.identity.model.SUserMembership;
import org.bonitasoft.engine.identity.model.builder.GroupUpdateBuilder;
import org.bonitasoft.engine.identity.model.builder.IdentityModelBuilder;
import org.bonitasoft.engine.identity.model.builder.RoleUpdateBuilder;
import org.bonitasoft.engine.identity.model.builder.SContactInfoUpdateBuilder;
import org.bonitasoft.engine.identity.model.builder.UserUpdateBuilder;
import org.bonitasoft.engine.identity.xml.DeleteOrganization;
import org.bonitasoft.engine.identity.xml.ExportOrganization;
import org.bonitasoft.engine.identity.xml.ImportOrganization;
import org.bonitasoft.engine.persistence.OrderByOption;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.profile.ProfileService;
import org.bonitasoft.engine.profile.builder.SProfileBuilderAccessor;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.search.descriptor.SearchEntitiesDescriptor;
import org.bonitasoft.engine.search.identity.SearchGroups;
import org.bonitasoft.engine.search.identity.SearchRoles;
import org.bonitasoft.engine.search.identity.SearchUsers;
import org.bonitasoft.engine.service.ModelConvertor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;
import org.bonitasoft.engine.transaction.STransactionException;

/**
 * @author Matthieu Chaffotte
 * @author Elias Ricken de Medeiros
 * @author Feng Hui
 * @author Zhang Bole
 * @author Yanyan Liu
 * @author Lu Kai
 * @author Hongwen Zang
 * @author Celine Souchet
 */
public class IdentityAPIImpl implements IdentityAPI {

    protected TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    @Override
    public User createUser(final String userName, final String password) throws AlreadyExistsException, CreationException {
        final UserCreator creator = new UserCreator(userName, password);
        return createUser(creator);
    }

    @Override
    public User createUser(final String userName, final String password, final String firstName, final String lastName) throws AlreadyExistsException,
            CreationException {
        final UserCreator creator = new UserCreator(userName, password);
        creator.setFirstName(firstName).setLastName(lastName);
        return createUser(creator);
    }

    @Override
    public User createUser(final UserCreator creator) throws AlreadyExistsException, CreationException {
        if (creator == null) {
            throw new CreationException("Can not create a null user.");
        }
        final Map<org.bonitasoft.engine.identity.UserCreator.UserField, Serializable> fields = creator.getFields();
        final String userName = (String) fields.get(org.bonitasoft.engine.identity.UserCreator.UserField.NAME);
        if (userName == null || userName.trim().isEmpty()) {
            throw new CreationException("The user name cannot be null or empty.");
        }
        final String password = (String) fields.get(org.bonitasoft.engine.identity.UserCreator.UserField.PASSWORD);
        if (password == null || password.trim().isEmpty()) {
            throw new CreationException("The password cannot be null or empty.");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SUser sUser = ModelConvertor.constructSUser(creator, tenantAccessor.getIdentityModelBuilder());
        final SContactInfo sPersoData = ModelConvertor.constructSUserContactInfo(creator, sUser.getId(), true, tenantAccessor.getIdentityModelBuilder());
        final SContactInfo sProlData = ModelConvertor.constructSUserContactInfo(creator, sUser.getId(), false, tenantAccessor.getIdentityModelBuilder());
        return createUser(tenantAccessor, sUser, sPersoData, sProlData);
    }

    @Override
    public User updateUser(final long userId, final UserUpdater updater) throws UserNotFoundException, UpdateException {
        if (updater == null || !updater.hasFields()) {
            throw new UpdateException("The update descriptor does not contain field updates");
        }

        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();

        // User change
        final EntityUpdateDescriptor userChangeDescriptor = getUserUpdateDescriptor(modelBuilder.getUserUpdateBuilder(), updater);
        // Personal data change
        final EntityUpdateDescriptor persoDataChangeDescriptor = getUserContactInfoUpdateDescriptor(modelBuilder.getUserContactInfoUpdateBuilder(),
                updater.getPersoContactUpdater());
        // Professional data change
        final EntityUpdateDescriptor proDataChangeDescriptor = getUserContactInfoUpdateDescriptor(modelBuilder.getUserContactInfoUpdateBuilder(),
                updater.getProContactUpdater());

        try {
            final UpdateUser updateUserTransaction = new UpdateUser(identityService, userId, userChangeDescriptor, persoDataChangeDescriptor,
                    proDataChangeDescriptor, modelBuilder.getUserContactInfoBuilder());
            transactionExecutor.execute(updateUserTransaction);
            return ModelConvertor.toUser(updateUserTransaction.getResult());
        } catch (final SUserNotFoundException sunfe) {
            throw new UserNotFoundException(sunfe);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    private EntityUpdateDescriptor getUserUpdateDescriptor(final UserUpdateBuilder userUpdateBuilder, final UserUpdater updateDescriptor) {
        if (updateDescriptor != null) {
            final Map<UserField, Serializable> fields = updateDescriptor.getFields();
            for (final Entry<UserField, Serializable> field : fields.entrySet()) {
                switch (field.getKey()) {
                    case USER_NAME:
                        userUpdateBuilder.updateUserName((String) field.getValue());
                        break;
                    case PASSWORD:
                        userUpdateBuilder.updatePassword((String) field.getValue());
                        break;
                    case FIRST_NAME:
                        userUpdateBuilder.updateFirstName((String) field.getValue());
                        break;
                    case LAST_NAME:
                        userUpdateBuilder.updateLastName((String) field.getValue());
                        break;
                    case MANAGER_ID:
                        userUpdateBuilder.updateManagerUserId((Long) field.getValue());
                        break;
                    case ICON_NAME:
                        userUpdateBuilder.updateIconName((String) field.getValue());
                        break;
                    case ICON_PATH:
                        userUpdateBuilder.updateIconPath((String) field.getValue());
                        break;
                    case TITLE:
                        userUpdateBuilder.updateTitle((String) field.getValue());
                        break;
                    case JOB_TITLE:
                        userUpdateBuilder.updateJobTitle((String) field.getValue());
                        break;
                    case ENABLED:
                        userUpdateBuilder.updateEnabled((Boolean) field.getValue());
                        break;
                }
            }
            userUpdateBuilder.updateLastUpdate(System.currentTimeMillis());
            return userUpdateBuilder.done();
        } else {
            return null;
        }
    }

    private EntityUpdateDescriptor getUserContactInfoUpdateDescriptor(final SContactInfoUpdateBuilder updateBuilder, final ContactDataUpdater updater) {
        if (updater != null) {
            final Map<ContactDataField, Serializable> fields = updater.getFields();
            for (final Entry<ContactDataField, Serializable> field : fields.entrySet()) {
                switch (field.getKey()) {
                    case EMAIL:
                        updateBuilder.updateEmail((String) field.getValue());
                        break;
                    case PHONE:
                        updateBuilder.updatePhoneNumber((String) field.getValue());
                        break;
                    case MOBILE:
                        updateBuilder.updateMobileNumber((String) field.getValue());
                        break;
                    case FAX:
                        updateBuilder.updateFaxNumber((String) field.getValue());
                        break;
                    case BUILDING:
                        updateBuilder.updateBuilding((String) field.getValue());
                        break;
                    case ROOM:
                        updateBuilder.updateRoom((String) field.getValue());
                        break;
                    case ADDRESS:
                        updateBuilder.updateAddress((String) field.getValue());
                        break;
                    case ZIP_CODE:
                        updateBuilder.updateZipCode((String) field.getValue());
                        break;
                    case CITY:
                        updateBuilder.updateCity((String) field.getValue());
                        break;
                    case STATE:
                        updateBuilder.updateState((String) field.getValue());
                        break;
                    case COUNTRY:
                        updateBuilder.updateCountry((String) field.getValue());
                        break;
                    case WEBSITE:
                        updateBuilder.updateWebsite((String) field.getValue());
                        break;
                }
            }
            return updateBuilder.done();
        } else {
            return null;
        }
    }

    @Override
    public void deleteUser(final long userId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();

        try {
            final DeleteUser deleteUser = new DeleteUser(identityService, actorMappingService, profileService, userId, sProfileBuilderAccessor);
            transactionExecutor.execute(deleteUser);
            final Set<Long> removedActorIds = deleteUser.getRemovedActorIds();
            updateActorProcessDependencies(tenantAccessor, actorMappingService, transactionExecutor, removedActorIds);
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public void deleteUser(final String userName) throws DeletionException {
        if (userName == null) {
            throw new DeletionException("User name can not be null!");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();

        try {
            final DeleteUser deleteUser = new DeleteUser(identityService, actorMappingService, profileService, userName, sProfileBuilderAccessor);
            transactionExecutor.execute(deleteUser);
            final Set<Long> removedActorIds = deleteUser.getRemovedActorIds();
            updateActorProcessDependencies(tenantAccessor, actorMappingService, transactionExecutor, removedActorIds);
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public void deleteUsers(final List<Long> userIds) throws DeletionException {
        if (userIds != null && !userIds.isEmpty()) {
            final TenantServiceAccessor tenantAccessor = getTenantAccessor();
            final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
            final IdentityService identityService = tenantAccessor.getIdentityService();
            final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
            final ProfileService profileService = tenantAccessor.getProfileService();
            final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();
            try {
                final DeleteUsers deleteUsers = new DeleteUsers(identityService, actorMappingService, profileService, sProfileBuilderAccessor, userIds);
                transactionExecutor.execute(deleteUsers);
            } catch (final SBonitaException sbe) {
                throw new DeletionException(sbe);
            }
        }
    }

    @Override
    public User getUser(final long userId) throws UserNotFoundException {
        if (userId == -1) {
            throw new UserNotFoundException("The technical user is not a usable user");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetSUser transactionContent = new GetSUser(identityService, userId);
            transactionExecutor.execute(transactionContent);
            return ModelConvertor.toUser(transactionContent.getResult());
        } catch (final SUserNotFoundException sunfe) {
            throw new UserNotFoundException(sunfe);
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public User getUserByUserName(final String userName) throws UserNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetSUser transactionContent = new GetSUser(identityService, userName);
            transactionExecutor.execute(transactionContent);
            return ModelConvertor.toUser(transactionContent.getResult());
        } catch (final SUserNotFoundException sunfe) {
            throw new UserNotFoundException(sunfe);
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public ContactData getUserContactData(final long userId, final boolean personal) throws UserNotFoundException {
        if (userId == -1) {
            throw new UserNotFoundException("The technical user is not a usable user");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetSContactInfo txContent = new GetSContactInfo(userId, identityService, personal);
            transactionExecutor.execute(txContent);
            final SContactInfo result = txContent.getResult();
            if (result == null) {
                return null;
            }
            return ModelConvertor.toUserContactData(result);
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public long getNumberOfUsers() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetNumberOfInstance transactionContent = new GetNumberOfInstance("getNumberOfUsers", identityService);
            transactionExecutor.execute(transactionContent);
            return transactionContent.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    // FIXME rewrite ME!!!
    public List<User> getUsers(final int startIndex, final int maxResults, final UserCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final IdentityService identityService = tenantAccessor.getIdentityService();
                return getUsersWithOrder(startIndex, maxResults, criterion, tenantAccessor, identityService);
            } catch (final SBonitaException sbe) {
                transactionExecutor.setTransactionRollback();
                throw new RetrieveException(sbe);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Map<Long, User> getUsers(final List<Long> userIds) {
        final Map<Long, User> users = new HashMap<Long, User>();
        for (final Long userId : userIds) {
            try {
                final User user = getUser(userId);
                users.put(userId, user);
            } catch (final UserNotFoundException e) {
                // if the user does not exist; skip the user
            }
        }
        return users;
    }

    @Override
    public SearchResult<User> searchUsers(final SearchOptions options) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchUsers searchUsers = new SearchUsers(identityService, searchEntitiesDescriptor.getUserDescriptor(), options);
        try {
            transactionExecutor.execute(searchUsers);
            return searchUsers.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    @Override
    public long getNumberOfUsersInRole(final long roleId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetNumberOfUsersInType transactionContentWithResult = new GetNumberOfUsersInType(roleId, "getNumberOfUsersInRole", identityService);
            transactionExecutor.execute(transactionContentWithResult);
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<User> getUsersInRole(final long roleId, final int startIndex, final int maxResults, final UserCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();
        String field = null;
        OrderByType order = null;
        switch (criterion) {
            case FIRST_NAME_ASC:
                field = modelBuilder.getUserBuilder().getFirstNameKey();
                order = OrderByType.ASC;
                break;
            case LAST_NAME_ASC:
                field = modelBuilder.getUserBuilder().getLastNameKey();
                order = OrderByType.ASC;
                break;
            case USER_NAME_ASC:
                field = modelBuilder.getUserBuilder().getUserNameKey();
                order = OrderByType.ASC;
                break;
            case FIRST_NAME_DESC:
                field = modelBuilder.getUserBuilder().getFirstNameKey();
                order = OrderByType.DESC;
                break;
            case LAST_NAME_DESC:
                field = modelBuilder.getUserBuilder().getLastNameKey();
                order = OrderByType.DESC;
                break;
            case USER_NAME_DESC:
                field = modelBuilder.getUserBuilder().getUserNameKey();
                order = OrderByType.DESC;
                break;
        }
        try {
            final String fieldExecutor = field;
            final OrderByType orderExecutor = order;
            final GetUsersInRole getUsersInRole = new GetUsersInRole(roleId, startIndex, maxResults, fieldExecutor, orderExecutor, identityService);
            transactionExecutor.execute(getUsersInRole);
            return ModelConvertor.toUsers(getUsersInRole.getResult());
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public long getNumberOfUsersInGroup(final long groupId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final GetNumberOfUsersInType transactionContentWithResult = new GetNumberOfUsersInType(groupId, "getNumberOfUsersInGroup", identityService);
        try {
            transactionExecutor.execute(transactionContentWithResult);
            return transactionContentWithResult.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<User> getUsersInGroup(final long groupId, final int startIndex, final int maxResults, final UserCriterion crterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();
        String field = null;
        OrderByType order = null;
        switch (crterion) {
            case FIRST_NAME_ASC:
                field = modelBuilder.getUserBuilder().getFirstNameKey();
                order = OrderByType.ASC;
                break;
            case LAST_NAME_ASC:
                field = modelBuilder.getUserBuilder().getLastNameKey();
                order = OrderByType.ASC;
                break;
            case USER_NAME_ASC:
                field = modelBuilder.getUserBuilder().getUserNameKey();
                order = OrderByType.ASC;
                break;
            case FIRST_NAME_DESC:
                field = modelBuilder.getUserBuilder().getFirstNameKey();
                order = OrderByType.DESC;
                break;
            case LAST_NAME_DESC:
                field = modelBuilder.getUserBuilder().getLastNameKey();
                order = OrderByType.DESC;
                break;
            case USER_NAME_DESC:
                field = modelBuilder.getUserBuilder().getUserNameKey();
                order = OrderByType.DESC;
                break;
        }
        try {
            final GetUsersInGroup getUsersOfGroup = new GetUsersInGroup(groupId, startIndex, maxResults, order, field, identityService);
            transactionExecutor.execute(getUsersOfGroup);
            return ModelConvertor.toUsers(getUsersOfGroup.getResult());
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public Role createRole(final String roleName) throws AlreadyExistsException, CreationException {
        return createRole(new RoleCreator(roleName));
    }

    @Override
    public Role createRole(final RoleCreator creator) throws AlreadyExistsException, CreationException {
        if (creator == null) {
            throw new CreationException("Unable to create a role with a null creator!");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final SRole sRole = ModelConvertor.constructSRole(creator, tenantAccessor.getIdentityModelBuilder());
        try {
            final CreateRole createRole = new CreateRole(sRole, identityService);
            transactionExecutor.execute(createRole);
            return ModelConvertor.toRole(sRole);
        } catch (final SBonitaException sbe) {
            try {
                getRoleByName(sRole.getName());
                throw new AlreadyExistsException("A role named \"" + sRole.getName() + "\" already exists");
            } catch (final RoleNotFoundException rnfe) {
            }
            throw new CreationException("Role create exception!", sbe);
        }
    }

    @Override
    public Role updateRole(final long roleId, final RoleUpdater updateDescriptor) throws RoleNotFoundException, UpdateException {
        if (updateDescriptor == null || updateDescriptor.getFields().isEmpty()) {
            throw new UpdateException("The update descriptor does not contain field updates");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final IdentityModelBuilder identityModelBuilder = tenantAccessor.getIdentityModelBuilder();
        final RoleUpdateBuilder roleUpdateBuilder = identityModelBuilder.getRoleUpdateBuilder();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final EntityUpdateDescriptor changeDescriptor = getRoleUpdateDescriptor(roleUpdateBuilder, updateDescriptor);
            final UpdateRole updateRole = new UpdateRole(changeDescriptor, roleId, identityService);
            transactionExecutor.execute(updateRole);
            return getRole(roleId);
        } catch (final SRoleNotFoundException srnfe) {
            throw new RoleNotFoundException(srnfe);
        } catch (final SBonitaException e) {
            throw new UpdateException(e);
        }
    }

    private EntityUpdateDescriptor getRoleUpdateDescriptor(final RoleUpdateBuilder roleUpdateBuilder, final RoleUpdater updateDescriptor) {
        final Map<RoleField, Serializable> fields = updateDescriptor.getFields();
        for (final Entry<RoleField, Serializable> field : fields.entrySet()) {
            switch (field.getKey()) {
                case NAME:
                    roleUpdateBuilder.updateName((String) field.getValue());
                    break;
                case DISPLAY_NAME:
                    roleUpdateBuilder.updateDisplayName((String) field.getValue());
                    break;
                case DESCRIPTION:
                    roleUpdateBuilder.updateDescription((String) field.getValue());
                    break;
                case ICON_NAME:
                    roleUpdateBuilder.updateIconName((String) field.getValue());
                    break;
                case ICON_PATH:
                    roleUpdateBuilder.updateIconPath((String) field.getValue());
                    break;
                default:
                    break;
            }
        }
        roleUpdateBuilder.updateLastUpdate(System.currentTimeMillis());
        return roleUpdateBuilder.done();
    }

    @Override
    public void deleteRole(final long roleId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();
        final DeleteRole deleteRole = new DeleteRole(identityService, actorMappingService, profileService, roleId, sProfileBuilderAccessor);
        try {
            transactionExecutor.execute(deleteRole);
            final Set<Long> removedActorIds = deleteRole.getRemovedActorIds();
            updateActorProcessDependencies(tenantAccessor, actorMappingService, transactionExecutor, removedActorIds);
        } catch (final SRoleNotFoundException srnfe) {

        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public void deleteRoles(final List<Long> roleIds) throws DeletionException {
        NullCheckingUtil.checkArgsNotNull(roleIds);
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();
        final DeleteRoles deleteRoles = new DeleteRoles(identityService, actorMappingService, profileService, sProfileBuilderAccessor, roleIds);
        try {
            transactionExecutor.execute(deleteRoles);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public Role getRole(final long roleId) throws RoleNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        try {
            final GetRole getRole = new GetRole(roleId, identityService);
            transactionExecutor.execute(getRole);
            return ModelConvertor.toRole(getRole.getResult());
        } catch (final SBonitaException sbe) {
            throw new RoleNotFoundException(sbe);
        }
    }

    @Override
    public Role getRoleByName(final String roleName) throws RoleNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        try {
            final GetRoleByName getRoleByName = new GetRoleByName(roleName, identityService);
            transactionExecutor.execute(getRoleByName);
            return ModelConvertor.toRole(getRoleByName.getResult());
        } catch (final SBonitaException e) {
            throw new RoleNotFoundException(e);
        }
    }

    @Override
    public long getNumberOfRoles() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        try {
            final GetNumberOfInstance getNumberOfInstance = new GetNumberOfInstance("getNumberOfRoles", identityService);
            transactionExecutor.execute(getNumberOfInstance);
            return getNumberOfInstance.getResult();
        } catch (final SBonitaException e) {
            return 0;
        }
    }

    @Override
    public List<Role> getRoles(final int startIndex, final int maxResults, final RoleCriterion criterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();
        String field = null;
        OrderByType order = null;
        switch (criterion) {
            case NAME_ASC:
                field = modelBuilder.getRoleBuilder().getNameKey();
                order = OrderByType.ASC;
                break;
            case NAME_DESC:
                field = modelBuilder.getRoleBuilder().getNameKey();
                order = OrderByType.DESC;
                break;
            case DISPLAY_NAME_ASC:
                field = modelBuilder.getRoleBuilder().getDisplayNameKey();
                order = OrderByType.ASC;
                break;
            case DISPLAY_NAME_DESC:
                field = modelBuilder.getRoleBuilder().getDisplayNameKey();
                order = OrderByType.DESC;
                break;
        }
        try {
            final String fieldExecutor = field;
            final OrderByType orderExecutor = order;
            final GetRoles getRolesWithOrder = new GetRoles(identityService, startIndex, maxResults, fieldExecutor, orderExecutor);
            transactionExecutor.execute(getRolesWithOrder);
            return ModelConvertor.toRoles(getRolesWithOrder.getResult());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public Map<Long, Role> getRoles(final List<Long> roleIds) {
        final Map<Long, Role> roles = new HashMap<Long, Role>();
        for (final Long roleId : roleIds) {
            try {
                final Role role = getRole(roleId);
                roles.put(roleId, role);
            } catch (final RoleNotFoundException e) {
                // if the role does not exist; skip the role
            }
        }
        return roles;
    }

    @Override
    public SearchResult<Role> searchRoles(final SearchOptions options) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchRoles searchRoles = new SearchRoles(identityService, searchEntitiesDescriptor.getRoleDescriptor(), options);
        try {
            transactionExecutor.execute(searchRoles);
            return searchRoles.getResult();
        } catch (final SBonitaException sbe) {
            throw new BonitaRuntimeException(sbe);
        }
    }

    @Override
    public Group createGroup(final String name, final String parentPath) throws AlreadyExistsException, CreationException {
        final GroupCreator groupCreator = new GroupCreator(name);
        groupCreator.setParentPath(parentPath);
        return createGroup(groupCreator);
    }

    @Override
    public Group createGroup(final GroupCreator creator) throws AlreadyExistsException, CreationException {
        if (creator == null) {
            throw new CreationException("Cannot create a null group");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final SGroup sGroup = ModelConvertor.constructSGroup(creator, tenantAccessor.getIdentityModelBuilder());
        try {
            final CreateGroup createGroup = new CreateGroup(sGroup, identityService);
            transactionExecutor.execute(createGroup);
            return ModelConvertor.toGroup(sGroup);
        } catch (final SObjectAlreadyExistsException e) {
            throw new AlreadyExistsException(e.getMessage());
        } catch (final SBonitaException e) {
            throw new CreationException(e);
        }
    }

    @Override
    public Group updateGroup(final long groupId, final GroupUpdater updater) throws GroupNotFoundException, UpdateException {
        if (updater == null || updater.getFields().isEmpty()) {
            throw new UpdateException("The update descriptor does not contain field updates");
        }
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final IdentityModelBuilder identityModelBuilder = tenantAccessor.getIdentityModelBuilder();
        final GroupUpdateBuilder groupUpdateBuilder = identityModelBuilder.getGroupUpdateBuilder();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final SGroup group = getSGroup(groupId, tenantAccessor);
            final EntityUpdateDescriptor changeDescriptor = getGroupUpdateDescriptor(groupUpdateBuilder, updater);
            final UpdateGroup updateGroup = new UpdateGroup(group.getId(), changeDescriptor, identityService, identityModelBuilder);
            transactionExecutor.execute(updateGroup);
            return getGroup(groupId);
        } catch (final SGroupNotFoundException sgnfe) {
            throw new GroupNotFoundException(sgnfe);
        } catch (final SBonitaException sbe) {
            throw new UpdateException(sbe);
        }
    }

    private EntityUpdateDescriptor getGroupUpdateDescriptor(final GroupUpdateBuilder groupUpdateBuilder, final GroupUpdater updateDescriptor)
            throws UpdateException {
        final Map<GroupField, Serializable> fields = updateDescriptor.getFields();
        for (final Entry<GroupField, Serializable> field : fields.entrySet()) {
            switch (field.getKey()) {
                case NAME:
                    groupUpdateBuilder.updateName((String) field.getValue());
                    break;
                case DISPLAY_NAME:
                    groupUpdateBuilder.updateDisplayName((String) field.getValue());
                    break;
                case DESCRIPTION:
                    groupUpdateBuilder.updateDescription((String) field.getValue());
                    break;
                case ICON_NAME:
                    groupUpdateBuilder.updateIconName((String) field.getValue());
                    break;
                case ICON_PATH:
                    groupUpdateBuilder.updateIconPath((String) field.getValue());
                    break;
                case PARENT_PATH:
                    groupUpdateBuilder.updateParentPath((String) field.getValue());
                    break;
                default:
                    throw new UpdateException("Invalid field: " + field.getKey().name());
            }
        }
        groupUpdateBuilder.updateLastUpdate(System.currentTimeMillis());
        return groupUpdateBuilder.done();
    }

    @Override
    public void deleteGroup(final long groupId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();
        final DeleteGroup deleteGroup = new DeleteGroup(identityService, actorMappingService, profileService, groupId, sProfileBuilderAccessor);
        try {
            transactionExecutor.execute(deleteGroup);
            updateActorProcessDependencies(tenantAccessor, actorMappingService, transactionExecutor, deleteGroup.getRemovedActorIds());
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public void deleteGroups(final List<Long> groupIds) throws DeletionException {
        if (groupIds == null) {
            throw new IllegalArgumentException("the list of groups is null");
        }
        if (!groupIds.isEmpty()) {
            final TenantServiceAccessor tenantAccessor = getTenantAccessor();
            final IdentityService identityService = tenantAccessor.getIdentityService();
            final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
            final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
            final ProfileService profileService = tenantAccessor.getProfileService();
            final SProfileBuilderAccessor sProfileBuilderAccessor = tenantAccessor.getSProfileBuilderAccessor();
            try {
                final DeleteGroups deleteGroups = new DeleteGroups(identityService, actorMappingService, profileService, sProfileBuilderAccessor, groupIds);
                transactionExecutor.execute(deleteGroups);
                updateActorProcessDependencies(tenantAccessor, actorMappingService, transactionExecutor, deleteGroups.getRemovedActorIds());
            } catch (final SBonitaException e) {
                throw new DeletionException(e);
            }
        }
    }

    @Override
    public Group getGroup(final long groupId) throws GroupNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        try {
            final SGroup sGroup = getSGroup(groupId, tenantAccessor);
            return ModelConvertor.toGroup(sGroup);
        } catch (final SGroupNotFoundException sgnfe) {
            throw new GroupNotFoundException(sgnfe);
        }
    }

    private SGroup getSGroup(final long groupId, final TenantServiceAccessor tenantAccessor) throws SGroupNotFoundException {
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetSGroup getSGroup = new GetSGroup(groupId, identityService);
            transactionExecutor.execute(getSGroup);
            return getSGroup.getResult();
        } catch (final SGroupNotFoundException sgnfe) {
            throw sgnfe;
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public Group getGroupByPath(final String groupPath) throws GroupNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        try {
            final GetGroupByPath getGroup = new GetGroupByPath(groupPath, identityService);
            transactionExecutor.execute(getGroup);
            return ModelConvertor.toGroup(getGroup.getResult());
        } catch (final SGroupNotFoundException sgnfe) {
            throw new GroupNotFoundException(sgnfe);
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public long getNumberOfGroups() {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetNumberOfInstance getNumberOfInstance = new GetNumberOfInstance("getNumberOfGroups", identityService);
            transactionExecutor.execute(getNumberOfInstance);
            return getNumberOfInstance.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public Map<Long, Group> getGroups(final List<Long> groupIds) {
        final Map<Long, Group> groups = new HashMap<Long, Group>();
        for (final Long groupId : groupIds) {
            try {
                final Group group = getGroup(groupId);
                groups.put(groupId, group);
            } catch (final GroupNotFoundException e) {
                // if the group does not exist; skip the group
            }
        }
        return groups;
    }

    @Override
    public List<Group> getGroups(final int startIndex, final int maxResults, final GroupCriterion pagingCriterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();
        String field = null;
        OrderByType order = null;
        switch (pagingCriterion) {
            case NAME_ASC:
                field = modelBuilder.getGroupBuilder().getNameKey();
                order = OrderByType.ASC;
                break;
            case LABEL_ASC:
                field = modelBuilder.getGroupBuilder().getDisplayNameKey();
                order = OrderByType.ASC;
                break;
            case NAME_DESC:
                field = modelBuilder.getGroupBuilder().getNameKey();
                order = OrderByType.DESC;
                break;
            case LABEL_DESC:
                field = modelBuilder.getGroupBuilder().getDisplayNameKey();
                order = OrderByType.DESC;
                break;
        }
        try {
            final GetGroups getGroups = new GetGroups(identityService, startIndex, maxResults, order, field);
            transactionExecutor.execute(getGroups);
            return ModelConvertor.toGroups(getGroups.getResult());
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public SearchResult<Group> searchGroups(final SearchOptions options) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchGroups searchGroups = new SearchGroups(identityService, searchEntitiesDescriptor.getGroupDescriptor(), options);
        try {
            transactionExecutor.execute(searchGroups);
            return searchGroups.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    private User createUser(final TenantServiceAccessor tenantAccessor, final SUser sUser, final SContactInfo sPersonalData,
            final SContactInfo sProfessionalData) throws AlreadyExistsException, CreationException {
        try {
            final CreateUser createUser = new CreateUser(sUser, sPersonalData, sProfessionalData, tenantAccessor.getIdentityService(), tenantAccessor
                    .getIdentityModelBuilder().getUserContactInfoBuilder());
            tenantAccessor.getTransactionExecutor().execute(createUser);
            return ModelConvertor.toUser(createUser.getResult());
        } catch (final SBonitaException sbe) {
            try {
                getUserByUserName(sUser.getUserName());
                throw new AlreadyExistsException("A user with name \"" + sUser.getUserName() + "\" already exists");
            } catch (final UserNotFoundException unfe) {
                // user does not exists but was unable to be created
            }
            throw new CreationException(sbe);
        }
    }

    /**
     * Check / update process resolution information, for all processes in a list of actor IDs.
     */
    private void updateActorProcessDependencies(final TenantServiceAccessor tenantAccessor, final ActorMappingService actorMappingService,
            final TransactionExecutor transactionExecutor, final Set<Long> removedActorIds) throws SBonitaException {
        final Set<Long> processDefinitionIds = new HashSet<Long>(removedActorIds.size());
        for (final Long actorId : removedActorIds) {
            final GetActor getActor = new GetActor(actorMappingService, actorId);
            transactionExecutor.execute(getActor);
            final SActor actor = getActor.getResult();
            final Long processDefId = Long.valueOf(actor.getScopeId());
            if (!processDefinitionIds.contains(processDefId)) {
                processDefinitionIds.add(processDefId);
                tenantAccessor.getDependencyResolver().resolveDependencies(actor.getScopeId(), tenantAccessor);
            }
        }
    }

    /**
     * Check / update process resolution information, for all processes in a list of actor IDs.
     */
    private void updateActorProcessDependenciesForAllActors(final TenantServiceAccessor tenantAccessor, final ActorMappingService actorMappingService,
            final TransactionExecutor transactionExecutor) throws SBonitaException {
        final ProcessDefinitionService processDefinitionService = tenantAccessor.getProcessDefinitionService();
        List<Long> processDefinitionIds;
        final ActorProcessDependencyResolver dependencyResolver = new ActorProcessDependencyResolver();
        do {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                processDefinitionIds = processDefinitionService.getProcessDefinitionIds(0, QueryOptions.DEFAULT_NUMBER_OF_RESULTS);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
            for (final Long processDefinitionId : processDefinitionIds) {
                tenantAccessor.getDependencyResolver().resolveDependencies(processDefinitionId, tenantAccessor, dependencyResolver);
            }
        } while (processDefinitionIds.size() == QueryOptions.DEFAULT_NUMBER_OF_RESULTS);

    }

    private List<User> getUsersWithOrder(final int startIndex, final int maxResults, final UserCriterion pagingCriterion,
            final TenantServiceAccessor tenantAccessor, final IdentityService identityService) throws SIdentityException {
        final String field = getUserFieldKey(pagingCriterion, tenantAccessor);
        final OrderByType order = getUserOrderByType(pagingCriterion);
        if (field == null) {
            return ModelConvertor.toUsers(identityService.getUsers(startIndex, maxResults));
        } else {
            return ModelConvertor.toUsers(identityService.getUsers(startIndex, maxResults, field, order));
        }
    }

    private OrderByType getUserOrderByType(final UserCriterion pagingCriterion) {
        OrderByType order = null;

        switch (pagingCriterion) {
            case USER_NAME_ASC:
                order = OrderByType.ASC;
                break;
            case FIRST_NAME_ASC:
                order = OrderByType.ASC;
                break;
            case LAST_NAME_ASC:
                order = OrderByType.ASC;
                break;
            case FIRST_NAME_DESC:
                order = OrderByType.DESC;
                break;
            case LAST_NAME_DESC:
                order = OrderByType.DESC;
                break;
            case USER_NAME_DESC:
                order = OrderByType.DESC;
                break;
        }
        return order;
    }

    private String getUserFieldKey(final UserCriterion pagingCriterion, final TenantServiceAccessor tenantAccessor) {
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();

        String field = null;
        switch (pagingCriterion) {
            case USER_NAME_ASC:
                field = modelBuilder.getUserBuilder().getUserNameKey();
                break;
            case FIRST_NAME_ASC:
                field = modelBuilder.getUserBuilder().getFirstNameKey();
                break;
            case LAST_NAME_ASC:
                field = modelBuilder.getUserBuilder().getLastNameKey();
                break;
            case FIRST_NAME_DESC:
                field = modelBuilder.getUserBuilder().getFirstNameKey();
                break;
            case LAST_NAME_DESC:
                field = modelBuilder.getUserBuilder().getLastNameKey();
                break;
            case USER_NAME_DESC:
                field = modelBuilder.getUserBuilder().getUserNameKey();
                break;
        }
        return field;
    }

    @Override
    public UserMembership addUserMembership(final long userId, final long groupId, final long roleId) throws AlreadyExistsException, CreationException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityModelBuilder identityModelBuilder = tenantAccessor.getIdentityModelBuilder();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final long assignedBy = ModelConvertor.getCurrentUserId();
        try {
            final AddUserMembership createUserMembership = new AddUserMembership(userId, groupId, roleId, assignedBy, identityService,
                    identityModelBuilder.getUserMembershipBuilder());
            transactionExecutor.execute(createUserMembership);
            final SUserMembership sUserMembership = createUserMembership.getResult();
            return ModelConvertor.toUserMembership(sUserMembership);
        } catch (final SBonitaException sbe) {
            try {
                final GetUserMembership getUserMembership = new GetUserMembership(userId, groupId, roleId, identityService);
                transactionExecutor.execute(getUserMembership);
                if (getUserMembership.getResult() != null) {
                    throw new AlreadyExistsException("A userMembership with userId \"" + userId + "\", groupId \"" + groupId + "\" and roleId \"" + roleId
                            + "\" already exists");
                }
            } catch (final SBonitaException e) {
                // Membership does not exists but was unable to be created
            }
            throw new CreationException(sbe);
        }
    }

    @Override
    public void addUserMemberships(final List<Long> userIds, final long groupId, final long roleId) throws AlreadyExistsException, CreationException {
        // FIXME rewrite
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor tranExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();
        final long currentUserId = ModelConvertor.getCurrentUserId();
        try {
            final AddUserMemberships transactionContent = new AddUserMemberships(groupId, roleId, userIds, modelBuilder, identityService, currentUserId);
            tranExecutor.execute(transactionContent);
        } catch (final SBonitaException sbe) {
            throw new CreationException(sbe);
        }
    }

    @Override
    public UserMembership updateUserMembership(final long userMembershipId, final long newGroupId, final long newRoleId) throws MembershipNotFoundException,
            UpdateException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final EntityUpdateDescriptor changeDescriptor = tenantAccessor.getIdentityModelBuilder().getUserMembershipUpdateBuilder().updateGroupId(newGroupId)
                .updateRoleId(newRoleId).done();
        try {
            final TransactionContent transactionContent = new UpdateMembershipByRoleIdAndGroupId(userMembershipId, identityService, changeDescriptor);
            transactionExecutor.execute(transactionContent);
            final GetUserMembership getMembershipAfterUpdate = new GetUserMembership(userMembershipId, identityService);
            transactionExecutor.execute(getMembershipAfterUpdate);
            final SUserMembership sMembershipAfterUpdate = getMembershipAfterUpdate.getResult();
            return ModelConvertor.toUserMembership(sMembershipAfterUpdate);
        } catch (final SBonitaException sbe) {
            throw new UpdateException(sbe);
        }
    }

    @Override
    public void deleteUserMembership(final long userMembershipId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final DeleteUserMembership deleteMembership = new DeleteUserMembership(userMembershipId, identityService);
        try {
            transactionExecutor.execute(deleteMembership);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public void deleteUserMembership(final long userId, final long groupId, final long roleId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        try {
            final TransactionContent transactionContent = new DeleteUserMembership(userId, groupId, roleId, identityService);
            transactionExecutor.execute(transactionContent);
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public void deleteUserMemberships(final List<Long> userIds, final long groupId, final long roleId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final TransactionContent transactionContent = new DeleteUserMemberships(groupId, userIds, identityService, roleId);
        try {
            transactionExecutor.execute(transactionContent);
        } catch (final SBonitaException sbe) {
            throw new DeletionException(sbe);
        }
    }

    @Override
    public UserMembership getUserMembership(final long userMembershipId) throws MembershipNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final GetUserMembership getUserMembership = new GetUserMembership(userMembershipId, identityService);
        try {
            transactionExecutor.execute(getUserMembership);
            final SUserMembership sMembership = getUserMembership.getResult();
            return ModelConvertor.toUserMembership(sMembership);
        } catch (final SBonitaException sbe) {
            throw new MembershipNotFoundException(sbe);
        }
    }

    @Override
    public long getNumberOfUserMemberships(final long userId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final TransactionContentWithResult<Long> transactionContent = new GetNumberOfUserMemberships(userId, identityService);
            transactionExecutor.execute(transactionContent);
            return transactionContent.getResult();
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<UserMembership> getUserMemberships(final long userId, final int startIndex, final int maxResults, final UserMembershipCriterion pagingCrterion) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final boolean txOpened = transactionExecutor.openTransaction();
            try {
                final IdentityService identityService = tenantAccessor.getIdentityService();
                final IdentityModelBuilder modelBuilder = tenantAccessor.getIdentityModelBuilder();
                OrderByOption orderByOption = null;
                switch (pagingCrterion) {
                    case ROLE_NAME_DESC:
                        orderByOption = new OrderByOption(SRole.class, modelBuilder.getRoleBuilder().getNameKey(), OrderByType.DESC);
                        break;
                    case GROUP_NAME_ASC:
                        orderByOption = new OrderByOption(SGroup.class, modelBuilder.getGroupBuilder().getNameKey(), OrderByType.ASC);
                        break;
                    case GROUP_NAME_DESC:
                        orderByOption = new OrderByOption(SGroup.class, modelBuilder.getGroupBuilder().getNameKey(), OrderByType.DESC);
                        break;
                    // case ASSIGNED_BY_ASC:
                    // orderByOption = new OrderByOption(SUserMembership.class, modelBuilder.getUserMembershipBuilder().getAssignedByKey(), OrderByType.ASC);
                    // break;
                    // case ASSIGNED_BY_DESC:
                    // orderByOption = new OrderByOption(SUserMembership.class, modelBuilder.getUserMembershipBuilder().getAssignedByKey(), OrderByType.DESC);
                    // break;
                    case ASSIGNED_DATE_ASC:
                        orderByOption = new OrderByOption(SUserMembership.class, modelBuilder.getUserMembershipBuilder().getAssignedDateKey(), OrderByType.ASC);
                        break;
                    case ASSIGNED_DATE_DESC:
                        orderByOption = new OrderByOption(SUserMembership.class, modelBuilder.getUserMembershipBuilder().getAssignedDateKey(), OrderByType.DESC);
                        break;
                    case ROLE_NAME_ASC:
                    default:
                        orderByOption = new OrderByOption(SRole.class, modelBuilder.getRoleBuilder().getNameKey(), OrderByType.ASC);
                        break;
                }

                return getUserMemberships(userId, startIndex, maxResults, orderByOption, identityService);
            } catch (final SBonitaException sbe) {
                transactionExecutor.setTransactionRollback();
                throw new RetrieveException(sbe);
            } finally {
                transactionExecutor.completeTransaction(txOpened);
            }
        } catch (final STransactionException ste) {
            throw new RetrieveException(ste);
        }
    }

    private List<UserMembership> getUserMemberships(final long userId, final int startIndex, final int maxResults, final OrderByOption orderByOption,
            final IdentityService identityService) throws SBonitaException {
        return getUserMemberships(userId, startIndex, maxResults, null, null, orderByOption, identityService);
    }

    private List<UserMembership> getUserMemberships(final long userId, final int startIndex, final int maxResults, final OrderByType orderExecutor,
            final String fieldExecutor, final OrderByOption orderByOption, final IdentityService identityService) throws SBonitaException {
        List<SUserMembership> sUserMemberships;
        if (userId == -1) {
            sUserMemberships = identityService.getUserMemberships(startIndex, maxResults);
        } else if (orderByOption != null) {
            sUserMemberships = identityService.getUserMembershipsOfUser(userId, startIndex, maxResults, orderByOption);
        } else if (fieldExecutor == null) {
            sUserMemberships = identityService.getUserMembershipsOfUser(userId, startIndex, maxResults);
        } else {
            sUserMemberships = identityService.getUserMembershipsOfUser(userId, startIndex, maxResults, fieldExecutor, orderExecutor);
        }
        return ModelConvertor.toUserMembership(sUserMemberships);
    }

    @Override
    public List<UserMembership> getUserMembershipsByGroup(final long groupId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final GetUserMembershipsOfGroup transactionContentWithResult = new GetUserMembershipsOfGroup(groupId, identityService, startIndex, maxResults);
        try {
            transactionExecutor.execute(transactionContentWithResult);
            return ModelConvertor.toUserMembership(transactionContentWithResult.getResult());
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public List<UserMembership> getUserMembershipsByRole(final long roleId, final int startIndex, final int maxResults) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        try {
            final GetUserMembershipsOfRole transactionContentWithResult = new GetUserMembershipsOfRole(roleId, identityService, startIndex, maxResults);
            transactionExecutor.execute(transactionContentWithResult);
            return ModelConvertor.toUserMembership(transactionContentWithResult.getResult());
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public void deleteOrganization() throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final IdentityService identityService = tenantAccessor.getIdentityService();
        final ActorMappingService actorMappingService = tenantAccessor.getActorMappingService();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final DeleteOrganization deleteOrganization = new DeleteOrganization(identityService, profileService, actorMappingService);
        try {
            transactionExecutor.execute(deleteOrganization);
            updateActorProcessDependenciesForAllActors(tenantAccessor, actorMappingService, transactionExecutor);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

    @Override
    public void importOrganization(final String organizationContent) throws OrganizationImportException {
        importOrganization(organizationContent, ImportPolicy.MERGE_DUPLICATES);
    }

    @Override
    public void importOrganization(final String organizationContent, final ImportPolicy policy) throws OrganizationImportException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ImportOrganization importOrganization = new ImportOrganization(tenantAccessor, organizationContent, policy);
        try {
            transactionExecutor.execute(importOrganization);
        } catch (final SBonitaException e) {
            throw new OrganizationImportException(e);
        }
    }

    @Override
    public String exportOrganization() throws OrganizationExportException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final TransactionExecutor transactionExecutor = tenantAccessor.getTransactionExecutor();
        final ExportOrganization exportOrganization = new ExportOrganization(tenantAccessor.getXMLWriter(), tenantAccessor.getIdentityService());
        try {
            transactionExecutor.execute(exportOrganization);
            return exportOrganization.getResult();
        } catch (final SBonitaException e) {
            throw new OrganizationExportException(e);
        }
    }
}
