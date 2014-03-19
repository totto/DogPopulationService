package no.nkk.dogpopulation.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ExecutorServiceHelper implements ExecutorService {

    private final ExecutorService executorService;

    public ExecutorServiceHelper(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Submit a collection of tasks.
     *
     * @param tasks
     * @param <V>
     * @return
     */
    public <V> List<Future<V>> submit(Iterable<Callable<V>> tasks) {
        List<Future<V>> puppyFutures = new ArrayList<>();
        for (Callable<V> task : tasks) {
            puppyFutures.add(executorService.submit(task));
        }
        return puppyFutures;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return executorService.awaitTermination(l, timeUnit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> tCallable) {
        if (tCallable == null) {
            return null;
        }
        return executorService.submit(tCallable);
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T t) {
        return executorService.submit(runnable, t);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return executorService.submit(runnable);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables) throws InterruptedException {
        return executorService.invokeAll(callables);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit) throws InterruptedException {
        return executorService.invokeAll(callables, l, timeUnit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables) throws InterruptedException, ExecutionException {
        return executorService.invokeAny(callables);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> callables, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return executorService.invokeAny(callables, l, timeUnit);
    }

    @Override
    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }
}
