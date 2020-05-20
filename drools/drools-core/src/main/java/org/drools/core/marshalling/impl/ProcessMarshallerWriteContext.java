/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.marshalling.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.drools.core.common.BaseNode;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.impl.InternalKnowledgeBase;
import org.kie.api.marshalling.ObjectMarshallingStrategyStore;
import org.kie.api.runtime.Environment;

/**
 * Extension to default <code>MarshallerWriteContext</code> that allows to pass additional
 * information to marshaller strategies, such as process instance id, task it, state
 */
public class ProcessMarshallerWriteContext extends MarshallerWriteContext {
    
    public static final int STATE_ACTIVE = 1;
    public static final int STATE_COMPLETED = 2;

    private String processInstanceId;
    private String taskId;
    private String workItemId;
    private int state;
    

    public ProcessMarshallerWriteContext(OutputStream stream, 
            InternalKnowledgeBase kBase, 
            InternalWorkingMemory wm, 
            Map<Integer, BaseNode> sinks, 
            ObjectMarshallingStrategyStore resolverStrategyFactory, 
            Environment env) throws IOException {
        super(stream, kBase, wm, sinks, resolverStrategyFactory, env);
    }
    
    public ProcessMarshallerWriteContext(
                                         InternalKnowledgeBase kBase,
                                         InternalWorkingMemory wm,
                                         Map<Integer, BaseNode> sinks,
                                         ObjectMarshallingStrategyStore resolverStrategyFactory,
                                         Environment env) throws IOException {
        super(kBase, wm, sinks, resolverStrategyFactory, env, true, true);
    }
    
    public String getProcessInstanceId() {
        return processInstanceId;
    }
    
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getWorkItemId() {
        return workItemId;
    }
    
    public void setWorkItemId(String workItemId) {
        this.workItemId = workItemId;
    }
    
    public int getState() {
        return state;
    }
    
    public void setState(int state) {
        this.state = state;
    }

}
