package no.nkk.dogpopulation.graph.bulkwrite;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.concurrent.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class WriteTask<V> implements Future<V> {

    private final CountDownLatch countDownLatch;
    private final Builder<V> builder;
    private V value;

    public WriteTask(CountDownLatch countDownLatch, Builder<V> builder) {
        this.countDownLatch = countDownLatch;
        this.builder = builder;
    }

    void performDirty(GraphDatabaseService graphDb) {
        V value = builder.build(graphDb);
        // Value is dirty and must be unavailable until graph-database transaction is committed, signalled with count-down-latch.
        this.value = value;
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return countDownLatch.getCount() <= 0;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        return value;
    }

    @Override
    public V get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        countDownLatch.await(l, timeUnit);
        return value;
    }
}
