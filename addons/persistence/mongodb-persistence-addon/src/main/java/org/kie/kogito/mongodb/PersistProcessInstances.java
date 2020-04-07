package org.kie.kogito.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceDuplicatedException;
import org.kie.kogito.process.impl.marshalling.ProcessInstanceMarshaller;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

@SuppressWarnings({ "rawtypes" })
public class PersistProcessInstances implements MutableProcessInstances {

	private org.kie.kogito.process.Process<?> process;
	private MongoDatabase mongoDatabase;
	private final MongoCollection<Document> collection;
	private ProcessInstanceMarshaller marshaller;
	private static final String KOGITO_STORE = "Kogito_store";

	public PersistProcessInstances(MongoClient mongoClient, org.kie.kogito.process.Process<?> process) {

		this.process = process;
		this.mongoDatabase = mongoClient.getDatabase(KOGITO_STORE);
		this.collection = this.mongoDatabase.getCollection(process.id() + "_store");
		this.marshaller = new ProcessInstanceMarshaller(null);
	}

	@Override
	public Optional findById(String id) {
		MongoCollection<Document> collection = mongoDatabase.getCollection(process.id() + "_store");
		FindIterable<Document> processDoc = collection.find(Filters.eq("id", resolveId(id)));
		if (processDoc != null && processDoc.first() != null) {
			org.bson.types.Binary binData = (org.bson.types.Binary) (processDoc.first().get("data"));
			byte[] data = binData.getData();
			return (Optional<? extends ProcessInstance>) Optional
					.of(marshaller.unmarshallProcessInstance(data, process));
		}
		return Optional.empty();
	}

	@Override
	public Collection values() {
		List<ProcessInstance> list = new ArrayList<>();
		MongoCollection<Document> collection = mongoDatabase.getCollection(process.id() + "_store");
		FindIterable<Document> fi = collection.find();
		MongoCursor<Document> cursor = fi.iterator();
		try {
			while (cursor.hasNext()) {
				org.bson.types.Binary binData = (org.bson.types.Binary) (cursor.next().get("data"));
				byte[] data = binData.getData();

				Optional opt = (Optional<? extends ProcessInstance>) Optional
						.of(marshaller.unmarshallProcessInstance(data, process));
				if (opt.get() instanceof ProcessInstance) {
					ProcessInstance<?> pi = (ProcessInstance) opt.get();
					list.add(pi);
				}

			}
		} finally {
			cursor.close();
		}
		return list;
	}

	@Override
	public boolean exists(String id) {
		String resolvedId = resolveId(id);
		Document existing = collection.find(Filters.eq("id", resolvedId)).first();
		if (existing != null) {
			return true;
		}
		return false;
	}

	@Override
	public void create(String id, ProcessInstance instance) {
		updateStorage(id, instance, true);
	}

	@Override
	public void update(String id, ProcessInstance instance) {
		updateStorage(id, instance, false);
	}

	@SuppressWarnings("unchecked")
	protected void updateStorage(String id, ProcessInstance instance, boolean checkDuplicates) {

		if (isActive(instance)) {
			String resolvedId = resolveId(id);
			byte[] data = marshaller.marhsallProcessInstance(instance);
			if (checkDuplicates) { // inserting
				Document existing = collection.find(Filters.eq("id", resolvedId)).first();
				if (existing != null) {
					throw new ProcessInstanceDuplicatedException(id);
				} else {
					Document document = new Document("id", resolvedId).append("data", data);
					collection.insertOne(document);
				}
			} else { // updating - updateOne
				collection.updateOne(Filters.eq("id", resolvedId), Updates.set("data", data));
			}

			/*
			 * ((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {
			 * byte[] reloaded = ((org.bson.types.Binary) (collection.find(Filters.eq("id",
			 * resolvedId)).first() .get("data"))).getData(); if (reloaded != null) { return
			 * ((AbstractProcessInstance<?>) marshaller.unmarshallProcessInstance(reloaded,
			 * process, (AbstractProcessInstance<?>)
			 * instance)).internalGetProcessInstance(); }
			 * 
			 * return null; });
			 */
		}
	}

	@Override
	public void remove(String id) {
		String resolvedId = resolveId(id);
		collection.deleteOne(Filters.eq("id", resolvedId));

	}

}