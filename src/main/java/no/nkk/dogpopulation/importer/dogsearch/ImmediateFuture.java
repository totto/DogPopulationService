package no.nkk.dogpopulation.importer.dogsearch;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ImmediateFuture<V> implements Future<V> {

    private final V value;

    public ImmediateFuture(V value) {
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
        return true;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return value;
    }

    @Override
    public V get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }
}
