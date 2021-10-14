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
package org.apache.isis.viewer.wicket.ui.components.widgets.linkandlabel;

import java.io.Serializable;
import java.util.Optional;

import org.apache.wicket.Application;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;

import org.apache.isis.applib.layout.grid.Grid;
import org.apache.isis.applib.layout.grid.bootstrap3.BS3Grid;
import org.apache.isis.commons.internal.base._Either;
import org.apache.isis.core.metamodel.facets.object.grid.GridFacet;
import org.apache.isis.core.metamodel.spec.ManagedObject;
import org.apache.isis.core.metamodel.spec.ObjectSpecification;
import org.apache.isis.core.metamodel.spec.feature.ObjectAction;
import org.apache.isis.core.metamodel.specloader.specimpl.ObjectActionMixedIn;
import org.apache.isis.core.runtime.context.IsisAppCommonContext;
import org.apache.isis.core.security.authentication.logout.LogoutMenu.LoginRedirect;
import org.apache.isis.viewer.common.model.components.ComponentType;
import org.apache.isis.viewer.wicket.model.models.ActionModel;
import org.apache.isis.viewer.wicket.model.models.ActionPrompt;
import org.apache.isis.viewer.wicket.model.models.ActionPromptProvider;
import org.apache.isis.viewer.wicket.model.models.ActionPromptWithExtraContent;
import org.apache.isis.viewer.wicket.model.models.EntityModel;
import org.apache.isis.viewer.wicket.model.models.FormExecutor;
import org.apache.isis.viewer.wicket.model.models.InlinePromptContext;
import org.apache.isis.viewer.wicket.model.models.PageType;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.app.registry.ComponentFactoryRegistry;
import org.apache.isis.viewer.wicket.ui.app.registry.ComponentFactoryRegistryAccessor;
import org.apache.isis.viewer.wicket.ui.components.actions.ActionParametersPanel;
import org.apache.isis.viewer.wicket.ui.components.layout.bs3.BS3GridPanel;
import org.apache.isis.viewer.wicket.ui.components.scalars.ScalarPanelAbstract;
import org.apache.isis.viewer.wicket.ui.pages.PageClassRegistry;
import org.apache.isis.viewer.wicket.ui.pages.entity.EntityPage;
import org.apache.isis.viewer.wicket.ui.panels.FormExecutorDefault;
import org.apache.isis.viewer.wicket.ui.util.CssClassAppender;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ActionLinkFactory
implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final @NonNull String linkId;
    protected final @NonNull ActionModel actionModel;
    protected final ScalarModel scalarModelForAssociationIfAny;

    public ActionLink newLinkComponent() {

        final ActionLink link = new ActionLink(linkId, actionModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void doOnClick(final AjaxRequestTarget target) {
                performOnClick(target);
            }

            private ActionPrompt performOnClick(final AjaxRequestTarget target) {
                return ActionLinkFactory.this.onClick(this, target);
            }

        };

        link.add(new CssClassAppender("noVeil"));
        return link;
    }

    /**
     * @return the prompt, if not inline prompt
     */
    private ActionPrompt onClick(
            final ActionLink actionLink,
            final AjaxRequestTarget target) {

        val actionModel = actionLink.getActionModel();

        val inlinePromptContext = determineInlinePromptContext();
        val promptStyle = actionModel.getPromptStyle();

        if(inlinePromptContext == null || promptStyle.isDialog()) {
            final ActionPromptProvider promptProvider = ActionPromptProvider.getFrom(actionLink.getPage());
            val spec = actionModel.getOwner().getSpecification();

            final ActionPrompt prompt = promptProvider.getActionPrompt(promptStyle, spec.getBeanSort());


            //
            // previously this if/else was in the ActionParametersPanel
            //
            // now though we only build that panel if we know that there *are* parameters.
            //
            if(actionModel.hasParameters()) {

                val actionParametersPanel = (ActionParametersPanel)
                        getComponentFactoryRegistry()
                        .createComponent(
                                ComponentType.ACTION_PROMPT, prompt.getContentId(), actionModel);

                actionParametersPanel.setShowHeader(false);

                final Label label = new Label(prompt.getTitleId(), new IModel<String>() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public String getObject() {
                        return actionModel.getFriendlyName();
                    }
                });
                prompt.setTitle(label, target);
                prompt.setPanel(actionParametersPanel, target);
                prompt.showPrompt(target);

                if(prompt instanceof ActionPromptWithExtraContent) {
                    final ActionPromptWithExtraContent promptWithExtraContent =
                            (ActionPromptWithExtraContent) prompt;

                    final ObjectAction action = actionModel.getMetaModel();
                    if(action instanceof ObjectActionMixedIn) {
                        final ObjectActionMixedIn actionMixedIn = (ObjectActionMixedIn) action;
                        final ObjectSpecification mixinSpec = actionMixedIn.getMixinType();

                        if(mixinSpec.isViewModel()) {

                            val commonContext = getCommonContext();
                            final ManagedObject targetAdapterForMixin = action.realTargetAdapter(actionModel.getOwner());
                            final EntityModel entityModelForMixin =
                                    EntityModel.ofAdapter(commonContext, targetAdapterForMixin);

                            final GridFacet facet = mixinSpec.getFacet(GridFacet.class);
                            final Grid gridForMixin = facet.getGrid(targetAdapterForMixin);

                            final String extraContentId = promptWithExtraContent.getExtraContentId();

                            if(gridForMixin instanceof BS3Grid) {
                                final BS3Grid bs3Grid = (BS3Grid) gridForMixin;
                                final BS3GridPanel gridPanel = new BS3GridPanel(extraContentId, entityModelForMixin, bs3Grid);
                                promptWithExtraContent.setExtraContentPanel(gridPanel, target);
                            }
                        }
                    }
                }

                return prompt;

            } else {


                final Page page = actionLink.getPage();

                // returns true - if redirecting to new page, or repainting all components.
                // returns false - if invalid args; if concurrency exception;

                final FormExecutor formExecutor =
                        new FormExecutorDefault(_Either.left(actionModel));
                boolean succeeded = formExecutor.executeAndProcessResults(page, null, null, actionModel.isWithinPrompt());

                if(succeeded) {

                    // intercept redirect request to sign-in page
                    Optional.ofNullable(actionModel.getObject())
                    .ifPresent(actionResultAdapter->{
                        val actionResultObjectType = actionResultAdapter.getSpecification().getLogicalTypeName();
                        if(LoginRedirect.LOGICAL_TYPE_NAME.equals(actionResultObjectType)) {
                            val commonContext = actionModel.getCommonContext();
                            val pageClassRegistry = commonContext.lookupServiceElseFail(PageClassRegistry.class);
                            val signInPage = pageClassRegistry.getPageClass(PageType.SIGN_IN);
                            RequestCycle.get().setResponsePage(signInPage);
                        }
                    });

                    // else nothing to do

                    //
                    // the formExecutor will have either redirected, or scheduled a response,
                    // or repainted components as required
                    //

                } else {

                    // render the target entity again
                    //
                    // (One way this can occur is if an event subscriber has a defect and throws an exception; in which case
                    // the EventBus' exception handler will automatically veto.  This results in a growl message rather than
                    // an error page, but is probably 'good enough').
                    val targetAdapter = actionModel.getOwner();

                    final EntityPage entityPage = EntityPage.ofAdapter(getCommonContext(), targetAdapter);

                    getCommonContext().getTransactionService().flushTransaction();

                    // "redirect-after-post"
                    RequestCycle.get().setResponsePage(entityPage);

                }
            }


        } else {

            MarkupContainer scalarTypeContainer = inlinePromptContext.getScalarTypeContainer();

            actionModel.setInlinePromptContext(inlinePromptContext);
            getComponentFactoryRegistry().addOrReplaceComponent(scalarTypeContainer,
                    ScalarPanelAbstract.ID_SCALAR_IF_REGULAR_INLINE_PROMPT_FORM, ComponentType.PARAMETERS, actionModel);

            inlinePromptContext.getScalarIfRegular().setVisible(false);
            inlinePromptContext.getScalarIfRegularInlinePromptForm().setVisible(true);

            target.add(scalarTypeContainer);
        }

        return null;
    }

    private InlinePromptContext determineInlinePromptContext() {
        return scalarModelForAssociationIfAny != null
                ? scalarModelForAssociationIfAny.getInlinePromptContext()
                : null;
    }

    // -- DEPENDENCIES

    private ComponentFactoryRegistry getComponentFactoryRegistry() {
        return ((ComponentFactoryRegistryAccessor) Application.get()).getComponentFactoryRegistry();
    }

    private IsisAppCommonContext getCommonContext() {
        return actionModel.getCommonContext();
    }

}