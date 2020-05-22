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
import org.jbpm.marshalling.impl.ProcessInstanceDocument;
import org.kie.kogito.mongodb.model.ProcessInstanceModel;
import org.kie.kogito.mongodb.utils.CommonUtils;
import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceDuplicatedException;
import org.kie.kogito.process.impl.AbstractProcessInstance;
import org.kie.kogito.process.impl.marshalling.ProcessInstanceMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes"})
public class PersistProcessInstances<T> implements MutableProcessInstances<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistProcessInstances.class);
    private org.kie.kogito.process.Process<?> process;
    private final MongoCollection<ProcessInstanceModel> collection;
    private ProcessInstanceMarshaller marshaller;

    public PersistProcessInstances(MongoClient mongoClient, org.kie.kogito.process.Process<?> process) {
        this.process = process;
        collection = CommonUtils.getCollection(mongoClient, process.id());
        marshaller = new ProcessInstanceMarshaller(new DocumentMarshallingStrategy());
    }

    @Override
    public Optional<? extends ProcessInstance<T>> findById(String id) {
        ProcessInstanceModel processDoc = collection.find(Filters.eq("id", resolveId(id))).first();
        if (processDoc == null) {
            return Optional.empty();
        }
        ProcessInstanceDocument pidata = CommonUtils.convertProcessInstance(processDoc);
        return Optional
                       .of(marshaller.unmarshalProcessInstanceDocument(pidata, process));
    }

    @Override
    public Collection<? extends ProcessInstance<T>> values() {
        List<ProcessInstance<T>> list = new ArrayList<>();
        FindIterable<ProcessInstanceModel> fi = collection.find();
        MongoCursor<ProcessInstanceModel> cursor = fi.iterator();
        try {
            while (cursor.hasNext()) {
                Optional<? extends ProcessInstance<T>> opt;
                ProcessInstanceModel processDoc = cursor.next();
                ProcessInstanceDocument pidata = CommonUtils.convertProcessInstance(processDoc);
                opt = Optional
                              .of(marshaller.unmarshalProcessInstanceDocument(pidata, process));
                if (opt.get() instanceof ProcessInstance) {
                    ProcessInstance<T> pi = opt.get();
                    list.add(pi);
                }
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void create(String id, ProcessInstance instance) {
        updateStorage(id, instance, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(String id, ProcessInstance instance) {
        updateStorage(id, instance, false);
    }

    protected void updateStorage(String id, ProcessInstance<T> instance, boolean checkDuplicates) {

        String resolvedId = resolveId(id);
        if (isActive(instance)) {
            ProcessInstanceDocument data = marshaller
                                                     .marhsalProcessInstanceForDocument(instance);
            data.setId(resolvedId);
            if (checkDuplicates) {
                ProcessInstanceModel existing = collection.find(Filters.eq("id", resolvedId)).first();
                if (existing != null) {
                    throw new ProcessInstanceDuplicatedException(id);
                } else {
                    ProcessInstanceModel doc = CommonUtils.convertProcessInstanceDoument(data);
                    collection.insertOne(doc);
                }
            } else {
                ProcessInstanceModel doc = CommonUtils.convertProcessInstanceDoument(data);
                collection.replaceOne(Filters.eq("id", resolvedId), doc);
            }
        }
        reloadProcessInstance(instance, resolvedId);
    }

    @Override
    public boolean exists(String id) {
        String resolvedId = resolveId(id);
        ProcessInstanceModel existing = collection.find(Filters.eq("id", resolvedId)).first();
        return existing != null;
    }

    @Override
    public void remove(String id) {
        String resolvedId = resolveId(id);
        collection.deleteOne(Filters.eq("id", resolvedId));
    }

    private void reloadProcessInstance(ProcessInstance<T> instance, String resolvedId) {
        ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
            try {
                ProcessInstanceModel reloaded = collection.find(Filters.eq("id", resolvedId)).first();
                if (reloaded != null) {
                    ProcessInstanceDocument pidata = CommonUtils.convertProcessInstance(reloaded);
                    return ((AbstractProcessInstance<?>) marshaller.unmarshalProcessInstanceDocument(pidata, process,
                                                                                                     (AbstractProcessInstance<?>) instance)).internalGetProcessInstance();
                }
            } catch (RuntimeException e) {
                LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
                return null;
            }
            return null;
        });
    }
}
