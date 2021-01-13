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

package org.kie.kogito.persistence;

import org.junit.jupiter.api.Test;
import org.kie.kogito.mongodb.MongoDBProcessInstances;
import org.kie.kogito.mongodb.TestHelper;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.persistence.transaction.TransactionExecutor;
import org.kie.kogito.process.Process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class KogitoProcessInstancesFactoryTest extends TestHelper {

    @Test
    void test() {
        TransactionExecutor transactionExecutor = mock(TransactionExecutor.class);

        KogitoProcessInstancesFactory factory = new KogitoProcessInstancesFactory(getMongoClient()) {

            @Override
            public String dbName() {
                return DB_NAME;
            }

            @Override
            public TransactionExecutor transactionExecutor() {
                return transactionExecutor;
            }
        };
        assertNotNull(factory);
        assertThat(factory.dbName()).isEqualTo(DB_NAME);
        Process<?> process = mock(Process.class);
        lenient().when(process.id()).thenReturn(PROCESS_NAME);
        lenient().when(process.name()).thenReturn(PROCESS_NAME);
        MongoDBProcessInstances<?> instance = factory.createProcessInstances(process);
        assertNotNull(instance);
        assertEquals(transactionExecutor, factory.transactionExecutor());
    }
}
