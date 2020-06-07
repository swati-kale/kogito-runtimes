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

package org.jbpm.marshalling.impl;

import java.util.Map;

public class ProcessInstanceData {

    private String legacyProcessInstance;
    private Map<String, Integer> strategies;

    public ProcessInstanceData() {
        super();
    }

    public String getlegacyProcessInstance() {
        return legacyProcessInstance;
    }

    public void setlegacyProcessInstance(String legacyProcessInstance) {
        this.legacyProcessInstance = legacyProcessInstance;
    }

    public Map<String, Integer> getStrategies() {
        return strategies;
    }

    public void setStrategies(Map<String, Integer> strategies) {
        this.strategies = strategies;
    }
}
