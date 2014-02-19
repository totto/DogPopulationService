package no.nkk.dogpopulation.graph.bulkwrite;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BulkState {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final List<WriteTask<?>> requests = new LinkedList<>();

    public void addBuilder(WriteTask<?> builder) {
        requests.add(builder);
    }
}
