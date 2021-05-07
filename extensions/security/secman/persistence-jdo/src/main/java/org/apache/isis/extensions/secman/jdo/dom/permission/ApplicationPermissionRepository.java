/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.extensions.secman.jdo.dom.permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.stereotype.Repository;

import org.apache.isis.applib.query.Query;
import org.apache.isis.applib.services.appfeat.ApplicationFeature;
import org.apache.isis.applib.services.appfeat.ApplicationFeatureId;
import org.apache.isis.applib.services.appfeat.ApplicationFeatureRepository;
import org.apache.isis.applib.services.appfeat.ApplicationFeatureSort;
import org.apache.isis.applib.services.factory.FactoryService;
import org.apache.isis.applib.services.message.MessageService;
import org.apache.isis.applib.services.queryresultscache.QueryResultsCache;
import org.apache.isis.applib.services.repository.RepositoryService;
import org.apache.isis.commons.internal.base._Casts;
import org.apache.isis.commons.internal.base._NullSafe;
import org.apache.isis.commons.internal.collections._Lists;
import org.apache.isis.commons.internal.collections._Multimaps;
import org.apache.isis.commons.internal.collections._Multimaps.ListMultimap;
import org.apache.isis.commons.internal.collections._Sets;
import org.apache.isis.extensions.secman.api.permission.ApplicationPermissionMode;
import org.apache.isis.extensions.secman.api.permission.ApplicationPermissionRule;
import org.apache.isis.extensions.secman.api.permission.ApplicationPermissionValue;
import org.apache.isis.extensions.secman.jdo.dom.role.ApplicationRole;
import org.apache.isis.extensions.secman.jdo.dom.user.ApplicationUser;

import lombok.NonNull;
import lombok.val;

