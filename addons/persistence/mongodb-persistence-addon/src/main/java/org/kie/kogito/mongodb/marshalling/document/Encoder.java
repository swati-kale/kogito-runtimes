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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import org.bson.BsonBinary;
import org.bson.BsonWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

public class Encoder {

    public static final String MONGOBD_DOCUMENT_ID = "_id";

    public static final String TARGET_CLASS_TYPE_NAME = "_type";

    private Encoder() {}

    public static void encode(final BsonWriter writer,
                              final Object domainObject,
                              final EncoderContext encoderContext,
                              final CodecRegistry codecRegistry) {
        try {
            writer.writeStartDocument();
            encodeDomainObjectContent(writer, domainObject, encoderContext, codecRegistry);
            writer.writeEndDocument();
        } catch (IllegalArgumentException e) {
            throw new ConversionException(
                                          "Failed to convert following domain object to BSON document: " + domainObject, e);
        } finally {
            writer.flush();
        }
    }

    private static void encodeDomainObject(final BsonWriter writer,
                                           final String documentName,
                                           final Object domainObject,
                                           final EncoderContext encoderContext,
                                           final CodecRegistry codecRegistry) {
        try {
            writer.writeStartDocument(documentName);
            encodeDomainObjectContent(writer, domainObject, encoderContext, codecRegistry);
            writer.writeEndDocument();
        } catch (IllegalArgumentException e) {
            throw new ConversionException(
                                          "Failed to convert following domain object to BSON document: " + domainObject, e);
        } finally {
            writer.flush();
        }
    }

    public static void encodeDomainObjectContent(final BsonWriter writer,
                                                 final Object domainObject,
                                                 final EncoderContext encoderContext,
                                                 final CodecRegistry codecRegistry) {
        final Map<String, Field> bindings =
                MappingService.getInstance().getMappings(domainObject.getClass());

        writer.writeString(TARGET_CLASS_TYPE_NAME, domainObject.getClass().getName());

        bindings.entrySet().stream()
                .forEach(binding -> writeNamedValue(writer, binding.getKey(),
                                                    MappingService.getFieldValue(domainObject, binding.getValue()), encoderContext,
                                                    codecRegistry));
    }

    public static void writeValue(final BsonWriter writer,
                                  final Object value,
                                  final EncoderContext encoderContext,
                                  final CodecRegistry codecRegistry) {
        if (value == null) {
            writer.writeNull();
        } else if (value.getClass().isEnum()) {

            writer.writeString(((Enum<?>) value).name());
        } else if (value instanceof Collection) {
            writer.writeStartArray();
            final Collection<?> values = (Collection<?>) value;
            values.stream().forEach(v -> writeValue(writer, v, encoderContext, codecRegistry));
            writer.writeEndArray();
        } else if (value instanceof byte[]) {
            // binary format
            writer.writeBinaryData(new BsonBinary((byte[]) value));
        } else if (value.getClass().isArray()) {
            writer.writeStartDocument();
            Arrays.asList(value);
            if (value instanceof boolean[]) {
                for (boolean b : (boolean[]) value) {
                    writer.writeBoolean(b);
                }
            }
            writer.writeEndDocument();
        } else {
            writeAtrributeValue(writer, value, encoderContext, codecRegistry);
        }
    }

    private static void writeAtrributeValue(final BsonWriter writer, final Object value, final EncoderContext encoderContext, final CodecRegistry codecRegistry) {
        final String attributeValueClassName = value.getClass().getName();
        switch (attributeValueClassName) {
            case "org.bson.types.ObjectId":
                writer.writeObjectId(MONGOBD_DOCUMENT_ID, (ObjectId) value);
                break;
            case "java.lang.Boolean": // also covers "boolean"

                writer.writeBoolean((boolean) value);
                break;
            case "java.lang.Byte": // covers also "int" which is autoboxed
                final byte byteValue = (byte) value;
                if (byteValue != 0) {
                    writer.writeInt32(byteValue);
                }
                break;
            case "java.lang.Short": // covers also "int" which is autoboxed
                final short shortValue = (short) value;
                if (shortValue != 0) {
                    writer.writeInt32(shortValue);
                }
                break;
            case "java.lang.Character": // covers also "int" which is autoboxed
                final char charValue = (char) value;
                if (charValue != 0) {
                    writer.writeInt32(charValue);
                }
                break;
            case "java.lang.Integer": // covers also "int" which is autoboxed
                final int intValue = (int) value;
                if (intValue != 0) {
                    writer.writeInt32(intValue);
                }
                break;
            case "java.lang.Long": // covers also "long" which is autoboxed
                final long longValue = (long) value;
                if (longValue != 0) {
                    writer.writeInt64(longValue);
                }
                break;
            case "java.lang.Float": // covers also "float" which is autoboxed
                final float floatValue = (float) value;
                if (floatValue != 0) {
                    writer.writeDouble(floatValue);
                }
                break;
            case "java.lang.Double": // covers also "double" which is autoboxed
                final double doubleValue = (double) value;
                if (doubleValue != 0) {
                    writer.writeDouble(doubleValue);
                }
                break;
            case "java.lang.String":
                writer.writeString((String) value);
                break;
            case "java.util.Date":
                writer.writeDateTime(((Date) value).getTime());
                break;
            default:
                encode(writer, value, encoderContext, codecRegistry);
        }
    }

