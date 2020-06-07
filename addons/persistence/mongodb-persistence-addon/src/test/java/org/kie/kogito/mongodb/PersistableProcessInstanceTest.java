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

package org.kie.kogito.mongodb;

import java.util.Collections;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.drools.core.io.impl.ClassPathResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.process.bpmn2.BpmnVariables;
import org.kie.kogito.services.identity.StaticIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED;

class PersistableProcessInstanceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistableProcessInstanceTest.class);
    private static final String MONGODB_VERSION = System.getProperty("mongodb.version");
    private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));
    private static final int MONGO_DEFAULT_PORT = 27017;

    @Container
    public GenericContainer mongoDb = new FixedHostPortGenericContainer("mongo:" + MONGODB_VERSION)
                                                                                                   .withFixedExposedPort(MONGO_DEFAULT_PORT, MONGO_DEFAULT_PORT)
                                                                                                   .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                                                                                                   .waitingFor(Wait.forLogMessage(".*build index done.*", 1));

    @Test
    void test() {
        if (MONGODB_VERSION == null) {
            throw new RuntimeException("Please define a valid MongoDB image version in system property mongodb.version");
        }
        LOGGER.info("Using MongoDB image version: {}", MONGODB_VERSION);
        MongoClient mongoClient = MongoClients.create();
        BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2"))
                                         .get(0);
        process.setProcessInstancesFactory(new PersistProcessInstancesFactory(mongoClient));
        process.configure();
        ProcessInstance<BpmnVariables> processInstance = process
                                                                .createInstance(BpmnVariables.create(Collections.singletonMap("test", "test")));

        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        assertThat(process.instances().values()).hasSize(1);

        String testVar = (String) processInstance.variables().get("test");
        assertThat(testVar).isEqualTo("test");

        assertThat(processInstance.description()).isEqualTo("User Task");

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);
    }
    
    @BeforeEach
    public void setUp() {
        mongoDb.start();
    }

    @AfterEach
    public void tearDown() {
        if (mongoDb != null) {
            mongoDb.stop();
        }
    }

    private class PersistProcessInstancesFactory extends KogitoProcessInstancesFactory {

        public PersistProcessInstancesFactory(MongoClient mongoClient) {
            super(mongoClient);

        }
    }
}
