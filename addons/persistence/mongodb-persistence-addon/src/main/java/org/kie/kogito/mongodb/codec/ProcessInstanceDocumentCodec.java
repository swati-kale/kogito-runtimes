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
import org.kie.kogito.mongodb.model.ProcessInstanceDocument;
import org.kie.kogito.mongodb.model.Strategy;

public class ProcessInstanceDocumentCodec implements CollectibleCodec<ProcessInstanceDocument> {

    private final Codec<Document> documentCodec;

    public ProcessInstanceDocumentCodec() {
        documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, ProcessInstanceDocument piDoc, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("processInstance", piDoc.getProcessInstance());
        List<Document> strategies = new ArrayList<>();
        for (Strategy s : piDoc.getStrategies()) {
            Document d = new Document();
            d.put("strategyId", s.getStrategyId());
            d.put("strategyName", s.getStrategyName());
            strategies.add(d);
        }

        doc.put("strategies", strategies);
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<ProcessInstanceDocument> getEncoderClass() {
        return ProcessInstanceDocument.class;
    }

    @Override
    public ProcessInstanceDocument generateIdIfAbsentFromDocument(ProcessInstanceDocument document) {
        if (!documentHasId(document)) {
            document.setId(UUID.randomUUID().toString());
        }
        return document;
    }

    @Override
    public boolean documentHasId(ProcessInstanceDocument document) {
        return document.getId() != null;
    }

    @Override
    public BsonValue getDocumentId(ProcessInstanceDocument document) {
        return new BsonString(document.getId());
    }

    @Override
    public ProcessInstanceDocument decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        ProcessInstanceDocument piDoc = new ProcessInstanceDocument();
        piDoc.setProcessInstance((Document) (document.get("processInstance")));
        List<Strategy> strategies = new ArrayList<>();
        for (Document d : document.getList("strategies", Document.class)) {
            Strategy s = new Strategy();
            s.setStrategyId(d.getInteger("strategyId"));
            s.setStrategyName(d.getString("strategyName"));
            strategies.add(s);
        }
        piDoc.setStrategies(strategies);
        return piDoc;
    }
}
