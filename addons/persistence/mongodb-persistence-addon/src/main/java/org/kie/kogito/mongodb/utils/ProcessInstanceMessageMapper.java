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

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.util.JsonFormat;
import org.bson.Document;
import org.drools.core.marshalling.impl.MarshallerReaderContext;
import org.jbpm.marshalling.impl.JBPMMessages;
import org.jbpm.marshalling.impl.JBPMMessages.ProcessInstance;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.kogito.mongodb.marshalling.DocumentUnmarshallingException;

import static org.kie.kogito.mongodb.utils.DocumentUtils.DOCUMENT_ID;
import static org.kie.kogito.mongodb.utils.DocumentUtils.NAME;
import static org.kie.kogito.mongodb.utils.DocumentUtils.PROCESS_INSTANCE_ID;
import static org.kie.kogito.mongodb.utils.DocumentUtils.STRATEGIES;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VALUE;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VARIABLE;
import static org.kie.kogito.mongodb.utils.DocumentUtils.VERSION;
import static org.kie.kogito.mongodb.utils.DocumentUtils.getObjectMapper;

public class ProcessInstanceMessageMapper implements Function<MarshallerReaderContext, JBPMMessages.ProcessInstance> {

    @Override
    public ProcessInstance apply(MarshallerReaderContext context) {
        JBPMMessages.ProcessInstance.Builder builder = JBPMMessages.ProcessInstance.newBuilder();
        JsonFormat.Parser parser = JsonFormat.parser();
        Document doc = (Document) context.parameterObject;
        applyStrategy(doc, context);

        doc.append(PROCESS_INSTANCE_ID, doc.getString(DOCUMENT_ID));
        doc.remove(DOCUMENT_ID);
        doc.remove(VERSION);
        try {
            JsonNode rootNode = getObjectMapper().readTree(doc.toJson());
            applyVariables(rootNode, VARIABLE);
            String json = getObjectMapper().writeValueAsString(rootNode);
            parser.merge(json, builder);
        } catch (Exception e) {
            throw new DocumentUnmarshallingException(e);
        }
        return builder.build();
    }

    private void applyStrategy(Document doc, MarshallerReaderContext context) {
        for (Document strategy : doc.getList(STRATEGIES, Document.class)) {
            ObjectMarshallingStrategy strategyObject = context.resolverStrategyFactory.getStrategyObject(strategy.getString(NAME));
            if (strategyObject != null) {
                context.usedStrategies.put(strategy.getInteger(VALUE), strategyObject);
            }
        }
        doc.remove(STRATEGIES);
    }

    private void applyVariables(JsonNode parent, String variable) {
        if (parent.has(variable) && parent.get(variable).isArray()) {
            parent.get(variable).forEach(node -> {
                if (node.get(VALUE) != null) {

                    try {
                        String valueAsString = getObjectMapper().writeValueAsString(node.path(VALUE));
                        ((ObjectNode) node).put(VALUE, valueAsString.getBytes());
                    } catch (Exception e) {
                        throw new DocumentUnmarshallingException(e);
                    }
                }
            });
        }
        parent.forEach(child -> applyVariables(child, variable));
    }
}
