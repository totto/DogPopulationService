package no.nkk.dogpopulation.graph.bulkwrite;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BulkState {
    private final CountDownLatch countDownLatch;
    private final List<WriteTask<?>> tasks = new LinkedList<>();

    public BulkState(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public void addBuilder(WriteTask<?> builder) {
        tasks.add(builder);
    }

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    public List<WriteTask<?>> getTasks() {
        return tasks;
    }
}
