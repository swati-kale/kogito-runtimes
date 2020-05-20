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

package org.kie.kogito.mongodb.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mongodb.MongoClientSettings;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.kie.kogito.mongodb.model.ProcessInstanceModel;
import org.kie.kogito.mongodb.model.Strategy;

public class ProcessInstanceModelCodec implements CollectibleCodec<ProcessInstanceModel> {

    private final Codec<Document> documentCodec;

    public ProcessInstanceModelCodec() {
        documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, ProcessInstanceModel processInstanceModel, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("processInstance", processInstanceModel.getProcessInstance());
        List<Document> strategies = new ArrayList<>();
        for (Strategy s : processInstanceModel.getStrategies()) {
            Document d = new Document();
            d.put("strategyId", s.getStrategyId());
            d.put("strategyName", s.getStrategyName());
            strategies.add(d);
        }

        doc.put("strategies", strategies);
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<ProcessInstanceModel> getEncoderClass() {
        return ProcessInstanceModel.class;
    }

    @Override
    public ProcessInstanceModel generateIdIfAbsentFromDocument(ProcessInstanceModel document) {
        if (!documentHasId(document)) {
            document.setId(UUID.randomUUID().toString());
        }
        return document;
    }

    @Override
    public boolean documentHasId(ProcessInstanceModel document) {
        return document.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(ProcessInstanceModel document) {
        return new BsonString(document.getId());
    }

    @Override
    public ProcessInstanceModel decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        ProcessInstanceModel processInstanceModel = new ProcessInstanceModel();
        processInstanceModel.setProcessInstance((Document) (document.get("processInstance")));
        List<Strategy> strategies = new ArrayList<>();
        for (Document d : document.getList("strategies", Document.class)) {
            Strategy s = new Strategy();
            s.setStrategyId(d.getInteger("strategyId"));
            s.setStrategyName(d.getString("strategyName"));
            strategies.add(s);
        }
        processInstanceModel.setStrategies(strategies);
        return processInstanceModel;
    }
}
