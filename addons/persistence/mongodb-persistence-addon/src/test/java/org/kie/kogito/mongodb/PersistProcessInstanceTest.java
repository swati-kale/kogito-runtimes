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

import static org.assertj.core.api.Assertions.assertThat;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;
import static org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED;

import java.util.Collections;

import org.drools.core.io.impl.ClassPathResource;
import org.junit.jupiter.api.Test;
import org.kie.kogito.auth.SecurityPolicy;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.WorkItem;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.process.bpmn2.BpmnVariables;
import org.kie.kogito.services.identity.StaticIdentityProvider;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

class PersistProcessInstanceTest {
	private MongoClient mongoClient = MongoClients.create();
	private SecurityPolicy securityPolicy = SecurityPolicy.of(new StaticIdentityProvider("john"));
	@Test
	void test() {

		BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2"))
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

	private class PersistProcessInstancesFactory extends KogitoProcessInstancesFactory {

		public PersistProcessInstancesFactory(MongoClient mongoClient) {
			super(mongoClient);

		}
	}
}
