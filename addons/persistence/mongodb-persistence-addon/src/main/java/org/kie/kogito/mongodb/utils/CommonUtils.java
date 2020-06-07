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

import java.util.stream.Collectors;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jbpm.marshalling.impl.ProcessInstanceData;
import org.kie.kogito.mongodb.codec.ProcessInstanceDocumentCodecProvider;
import org.kie.kogito.mongodb.model.ProcessInstanceDocument;
import org.kie.kogito.mongodb.model.Strategy;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class CommonUtils {

    private static final String KOGITO_STORE = "Kogito_store";

    private CommonUtils() {}

    public static MongoCollection<ProcessInstanceDocument> getCollection(MongoClient mongoClient, String processId) {
        CodecRegistry registry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(PojoCodecProvider.builder().automatic(true).build(), new ProcessInstanceDocumentCodecProvider()));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(KOGITO_STORE).withCodecRegistry(registry);
        return mongoDatabase.getCollection(processId, ProcessInstanceDocument.class).withCodecRegistry(registry);
    }

    public static ProcessInstanceDocument convertToProcessInstanceDoument(ProcessInstanceData data) {
        return new ProcessInstanceDocument(Document.parse(data.getlegacyProcessInstance()), data.getStrategies().entrySet().stream().map(e -> new Strategy(e.getValue(), e.getKey())).collect(Collectors.toList()));
    }

    public static ProcessInstanceData convertToProcessInstanceData(ProcessInstanceDocument pidoc) {
        ProcessInstanceData piData = new ProcessInstanceData();
        piData.setlegacyProcessInstance(pidoc.getProcessInstance().toJson());
        piData.setStrategies(pidoc.getStrategies().stream().collect(Collectors.toMap(Strategy::getStrategyName, Strategy::getStrategyId)));
        return piData;
    }
}
