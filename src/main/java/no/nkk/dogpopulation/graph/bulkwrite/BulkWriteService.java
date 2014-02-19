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

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BulkWriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkWriteService.class);

    private final GraphDatabaseService graphDb;

    // guards nextBulk and requestQueue
    private final Object lock = new Object();

    // access is guarded by lock
    private final Queue<WriteTask<?>> requestQueue = new ArrayDeque<>();

    // access is guarded by lock
    private CountDownLatch nextBulk = new CountDownLatch(1);

    // access to this map is guarded by locking the map instance itself
    private final Map<String, Future<Node>> inProgress = new LinkedHashMap<>();


    public BulkWriteService(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (;;) {
                    writeNextBulk();
                }
            }
        }).start();
    }


    /**
     * Will perform cleanup of key when build is done and committed to graph.
     * Only one builder with the same key may ever exist on the queue when built using this method.
     *
     * @param key
     * @param builder
     * @return
     */
    public ConcurrentProgress build(final String key, Builder<Node> builder) {
        if (!(builder instanceof PostStepBuilder)) {
            throw new IllegalArgumentException("builder must be an instance of " + PostStepBuilder.class.getName());
        }
        synchronized (inProgress) {
            Future<Node> existingFuture = inProgress.get(key);
            if (existingFuture != null) {
                return new ConcurrentProgress(true, existingFuture);
            }
            ((PostStepBuilder) builder).setPostBuildTask(createInProgressCleanupTask(key));
            Future<Node> future = build(builder);
            inProgress.put(key, future);
            return new ConcurrentProgress(false, future);
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

        waitForAtLeastOneTaskInQueue();

        // queue might still be empty if we have multiple writer threads, that is ok.

        List<WriteTask<?>> tasks = new LinkedList<>();
        CountDownLatch countDownLatch = populateTaskList(tasks);

        long startTime = System.currentTimeMillis();

        bulkWriteToGraph(tasks);

        // signal that all pieces are completed
        countDownLatch.countDown();

        runTaskPoststeps(tasks);

        if (LOGGER.isTraceEnabled()) {
            int size;
            synchronized (inProgress) {
                size = inProgress.size();
            }
            LOGGER.trace("Completed bulk write of {} tasks in {} ms. There are now {} tasks in-progress", tasks.size(), System.currentTimeMillis() - startTime, size);
        }
    }


    private void throttle() {
        final int MAX_PENDING_BUILDERS = 10000;
        final int SECONDS_TO_WAIT = 2;
        synchronized (lock) {
            while (requestQueue.size() > MAX_PENDING_BUILDERS) {
                try {
                    lock.wait(SECONDS_TO_WAIT * 1000);
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
            if (requestQueue.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private CountDownLatch populateTaskList(List<WriteTask<?>> tasks) {
        CountDownLatch countDownLatch;
        synchronized (lock) {
            countDownLatch = nextBulk;
            WriteTask<?> task;
            // drain queue
            while ((task = requestQueue.poll()) != null) {
                tasks.add(task);
            }
            nextBulk = new CountDownLatch(1);
            lock.notifyAll();
        }
        return countDownLatch;
    }

    private void bulkWriteToGraph(List<WriteTask<?>> tasks) {
        try (Transaction tx = graphDb.beginTx()) {

            /*
             * Build all the pieces
             */

            for (WriteTask<?> task : tasks) {
                task.performDirty(graphDb);
            }

            // commit in single transaction
            tx.success();
        }
    }

    private void runTaskPoststeps(List<WriteTask<?>> tasks) {
        for (WriteTask<?> task : tasks) {
            task.runPostStep();
        }
    }
}
