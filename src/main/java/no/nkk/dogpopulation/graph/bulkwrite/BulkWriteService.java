package no.nkk.dogpopulation.graph.bulkwrite;

import no.nkk.dogpopulation.graph.Builder;
import no.nkk.dogpopulation.graph.PostStepBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BulkWriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkWriteService.class);
    private static final AtomicLong bulkWriterServiceSequence = new AtomicLong(1);

    private static final int MAX_PENDING_BUILDERS = 50;

    private final GraphDatabaseService graphDb;

    // guards nextBulk and requestQueue
    private final Object lock = new Object();

    // access is guarded by lock
    private final Queue<WriteTask<?>> requestQueue = new ArrayDeque<>();

    // access is guarded by lock
    private CountDownLatch nextBulk = new CountDownLatch(1);

    // access to this map is guarded by locking the map instance itself
    private final Map<String, BuilderProgress> inProgress = new LinkedHashMap<>();

    private AtomicBoolean done = new AtomicBoolean(false);

    // only accessed by writer thread
    private BulkState currentBulk;

    private final Random rnd;

    public BulkWriteService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!done.get()) {
                    final int MAX_RETRIES = 10;
                    for (int retry=0; !done.get() && retry<MAX_RETRIES; retry++) {
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
                LOGGER.info("{} shutting down.", Thread.currentThread().getName());
            }
        });
        long mySequence = bulkWriterServiceSequence.getAndIncrement();
        rnd = new Random(System.currentTimeMillis() + mySequence);
        thread.setName("Bulk-Graph-Writer--" + mySequence);
        thread.start();
    }


    public void stop() {
        done.set(true);
        synchronized (lock) {
            lock.notifyAll();
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
        throttle(); // important to throttle before we acquire inProgress monitor, calling it inside will most likely cause deadlock
        synchronized (inProgress) {
            BuilderProgress existingProgress = inProgress.get(key);
            if (existingProgress != null) {
                return new BuilderProgress(true, existingProgress.getFuture(), existingProgress.getBuilder());
            }
            ((PostStepBuilder) builder).setPostBuildTask(createInProgressCleanupTask(key));
            WriteTask<Node> task = addTaskToQueue(builder);
            BuilderProgress progress = new BuilderProgress(false, task, builder);
            inProgress.put(key, progress);
            return progress;
        }
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
        throttle();
        WriteTask<V> task = addTaskToQueue(builder);
        return task;
    }


    /**
     *
     * @throws InterruptedException
     */
    public void writeNextBulk() {

        if (currentBulk == null) {
            waitForAtLeastOneTaskInQueue();
            currentBulk = getNextBulk();
        }

        CountDownLatch countDownLatch = currentBulk.getCountDownLatch();

        long startTime = System.currentTimeMillis();

        bulkWriteToGraph(currentBulk);

        // signal that all pieces are completed
        countDownLatch.countDown();

        runTaskPoststeps(currentBulk);

        if (LOGGER.isTraceEnabled()) {
            int size;
            synchronized (inProgress) {
                size = inProgress.size();
            }
            LOGGER.trace("Completed bulk write of {} tasks in {} ms. There are now {} tasks in-progress", currentBulk.getTasks().size(), System.currentTimeMillis() - startTime, size);
        }

        currentBulk = null;
    }


    private void throttle() {
        synchronized (lock) {
            while (requestQueue.size() > MAX_PENDING_BUILDERS) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private <V> WriteTask<V> addTaskToQueue(Builder<V> builder) {
        WriteTask<V> task;
        synchronized (lock) {
            task = new WriteTask<>(nextBulk, builder);
            requestQueue.add(task);
            lock.notifyAll();
        }
        return task;
    }

    private void waitForAtLeastOneTaskInQueue() {
        synchronized (lock) {
            while (!done.get() && requestQueue.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private BulkState getNextBulk() {
        BulkState bulkState;
        synchronized (lock) {
            bulkState = new BulkState(nextBulk);
            WriteTask<?> task;
            // drain queue - it is very important to drain entire queue here, otherwise we end up with wrong count-down-latch assignments.
            // all tasks on queue belong to same count-down-latch!
            while ((task = requestQueue.poll()) != null) {
                bulkState.addBuilder(task);
            }

            nextBulk = new CountDownLatch(1);
            lock.notifyAll();
        }
        return bulkState;
    }

    private void bulkWriteToGraph(BulkState bulkState) {
        boolean success = false;

        try (Transaction tx = graphDb.beginTx()) {

            /*
             * Build all the pieces
             */

            for (WriteTask<?> task : bulkState.getTasks()) {
                task.performDirty(graphDb);
            }

            // commit in single transaction
            tx.success();
            success = true;

        } finally {
            if (!success) {
                // reset all builders
                for (WriteTask<?> task : bulkState.getTasks()) {
                    task.rollback();
                }
            }
        }
    }

    private void runTaskPoststeps(BulkState bulkState) {
        for (WriteTask<?> task : bulkState.getTasks()) {
            task.runPostStep();
        }
    }
}
