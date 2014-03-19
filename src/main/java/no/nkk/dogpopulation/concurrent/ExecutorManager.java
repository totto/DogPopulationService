package no.nkk.dogpopulation.concurrent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ExecutorManager {

    public static final String TRAVERSER_MAP_KEY = "system_traverser";
    public static final String BREED_IMPORTER_MAP_KEY = "system_breed";
    public static final String SOLR_MAP_KEY = "system_solrj";
    public static final String GRAPH_QUERY_MAP_KEY = "system_graphQuery";
    public static final String BULK_WRITER_MAP_KEY = "system_bulkWriter";
    public static final String UPDATES_LIST_UPDATES_MAP_KEY = "system_update_listing";
    public static final String UPDATES_IMPORTER_MAP_KEY = "system_updates_traverser";

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
}
