package no.nkk.dogpopulation.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class ManageableExecutor extends ThreadPoolExecutor {
    private final static Logger LOGGER = LoggerFactory.getLogger(ManageableExecutor.class);

    private final String queuingStrategy;

    private final Object lock = new Object(); // guards all mutable internal state
    private final Map<Runnable, TaskMetadata> metadataByRunnable = new LinkedHashMap<>(); // guarded by lock
    private long submittedTaskCount = 0; // guarded by lock
    private long completedTaskCount = 0; // guarded by lock

    public ManageableExecutor(int corePoolSize) {
        super(corePoolSize, corePoolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(), new CallerRunsPolicy());
        queuingStrategy = "unbounded queue";
    }

    public ManageableExecutor(int corePoolSize, int keepAliveSeconds) {
        super(corePoolSize, Integer.MAX_VALUE, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), Executors.defaultThreadFactory(), new CallerRunsPolicy());
        queuingStrategy = "direct handoff";
    }

    public ManageableExecutor(int corePoolSize, int maximumPoolSize, int workQueueCapacity) {
        super(corePoolSize, maximumPoolSize, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(workQueueCapacity), Executors.defaultThreadFactory(), new CallerRunsPolicy());
        queuingStrategy = "bounded queue";
    }

    public Collection<TaskMetadata> getTaskMetadata() {
        synchronized (lock) {
            return new ArrayList<>(metadataByRunnable.values());
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        synchronized (lock) {
            submittedTaskCount++;
            metadataByRunnable.put(r, new TaskMetadata(t, r, new Date()));
        }
        LOGGER.trace("STARTING  {} -- {}", r.toString(), t.getName());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        TaskMetadata metadata;
        synchronized (lock) {
            completedTaskCount++;
            metadata = metadataByRunnable.remove(r);
        }
        long durationMillis = System.currentTimeMillis() - metadata.getStart().getTime();
        LOGGER.trace("COMPLETED {} -- {}, duration {} sec", r.toString(), t == null ? "" : t.getMessage(), durationMillis / 1000.0);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new DescriptiveFutureTask<>(runnable, value);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new DescriptiveFutureTask<>(callable);
    }

    public long getSubmittedTaskCount() {
        synchronized (lock) {
            return submittedTaskCount;
        }
    }

    public long getCompletedTaskCount() {
        synchronized (lock) {
            return completedTaskCount;
        }
    }

    public ExecutorStatus getStatus() {
        return new ExecutorStatus(this);
    }

    public String getQueuingStrategy() {
        return queuingStrategy;
    }
}