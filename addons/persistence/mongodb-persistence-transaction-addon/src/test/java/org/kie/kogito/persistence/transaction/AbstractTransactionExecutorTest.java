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

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AbstractTransactionExecutorTest {

    private static final int TEST_THREADS = 10;

    @Test
    void test() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(TEST_THREADS);
        CountDownLatch latch = new CountDownLatch(TEST_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        for (int i = 0; i < TEST_THREADS; i++) {
            service.execute(() -> {
                try {
                    startLatch.await(1, TimeUnit.MINUTES);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                MongoClient mongoClient = mock(MongoClient.class);
                ClientSession clientSession = mock(ClientSession.class);
                when(mongoClient.startSession()).thenReturn(clientSession);

                AbstractTransactionExecutor executor = new AbstractTransactionExecutor(mongoClient) {
                };

                try {
                    ClientSession result = executor.execute(executor::getResource);
                    assertEquals(clientSession, result);
                    verify(mongoClient, times(1)).startSession();
                    verify(clientSession, times(1)).startTransaction(any());
                    verify(clientSession, times(1)).commitTransaction();
                    verify(clientSession, times(1)).close();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                } finally {
                    latch.countDown();
                }
            });
        }
        startLatch.countDown();
        latch.await(2, TimeUnit.MINUTES);
    }
}