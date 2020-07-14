/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.kogito.mongodb.marshalling.document;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

public class VariableCodec<T> implements Codec<T>, CollectibleCodec<T> {

    public static final String TARGET_CLASS_TYPE_NAME = "_type";

    private final Class<T> targetClass;

    private final CodecRegistry codecRegistry;

    public VariableCodec(final Class<T> targetClass, final CodecRegistry codecRegistry) {
        this.targetClass = targetClass;
        this.codecRegistry = codecRegistry;
    }

    public CodecRegistry getCodecRegistry() {
        return this.codecRegistry;
    }

    @Override
    public Class<T> getEncoderClass() {
        return this.targetClass;
    }

    @Override
    public void encode(final BsonWriter writer,
                       final Object object,
                       final EncoderContext encoderContext) {

        Encoder.encode(writer, object, encoderContext, this.codecRegistry);

    }

    @Override
    public T decode(final BsonReader reader, final DecoderContext decoderContext) {
        final Decoder decoder = new Decoder(this.targetClass, this.codecRegistry);
        return decoder.decodeDocument(reader, decoderContext);
    }

    @Override
    public T generateIdIfAbsentFromDocument(T document) {
        return null;
    }

    @Override
    public boolean documentHasId(T document) {
        return false;
    }

    @Override
    public BsonValue getDocumentId(T document) {
        return null;
    }

}
