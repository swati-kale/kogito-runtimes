/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.process.impl.marshalling;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.protobuf.util.JsonFormat;
import org.drools.core.impl.EnvironmentImpl;
import org.drools.core.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.drools.core.marshalling.impl.PersisterHelper;
import org.drools.core.marshalling.impl.ProcessMarshallerWriteContext;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.jbpm.marshalling.impl.JBPMMessages;
import org.jbpm.marshalling.impl.ProcessInstanceDocument;
import org.jbpm.marshalling.impl.ProcessMarshallerRegistry;
import org.jbpm.marshalling.impl.ProtobufRuleFlowProcessInstanceMarshaller;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceMarshallingException;
import org.kie.kogito.process.ProcessInstanceUnmarshallingException;
import org.kie.kogito.process.impl.AbstractProcess;
import org.kie.kogito.process.impl.AbstractProcessInstance;

public class ProcessInstanceMarshaller {
    
    private Environment env = new EnvironmentImpl();
    
    public ProcessInstanceMarshaller(ObjectMarshallingStrategy... strategies) {
        ObjectMarshallingStrategy[] strats = null;
        if ( strategies == null ) {
            strats = new ObjectMarshallingStrategy[]{new SerializablePlaceholderResolverStrategy( ClassObjectMarshallingStrategyAcceptor.DEFAULT  )};
        } else {
            strats = new ObjectMarshallingStrategy[strategies.length + 1];
            int i = 0;
            for (ObjectMarshallingStrategy strategy : strategies) {
                strats[i] = strategy;
                i++;
            }
            strats[i] = new SerializablePlaceholderResolverStrategy( ClassObjectMarshallingStrategyAcceptor.DEFAULT  );
        }
        
        env.set( EnvironmentName.OBJECT_MARSHALLING_STRATEGIES, strats );
    }

