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

package org.kie.kogito.mongodb.utils;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import org.bson.Document;
import org.drools.core.marshalling.impl.ProcessMarshallerWriteContext;
import org.drools.core.util.Drools;
import org.kie.kogito.mongodb.marshalling.DocumentMarshallingException;
import org.kie.kogito.mongodb.model.ProcessInstanceDocument;

import static org.kie.kogito.mongodb.utils.DocumentUtils.PROCESS_INSTANCE_ID;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VALUE;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VARIABLE;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION_MAJOR;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION_MINOR;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION_REVISION;
import static org.kie.kogito.mongodb.utils.DocumentUtils.getObjectMapper;

public class ProcessInstanceDocumentMapper implements Function<ProcessMarshallerWriteContext, ProcessInstanceDocument> {

    @Override
    public ProcessInstanceDocument apply(ProcessMarshallerWriteContext context) {
        String instance = (String) context.parameterObject;
        ProcessInstanceDocument doc = new ProcessInstanceDocument();
        try {
            JsonNode instanceNode = getObjectMapper().readTree(instance);
            doc.setId(instanceNode.get(PROCESS_INSTANCE_ID).asText());
            applyVariables(instanceNode, VARIABLE);
            doc.setProcessInstance(Optional.ofNullable(instanceNode).map(json -> Document.parse(json.toString())).orElse(null));
            doc.setStrategies(context.usedStrategies.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue)));
            doc.getVersions().put(VERSION_MAJOR, Drools.getMajorVersion());
            doc.getVersions().put(VERSION_MINOR, Drools.getMinorVersion());
            doc.getVersions().put(VERSION_REVISION, Drools.getRevisionVersion());

        } catch (Exception e) {
            throw new DocumentMarshallingException(e);
        }
        return doc;
    }

    private void applyVariables(JsonNode parent, String variable) {
        if (parent.has(variable) && parent.get(variable).isArray()) {
            parent.get(variable).forEach(node -> {
                if (node.get(VALUE) != null) {
                    try {
                        byte[] value = node.get(VALUE).binaryValue();
                        ((ObjectNode) node).set(VALUE, getObjectMapper().readTree(new String(value)));
                    } catch (Exception e) {
                        throw new DocumentMarshallingException(e);
                    }
                }
            });
        }
        parent.forEach(child -> applyVariables(child, variable));
    }
}
