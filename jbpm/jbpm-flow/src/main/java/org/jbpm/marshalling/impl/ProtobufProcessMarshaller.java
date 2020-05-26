/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.marshalling.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.ExtensionRegistry;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.drools.core.marshalling.impl.MarshallerWriteContext;
import org.drools.core.marshalling.impl.PersisterHelper;
import org.drools.core.marshalling.impl.ProcessMarshaller;
import org.drools.core.marshalling.impl.ProtobufMessages;
import org.drools.core.marshalling.impl.ProtobufMessages.Header;
import org.drools.core.process.instance.WorkItemManager;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.marshalling.impl.JBPMMessages.Variable;
import org.jbpm.marshalling.impl.JBPMMessages.VariableContainer;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;

public class ProtobufProcessMarshaller
        implements
        ProcessMarshaller {

	private static boolean persistWorkItemVars = Boolean.parseBoolean(System.getProperty("org.jbpm.wi.variable.persist", "true"));
	// mainly for testability as the setting is global
	public static void setWorkItemVarsPersistence(boolean turnOn) {
		persistWorkItemVars = turnOn;
	}

    public void writeProcessInstances(MarshallerWriteContext context) throws IOException {
        ProtobufMessages.ProcessData.Builder _pdata = (ProtobufMessages.ProcessData.Builder) context.parameterObject;

        List<org.kie.api.runtime.process.ProcessInstance> processInstances = new ArrayList<>(context.wm.getProcessInstances());
        Collections.sort( processInstances,
                Comparator.comparing(ProcessInstance::getId));

        for ( org.kie.api.runtime.process.ProcessInstance processInstance : processInstances ) {
            String processType = processInstance.getProcess().getType();
            JBPMMessages.ProcessInstance _instance = (JBPMMessages.ProcessInstance) ProcessMarshallerRegistry.INSTANCE.getMarshaller( processType )
                    .writeProcessInstance( context,
                                           processInstance );
            _pdata.addExtension( JBPMMessages.processInstance, _instance );
        }
    }

    public void writeWorkItems(MarshallerWriteContext context) {
        ProtobufMessages.ProcessData.Builder _pdata = (ProtobufMessages.ProcessData.Builder) context.parameterObject;

        List<WorkItem> workItems = new ArrayList<>(((WorkItemManager) context.wm.getWorkItemManager()).getWorkItems());
        Collections.sort( workItems,
                (o1, o2) -> o2.getId().compareTo(o1.getId()));
        for ( WorkItem workItem : workItems ) {
            _pdata.addExtension( JBPMMessages.workItem,
                                 writeWorkItem( context,
                                                workItem ) );
        }
    }

    public static JBPMMessages.WorkItem writeWorkItem(MarshallerWriteContext context,
                                                      WorkItem workItem) {
        return writeWorkItem( context, workItem, true );
    }

    public List<ProcessInstance> readProcessInstances(MarshallerReaderContext context) throws IOException {
        ProtobufMessages.ProcessData _pdata = (ProtobufMessages.ProcessData) context.parameterObject;
        List<ProcessInstance> processInstanceList = new ArrayList<>();
        for ( JBPMMessages.ProcessInstance _instance : _pdata.getExtension( JBPMMessages.processInstance ) ) {
            context.parameterObject = _instance;
            ProcessInstance processInstance = ProcessMarshallerRegistry.INSTANCE.getMarshaller( _instance.getProcessType() ).readProcessInstance( context );
            ((WorkflowProcessInstanceImpl)processInstance).reconnect();
            processInstanceList.add( processInstance );
        }
        return processInstanceList;
    }

    public void readWorkItems(MarshallerReaderContext context) {
        ProtobufMessages.ProcessData _pdata = (ProtobufMessages.ProcessData) context.parameterObject;
        InternalWorkingMemory wm = context.wm;
        for ( JBPMMessages.WorkItem _workItem : _pdata.getExtension( JBPMMessages.workItem ) ) {
            WorkItem workItem = readWorkItem( context,
                                              _workItem );
            ((WorkItemManager) wm.getWorkItemManager()).internalAddWorkItem( (org.drools.core.process.instance.WorkItem) workItem );
        }
    }

    public static JBPMMessages.WorkItem writeWorkItem(MarshallerWriteContext context,
                                                      WorkItem workItem,
                                                      boolean includeVariables) {
        JBPMMessages.WorkItem.Builder _workItem = JBPMMessages.WorkItem.newBuilder()
                .setId( workItem.getId() )
                .setProcessInstancesId( workItem.getProcessInstanceId() )
                .setName( workItem.getName() )
                .setState( workItem.getState() );

        if (workItem instanceof org.drools.core.process.instance.WorkItem) {
        	if (((org.drools.core.process.instance.WorkItem)workItem).getDeploymentId() != null){
        	_workItem.setDeploymentId(((org.drools.core.process.instance.WorkItem)workItem).getDeploymentId());
        	}
        	_workItem.setNodeId(((org.drools.core.process.instance.WorkItem)workItem).getNodeId())
        	.setNodeInstanceId(((org.drools.core.process.instance.WorkItem)workItem).getNodeInstanceId());
        }

        if ( includeVariables ) {
            Map<String, Object> parameters = workItem.getParameters();
            for ( Map.Entry<String, Object> entry : parameters.entrySet() ) {
                _workItem.addVariable( marshalVariable( context, entry.getKey(), entry.getValue() ) );
            }
        }
        return _workItem.build();
    }

    public static WorkItem readWorkItem(MarshallerReaderContext context,
                                        JBPMMessages.WorkItem _workItem ) {
        return readWorkItem( context,
                             _workItem,
                             true );
    }

    public static WorkItem readWorkItem(MarshallerReaderContext context,
                                        JBPMMessages.WorkItem _workItem,
                                        boolean includeVariables) {
        WorkItemImpl workItem = new WorkItemImpl();
        workItem.setId( _workItem.getId() );
        workItem.setProcessInstanceId( _workItem.getProcessInstancesId() );
        workItem.setName( _workItem.getName() );
        workItem.setState( _workItem.getState() );
        workItem.setDeploymentId(_workItem.getDeploymentId());
        workItem.setNodeId(_workItem.getNodeId());
        workItem.setNodeInstanceId(_workItem.getNodeInstanceId());

        if ( includeVariables ) {
            for ( JBPMMessages.Variable _variable : _workItem.getVariableList() ) {
                    Object value = unmarshalVariableValue( context, _variable );
                    workItem.setParameter( _variable.getName(),
                                           value );
            }
        }

        return workItem;
    }

    public static Variable marshalVariable(MarshallerWriteContext context,
                                           String name,
                                           Object value) {
        JBPMMessages.Variable.Builder builder = JBPMMessages.Variable.newBuilder().setName( name );
        if(value != null){
            ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject( value );
            Integer index = context.getStrategyIndex( strategy );
            builder.setStrategyIndex( index )
                   .setDataType(strategy.getType(value.getClass()))      
                   .setValue(strategy.marshalToJson(value));
        }
        return builder.build();
    }

    public static Variable marshalVariablesMap(MarshallerWriteContext context, Map<String, Object> variables) {
        Map<String, Variable> marshalledVariables = new HashMap<>();
        for(String key : variables.keySet()){
            JBPMMessages.Variable.Builder builder = JBPMMessages.Variable.newBuilder().setName( key );
            Object variable = variables.get(key);
            if(variable != null){
                ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject( variable );
                Integer index = context.getStrategyIndex( strategy );
                builder.setStrategyIndex( index )
                   .setDataType(strategy.getType(variable.getClass()))
                   .setValue(strategy.marshalToJson(variable));

            }
            marshalledVariables.put(key, builder.build());
        }

        return marshalVariable(context, "variablesMap" ,marshalledVariables);
    }

    public static VariableContainer marshalVariablesContainer(MarshallerWriteContext context, Map<String, Object> variables) {
    	JBPMMessages.VariableContainer.Builder vcbuilder = JBPMMessages.VariableContainer.newBuilder();
        for(String key : variables.keySet()){
            JBPMMessages.Variable.Builder builder = JBPMMessages.Variable.newBuilder().setName( key );
            if(variables.get(key) != null){
                ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject( variables.get(key) );
                Integer index = context.getStrategyIndex( strategy );
                builder.setStrategyIndex( index )
                       .setValue(strategy.marshalToJson( variables.get(key) ));

            }
            vcbuilder.addVariable(builder.build());
        }

        return vcbuilder.build();
    }

    public static Object unmarshalVariableValue(MarshallerReaderContext context,
                                                JBPMMessages.Variable _variable) {
        if(_variable.getValue() == null || _variable.getValue().isEmpty()){
            return null;
        }
        ObjectMarshallingStrategy strategy = context.usedStrategies.get( _variable.getStrategyIndex() );
        return strategy.unmarshalFromJson( _variable.getDataType(), _variable.getValue());
    }

    public static String marshalVariableToJson(MarshallerWriteContext context,
                                               String name,
                                               Object value) {
       	String json ="";
		if(value != null){
		ObjectMarshallingStrategy strategy = context.objectMarshallingStrategyStore.getStrategyObject( value );
		json = strategy. marshalToJson(value);
		}
		return json;
    }
    
	public static Object unmarshalVariableValueFromJson(String datatype, String json, MarshallerReaderContext context,
                                                        JBPMMessages.Variable _variable) {
		if (json == null || json.isEmpty()) {
			return null;
		}
		ObjectMarshallingStrategy strategy = context.usedStrategies.get(_variable.getStrategyIndex());
		return strategy.unmarshalFromJson(json, datatype);
	}
	
	public static Map<String, Object> unmarshalVariableContainerValue(MarshallerReaderContext context,
                                                                      JBPMMessages.VariableContainer _variableContiner) {
		Map<String, Object> variables = new HashMap<>();
		if (_variableContiner.getVariableCount() == 0) {
			return variables;
		}
	
		for (Variable _variable : _variableContiner.getVariableList()) {
	
			Object value = ProtobufProcessMarshaller.unmarshalVariableValue(context, _variable);
			variables.put(_variable.getName(), value);
		}
		return variables;
	}

    public void init(MarshallerReaderContext context) {
        ExtensionRegistry registry = (ExtensionRegistry) context.parameterObject;
        registry.add( JBPMMessages.processInstance );
        registry.add( JBPMMessages.processTimer );
        registry.add( JBPMMessages.procTimer );
        registry.add( JBPMMessages.workItem );
        registry.add( JBPMMessages.timerId );
    }

    @Override
    public void writeWorkItem(MarshallerWriteContext context, org.drools.core.process.instance.WorkItem workItem) {
        try {
            JBPMMessages.WorkItem _workItem = writeWorkItem(context, workItem, persistWorkItemVars);
            PersisterHelper.writeToStreamWithHeader( context, _workItem );
        } catch (IOException e) {
            throw new IllegalArgumentException( "IOException while storing work item instance "
                    + workItem.getId() + ": " + e.getMessage(), e );
        }
    }

    @Override
    public org.drools.core.process.instance.WorkItem readWorkItem(MarshallerReaderContext context) {
        try {
            ExtensionRegistry registry = PersisterHelper.buildRegistry(context, null);
            Header _header = PersisterHelper.readFromStreamWithHeaderPreloaded(context, registry);
            JBPMMessages.WorkItem _workItem = JBPMMessages.WorkItem.parseFrom(_header.getPayload(), registry);
            return (org.drools.core.process.instance.WorkItem) readWorkItem(context, _workItem, persistWorkItemVars);
        } catch (IOException e) {
            throw new IllegalArgumentException( "IOException while fetching work item instance : " + e.getMessage(), e );
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException( "ClassNotFoundException while fetching work item instance : " + e.getMessage(), e );
        }
    }
}