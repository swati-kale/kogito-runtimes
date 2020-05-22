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

import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class ProcessInstanceModel {

    @BsonProperty("id")
    private String id;

    @BsonProperty("processInstance")
    private Document processInstance;
    @BsonProperty("content")
    private byte[] content;
    @BsonProperty("header")
    private Document header;

    public ProcessInstanceModel() {

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

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Document getHeader() {
        return header;
    }

    public void setHeader(Document header) {
        this.header = header;
    }

    public ProcessInstanceModel(String id, byte[] content, Document doc, Document header) {
        super();
        this.id = id;
        //this.pid = pid;
        this.content = content;
        processInstance = doc;
        this.header = header;
    }
}
