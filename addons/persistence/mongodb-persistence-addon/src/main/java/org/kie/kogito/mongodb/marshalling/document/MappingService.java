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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MappingService {

    private final Map<Class<?>, Map<String, Field>> mappings = new HashMap<>();

    private static final MappingService instance = new MappingService();

    public static MappingService getInstance() {
        return instance;
    }

    private MappingService() {
        super();
    }

    public static Object getFieldValue(final Object domainObject, final Field domainField) {
        domainField.setAccessible(true);
        try {
            return domainField.get(domainObject);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new ConversionException("Failed to retrieve value for field '" + domainField.getName() + "' in domain object '" + domainObject + "'", e);
        }
    }

    public Map<String, Field> getMappings(final Class<?> targetClass) {
        if (!mappings.containsKey(targetClass)) {
            final Map<String, Field> classMappings = new TreeMap<>();

            for (Field field : targetClass.getDeclaredFields()) {
                classMappings.put(field.getName(), field);
            }
            mappings.put(targetClass, classMappings);
        }
        return Collections.unmodifiableMap(mappings.get(targetClass));
    }

}