@Repository
@Named("isis.ext.secman.ApplicationPermissionRepository")
public class ApplicationPermissionRepository
implements org.apache.isis.extensions.secman.api.permission.ApplicationPermissionRepository {

    @Inject private RepositoryService repository;
    @Inject private ApplicationFeatureRepository featureRepository;
    @Inject private FactoryService factory;
    @Inject private MessageService messages;

    @Inject private javax.inject.Provider<QueryResultsCache> queryResultsCacheProvider;

    @Override
    public ApplicationPermission newApplicationPermission() {
        return factory.detachedEntity(new ApplicationPermission());
    }

    // -- findByRole (programmatic)
    public List<ApplicationPermission> findByRoleCached(@NonNull final ApplicationRole role) {
        return queryResultsCacheProvider.get().execute(this::findByRole,
                ApplicationPermissionRepository.class, "findByRoleCached", role);
    }

    public List<ApplicationPermission> findByRole(@NonNull final ApplicationRole role) {
        return repository.allMatches(
                Query.named(ApplicationPermission.class, "findByRole")
                    .withParameter("role", role));
    }


    // -- findByUser (programmatic)
    public List<ApplicationPermission> findByUserCached(@NonNull final ApplicationUser user) {
        return queryResultsCacheProvider.get().execute(this::findByUser,
                ApplicationPermissionRepository.class, "findByUserCached", user);
    }

    public List<ApplicationPermission> findByUser(@NonNull final ApplicationUser user) {
        return findByUser(user.getUsername());
    }

    private List<ApplicationPermission> findByUser(final String username) {
        return repository.allMatches(
                Query.named(ApplicationPermission.class, "findByUser")
                    .withParameter("username", username));
    }


    // -- findByUserAndPermissionValue (programmatic)
    /**
     * Uses the {@link QueryResultsCache} in order to support
     * multiple lookups from <code>org.apache.isis.extensions.secman.jdo.app.user.UserPermissionViewModel</code>.
     * @return
     */
    @Override
    public Optional<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> findByUserAndPermissionValue(final String username, final ApplicationPermissionValue permissionValue) {

        // obtain all permissions for this user, map by its value, and
        // put into query cache (so that this method can be safely called in a tight loop)
        val permissions =
                queryResultsCacheProvider.get().execute(
                        this::permissionsByPermissionValue,
                        ApplicationPermissionRepository.class, "findByUserAndPermissionValue",
                        username);

        // now simply return the permission from the required value (if it exists)
        final List<ApplicationPermission> applicationPermissions = permissions.get(permissionValue);
        return applicationPermissions != null && !applicationPermissions.isEmpty()
                ? Optional.of(applicationPermissions.get(0))
                        : Optional.empty();
    }

    private ListMultimap<ApplicationPermissionValue, ApplicationPermission> permissionsByPermissionValue(
            final String username) {

        // only username (and not permissionValue) is the key
        // (we are obtaining all the perms for this user)

        val permissionsByPermissionValue =
                _Multimaps.<ApplicationPermissionValue, ApplicationPermission>newListMultimap();

        val permissions = findByUser(username);

        _NullSafe.stream(permissions)
        .forEach(permission->{
            val newPermissionValue = ApplicationPermission.Functions.AS_VALUE.apply(permission);
            permissionsByPermissionValue.putElement(newPermissionValue, permission);
        });

        return permissionsByPermissionValue;
    }

    // -- findByRoleAndRuleAndFeatureType (programmatic)
    @Override
    public Collection<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> findByRoleAndRuleAndFeatureTypeCached(
            org.apache.isis.extensions.secman.api.role.ApplicationRole role,
            ApplicationPermissionRule rule,
            ApplicationFeatureSort type) {
        //noinspection unchecked
        return (Collection)queryResultsCacheProvider.get().execute(this::findByRoleAndRuleAndFeatureType,
                ApplicationPermissionRepository.class, "findByRoleAndRuleAndFeatureTypeCached",
                role, rule, type)
                ;
    }

    public Collection<ApplicationPermission> findByRoleAndRuleAndFeatureType(
            org.apache.isis.extensions.secman.api.role.ApplicationRole role,
            final ApplicationPermissionRule rule,
            final ApplicationFeatureSort type) {
        return repository.allMatches(Query.named(
                        ApplicationPermission.class, "findByRoleAndRuleAndFeatureType")
                    .withParameter("role", role)
                    .withParameter("rule", rule)
                    .withParameter("featureSort", type))
                .stream()
                .collect(_Sets.toUnmodifiableSorted());
    }


    // -- findByRoleAndRuleAndFeature (programmatic)
    public Optional<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> findByRoleAndRuleAndFeatureCached(
            final org.apache.isis.extensions.secman.api.role.ApplicationRole role,
            final ApplicationPermissionRule rule,
            final ApplicationFeatureSort featureSort,
            final String featureFqn) {
        return queryResultsCacheProvider.get().execute(
                this::findByRoleAndRuleAndFeature,
                ApplicationPermissionRepository.class, "findByRoleAndRuleAndFeatureCached",
                role, rule, featureSort, featureFqn);
    }

    @Override
    public Optional<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> findByRoleAndRuleAndFeature(
            final org.apache.isis.extensions.secman.api.role.ApplicationRole role,
            final ApplicationPermissionRule rule,
            final ApplicationFeatureSort featureSort,
            final String featureFqn) {

        return findByRoleAndRuleAndFeature_(role, rule, featureSort, featureFqn)
                .map(org.apache.isis.extensions.secman.api.permission.ApplicationPermission.class::cast);
    }

    private Optional<ApplicationPermission> findByRoleAndRuleAndFeature_(
            final org.apache.isis.extensions.secman.api.role.ApplicationRole role,
            final ApplicationPermissionRule rule,
            final ApplicationFeatureSort featureSort,
            final String featureFqn) {

        return repository
                .uniqueMatch(Query.named(
                                ApplicationPermission.class, "findByRoleAndRuleAndFeature")
                        .withParameter("role", role)
                        .withParameter("rule", rule)
                        .withParameter("featureSort", featureSort)
                        .withParameter("featureFqn", featureFqn ));
    }


    // -- findByFeature (programmatic)

    @Override
    public Collection<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> findByFeatureCached(final ApplicationFeatureId featureId) {
        //noinspection unchecked
        return (Collection)findByFeatureCached_(featureId);
    }

    private Collection<ApplicationPermission> findByFeatureCached_(final ApplicationFeatureId featureId) {
        return queryResultsCacheProvider.get().execute(
                this::findByFeature, ApplicationPermissionRepository.class, "findByFeatureCached_",
                featureId);
    }

    private Collection<ApplicationPermission> findByFeature(final ApplicationFeatureId featureId) {
        return repository.allMatches(
                Query.named(
                        ApplicationPermission.class, "findByFeature")
                .withParameter("featureSort", featureId.getSort())
                .withParameter("featureFqn", featureId.getFullyQualifiedName()))
                .stream()
                .collect(_Sets.toUnmodifiableSorted());
    }

    // -- newPermission (programmatic)

    @Override
    public ApplicationPermission newPermission(
            final org.apache.isis.extensions.secman.api.role.ApplicationRole genericRole,
            final ApplicationPermissionRule rule,
            final ApplicationPermissionMode mode,
            final ApplicationFeatureSort featureSort,
            final String featureFqn) {

        val role = _Casts.<ApplicationRole>uncheckedCast(genericRole);

        final ApplicationFeatureId featureId = ApplicationFeatureId.newFeature(featureSort, featureFqn);
        final ApplicationFeature feature = featureRepository.findFeature(featureId);
        if(feature == null) {
            messages.warnUser("No such " + featureSort.name().toLowerCase() + ": " + featureFqn);
            return null;
        }
        return newPermissionNoCheck(role, rule, mode, featureSort, featureFqn);
    }

    public ApplicationPermission newPermissionNoCheck(
            final ApplicationRole role,
            final ApplicationPermissionRule rule,
            final ApplicationPermissionMode mode,
            final ApplicationFeatureSort featureSort,
            final String featureFqn) {

        ApplicationPermission permission = findByRoleAndRuleAndFeature(role, rule, featureSort, featureFqn)
                .map(ApplicationPermission.class::cast)
                .orElse(null);
        if (permission != null) {
            return permission;
        }
        permission = newApplicationPermission();
        permission.setRole(role);
        permission.setRule(rule);
        permission.setMode(mode);
        permission.setFeatureSort(featureSort);
        permission.setFeatureFqn(featureFqn);
        repository.persist(permission);
        return permission;
    }

    @Override
    public ApplicationPermission newPermission(
            final org.apache.isis.extensions.secman.api.role.ApplicationRole genericRole,
            final ApplicationPermissionRule rule,
            final ApplicationPermissionMode mode,
            final String featurePackage,
            final String featureClassName,
            final String featureMemberName) {

        val featureId = ApplicationFeatureId.newFeature(featurePackage, featureClassName, featureMemberName);
        return newPermission(genericRole, rule, mode, featureId);
    }

    @Override
    public ApplicationPermission newPermission(
            final org.apache.isis.extensions.secman.api.role.ApplicationRole genericRole,
            final ApplicationPermissionRule rule,
            final ApplicationPermissionMode mode,
            final ApplicationFeatureId featureId) {

        val role = _Casts.<ApplicationRole>uncheckedCast(genericRole);
        val featureSort = featureId.getSort();
        val featureFqn = featureId.getFullyQualifiedName();

        val feature = featureRepository.findFeature(featureId);
        if(feature == null) {
            messages.warnUser("No such " + featureSort.name().toLowerCase() + ": " + featureFqn);
            return null;
        }

        val permission = factory.detachedEntity(new ApplicationPermission());
        permission.setRole(role);
        permission.setRule(rule);
        permission.setMode(mode);
        permission.setFeatureSort(featureSort);
        permission.setFeatureFqn(featureFqn);
        repository.persist(permission);

        return permission;
    }


    // -- allPermission (programmatic)
    @Override
    public Collection<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> allPermissions() {
        //noinspection unchecked
        return (Collection)allPermissions_();
    }

    private Collection<ApplicationPermission> allPermissions_() {
        return repository.allInstances(ApplicationPermission.class)
                .stream()
                .collect(_Sets.toUnmodifiableSorted());
    }

    // -- findOrphaned (programmatic)

    @Override
    public Collection<org.apache.isis.extensions.secman.api.permission.ApplicationPermission> findOrphaned() {
        return (Collection)findOrphaned_();
    }

    private Collection<ApplicationPermission> findOrphaned_() {

        val featureNamesKnownToTheMetamodel =
                featureRepository.getFeatureIdentifiersByName().keySet();

        val orphaned = _Lists.<ApplicationPermission>newArrayList();

        for (val permission : allPermissions_()) {

            val featId = permission.asFeatureId().orElse(null);
            if(featId==null) {
                orphaned.add(permission);
                continue;
            }

            if(!featureNamesKnownToTheMetamodel.contains(featId.getFullyQualifiedName())) {
                orphaned.add(permission);
            }
        }

        return orphaned;
    }


}
