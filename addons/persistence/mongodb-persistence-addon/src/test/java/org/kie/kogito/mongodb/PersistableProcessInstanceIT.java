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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.drools.core.io.impl.ClassPathResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.process.bpmn2.BpmnVariables;
import org.kie.kogito.services.identity.StaticIdentityProvider;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED;

@Testcontainers
class PersistableProcessInstanceIT {

    private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));
    final static MongoDBContainer mongoDBContainer = new MongoDBContainer();
    final static String DB_NAME = "testdb";

    @BeforeAll
    public static void startContainerAndPublicPortIsAvailable() {
        mongoDBContainer.start();
    }

    @Test
    void test() {
        MongoClient mongoClient = MongoClients.create();
        BpmnProcess process = BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2"))
                                         .get(0);
        process.setProcessInstancesFactory(new PersistProcessInstancesFactory(mongoClient));
        process.configure();
        Map<String, Object> parameters = new HashMap<>();
      parameters.put("test", "test");
        parameters.put("integerVar", 10);
        parameters.put("booleanVar", true);
        parameters.put("doubleVar", 10.11);
        parameters.put("floatVar", 3.5f);
        parameters.put("address", new Address("main street", "Boston", "10005", "US"));
        
        PersonWithAddresses pa = new PersonWithAddresses("bob", 16);
        List<Address> list = new ArrayList<>();
        list.add(new Address("main street", "Boston", "10005", "US"));
        list.add(new Address("new Street", "Charlotte", "28200", "US"));

        pa.setAddresses(list);
        parameters.put("pa",pa);
        parameters.put("addresslist", list);
        
        Map<Object,Object> map = new HashMap<>();
        map.put("addresslist", list);
        map.put(1, "ss");
        map.put("2", "kk");
        Map<Object,Object>testMap = new HashMap<>();
        testMap.put("addresslist", list);
        testMap.put(1, "integer");
        testMap.put("2", "string");
        testMap.put("map", map);
        parameters.put("testMap", testMap);
        ProcessInstance<BpmnVariables> processInstance = process.createInstance(BpmnVariables.create(parameters));

        processInstance.start();
        assertThat(processInstance.status()).isEqualTo(STATE_ACTIVE);
        assertThat(processInstance.description()).isEqualTo("User Task");

        Collection<? extends ProcessInstance<BpmnVariables>> values = process.instances().values();
        assertThat(values).hasSize(1);

     String testVar = (String) processInstance.variables().get("test");
        assertThat(testVar).isEqualTo("test");
        Object addr = processInstance.variables().get("address");
       assertThat(addr.getClass().getName()).isEqualTo("org.kie.kogito.mongodb.Address");
        Object flt = processInstance.variables().get("floatVar");
        assertThat(flt.getClass().getName()).isEqualTo("java.lang.Float");
        assertThat(processInstance.description()).isEqualTo("User Task");

        WorkItem workItem = processInstance.workItems(securityPolicy).get(0);
        assertThat(workItem).isNotNull();
        assertThat(workItem.getParameters().get("ActorId")).isEqualTo("john");
        processInstance.completeWorkItem(workItem.getId(), null, securityPolicy);
        assertThat(processInstance.status()).isEqualTo(STATE_COMPLETED);
    }

    private class PersistProcessInstancesFactory extends KogitoProcessInstancesFactory {

        public PersistProcessInstancesFactory(MongoClient mongoClient) {
            super(mongoClient);
        }

        @Override
        public String dbName() {
            return DB_NAME;
        }
    }
}
