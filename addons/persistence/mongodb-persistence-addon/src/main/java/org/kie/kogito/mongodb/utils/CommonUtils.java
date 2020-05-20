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

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jbpm.marshalling.impl.ProcessInstanceDocument;
import org.kie.kogito.mongodb.model.ProcessInstanceModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class CommonUtils {

	private static MongoDatabase mongoDatabase;
	private static MongoCollection<ProcessInstanceModel> collection;
	private static final String KOGITO_STORE = "Kogito_store";

	private static ObjectMapper MAPPER = new ObjectMapper();

	public static MongoCollection<ProcessInstanceModel> getCollection(MongoClient mongoClient, String processId) {

		PojoCodecProvider provider = PojoCodecProvider.builder().register(ProcessInstanceModel.class)
				.register(org.jbpm.marshalling.impl.ProcessInstanceDocument.class).automatic(true)
				.build();
		CodecRegistry registry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				fromProviders(provider));
		mongoDatabase = mongoClient.getDatabase(KOGITO_STORE).withCodecRegistry(registry);
		collection = mongoDatabase.getCollection(processId + "_store", ProcessInstanceModel.class)
				.withCodecRegistry(registry);
		return collection;
	}

	public static ObjectMapper getObjectMapper() {
		return MAPPER;
	}

	public static ProcessInstanceModel convertProcessInstanceDoument(ProcessInstanceDocument data) {

		ProcessInstanceModel model = new ProcessInstanceModel(data.getId(), data.getPid(), data.getContent(),
				Document.parse(data.getLegacyPIJson()), Document.parse(data.getHeader()));
		return model;
	}

	public static ProcessInstanceDocument convertProcessInstance(ProcessInstanceModel data) {
		ProcessInstanceDocument doc = new ProcessInstanceDocument();
		doc.setId(data.getPid());
		doc.setPid(data.getPid());
		doc.setHeader(data.getHeader().toJson());
		doc.setContent(data.getContent());
		doc.setLegacyPIJson(data.getProcessInstance().toJson());
		return doc;
	}

}
