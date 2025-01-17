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
package org.apache.isis.core.metamodel.facets.members.cssclass.annotprop;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.isis.core.metamodel.context.MetaModelContext;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;
import org.apache.isis.core.metamodel.facetapi.FeatureType;
import org.apache.isis.core.metamodel.facets.FacetFactoryAbstract;
import org.apache.isis.core.metamodel.facets.FacetedMethod;
import org.apache.isis.core.metamodel.facets.members.cssclass.CssClassFacet;

public class CssClassFacetOnActionFromConfiguredRegexFactory
extends FacetFactoryAbstract {

    private final Map<Pattern, String> cssClassByPattern;

    @Inject
    public CssClassFacetOnActionFromConfiguredRegexFactory(final MetaModelContext mmc) {
        super(mmc, FeatureType.ACTIONS_ONLY);
        this.cssClassByPattern = getConfiguration().getApplib().getAnnotation().getActionLayout().getCssClass().getPatternsAsMap();
    }

    @Override
    public void process(final ProcessMethodContext processMethodContext) {

        final FacetedMethod facetHolder = processMethodContext.getFacetHolder();
        if(facetHolder.containsNonFallbackFacet(CssClassFacet.class)) {
            return;
        }

        final Method method = processMethodContext.getMethod();
        final String name = method.getName();

        addFacetIfPresent(createFromConfiguredRegexIfPossible(name, facetHolder));
    }

    // -- cssClassFromPattern


    private Optional<CssClassFacet> createFromConfiguredRegexIfPossible(
            final String name,
            final FacetHolder facetHolder) {
        return cssIfAnyFor(name)
                .map(css->new CssClassFacetOnActionFromConfiguredRegex(css, facetHolder));
    }

    private Optional<String> cssIfAnyFor(final String name) {

        for (Map.Entry<Pattern, String> entry : cssClassByPattern.entrySet()) {
            final Pattern pattern = entry.getKey();
            final String cssClass = entry.getValue();
            if(pattern.matcher(name).matches()) {
                return Optional.ofNullable(cssClass);
            }
        }
        return Optional.empty();
    }

}

