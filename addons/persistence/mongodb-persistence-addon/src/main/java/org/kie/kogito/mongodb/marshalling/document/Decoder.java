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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonElement;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Decoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Decoder.class);

    private final CodecRegistry codecRegistry;

    private final Class<?> targetClass;

    public Decoder(final Class<?> targetClass, final CodecRegistry codecRegistry) {
        this.targetClass = targetClass;
        this.codecRegistry = codecRegistry;
    }

    public <T> T decodeDocument(final BsonReader reader,
                                final DecoderContext decoderContext) {
        final Map<String, BsonElement> keyValuePairs = new HashMap<>();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            keyValuePairs.put(fieldName,
                              new BsonElement(fieldName, readBsonValue(reader, decoderContext)));
        }
        reader.readEndDocument();

        // now, convert the map of key-value pairs into an instance of the target document
        final T domainDocument = getTargetClass(keyValuePairs);
        final Map<String, Field> bindings =
                MappingService.getInstance().getMappings(domainDocument.getClass());
        for (Iterator<String> iterator = keyValuePairs.keySet().iterator(); iterator.hasNext();) {
            final String key = iterator.next();
            final Field field = bindings.get(key);
            if (field == null) {
                LOGGER.debug("Field '{}' does not exist in class '{}'", key, domainDocument.getClass());
                continue;
            }
            final Object fieldValue = getValue(keyValuePairs.get(key), field.getType());
            try {
                field.setAccessible(true);
                field.set(domainDocument, fieldValue);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new ConversionException("Unable to set value '" + fieldValue + "' to field '" + domainDocument.getClass().getName() + "." + field.getName() + "'", e);
            }
        }
        return domainDocument;
    }

    @SuppressWarnings("unchecked")
    private <T> T getTargetClass(final Map<String, BsonElement> keyValuePairs) {
        final String targetClassName = getTargetClassName(keyValuePairs);
        try {
            return (T) Thread.currentThread().getContextClassLoader().loadClass(targetClassName).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new ConversionException("Failed to create a new instance of '" + targetClassName + "'",
                                          e);
        }
    }

    private String getTargetClassName(final Map<String, BsonElement> keyValuePairs) {
        if (keyValuePairs.containsKey(Encoder.TARGET_CLASS_TYPE_NAME)) {
            final BsonElement targetClassElement = keyValuePairs.get(Encoder.TARGET_CLASS_TYPE_NAME);
            return targetClassElement.getValue().asString().getValue();
        }
        return targetClass.getName();
    }

    private Object convert(final Object value, final Class<?> targetType) {
        if(value.getClass().isAssignableFrom(targetType)) {
            return value;
         }

        if (targetType.isEnum() && value instanceof String) {
            for (Object e : targetType.getEnumConstants()) {
                if (e.toString().equals(value)) {
                    return e;
                }
            }
        } else if (value instanceof BsonDocument) {
            try (final BsonDocumentReader reader = new BsonDocumentReader((BsonDocument) value)) {
                return decodeDocument(reader, DecoderContext.builder().build());
            }
        } else if (List.class.isAssignableFrom(targetType)) {
            return (value);
        } else if (Set.class.isAssignableFrom(targetType)) {
            return value;
        } else if (Map.class.isAssignableFrom(targetType)) {
            return value;
        } else if (targetType.isArray()) {
            return value;
        } else {

            switch (targetType.getName()) {
                case "boolean":
                case "java.lang.Boolean":
                    return Boolean.parseBoolean(value.toString());
                case "byte":
                case "java.lang.Byte":
                    return Byte.parseByte(value.toString());
                case "short":
                case "java.lang.Short":
                    return Short.parseShort(value.toString());
                case "int":
                case "java.lang.Integer":
                    return Integer.parseInt(value.toString());
                case "long":
                case "java.lang.Long":
                    return Long.parseLong(value.toString());
                case "float":
                case "java.lang.Float":
                    return Float.parseFloat(value.toString());
                case "double":
                case "java.lang.Double":
                    return Double.parseDouble(value.toString());
                case "java.lang.String":
                    return value.toString();
                case "org.bson.types.ObjectId":
                    return new ObjectId(value.toString());
                case "java.util.Date":
                    return new Date((long) value);
                default:
                    // will throw ConversionException
                    break;
            }
        }
        throw new ConversionException("Unable to convert value '" + value + "' to type " + targetType.getName());
    }

    protected Object getValue(final BsonElement bsonElement, final Class<?> expectedType) {
        if (bsonElement != null) {
            final Object bsonValue = decodeValue(bsonElement.getValue(), expectedType);
            return convert(bsonValue, expectedType);
        }
        return null;
    }

    public Object decodeValue(final BsonValue bsonValue, final Class<?> expectedType) {
        if (bsonValue != null) {
            switch (bsonValue.getBsonType()) {
                case ARRAY:
                    if (List.class.isAssignableFrom(expectedType)) {
                        return bsonValue.asArray().getValues().stream().map(v -> decodeValue(v, null))
                                        .collect(Collectors.toList());
                    } else if (Set.class.isAssignableFrom(expectedType)) {
                        return bsonValue.asArray().getValues().stream().map(v -> decodeValue(v, null))
                                        .collect(Collectors.toSet());
                    } else if (Map.class.isAssignableFrom(expectedType)) {
                        return bsonValue.asArray().getValues().stream()
                                        .collect(Collectors.<BsonValue, Object, Object, TreeMap<Object, Object>> toMap(
                                                                                                                       k -> readFirstName(k),
                                                                                                                       v -> readFirstValue(v),
                                                                                                                       (k1, k2) -> {
                                                                                                                           throw new IllegalStateException(String.format("Duplicate key %s", k1));
                                                                                                                       },
                                                                                                                       TreeMap::new));
                    } else if (expectedType.isArray()) {
                        return bsonValue.asArray().getValues().stream()
                                        .map(v -> decodeValue(v, expectedType.getComponentType())).toArray(
                                                                                                           size -> (Object[]) Array.newInstance(expectedType.getComponentType(), size));
                    }
                    break;
                case BINARY:
                    return bsonValue.asBinary().getData();
                case BOOLEAN:
                    return bsonValue.asBoolean().getValue();
                case DATE_TIME:
                    return bsonValue.asDateTime().getValue();
                case DB_POINTER:
                    return bsonValue.asDBPointer().getId();
                case DOUBLE:
                    return bsonValue.asDouble().getValue();
                case INT32:
                    return bsonValue.asInt32().getValue();
                case INT64:
                    return bsonValue.asInt64().getValue();
                case JAVASCRIPT:
                    return bsonValue.asJavaScript().getCode();
                case JAVASCRIPT_WITH_SCOPE:
                    return bsonValue.asJavaScriptWithScope().getCode();
                case NULL:
                    return null;
                case OBJECT_ID:
                    return bsonValue.asObjectId().getValue();
                case REGULAR_EXPRESSION:
                    return bsonValue.asRegularExpression().getPattern();
                case STRING:
                    return bsonValue.asString().getValue();
                case SYMBOL:
                    return bsonValue.asSymbol().getSymbol();
                case TIMESTAMP:
                    return bsonValue.asTimestamp().getTime();
                case DOCUMENT:
                    final BsonDocument bsonDocument = (BsonDocument) bsonValue;
                    return new VariableCodec<>(expectedType, codecRegistry).decode(new BsonDocumentReader(bsonDocument), DecoderContext.builder().build());
                default:
                    throw new ConversionException("Unexpected BSON Element value of type '" + bsonValue.getBsonType() + "'");
            }
        }
        return null;
    }

    public BsonValue readBsonValue(final BsonReader reader, final DecoderContext decoderContext) {
        final Class<? extends BsonValue> classForBsonType =
                BsonValueCodecProvider.getClassForBsonType(reader.getCurrentBsonType());
        final Codec<? extends BsonValue> codec = codecRegistry.get(classForBsonType);
        return codec.decode(reader, decoderContext);
    }

    private static Object readFirstName(final BsonValue bsonValue) {
        if (bsonValue == null) {
            throw new ConversionException("Expected a BsonDocument but the given value was a null");

        } else if (!bsonValue.isDocument()) {
            throw new ConversionException(
                                          "Expected a BsonDocument but the given value was a " + bsonValue.getBsonType().name());
        }
        try (final BsonDocumentReader reader = new BsonDocumentReader((BsonDocument) bsonValue)) {
            reader.readStartDocument();
            return reader.readName();
        }
    }

    private Object readFirstValue(final BsonValue bsonValue) {
        if (bsonValue == null) {
            throw new ConversionException("Expected a BsonDocument but the given value was a null");

        } else if (!bsonValue.isDocument()) {
            throw new ConversionException(
                                          "Expected a BsonDocument but the given value was a " + bsonValue.getBsonType().name());
        }
        try (final BsonDocumentReader reader = new BsonDocumentReader((BsonDocument) bsonValue)) {
            reader.readStartDocument();
            reader.readName();
            return decodeValue(readBsonValue(reader, DecoderContext.builder().build()), null);

        }
    }

}