    public static void writeNamedValue(final BsonWriter writer,
                                       final String name,
                                       final Object value,
                                       final EncoderContext encoderContext,
                                       final CodecRegistry codecRegistry) {

        if (value == null) {
            // skip null named values
            return;
        } else

        if (value.getClass().isEnum()) {
            // Enum
            writer.writeString(name, ((Enum<?>) value).name());
        } else if (value instanceof Collection) {
            // List and Sets
            writer.writeStartArray(name);
            final Collection<?> values = (Collection<?>) value;
            values.stream().forEach(v -> writeValue(writer, v, encoderContext, codecRegistry));
            writer.writeEndArray();
        } else if (value instanceof Map) {
            // Maps
            writer.writeStartArray(name);
            @SuppressWarnings("unchecked")
            final Map<String, ?> values = (Map<String, ?>) value;
            values.entrySet().stream().forEach(e -> {
                writer.writeStartDocument();
                writeNamedValue(writer, e.getKey(), e.getValue(), encoderContext, codecRegistry);
                writer.writeEndDocument();
            });
            writer.writeEndArray();
        } else if (value instanceof byte[]) {
            // special case for Binary data stored in byte[]
            final byte[] bytes = (byte[]) value;
            writer.writeBinaryData(name, new BsonBinary(bytes));
        } else if (value.getClass().isArray()) {
            // Arrays
            writer.writeStartArray(name);
            final Object[] values = (Object[]) value;
            Stream.of(values).forEach(v -> writeValue(writer, v, encoderContext, codecRegistry));
            writer.writeEndArray();
        } else {

            writeNamedAttributeValue(writer, name, value, encoderContext, codecRegistry);
        }
    }

    private static void writeNamedAttributeValue(final BsonWriter writer, final String name, final Object value, final EncoderContext encoderContext, final CodecRegistry codecRegistry) {
        final String attributeValueClassName = value.getClass().getName();

        switch (attributeValueClassName) {
            case "java.lang.Boolean": // also covers "boolean"
                final boolean booleanValue = (boolean) value;
                writer.writeBoolean(name, booleanValue);
                break;
            case "java.lang.Byte": // covers also "int" which is autoboxed
                final byte byteValue = (byte) value;
                if (byteValue != 0) {
                    writer.writeInt32(name, byteValue);
                }
                break;
            case "java.lang.Short": // covers also "int" which is autoboxed
                final short shortValue = (short) value;
                if (shortValue != 0) {
                    writer.writeInt32(name, shortValue);
                }
                break;
            case "java.lang.Character": // covers also "int" which is autoboxed
                final char charValue = (char) value;
                if (charValue != 0) {
                    writer.writeInt32(name, charValue);
                }
                break;
            case "java.lang.Integer": // covers also "int" which is autoboxed
                final int intValue = (int) value;
                if (intValue != 0) {
                    writer.writeInt32(name, intValue);
                }
                break;
            case "java.lang.Long": // covers also "long" which is autoboxed
                final long longValue = (long) value;
                if (longValue != 0) {
                    writer.writeInt64(name, longValue);
                }
                break;
            case "java.lang.Float": // covers also "float" which is autoboxed
                final float floatValue = (float) value;
                if (floatValue != 0) {
                    writer.writeDouble(name, floatValue);
                }
                break;
            case "java.lang.Double": // covers also "double" which is autoboxed
                final double doubleValue = (double) value;
                if (doubleValue != 0) {
                    writer.writeDouble(name, doubleValue);
                }
                break;
            case "java.lang.String":
                writer.writeString(name, (String) value);
                break;
            case "java.util.Date":
                writer.writeDateTime(name, ((Date) value).getTime());
                break;

            default:
                encodeDomainObject(writer, name, value, encoderContext, codecRegistry);
        }
    }

}
