package no.nkk.dogpopulation.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * A task described by its toString() method. Useful for inspecting running tasks.
 *
 * @param <V>
 */
public class DescriptiveFutureTask<V> extends FutureTask<V> {

    private final Object descriptiveTask;

    public DescriptiveFutureTask(Callable<V> descriptiveTask) {
        super(descriptiveTask);
        this.descriptiveTask = descriptiveTask;
    }

    public DescriptiveFutureTask(Runnable descriptiveTask, V result) {
        super(descriptiveTask, result);
        this.descriptiveTask = descriptiveTask;
    }

    @Override
    public String toString() {
        return descriptiveTask.toString();
    }
}
