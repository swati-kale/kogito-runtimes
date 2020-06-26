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

package org.kie.kogito.mongodb.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.kie.kogito.mongodb.marshalling.DocumentMarshallingException;
import org.kie.kogito.mongodb.marshalling.DocumentUnmarshallingException;

public class DocumentUtils {

    private DocumentUtils() {}

    public static MongoCollection<Document> getCollection(MongoClient mongoClient, String processId, String dbName) {
        MongoDatabase mongoDatabase = mongoClient.getDatabase(dbName);
        return mongoDatabase.getCollection(processId);
    }

    public static final String VARIABLE = "variable";
    public static final String VALUE = "value";
    public static final String DOCUMENT_ID = "_id";
    public static final String PROCESS_INSTANCE_ID = "id";
    public static final String STRATEGIES = "stategies";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String VERSION_MAJOR = "versionMajor";
    public static final String VERSION_MINOR = "versionMinor";
    public static final String VERSION_REVISION = "versionRevision";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String DOCUMENT_MARSHALLING_ERROR_MSG = "Error while marshalling process instance with id as document : ";
    public static final String DOCUMENT_UNMARSHALLING_ERROR_MSG = "Error while unmarshalling document for process instance with id : ";

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    public static byte[] toByteArray(Object object) {
        String json = null;
        try {
            json = MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new DocumentMarshallingException(e);
        }
        return json.getBytes();
    }

    public static Object fromByteArray(String dataType, byte[] object) {
        try {
            Class<?> loadClass = Thread.currentThread().getContextClassLoader().loadClass(dataType);
            return MAPPER.readValue(new String(object), loadClass);
        } catch (ClassNotFoundException | JsonProcessingException e) {
            throw new DocumentUnmarshallingException(e);
        }
    }
}
