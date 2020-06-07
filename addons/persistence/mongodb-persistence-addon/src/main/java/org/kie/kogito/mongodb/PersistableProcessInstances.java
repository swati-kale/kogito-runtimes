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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.drools.core.marshalling.impl.DocumentMarshallingStrategy;
import org.jbpm.marshalling.impl.ProcessInstanceData;
import org.kie.kogito.Model;
import org.kie.kogito.mongodb.model.ProcessInstanceDocument;
import org.kie.kogito.mongodb.utils.CommonUtils;
import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceDuplicatedException;
import org.kie.kogito.process.impl.AbstractProcessInstance;
import org.kie.kogito.process.impl.marshalling.ProcessInstanceMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistableProcessInstances<T extends Model> implements MutableProcessInstances<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistableProcessInstances.class);
    private org.kie.kogito.process.Process<?> process;
    private final MongoCollection<ProcessInstanceDocument> collection;
    private ProcessInstanceMarshaller marshaller;
    private static final String PROCESS_INSTANCE_ID = "processInstance.id";

    public PersistableProcessInstances(MongoClient mongoClient, org.kie.kogito.process.Process<?> process) {
        this.process = process;
        collection = CommonUtils.getCollection(mongoClient, process.id());
        marshaller = new ProcessInstanceMarshaller(new DocumentMarshallingStrategy());
    }

    @Override
    public Optional<? extends ProcessInstance<T>> findById(String id) {
        ProcessInstanceDocument piDoc = collection.find(Filters.eq(PROCESS_INSTANCE_ID, resolveId(id))).first();
        if (piDoc == null) {
            return Optional.empty();
        }
        ProcessInstanceData pidata = CommonUtils.convertToProcessInstanceData(piDoc);
        return Optional.of(marshaller.unmarshalProcessInstanceDocument(pidata, process));
    }

    @Override
    public Collection<? extends ProcessInstance<T>> values() {
        List<ProcessInstance<T>> list = new ArrayList<>();
        FindIterable<ProcessInstanceDocument> fi = collection.find();
        try (MongoCursor<ProcessInstanceDocument> cursor = fi.iterator()) {
            while (cursor.hasNext()) {
                ProcessInstanceDocument piDoc = cursor.next();
                Optional<? extends ProcessInstance<T>> opt = Optional.of(marshaller.unmarshalProcessInstanceDocument(CommonUtils.convertToProcessInstanceData(piDoc), process));
                list.add(opt.get());
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

        String resolvedId = resolveId(id);
        if (isActive(instance)) {
            ProcessInstanceData data = marshaller.marshalProcessInstanceToDocument(instance);
            if (checkDuplicates) {
                ProcessInstanceDocument existing = collection.find(Filters.eq(PROCESS_INSTANCE_ID, resolvedId)).first();
                if (existing != null) {
                    throw new ProcessInstanceDuplicatedException(id);
                } else {
                    ProcessInstanceDocument doc = CommonUtils.convertToProcessInstanceDoument(data);
                    collection.insertOne(doc);
                }
            } else {
                ProcessInstanceDocument doc = CommonUtils.convertToProcessInstanceDoument(data);
                collection.replaceOne(Filters.eq(PROCESS_INSTANCE_ID, resolvedId), doc);
            }
        }
        reloadProcessInstance(instance, resolvedId);
    }

    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        ProcessInstanceDocument existing = collection.find(Filters.eq(PROCESS_INSTANCE_ID, resolvedId)).first();
        return existing != null;
    }

    @Override
    public void remove(String id) {
        String resolvedId = resolveId(id);
        collection.deleteOne(Filters.eq(PROCESS_INSTANCE_ID, resolvedId));
    }

    @SuppressWarnings("unchecked")
    private void reloadProcessInstance(ProcessInstance<T> instance, String resolvedId) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
            try {
                ProcessInstanceDocument reloaded = collection.find(Filters.eq(PROCESS_INSTANCE_ID, resolvedId)).first();
                if (reloaded != null) {
                    ProcessInstanceData pidata = CommonUtils.convertToProcessInstanceData(reloaded);
                    return ((AbstractProcessInstance<T>) marshaller.unmarshalProcessInstanceDocument(pidata, process, (AbstractProcessInstance<T>) instance)).internalGetProcessInstance();
                }
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
            }
            return null;
        });
    }
}
