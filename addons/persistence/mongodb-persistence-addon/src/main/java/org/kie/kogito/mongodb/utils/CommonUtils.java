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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.jbpm.marshalling.impl.ProcessInstanceDocument;
import org.kie.kogito.mongodb.codec.ProcessInstanceModelCodecProvider;
import org.kie.kogito.mongodb.model.ProcessInstanceModel;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class CommonUtils {

    private static final String KOGITO_STORE = "Kogito_store";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CommonUtils() {
    }

    public static MongoCollection<ProcessInstanceModel> getCollection(MongoClient mongoClient, String processId) {

        CodecRegistry registry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(new ProcessInstanceModelCodecProvider()));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(KOGITO_STORE).withCodecRegistry(registry);
        return mongoDatabase.getCollection(processId,
                ProcessInstanceModel.class)
                .withCodecRegistry(registry);
    }

    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    public static ProcessInstanceModel convertProcessInstanceDoument(ProcessInstanceDocument data) {

        return new ProcessInstanceModel(data.getId(), data
                .getContent(),
                Document.parse(data.getLegacyPIJson()), Document.parse(data.getHeader()));
    }

    public static ProcessInstanceDocument convertProcessInstance(ProcessInstanceModel data) {
        ProcessInstanceDocument doc = new ProcessInstanceDocument();
        doc.setId(data.getId());
        doc.setHeader(data.getHeader().toJson());
        doc.setContent(data.getContent());
        doc.setLegacyPIJson(data.getProcessInstance().toJson());
        return doc;
    }
}
