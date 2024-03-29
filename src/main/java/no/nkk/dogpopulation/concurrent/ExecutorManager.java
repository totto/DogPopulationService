package no.nkk.dogpopulation.concurrent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ExecutorManager {

    public static final String TRAVERSER_MAP_KEY = "system_traverser";
    public static final String BREED_IMPORTER_MAP_KEY = "system_breed";
    public static final String SOLR_MAP_KEY = "system_solrj";
    public static final String BULK_WRITER_MAP_KEY = "system_bulkWriter";

    private final Map<String, ManageableExecutor> executorByName = new LinkedHashMap<>();

    public ManageableExecutor addBoundedQueueExecutor(String name, int corePoolSize, int maximumPoolSize, int workQueueCapacity) {
        synchronized(executorByName) {
            if (executorByName.containsKey(name)) {
                throw new IllegalArgumentException("Attempting to re-assign existing thread-pool");
            }
            ManageableExecutor manageableExecutor = new ManageableExecutor(corePoolSize, maximumPoolSize, workQueueCapacity);
            executorByName.put(name, manageableExecutor);
            return manageableExecutor;
        }
    }

    public ManageableExecutor addUnboundedQueueExecutor(String name, int corePoolSize) {
        synchronized(executorByName) {
            if (executorByName.containsKey(name)) {
                throw new IllegalArgumentException("Attempting to re-assign existing thread-pool");
            }
            ManageableExecutor manageableExecutor = new ManageableExecutor(corePoolSize);
            executorByName.put(name, manageableExecutor);
            return manageableExecutor;
        }
    }

    public ManageableExecutor addDirectHandoffExecutor(String name) {
        synchronized(executorByName) {
            if (executorByName.containsKey(name)) {
                throw new IllegalArgumentException("Attempting to re-assign existing thread-pool");
            }
            final int corePoolSize = 10;
            final int keepAliveSeconds = 30;
            ManageableExecutor manageableExecutor = new ManageableExecutor(corePoolSize, keepAliveSeconds);
            executorByName.put(name, manageableExecutor);
            return manageableExecutor;
        }
    }

    public ManageableExecutor getExecutor(String name) {
        synchronized(executorByName) {
            return executorByName.get(name);
        }
    }

    public Map<String, ManageableExecutor> executorSnapshot() {
        synchronized(executorByName) {
            return new LinkedHashMap<>(executorByName);
        }
    }

    public ManageableExecutor removeExecutor(String name) {
        synchronized(executorByName) {
            return executorByName.remove(name);
        }
    }

    public void shutdown() {
        Map<String, ManageableExecutor> map = executorSnapshot();
        for (Map.Entry<String, ManageableExecutor> e : map.entrySet()) {
            e.getValue().shutdown();
        }
        try {
            for (Map.Entry<String, ManageableExecutor> e : map.entrySet()) {
                e.getValue().awaitTermination(10, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        for (Map.Entry<String, ManageableExecutor> e : map.entrySet()) {
            e.getValue().shutdownNow();
        }
        try {
            for (Map.Entry<String, ManageableExecutor> e : map.entrySet()) {
                e.getValue().awaitTermination(1, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
