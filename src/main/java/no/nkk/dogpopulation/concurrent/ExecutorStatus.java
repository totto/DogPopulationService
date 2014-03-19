package no.nkk.dogpopulation.concurrent;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"queuingStrategy", "submittedTaskCount", "completedTaskCount", "activeTaskCount", "activeCount", "poolSize", "corePoolSize", "maximumPoolSize", "workQueueSize", "taskCount", "longRunningTaskCountByMinute", "longRunningTasks"})
public class ExecutorStatus {

    private final ManageableExecutor manageableExecutor;

    public ExecutorStatus(ManageableExecutor manageableExecutor) {
        this.manageableExecutor = manageableExecutor;
    }

    public Map<String, String> getLongRunningTasks() {
        TreeMap<Date, TaskMetadata> taskByStart = new TreeMap<>();
        for (TaskMetadata meta : manageableExecutor.getTaskMetadata()) {
            taskByStart.put(meta.getStart(), meta);
        }
        Map<String, String> longRunningTasks = new LinkedHashMap<>();
        Date oneMinuteAgo = new Date(System.currentTimeMillis() - (60 * 1000));
        int i = 0;
        for (TaskMetadata meta : taskByStart.values()) {
            if (i++ >= 10) {
                break; // only list top 10
            }
            if (meta.getStart().after(oneMinuteAgo)) {
                break;
            }
            longRunningTasks.put(meta.getThread().getName(), meta.shortStatus());
        }
        return longRunningTasks;
    }

    public Map<Integer, Integer> getLongRunningTaskCountByMinute() {
        Map<Integer, Integer> map = new TreeMap<>();
        for (TaskMetadata meta : manageableExecutor.getTaskMetadata()) {
            int durationMinutes = (int) ((System.currentTimeMillis() - meta.getStart().getTime()) / (60 * 1000));
            Integer count = map.get(durationMinutes);
            if (count == null) {
                map.put(durationMinutes, 1);
            } else {
                map.put(durationMinutes, 1 + count);
            }
        }
        return map;
    }

    public String getQueuingStrategy() {
        return manageableExecutor.getQueuingStrategy();
    }

    public long getCompletedTaskCount() {
        return manageableExecutor.getCompletedTaskCount();
    }
    public long getSubmittedTaskCount() {
        return manageableExecutor.getSubmittedTaskCount();
    }
    public int getActiveTaskCount() {
        return (int) (getSubmittedTaskCount() - getCompletedTaskCount());
    }

    public int getActiveCount() {
        return manageableExecutor.getActiveCount();
    }
    public int getPoolSize() {
        return manageableExecutor.getPoolSize();
    }
    public int getCorePoolSize() {
        return manageableExecutor.getCorePoolSize();
    }
    public int getMaximumPoolSize() {
        return manageableExecutor.getMaximumPoolSize();
    }
    public int getWorkQueueSize() {
        return manageableExecutor.getQueue().size();
    }
    public long getTaskCount() {
        return manageableExecutor.getTaskCount();
    }
}
