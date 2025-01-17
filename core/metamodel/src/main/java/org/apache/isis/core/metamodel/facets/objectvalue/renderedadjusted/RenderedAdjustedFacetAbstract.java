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
package org.apache.isis.core.metamodel.facets.objectvalue.renderedadjusted;

import java.util.function.BiConsumer;

import org.apache.isis.core.metamodel.facetapi.Facet;
import org.apache.isis.core.metamodel.facetapi.FacetAbstract;
import org.apache.isis.core.metamodel.facetapi.FacetHolder;

import lombok.NonNull;

public abstract class RenderedAdjustedFacetAbstract
extends FacetAbstract
implements RenderedAdjustedFacet {

    public static final Class<RenderedAdjustedFacet> type() {
        return RenderedAdjustedFacet.class;
    }

    private final int adjustBy;

    public RenderedAdjustedFacetAbstract(final int adjustBy, final FacetHolder holder) {
        super(type(), holder);
        this.adjustBy = adjustBy;
    }

    @Override
    public int value() {
        return adjustBy;
    }

    @Override
    public void visitAttributes(final BiConsumer<String, Object> visitor) {
        super.visitAttributes(visitor);
        visitor.accept("adjustBy", adjustBy == -1
                ? "default"
                : String.valueOf(adjustBy));
    }

    @Override
    public boolean semanticEquals(final @NonNull Facet other) {
        return other instanceof RenderedAdjustedFacet
                ? this.value() == ((RenderedAdjustedFacet)other).value()
                : false;
    }

}