    public byte[] marhsallProcessInstance(ProcessInstance<?> processInstance) {
        
        org.kie.api.runtime.process.ProcessInstance legacyProcessInstance = ((AbstractProcessInstance<?>) processInstance).internalGetProcessInstance();
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        
            ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext( baos,
                                                                                   null,
                                                                                   null,
                                                                                   null,
                                                                                   null,
                                                                                   this.env );
            context.setProcessInstanceId(legacyProcessInstance.getId());
            context.setState(legacyProcessInstance.getState());

            String processType = ((ProcessInstanceImpl) legacyProcessInstance).getProcess().getType();
            context.stream.writeUTF(processType);
            
            org.jbpm.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE.getMarshaller( processType );
            
            Object result = marshaller.writeProcessInstance( context,
                                                             legacyProcessInstance);
            if( marshaller instanceof ProtobufRuleFlowProcessInstanceMarshaller && result != null ) {
                JBPMMessages.ProcessInstance _instance = (JBPMMessages.ProcessInstance)result;
                PersisterHelper.writeToStreamWithHeader( context, 
                                                         _instance );
            }
            context.close();
            ((WorkflowProcessInstanceImpl) legacyProcessInstance).disconnect();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error while marshalling process instance", e);
        }
    }
    
    public ProcessInstance<?> unmarshallProcessInstance(byte[] data, Process<?> process) {
        
        Model m = (Model) process.createModel();
        AbstractProcessInstance<?> processInstance = (AbstractProcessInstance<?>) process.createInstance(m);
        
        return unmarshallProcessInstance(data, process, processInstance);
    }
    
    public ProcessInstance<?> unmarshallProcessInstance(byte[] data, Process<?> process, AbstractProcessInstance<?> processInstance) {
        
        org.kie.api.runtime.process.ProcessInstance legacyProcessInstance = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream( data )) {
            MarshallerReaderContext context = new MarshallerReaderContext( bais,
                                                                           Collections.singletonMap(process.id(), ((AbstractProcess<?>)process).legacyProcess()),
                                                                           null,
                                                                           null,
                                                                           null,
                                                                           this.env
                                                                          );
            ObjectInputStream stream = context.stream;
            String processInstanceType = stream.readUTF();
            
            org.jbpm.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE.getMarshaller( processInstanceType );
            
            legacyProcessInstance = marshaller.readProcessInstance(context);
     
            context.close();

            processInstance.internalSetProcessInstance(legacyProcessInstance);
            
            return processInstance;
        } catch (Exception e) {
            throw new RuntimeException("Error while unmarshalling process instance", e);
        }
    }
    
    public ProcessInstanceDocument marshalProcessInstanceToDocument(ProcessInstance<?> processInstance) {

        try {
            org.kie.api.runtime.process.ProcessInstance legacyProcessInstance = ((AbstractProcessInstance<?>) processInstance)
                                                                                                                              .internalGetProcessInstance();

            ProcessMarshallerWriteContext context = new ProcessMarshallerWriteContext(null, null, null, null,
                                                                                      env);
            org.jbpm.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                                                                                                               .getMarshaller(legacyProcessInstance.getProcess().getType());
            Object result = marshaller.writeProcessInstance(context, legacyProcessInstance);
            ProcessInstanceDocument document = new ProcessInstanceDocument();
            if (marshaller instanceof ProtobufRuleFlowProcessInstanceMarshaller && result != null) {
                JBPMMessages.ProcessInstance _instance = (JBPMMessages.ProcessInstance) result;
                document.setlegacyProcessInstance(JsonFormat.printer().print(_instance));
            }
            //saving marshalling strategy
            Map<String, Integer> strategies = new HashMap<>();
            for (Entry<ObjectMarshallingStrategy, Integer> entry : context.usedStrategies.entrySet()) {
                strategies.put(entry.getKey().getName(), entry.getValue());
            }
            document.setStrategies(strategies);

            ((WorkflowProcessInstanceImpl) legacyProcessInstance).disconnect();
            return document;
        } catch (Exception e) {
            throw new ProcessInstanceMarshallingException(processInstance.id(), e, "Error while marshalling process instance with id : ");
        }
    }

    public <T extends Model> ProcessInstance<T> unmarshalProcessInstanceDocument(ProcessInstanceDocument processDoc,
                                                                                 Process<?> process) {
        Model m = (Model) process.createModel();
        AbstractProcessInstance<?> processInstance = (AbstractProcessInstance<?>) process.createInstance(m);
        return unmarshalProcessInstanceDocument(processDoc, process, processInstance);
    }

    @SuppressWarnings("unchecked")
    public <T extends Model> ProcessInstance<T> unmarshalProcessInstanceDocument(ProcessInstanceDocument processDoc,
                                                                                 Process<?> process,
                                                                                 AbstractProcessInstance<?> processInstance) {
        try {
            MarshallerReaderContext context = new MarshallerReaderContext(
                                                                          Collections.singletonMap(process.id(), ((AbstractProcess<?>) process).legacyProcess()), null, null,
                                                                          null, env, null, true, true);
            //converting ProcessInstanceDocument to JBPMMessages
            JsonFormat.Parser parser = JsonFormat.parser();
            JBPMMessages.ProcessInstance.Builder builder = JBPMMessages.ProcessInstance.newBuilder();
            parser.merge(processDoc.getlegacyProcessInstance(), builder);
            
            JBPMMessages.ProcessInstance instance = builder.build();
            context.parameterObject = instance;
            org.jbpm.marshalling.impl.ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE
                                                                                                               .getMarshaller(RuleFlowProcess.RULEFLOW_TYPE);
            for (Map.Entry<String, Integer> entry : processDoc.getStrategies().entrySet()) {
                ObjectMarshallingStrategy strategyObject = context.resolverStrategyFactory
                                                                                          .getStrategyObject(entry.getKey());
                if (strategyObject != null) {
                    context.usedStrategies.put(entry.getValue(), strategyObject);
                }
            }
            org.kie.api.runtime.process.ProcessInstance legacyProcessInstance = marshaller.readProcessInstance(context);
            processInstance.internalSetProcessInstance(legacyProcessInstance);
            return (ProcessInstance<T>) processInstance;
        } catch (Exception e) {
            throw new ProcessInstanceUnmarshallingException(processInstance.id(), e, "Error while unmarshalling process instance with id : ");
        }
    }
}
