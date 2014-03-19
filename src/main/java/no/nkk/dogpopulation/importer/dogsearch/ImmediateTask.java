package no.nkk.dogpopulation.importer.dogsearch;

import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class ImmediateTask<V> implements Callable<V> {
    
    private final V value;

    public ImmediateTask(V value) {
        this.value = value;
    }

    @Override
    public V call() throws Exception {
        return value;
    }
}
