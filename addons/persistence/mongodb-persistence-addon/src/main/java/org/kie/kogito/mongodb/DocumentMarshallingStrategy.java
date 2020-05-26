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

package org.kie.kogito.mongodb;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.kie.api.marshalling.MarshallingException;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.marshalling.UnmarshallingException;
import org.kie.kogito.mongodb.utils.CommonUtils;

public class DocumentMarshallingStrategy implements ObjectMarshallingStrategy {

    @Override
    public String marshalToJson(Object object) {
        try {
            return CommonUtils.getObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new MarshallingException(e);
        }
    }

    @Override
    public Object unmarshalFromJson(String dataType, String json) {
        try {
            Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(dataType);
            return CommonUtils.getObjectMapper().readValue(json, loadClass);
        } catch (ClassNotFoundException | JsonProcessingException e) {
            throw new UnmarshallingException(e);
        }
    }

    @Override
    public boolean accept(Object object) {
        return object != null;
    }

    @Override
    public Context createContext() {
        return null;
    }

    @Override
    public void write(ObjectOutputStream os, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object read(ObjectInputStream os) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] marshal(Context context, ObjectOutputStream os, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object unmarshal(String dataType,
                            Context context,
                            ObjectInputStream is,
                            byte[] object,
                            ClassLoader classloader) {
        throw new UnsupportedOperationException();
    }
}
