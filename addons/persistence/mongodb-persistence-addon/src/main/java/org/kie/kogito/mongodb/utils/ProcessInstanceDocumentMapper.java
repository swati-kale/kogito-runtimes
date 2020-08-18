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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import org.bson.Document;
import org.drools.core.marshalling.impl.ProcessMarshallerWriteContext;
import org.drools.core.util.Drools;
import org.kie.kogito.mongodb.marshalling.DocumentMarshallingException;

import static org.kie.kogito.mongodb.utils.DocumentUtils.NAME;
import static org.kie.kogito.mongodb.utils.DocumentUtils.PROCESS_INSTANCE_ID;
import static org.kie.kogito.mongodb.utils.DocumentUtils.STRATEGIES;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VALUE;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VARIABLE;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION_MAJOR;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION_MINOR;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION_REVISION;
import static org.kie.kogito.mongodb.utils.DocumentUtils.getObjectMapper;

public class ProcessInstanceDocumentMapper implements Function<ProcessMarshallerWriteContext, Document> {

    @Override
    public Document apply(ProcessMarshallerWriteContext context) {
        String instance = (String) context.parameterObject;
        Document doc = null;
        try {
            JsonNode instanceNode = getObjectMapper().readTree(instance);
            applyVariables(instanceNode, VARIABLE);
            doc = Optional.ofNullable(instanceNode).map(json -> Document.parse(json.toString())).orElse(null);
            if (doc != null) {
                applyStrategy(context, doc);
                doc.remove(PROCESS_INSTANCE_ID);
                doc.append(VERSION, new Document().append(VERSION_MAJOR, Drools.getMajorVersion()).append(VERSION_MINOR, Drools.getMinorVersion()).append(VERSION_REVISION, Drools.getRevisionVersion()));
            }
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
                        JsonNode replace = getObjectMapper().readTree(new String(value));
                        ((ObjectNode) node).set(VALUE, replace);
                    } catch (Exception e) {
                        throw new DocumentMarshallingException(e);
                    }
                }
            });
        }
        parent.forEach(child -> applyVariables(child, variable));
    }

    private void applyStrategy(ProcessMarshallerWriteContext context, Document doc) {
        List<Document> stratsList = context.usedStrategies.entrySet().stream().map(e -> new Document().append(NAME, e.getKey().getName()).append(VALUE, e.getValue())).collect(Collectors.toList());
        doc.append(STRATEGIES, stratsList);
    }
}
