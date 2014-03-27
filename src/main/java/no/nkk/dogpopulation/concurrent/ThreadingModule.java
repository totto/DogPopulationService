package no.nkk.dogpopulation.concurrent;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ThreadingModule extends AbstractModule {

    @Override
    protected void configure() {
        final int MAX_CONCURRENT_BREED_IMPORTS = 20;
        ExecutorManager executorManager = new ExecutorManager();
        bind(ExecutorManager.class).toInstance(executorManager);
        bind(ExecutorService.class).annotatedWith(Names.named(ExecutorManager.BULK_WRITER_MAP_KEY)).toInstance(executorManager.addDirectHandoffExecutor(ExecutorManager.BULK_WRITER_MAP_KEY));
        bind(ExecutorService.class).annotatedWith(Names.named(ExecutorManager.SOLR_MAP_KEY)).toInstance(executorManager.addDirectHandoffExecutor(ExecutorManager.SOLR_MAP_KEY));
        bind(ExecutorService.class).annotatedWith(Names.named(ExecutorManager.BREED_IMPORTER_MAP_KEY)).toInstance(executorManager.addUnboundedQueueExecutor(ExecutorManager.BREED_IMPORTER_MAP_KEY, MAX_CONCURRENT_BREED_IMPORTS));
        bind(ExecutorService.class).annotatedWith(Names.named(ExecutorManager.TRAVERSER_MAP_KEY)).toInstance(executorManager.addDirectHandoffExecutor(ExecutorManager.TRAVERSER_MAP_KEY));
        bind(ExecutorService.class).annotatedWith(Names.named(ExecutorManager.UPDATES_IMPORTER_MAP_KEY)).toInstance(executorManager.addUnboundedQueueExecutor(ExecutorManager.UPDATES_IMPORTER_MAP_KEY, 3));
    }

    @Provides
    public Map<String, ManageableExecutor> executorSnapshot(ExecutorManager executorManager) {
        return executorManager.executorSnapshot();
    }

}
