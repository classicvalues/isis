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
package org.apache.isis.viewer.wicket.ui.components.scalars.jdkmath;

import java.math.BigDecimal;

import org.apache.isis.core.metamodel.facets.object.value.ValueRepresentation;
import org.apache.isis.core.metamodel.spec.feature.ObjectFeature;
import org.apache.isis.viewer.wicket.model.models.ScalarModel;
import org.apache.isis.viewer.wicket.ui.components.scalars.ConverterBasedOnValueSemantics;

import lombok.NonNull;

public class BigDecimalConverterForFeature
extends ConverterBasedOnValueSemantics<BigDecimal> {

    private static final long serialVersionUID = 1L;

    public BigDecimalConverterForFeature(
            final @NonNull ScalarModel scalarModel) {
        super(scalarModel);
    }

    public BigDecimalConverterForFeature(
            final @NonNull ObjectFeature objFeature,
            final @NonNull ValueRepresentation valueRepresentation) {
        super(objFeature, valueRepresentation);
    }

}