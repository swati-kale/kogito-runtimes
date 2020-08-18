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

package org.kie.kogito.mongodb.marshalling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mongodb.DBObjectCodecProvider;
import com.mongodb.DBRefCodecProvider;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.kogito.mongodb.marshalling.document.ArrayDocument;
import org.kie.kogito.mongodb.marshalling.document.VariableCodec;
import org.kie.kogito.mongodb.utils.DocumentUtils;

public class DocumentMarshallingStrategy implements ObjectMarshallingStrategy {

    public static final CodecRegistry DEFAULT_CODEC_REGISTRY =
            CodecRegistries.fromProviders(new ValueCodecProvider(), new DBRefCodecProvider(),
                                          new DBObjectCodecProvider(), new BsonValueCodecProvider());

    @Override
    public boolean accept(Object object) {
        return object != null;
    }

    @Override
    public Context createContext() {
        return null;
    }

    @Override
    public void write(ObjectOutputStream os, Object object) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object read(ObjectInputStream os) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {

        if (object.getClass().getName().startsWith("java.lang") || object.getClass().getCanonicalName().equals(Date.class.getCanonicalName())) {
            return DocumentUtils.toByteArray(object);

        } else if (object instanceof ArrayList<?>) {
            @SuppressWarnings("rawtypes")
            ArrayDocument<?> arrayDoc = new ArrayDocument<>("custom", (List<?>) object);
            return encode(arrayDoc);
        } else {

            return encode(object);
        }

    }

    private byte[] encode(Object object) {

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final BsonWriter bsonWriter = new JsonWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        final EncoderContext encoderContext =
                EncoderContext.builder().isEncodingCollectibleDocument(true).build();
        VariableCodec<?> d = new VariableCodec<>(object.getClass(), DEFAULT_CODEC_REGISTRY);
        d.encode(bsonWriter, object, encoderContext);
        return outputStream.toByteArray();
    }

    @Override
    public Object unmarshal(String dataType,
                            Context context,
                            ObjectInputStream is,
                            byte[] object,
                            ClassLoader classloader) throws IOException, ClassNotFoundException {
        Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(dataType);

        if (loadClass.getName().startsWith("java.lang") || loadClass.getCanonicalName().equals(Date.class.getCanonicalName())) {
            return DocumentUtils.fromByteArray(dataType, object);

        } else if (dataType.equals("java.util.ArrayList")) {
            VariableCodec<?> codec = new VariableCodec<>(ArrayDocument.class, DEFAULT_CODEC_REGISTRY);

            ArrayDocument<?> arrayDoc = (ArrayDocument<?>) decode(object, codec);
            return arrayDoc.getList();
        } else {
            VariableCodec<?> codec = new VariableCodec<>(loadClass, DEFAULT_CODEC_REGISTRY);
            return decode(object, codec);
        }

    }

    private Object decode(byte[] object, VariableCodec<?> d) {
        final DecoderContext decoderContext = DecoderContext.builder().build();
        final String json = new String(object, StandardCharsets.UTF_8);
        final BsonReader bsonReader = new JsonReader(json);
        return d.decode(bsonReader, decoderContext);
    }
}
