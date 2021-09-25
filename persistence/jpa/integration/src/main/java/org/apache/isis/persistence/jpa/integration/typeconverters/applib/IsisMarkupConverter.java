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
package org.apache.isis.persistence.jpa.integration.typeconverters.applib;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.isis.applib.value.Markup;

/**
 * @since 2.0 {@index}
 */
@Converter(autoApply = true)
public class IsisMarkupConverter implements AttributeConverter<Markup, String> {

    private static final long serialVersionUID = 1L;

    @Override
    public String convertToDatabaseColumn(final Markup memberValue) {
        return memberValue != null
                ? memberValue.asHtml()
                : null;
    }

    @Override
    public Markup convertToEntityAttribute(final String datastoreValue) {
        return datastoreValue != null
                ? new Markup(datastoreValue)
                : null;
    }

}
