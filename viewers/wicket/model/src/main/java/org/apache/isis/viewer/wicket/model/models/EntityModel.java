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
package org.apache.isis.viewer.wicket.model.models;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.lang.Nullable;

import org.apache.isis.applib.layout.component.CollectionLayoutData;
import org.apache.isis.applib.services.bookmark.Bookmark;
import org.apache.isis.commons.internal.assertions._Assert;
import org.apache.isis.commons.internal.collections._Maps;
import org.apache.isis.core.metamodel.interactions.managed.ManagedProperty;
import org.apache.isis.core.metamodel.interactions.managed.PropertyInteraction;
import org.apache.isis.core.metamodel.objectmanager.memento.ObjectMemento;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.feature.OneToOneAssociation;
import org.apache.isis.core.metamodel.spec.feature.memento.PropertyMemento;
import org.apache.isis.core.runtime.context.IsisAppCommonContext;
import org.apache.isis.viewer.common.model.object.ObjectUiModel;
import org.apache.isis.viewer.common.model.object.ObjectUiModel.HasRenderingHints;
import org.apache.isis.viewer.wicket.model.hints.UiHintContainer;
import org.apache.isis.viewer.wicket.model.mementos.PageParameterNames;
import org.apache.isis.viewer.wicket.model.models.interaction.prop.PropertyInteractionModelWkt;
import org.apache.isis.viewer.wicket.model.util.ComponentHintKey;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.log4j.Log4j2;

/**
 * Backing model to represent a domain object as {@link ManagedObject}.
 */
