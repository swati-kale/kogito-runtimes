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
import org.kie.kogito.Model;
import org.kie.kogito.mongodb.marshalling.DocumentMarshallingStrategy;
import org.kie.kogito.mongodb.marshalling.DocumentProcessInstanceMarshaller;
import org.kie.kogito.mongodb.model.ProcessInstanceDocument;
import org.kie.kogito.persistence.transaction.TransactionExecutor;
import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceConcurrencyException;
import org.kie.kogito.process.ProcessInstanceDuplicatedException;
import org.kie.kogito.process.ProcessInstanceReadMode;
import org.kie.kogito.process.impl.AbstractProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.kie.kogito.mongodb.utils.DocumentConstants.DOCUMENT_ID;
import static org.kie.kogito.mongodb.utils.DocumentConstants.VERSION;
import static org.kie.kogito.mongodb.utils.DocumentUtils.getCollection;
import static org.kie.kogito.process.ProcessInstanceReadMode.MUTABLE;

public class MongoDBProcessInstances<T extends Model> implements MutableProcessInstances<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBProcessInstances.class);
    private org.kie.kogito.process.Process<?> process;
    private DocumentProcessInstanceMarshaller marshaller;
    private final MongoCollection<ProcessInstanceDocument> collection;
    private TransactionExecutor transactionExecutor;

    public MongoDBProcessInstances(MongoClient mongoClient, org.kie.kogito.process.Process<?> process, String dbName, TransactionExecutor transactionExecutor) {
        this.process = process;
        collection = getCollection(mongoClient, process.id(), dbName);
        marshaller = new DocumentProcessInstanceMarshaller(new DocumentMarshallingStrategy());
        this.transactionExecutor = transactionExecutor;
    }

    @Override
    public Optional<ProcessInstance<T>> findById(String id, ProcessInstanceReadMode mode) {
        ProcessInstanceDocument piDoc = find(id);
        if (piDoc == null) {
            return Optional.empty();
        }
        return Optional.of(mode == MUTABLE ? marshaller.unmarshallProcessInstance(piDoc, process) : marshaller.unmarshallReadOnlyProcessInstance(piDoc, process));
    }

    @Override
    public Collection<ProcessInstance<T>> values(ProcessInstanceReadMode mode) {
        FindIterable<ProcessInstanceDocument> docs = Optional.ofNullable(transactionExecutor).map(TransactionExecutor::getResource)
                .map(r -> collection.find((ClientSession) r))
                .orElseGet(collection::find);
        List<ProcessInstance<T>> list = new ArrayList<>();
        try (MongoCursor<ProcessInstanceDocument> cursor = docs.iterator()) {
            while (cursor.hasNext()) {
                list.add(mode == MUTABLE ? marshaller.unmarshallProcessInstance(cursor.next(), process) : marshaller.unmarshallReadOnlyProcessInstance(cursor.next(), process));
            }
        }
        return list;
    }

    @Override
    public void create(String id, ProcessInstance<T> instance) {
        updateStorage(id, instance, true);
    }

    @Override
    public void update(String id, ProcessInstance<T> instance) {
        updateStorage(id, instance, false);
    }

    protected void updateStorage(String id, ProcessInstance<T> instance, boolean checkDuplicates) {
        if (isActive(instance)) {
            ClientSession clientSession = Optional.ofNullable(transactionExecutor).map(t -> (ClientSession) t.getResource()).orElse(null);
            ProcessInstanceDocument doc = marshaller.marshalProcessInstance(instance);
            if (checkDuplicates) {
                if (exists(id)) {
                    throw new ProcessInstanceDuplicatedException(id);
                } else {
                    if (clientSession != null) {
                        collection.insertOne(clientSession, doc);
                    } else {
                        collection.insertOne(doc);
                    }
                }
            } else {
                long version = doc.getVersion();
                doc.getProcessInstance().put(VERSION, version + 1);
                doc.setVersion(version + 1);
                if (clientSession != null) {
                    ProcessInstanceDocument piDoc = collection.findOneAndReplace(clientSession,Filters.and(Filters.eq(DOCUMENT_ID, id),Filters.eq(VERSION, version)), doc);
                    if (piDoc==null) {
                        throw new ProcessInstanceConcurrencyException("The document with ID: "+id+" was updated or deleted by other request.");
                    } 
                } else {
                    collection.replaceOne(Filters.and(Filters.eq(DOCUMENT_ID, id),Filters.eq(VERSION, version)), doc);
                }
            }
        }
        reloadProcessInstance(instance, id);
    }

    private ProcessInstanceDocument find(String id) {
        return Optional.ofNullable(transactionExecutor).map(TransactionExecutor::getResource)
                .map(r -> collection.find((ClientSession) r, Filters.eq(DOCUMENT_ID, id)).first())
                .orElseGet(() -> collection.find(Filters.eq(DOCUMENT_ID, id)).first());
    }

    @Override
    public boolean exists(String id) {
        return find(id) != null;
    }

    @Override
    public void remove(String id) {
        ClientSession clientSession = Optional.ofNullable(transactionExecutor).map(t -> (ClientSession) t.getResource()).orElse(null);
        if (clientSession != null) {
            collection.deleteOne(clientSession, Filters.eq(DOCUMENT_ID, id));
        } else {
            collection.deleteOne(Filters.eq(DOCUMENT_ID, id));
        }
    }
    
    @Override
    public void removeByVersion(String id, ProcessInstance<T> instance) {
        ClientSession clientSession = Optional.ofNullable(transactionExecutor).map(t -> (ClientSession) t.getResource()).orElse(null);
        if (clientSession != null) {
            ProcessInstanceDocument piDoc = collection. findOneAndDelete(clientSession,Filters.and(Filters.eq(DOCUMENT_ID, id),Filters.eq(VERSION, instance.version())));
            if (piDoc==null) {
                throw new ProcessInstanceConcurrencyException("The document with ID: "+id+" was updated or deleted by other request.");
            }
        } else {
            collection.deleteOne(Filters.and(Filters.eq(DOCUMENT_ID, id),Filters.eq(VERSION, instance.version())));
        }
    }

    private void reloadProcessInstance(ProcessInstance<T> instance, String id) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
            try {
                ProcessInstanceDocument reloaded = find(id);
                if (reloaded != null) {
                    return marshaller.unmarshallWorkflowProcessInstance(reloaded, process);
                }
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
            }
            return null;
        });
    }

    @Override
    public Integer size() {
        return Optional.ofNullable(transactionExecutor).map(TransactionExecutor::getResource)
                .map(r -> (int) collection.countDocuments((ClientSession) r))
                .orElseGet(() -> (int) collection.countDocuments());
    }
}
