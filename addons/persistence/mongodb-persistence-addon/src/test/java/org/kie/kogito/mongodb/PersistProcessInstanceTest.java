package org.kie.kogito.mongodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kie.api.runtime.process.ProcessInstance.STATE_ACTIVE;

import java.util.Collections;
import java.util.List;

import org.drools.core.io.impl.ClassPathResource;
import org.junit.jupiter.api.Test;
import org.kie.kogito.persistence.KogitoProcessInstancesFactory;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.bpmn2.BpmnProcess;
import org.kie.kogito.process.bpmn2.BpmnVariables;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

class PersistProcessInstanceTest {
	MongoClient mongoClient = MongoClients.create();

	@Test
	void test() {

		BpmnProcess process = (BpmnProcess) BpmnProcess.from(new ClassPathResource("BPMN2-UserTask.bpmn2")).get(0);
		// BpmnProcess process = (BpmnProcess) BpmnProcess.from(new
		// ClassPathResource("BPMN2-UserTask-Script.bpmn2")).get(0);
		process.setProcessInstancesFactory(new PersistProcessInstancesFactory(mongoClient));
		process.configure();

		ProcessInstance<BpmnVariables> processInstance = process
				.createInstance(BpmnVariables.create(Collections.singletonMap("sss", "test")));

		processInstance.start();

		// processInstance.updateVariables(BpmnVariables.create(Collections.singletonMap("s",
		// "test")));
		assertEquals(STATE_ACTIVE, processInstance.status());

		// String processId = process.legacyProcess().getId();
		// processInstance.abort();
//	        WorkItem workItem = processInstance.workItems().get(0);
//	        assertNotNull(workItem);
//	        assertEquals("john", workItem.getParameters().get("ActorId"));
//	        processInstance.completeWorkItem(workItem.getId(), null);
//	        assertEquals(STATE_COMPLETED, processInstance.status());
	}

	private class PersistProcessInstancesFactory extends KogitoProcessInstancesFactory {

		public PersistProcessInstancesFactory(MongoClient mongoClient) {
			super(mongoClient);

		}

		@Override
		public String proto() {
			return null;
		}

		@Override
		public List<?> marshallers() {
			return Collections.emptyList();
		}
	}
}
