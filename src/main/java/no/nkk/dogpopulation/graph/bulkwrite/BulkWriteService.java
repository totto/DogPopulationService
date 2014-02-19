package no.nkk.dogpopulation.graph.bulkwrite;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BulkWriteService {

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
    public ConcurrentProgress build(String key, Builder<Node> builder) {
        synchronized (inProgress) {
            Future<Node> existingFuture = inProgress.get(key);
            if (existingFuture != null) {
                return new ConcurrentProgress(true, existingFuture);
            }
            Future<Node> future = build(builder);
            inProgress.put(key, future);
            return new ConcurrentProgress(false, future);
        }
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

        bulkWriteToGraph(tasks);

        // signal that all pieces are completed
        countDownLatch.countDown();
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
}
