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
import org.bson.types.Binary;
import org.kie.kogito.mongodb.model.ProcessInstanceModel;

public class ProcessInstanceModelCodec implements CollectibleCodec<ProcessInstanceModel> {

    private final Codec<Document> documentCodec;

    public ProcessInstanceModelCodec() {
        documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, ProcessInstanceModel processInstanceModel, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("id", processInstanceModel.getId());
        doc.put("processInstance", processInstanceModel.getProcessInstance());
        doc.put("header", processInstanceModel.getHeader());
        doc.put("content", processInstanceModel.getContent());
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
        if (document.getString("id") != null) {
            processInstanceModel.setId(document.getString("id"));
        }
        processInstanceModel.setProcessInstance((Document) (document.get("processInstance")));
        processInstanceModel.setContent(((Binary) document.get("content")).getData());
        processInstanceModel.setHeader((Document) (document.get("header")));
        return processInstanceModel;
    }
}
