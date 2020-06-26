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

package org.kie.kogito.mongodb.model;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

public class ProcessInstanceDocument {

    private String id;

    private Document processInstance;

    private Map<String, Integer> strategies = new HashMap<>();

    private Map<String, Integer> versions = new HashMap<>();

    public ProcessInstanceDocument() {}

    public ProcessInstanceDocument(String id, Document processInstance, Map<String, Integer> strategies, Map<String, Integer> versions) {
        super();
        this.id = id;
        this.processInstance = processInstance;
        this.strategies = strategies;
        this.versions = versions;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Document getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(Document processInstance) {
        this.processInstance = processInstance;
    }

    public Map<String, Integer> getStrategies() {
        return strategies;
    }

    public void setStrategies(Map<String, Integer> strategies) {
        this.strategies = strategies;
    }

    public Map<String, Integer> getVersions() {
        return versions;
    }

    public void setVersions(Map<String, Integer> versions) {
        this.versions = versions;
    }
}

