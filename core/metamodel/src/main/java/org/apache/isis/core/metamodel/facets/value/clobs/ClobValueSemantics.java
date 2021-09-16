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
package org.apache.isis.core.metamodel.facets.value.clobs;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.springframework.stereotype.Component;

import org.apache.isis.applib.adapters.AbstractValueSemanticsProvider;
import org.apache.isis.applib.adapters.EncoderDecoder;
import org.apache.isis.applib.adapters.Renderer;
import org.apache.isis.applib.adapters.ValueSemanticsProvider;
import org.apache.isis.applib.value.Clob;

@Component
public class ClobValueSemantics
extends AbstractValueSemanticsProvider<Clob>
implements
    EncoderDecoder<Clob>,
    Renderer<Clob> {

    // RENDERER

    @Override
    public String presentationValue(final ValueSemanticsProvider.Context context, final Clob value) {
        return render(value, Clob::getName);
    }

    // -- ENCODER DECODER

    @Override
    public String toEncodedString(final Clob clob) {
        return clob.getName() + ":" + clob.getMimeType().getBaseType() + ":" + clob.getChars();
    }

    @Override
    public Clob fromEncodedString(final String data) {
        final int colonIdx = data.indexOf(':');
        final String name  = data.substring(0, colonIdx);
        final int colon2Idx  = data.indexOf(":", colonIdx+1);
        final String mimeTypeBase = data.substring(colonIdx+1, colon2Idx);
        final CharSequence chars = data.substring(colon2Idx+1);
        try {
            return new Clob(name, new MimeType(mimeTypeBase), chars);
        } catch (MimeTypeParseException e) {
            throw new RuntimeException(e);
        }
    }

}