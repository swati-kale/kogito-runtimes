import org.kie.api.event.process.ProcessEventListener;
import org.kie.kogito.event.EventPublisher;
import org.kie.kogito.jobs.JobsService;
import org.kie.kogito.process.ProcessEventListenerConfig;
import org.kie.kogito.process.WorkItemHandlerConfig;
import org.kie.kogito.persistence.transaction.TransactionExecutor;
import org.kie.kogito.uow.UnitOfWorkManager;

import javax.enterprise.inject.Instance;

@javax.inject.Singleton
public class ProcessConfig extends org.kie.kogito.process.impl.AbstractProcessConfig {

    @javax.inject.Inject
    public ProcessConfig(
            Instance<WorkItemHandlerConfig> workItemHandlerConfig,
            Instance<UnitOfWorkManager> unitOfWorkManager,
            Instance<JobsService> jobsService,
            Instance<ProcessEventListenerConfig> processEventListenerConfigs,
            Instance<ProcessEventListener> processEventListeners,
            Instance<EventPublisher> eventPublishers,
            ConfigBean configBean,
            Instance<TransactionExecutor> transactionExecutor) {

        super(workItemHandlerConfig,
              processEventListenerConfigs,
              processEventListeners,
              unitOfWorkManager,
              jobsService,
              eventPublishers,
              configBean.getServiceUrl(),
              transactionExecutor);
    }

}
