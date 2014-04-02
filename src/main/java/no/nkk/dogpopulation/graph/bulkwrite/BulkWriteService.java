package no.nkk.dogpopulation.graph.bulkwrite;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import no.nkk.dogpopulation.graph.Builder;
import no.nkk.dogpopulation.graph.PostStepBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@Singleton
public class BulkWriteService implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkWriteService.class);
    private static final AtomicLong bulkWriterServiceSequence = new AtomicLong(1);

    private final GraphDatabaseService graphDb;

    private final BlockingQueue<WriteTask<?>> requestQueue = new ArrayBlockingQueue<>(50);

    // access to this map is guarded by locking the map instance itself
    private final Map<String, BuilderProgress> inProgress = new LinkedHashMap<>();

    private final AtomicBoolean done = new AtomicBoolean(false);

    private final Random rnd;

    private final long mySequence;

    private final AtomicLong bulkCount = new AtomicLong();
    private final AtomicLong builderCount = new AtomicLong();

    private final long start;

    // only ever assigned and accessed by single writer thread, no synchronization necessary
    private List<WriteTask<?>> currentBulk;

    private AtomicReference<Thread> consumerThreadRef = new AtomicReference<>();

    @Inject
    public BulkWriteService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        mySequence = bulkWriterServiceSequence.getAndIncrement();
        rnd = new Random(System.currentTimeMillis() + mySequence);
        start = System.currentTimeMillis();
    }


    @Override
    public void run() {
        if (!consumerThreadRef.compareAndSet(null, Thread.currentThread())) {
            throw new RuntimeException(BulkWriteService.class.getSimpleName() + " already has consumer thread");
        }
        try {
            LOGGER.info("Started.");
            while (!done.get()) {
                bulkCount.incrementAndGet();
                final int MAX_RETRIES = 10;
                for (int retry = 0; !done.get() && retry < MAX_RETRIES; retry++) {
                    try {
                        writeNextBulk();
                        break; // success, no need for retry
                    } catch (RuntimeException e) {
                        if (retry + 1 == MAX_RETRIES) {
                            // last retry failed
                            LOGGER.error("", e);
                            currentBulk = null; // discard poisonous bulk
                            break;
                        } else {
                            int waitMs = 300 + rnd.nextInt(2701);
                            LOGGER.debug("Exception causing a retry in {} milliseconds: {}", waitMs, e.getMessage());
                            try {
                                // wait between 0.3 and 3 seconds before retrying again
                                Thread.sleep(waitMs);
                            } catch (InterruptedException ignore) {
                            }
                        }
                    }
                }
            }
            LOGGER.info("Shutting down.");
        } finally {
            consumerThreadRef.set(null);
        }
    }

    @Override
    public String toString() {
        long currentBulkCount = bulkCount.get();
        long load = builderCount.get() / (1 + ((System.currentTimeMillis() - start) / 1000));
        return String.format("%d (bulk %d, load %d builders/sec)", mySequence, currentBulkCount, load);
    }

    public BulkWriteService start(ExecutorService executorService) {
        executorService.submit(this);
        return this;
    }

    public void stop() {
        done.set(true);
        Thread consumerThread = consumerThreadRef.get();
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
    }


    /**
     * Will perform cleanup of key when build is done and committed to graph.
     * Only one builder with the same key may ever exist on the queue when built using this method.
     *
     * @param key
     * @param builder
     * @return
     */
    public BuilderProgress build(final String key, Builder<Node> builder) {
        if (!(builder instanceof PostStepBuilder)) {
            throw new IllegalArgumentException("builder must be an instance of " + PostStepBuilder.class.getName());
        }
        if (done.get()) {
            throw new RuntimeException(BulkWriteService.class.getSimpleName() + " was shut-down");
        }
        WriteTask<Node> task;
        BuilderProgress progress;
        synchronized (inProgress) {
            BuilderProgress existingProgress = inProgress.get(key);
            if (existingProgress != null) {
                return new BuilderProgress(true, existingProgress.getFuture(), existingProgress.getBuilder());
            }
            ((PostStepBuilder) builder).setPostBuildTask(createInProgressCleanupTask(key));
            task = new WriteTask<>(builder);
            progress = new BuilderProgress(false, task, builder);
            inProgress.put(key, progress);
        }
        addTaskToQueue(task);
        return progress;
    }

    private Runnable createInProgressCleanupTask(final String key) {
        return new Runnable() {
            @Override
            public void run() {
                synchronized (inProgress) {
                    inProgress.remove(key);
                }
            }
        };
    }


    /**
     *
     * @param builder
     * @param <V>
     * @return
     * @throws InterruptedException
     */
    public <V> Future<V> build(Builder<V> builder) {
        WriteTask<V> task = new WriteTask<>(builder);
        addTaskToQueue(task);
        return task;
    }


    /**
     *
     * @throws InterruptedException
     */
    public void writeNextBulk() {
        long startTime = System.currentTimeMillis();

        if (currentBulk == null) {
            currentBulk = createNextBulk();
        }

        bulkWriteToGraph(currentBulk);

        runTaskPoststeps(currentBulk);

        signalAllTasksComplete(currentBulk);

        if (LOGGER.isTraceEnabled()) {
            int size;
            synchronized (inProgress) {
                size = inProgress.size();
            }
            LOGGER.trace("Completed bulk write of {} tasks in {} ms. There are now {} tasks in-progress", currentBulk.size(), System.currentTimeMillis() - startTime, size);
        }

        currentBulk = null;
    }

    private void signalAllTasksComplete(List<WriteTask<?>> bulk) {
        // signal that all pieces are completed
        for (WriteTask<?> task : bulk) {
            task.signalComplete();
        }
    }

    private void addTaskToQueue(WriteTask<?> task) {
        try {
            requestQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private List<WriteTask<?>> createNextBulk() {
        List<WriteTask<?>> bulk = new LinkedList();
        try {
            WriteTask<?> task = requestQueue.take(); // block until at least one task is available on queue
            bulk.add(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        requestQueue.drainTo(bulk);
        return bulk;
    }

    private void bulkWriteToGraph(List<WriteTask<?>> bulk) {
        boolean success = false;

        try (Transaction tx = graphDb.beginTx()) {

            /*
             * Build all the pieces
             */

            for (WriteTask<?> task : bulk) {
                task.performDirty(graphDb);
            }

            // commit in single transaction
            tx.success();
            success = true;

        } finally {
            if (!success) {
                // reset all builders
                for (WriteTask<?> task : bulk) {
                    task.rollback();
                }
            }
        }
    }

    private void runTaskPoststeps(List<WriteTask<?>> bulk) {
        for (WriteTask<?> task : bulk) {
            task.runPostStep();
        }
    }
}
