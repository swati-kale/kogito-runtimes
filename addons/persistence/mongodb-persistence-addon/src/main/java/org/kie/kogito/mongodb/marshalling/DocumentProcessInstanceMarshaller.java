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

package org.kie.kogito.mongodb.marshalling;

import java.util.Collections;

import com.google.protobuf.util.JsonFormat;
import org.bson.Document;
import org.drools.core.impl.EnvironmentImpl;
import org.drools.core.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.drools.core.marshalling.impl.ProcessMarshallerWriteContext;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.jbpm.marshalling.impl.AbstractProtobufProcessInstanceMarshaller;
import org.jbpm.marshalling.impl.JBPMMessages;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstance;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.kogito.Model;
import org.kie.kogito.mongodb.utils.ProcessInstanceDocumentMapper;
import org.kie.kogito.mongodb.utils.ProcessInstanceMessageMapper;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.impl.AbstractProcess;
import org.kie.kogito.process.impl.AbstractProcessInstance;

import static org.kie.kogito.mongodb.utils.DocumentUtils.DOCUMENT_MARSHALLING_ERROR_MSG;
import static org.kie.kogito.mongodb.utils.DocumentUtils.DOCUMENT_UNMARSHALLING_ERROR_MSG;

public class DocumentProcessInstanceMarshaller extends AbstractProtobufProcessInstanceMarshaller {

    private Environment env = new EnvironmentImpl();

    public DocumentProcessInstanceMarshaller(ObjectMarshallingStrategy... strategies) {
        ObjectMarshallingStrategy[] strats = null;
        if (strategies == null) {
            strats = new ObjectMarshallingStrategy[]{new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT)};
        } else {
            strats = new ObjectMarshallingStrategy[strategies.length + 1];
            int i = 0;
            for (ObjectMarshallingStrategy strategy : strategies) {
                strats[i] = strategy;
                i++;
            }
            strats[i] = new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT);
        }
        env.set(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, strats);
    }

    @Override
    protected WorkflowProcessInstanceImpl createProcessInstance() {
        return new RuleFlowProcessInstance();
    }

    public Document marshalProcessInstance(ProcessInstance<?> processInstance) {

        try {
            org.kie.api.runtime.process.ProcessInstance legacyProcessInstance = ((AbstractProcessInstance<?>) processInstance).internalGetProcessInstance();
            ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext(null, null, null, null, env);
            context.parameterObject = JsonFormat.printer().print(super.writeProcessInstance(context, legacyProcessInstance));
            Document document = new ProcessInstanceDocumentMapper().apply(context);
            ((WorkflowProcessInstanceImpl) legacyProcessInstance).disconnect();
            return document;
        } catch (Exception e) {
            throw new DocumentMarshallingException(processInstance.id(), e, DOCUMENT_MARSHALLING_ERROR_MSG);
        }
    }

    public <T extends Model> ProcessInstance<T> unmarshallProcessInstance(Document doc, Process<?> process) {
        Model m = (Model) process.createModel();
        AbstractProcessInstance<?> processInstance = (AbstractProcessInstance<?>) process.createInstance(m);
        return readProcessInstance(doc, process, processInstance);
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> ProcessInstance<T> readProcessInstance(Document doc, Process<?> process, AbstractProcessInstance<?> processInstance) {
        try {
            MarshallerReaderContext context = new MarshallerReaderContext(Collections.singletonMap(process.id(), ((AbstractProcess<?>) process).process()), null, null, null, env, null);
            context.parameterObject = doc;
            JBPMMessages.ProcessInstance instance = new ProcessInstanceMessageMapper().apply(context);
            context.parameterObject = instance;
            org.kie.api.runtime.process.ProcessInstance legacyProcessInstance = super.readProcessInstance(context);
            processInstance.internalSetProcessInstance((WorkflowProcessInstance) legacyProcessInstance);
            return (ProcessInstance<T>) processInstance;
        } catch (Exception e) {
            throw new DocumentUnmarshallingException(processInstance.id(), e, DOCUMENT_UNMARSHALLING_ERROR_MSG);
        }
    }
}
