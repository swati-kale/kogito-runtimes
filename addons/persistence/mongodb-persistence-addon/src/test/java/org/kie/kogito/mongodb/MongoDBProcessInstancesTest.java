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

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.drools.core.io.impl.ClassPathResource;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.junit.jupiter.api.Test;
import org.kie.kogito.mongodb.model.ProcessInstanceDocument;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.persistence.transaction.TransactionExecutor;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceReadMode;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.process.bpmn2.BpmnProcessInstance;
import org.kie.kogito.process.bpmn2.BpmnVariables;
import org.kie.kogito.process.impl.AbstractProcessInstance;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.kogito.mongodb.utils.DocumentConstants.DOCUMENT_ID;
import static org.mockito.Mockito.*;

class MongoDBProcessInstancesTest extends TestHelper {

    @Test
    void testMongoDBPersistence() {
        BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
        process.setProcessInstancesFactory(new MongoDBProcessInstancesFactory(getMongoClient()));
        process.configure();

        ProcessInstance<BpmnVariables> processInstance = process.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertEquals(STATE_ACTIVE, processInstance.status());

        MongoDBProcessInstances<?> mongodbInstance = new MongoDBProcessInstances<>(getMongoClient(), process, DB_NAME, null);

        assertThat(mongodbInstance.size()).isOne();
        assertThat(mongodbInstance.size()).isEqualTo(process.instances().size());

        Optional<?> findById = mongodbInstance.findById(processInstance.id());
        BpmnProcessInstance found = (BpmnProcessInstance) findById.get();
        assertNotNull(found, "ProcessInstanceDocument cannot be null");
        assertThat(found.id()).isEqualTo(processInstance.id());
        assertThat(found.description()).isEqualTo("User Task");
        assertThat(found.variables().toMap()).containsExactly(entry("test", "test"));
        assertThat(mongodbInstance.exists(processInstance.id())).isTrue();
        assertThat(mongodbInstance.values().size()).isOne();

        ProcessInstance<?> readOnlyPI = mongodbInstance.findById(processInstance.id(), ProcessInstanceReadMode.READ_ONLY).get();
        assertNotNull(readOnlyPI, "ProcessInstanceDocument cannot be null");
        assertThat(mongodbInstance.values(ProcessInstanceReadMode.READ_ONLY).size()).isOne();

        mongodbInstance.remove(processInstance.id());
        assertThat(mongodbInstance.exists(processInstance.id())).isFalse();
        assertThat(mongodbInstance.values()).isEmpty();
    }

    @Test
    void testMongoDBPersistence_withTransaction() {
        TransactionExecutor transactionExecutor = mock(TransactionExecutor.class);
        ClientSession clientSession = mock(ClientSession.class);
        when(transactionExecutor.getResource()).thenReturn(clientSession);

        MongoClient mongoClient = mock(MongoClient.class);
        MongoDatabase mongoDatabase = mock(MongoDatabase.class);
        MongoCollection<ProcessInstanceDocument> mongoCollection = mock(MongoCollection.class);
        when(mongoClient.getDatabase(anyString())).thenReturn(mongoDatabase);
        when(mongoDatabase.withCodecRegistry(any())).thenReturn(mongoDatabase);
        when(mongoDatabase.getCollection(anyString(), eq(ProcessInstanceDocument.class))).thenReturn(mongoCollection);
        when(mongoCollection.withCodecRegistry(any())).thenReturn(mongoCollection);

        MongoCursor<ProcessInstanceDocument> cursor = mock(MongoCursor.class);
        when(cursor.hasNext()).thenReturn(false);
        FindIterable<ProcessInstanceDocument> results = mock(FindIterable.class);
        when(results.first()).thenReturn(null);
        when(results.iterator()).thenReturn(cursor);
        when(mongoCollection.find(eq(clientSession), any(Bson.class))).thenReturn(results);
        when(mongoCollection.find(eq(clientSession))).thenReturn(results);
        when(mongoCollection.find(any(Bson.class))).thenReturn(results);
        when(mongoCollection.find()).thenReturn(results);

        String id = "testId";

        BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
        process.setProcessInstancesFactory(new MongoDBProcessInstancesFactory(getMongoClient()));
        process.configure();

        MongoDBProcessInstances<BpmnVariables> mongodbInstance = new MongoDBProcessInstances<>(mongoClient, process, DB_NAME, transactionExecutor);

        mongodbInstance.size();
        verify(mongoCollection, times(1)).countDocuments(eq(clientSession));

        mongodbInstance.findById(id, ProcessInstanceReadMode.READ_ONLY);
        verify(mongoCollection, times(1)).find(eq(clientSession), eq(Filters.eq(DOCUMENT_ID, id)));

        mongodbInstance.exists(id);
        verify(mongoCollection, times(2)).find(eq(clientSession), eq(Filters.eq(DOCUMENT_ID, id)));

        mongodbInstance.values(ProcessInstanceReadMode.READ_ONLY);
        verify(mongoCollection, times(1)).find(eq(clientSession));

        mongodbInstance.remove(id);
        verify(mongoCollection, times(1)).deleteOne(eq(clientSession), eq(Filters.eq(DOCUMENT_ID, id)));

        WorkflowProcessInstance updatePi = ((AbstractProcessInstance<?>) process.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")))).internalGetProcessInstance();
        updatePi.setId(id);
        updatePi.setStartDate(new Date());

        AbstractProcessInstance mockUpdateProcessInstance = mock(AbstractProcessInstance.class);
        when(mockUpdateProcessInstance.status()).thenReturn(ProcessInstance.STATE_ACTIVE);
        when(mockUpdateProcessInstance.internalGetProcessInstance()).thenReturn(updatePi);

        mongodbInstance.update(id, mockUpdateProcessInstance);
        verify(mongoCollection, times(1)).replaceOne(eq(clientSession), eq(Filters.eq(DOCUMENT_ID, id)), any());

        WorkflowProcessInstance createPi = ((AbstractProcessInstance<?>) process.createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")))).internalGetProcessInstance();
        createPi.setId(id);
        createPi.setStartDate(new Date());

        AbstractProcessInstance mockCreateProcessInstance = mock(AbstractProcessInstance.class);
        when(mockCreateProcessInstance.status()).thenReturn(ProcessInstance.STATE_ACTIVE);
        when(mockCreateProcessInstance.internalGetProcessInstance()).thenReturn(createPi);

        mongodbInstance.create(id, mockCreateProcessInstance);
        verify(mongoCollection, times(1)).insertOne(eq(clientSession), any());
    }

    private class MongoDBProcessInstancesFactory extends KogitoProcessInstancesFactory {

        public MongoDBProcessInstancesFactory(MongoClient mongoClient) {
            super(mongoClient);
        }

        @Override
        public String dbName() {
            return DB_NAME;
        }

        @Override
        public TransactionExecutor transactionExecutor() {
            return null;
        }
    }

}
