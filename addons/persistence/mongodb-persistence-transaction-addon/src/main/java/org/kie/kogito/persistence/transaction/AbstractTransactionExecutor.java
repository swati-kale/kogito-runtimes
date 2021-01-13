/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.kie.kogito.persistence.transaction;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * This class must always have exact FQCN as
 * <code>org.kie.kogito.persistence.transaction.AbstractTransactionExecutor</code>
 */
public abstract class AbstractTransactionExecutor implements TransactionExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTransactionExecutor.class);

    private ThreadLocal<ClientSession> clientSessionLocal = new ThreadLocal<>();

    private MongoClient mongoClient;

    public AbstractTransactionExecutor(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public <T> T execute(Callable<T> callable) throws Exception {
        try (ClientSession clientSession = mongoClient.startSession()) {
            TransactionOptions txnOptions = TransactionOptions.builder()
                    .readPreference(ReadPreference.primary())
                    .readConcern(ReadConcern.MAJORITY)
                    .writeConcern(WriteConcern.MAJORITY)
                    .build();
            clientSession.startTransaction(txnOptions);

            clientSessionLocal.set(clientSession);

            T result;
            try {
                result = callable.call();
            } catch (Exception ex) {
                LOGGER.error("Error when executing unit of work. Abort the MongoDB transaction.", ex);
                clientSession.abortTransaction();
                throw ex;
            }
            clientSession.commitTransaction();
            return result;
        } finally {
            clientSessionLocal.remove();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getResource() {
        return (T) clientSessionLocal.get();
    }
}