@Log4j2
public class EntityModel
extends ManagedObjectModel
implements HasRenderingHints, ObjectAdapterModel, UiHintContainer, ObjectUiModel, BookmarkableModel {

    private static final long serialVersionUID = 1L;

    // -- FACTORIES

    public static EntityModel ofPageParameters(
            final IsisAppCommonContext commonContext,
            final PageParameters pageParameters) {

        val memento = bookmarkFrom(pageParameters)
                .map(commonContext::mementoForBookmark)
                .orElse(null);

        return ofMemento(commonContext, memento);
    }

    public static EntityModel ofAdapter(
            final @NonNull IsisAppCommonContext commonContext,
            final @Nullable ManagedObject adapter) {
        val adapterMemento = commonContext.mementoFor(adapter);
        return ofMemento(commonContext, adapterMemento);
    }

    public static EntityModel ofMemento(
            final @NonNull IsisAppCommonContext commonContext,
            final @Nullable ObjectMemento adapterMemento) {

        return new EntityModel(commonContext, adapterMemento,
                EitherViewOrEdit.VIEW, RenderingHint.REGULAR);
    }

    // -- CONSTRUCTORS

    /**
     * As used by TreeModel (same as {@link #ofAdapter(IsisAppCommonContext, ManagedObject)}
     */
    protected EntityModel(
            final IsisAppCommonContext commonContext,
            final ManagedObject adapter) {

        this(commonContext,
                commonContext.mementoFor(adapter),
                EitherViewOrEdit.VIEW, RenderingHint.REGULAR);
    }

    private EntityModel(
            final @NonNull IsisAppCommonContext commonContext,
            final @Nullable ObjectMemento adapterMemento,
            final EitherViewOrEdit mode,
            final RenderingHint renderingHint) {

        super(commonContext, adapterMemento);
        this.mode = mode;
        this.renderingHint = renderingHint;
    }

    // -- BOOKMARKABLE MODEL

    public static String oidStr(final PageParameters pageParameters) {
        return PageParameterNames.OBJECT_OID.getStringFrom(pageParameters);
    }

    private static Optional<Bookmark> bookmarkFrom(final PageParameters pageParameters) {
        return Bookmark.parse(oidStr(pageParameters));
    }

    @Override
    public PageParameters getPageParameters() {
        val pageParameters = getPageParametersWithoutUiHints();
        HintPageParameterSerializer.hintStoreToPageParameters(pageParameters, this);
        return pageParameters;
    }

    @Override
    public PageParameters getPageParametersWithoutUiHints() {
        return PageParameterUtil.createPageParametersForObject(getObject());
    }

    @Override
    public boolean isInlinePrompt() {
        return false;
    }

    // -- HINT SUPPORT

    @Getter(onMethod = @__(@Override))
    @Setter(onMethod = @__(@Override))
    private EitherViewOrEdit mode;

    @Getter(onMethod = @__(@Override))
    @Setter(onMethod = @__(@Override))
    private RenderingHint renderingHint;

    @Override
    public String getHint(final Component component, final String keyName) {
        final ComponentHintKey componentHintKey = ComponentHintKey.create(super.getCommonContext(), component, keyName);
        if(componentHintKey != null) {
            return componentHintKey.get(super.asHintingBookmarkIfSupported());
        }
        return null;
    }

    @Override
    public void setHint(final Component component, final String keyName, final String hintValue) {
        ComponentHintKey componentHintKey = ComponentHintKey.create(super.getCommonContext(), component, keyName);
        componentHintKey.set(super.asHintingBookmarkIfSupported(), hintValue);
    }

    @Override
    public void clearHint(final Component component, final String attributeName) {
        setHint(component, attributeName, null);
    }

    // -- OTHER OBJECT SPECIFIC

    @Override
    public String getTitle() {
        return getObject().titleString();
    }

    @Override
    public ManagedObject getManagedObject() {
        return getObject();
    }

    // -- PROPERTY MODELS (CHILDREN)

    private transient Map<PropertyMemento, ScalarPropertyModel> propertyScalarModels;
    private Map<PropertyMemento, ScalarPropertyModel> propertyScalarModels() {
        if(propertyScalarModels==null) {
            propertyScalarModels = _Maps.<PropertyMemento, ScalarPropertyModel>newHashMap();
        }
        return propertyScalarModels;
    }

    /**
     * Lazily populates with the current value of each property.
     */
    public ScalarModel getPropertyModel(
            final OneToOneAssociation property,
            final EitherViewOrEdit viewOrEdit,
            final RenderingHint renderingHint) {

        final var pm = property.getMemento();
        final var propertyScalarModels = propertyScalarModels();
        final ScalarModel existingScalarModel = propertyScalarModels.get(pm);
        if (existingScalarModel == null) {

            final var propertyInteraction =
                    PropertyInteraction.wrap(ManagedProperty.of(this.getManagedObject(), property, renderingHint.asWhere()));
            final var propertyInteractionModel = new PropertyInteractionModelWkt(getCommonContext(), propertyInteraction);

            final long modelsAdded = propertyInteractionModel.streamPropertyUiModels()
            .map(uiModel->ScalarPropertyModel.wrap(uiModel, viewOrEdit, renderingHint))
            .peek(scalarModel->log.info("adding: {}", scalarModel))
            .filter(scalarModel->propertyScalarModels.put(pm, scalarModel)==null)
            .count();

            // future extensions might allow to add multiple UI models per single property model (typed tuple support)
            _Assert.assertEquals(1L, modelsAdded, ()->
                String.format("unexpected number of propertyScalarModels added %d", modelsAdded));

        }
        return propertyScalarModels.get(pm);

    }

    /**
     * Resets the {@link #propertyScalarModels map} of {@link ScalarModel}s for
     * each {@link PropertyMemento property} to the value held in the underlying
     * {@link #getObject() entity}.
     */
    public void resetPropertyModels() {
        propertyScalarModels().values()
            .forEach(ScalarPropertyModel::syncUiWithModel);
    }

    // -- VIEW OR EDIT

    @Override
    public EntityModel toEditMode() {
        setMode(EitherViewOrEdit.EDIT);
        propertyScalarModels().values()
            .forEach(ScalarPropertyModel::toEditMode);
        return this;
    }

    @Override
    public EntityModel toViewMode() {
        setMode(EitherViewOrEdit.VIEW);
        propertyScalarModels().values()
            .forEach(ScalarPropertyModel::toViewMode);
        return this;
    }

    // -- DETACH

    @Override
    protected void onDetach() {
        propertyScalarModels().values()
            .forEach(ScalarPropertyModel::detach);
        super.onDetach();
        propertyScalarModels = null;
    }

    // -- TAB AND COLUMN (metadata if any)

    @Getter @Setter
    private CollectionLayoutData collectionLayoutData;

    private transient Optional<ManagedObject> contextObject;

    @Setter
    private @Nullable ObjectMemento contextAdapterIfAny;

    @Override @Synchronized
    public boolean isContextAdapter(final ManagedObject other) {
        if(contextObject==null) {
            contextObject = Optional.ofNullable(getMementoService().reconstructObject(contextAdapterIfAny));
        }
        return Objects.equals(contextObject.orElse(null), other);
    }


}
