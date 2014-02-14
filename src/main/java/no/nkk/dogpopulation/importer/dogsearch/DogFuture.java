package no.nkk.dogpopulation.importer.dogsearch;

import org.neo4j.graphdb.Node;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogFuture {

    private final Node dog;

    private final Future<DogFuture> fatherFuture;
    private final Future<DogFuture> motherFuture;

    private AtomicReference<Future<Node>[]> puppyFuturesRef = new AtomicReference<Future<Node>[]>(new Future[0]);

    DogFuture(Node dog, Future<DogFuture> fatherFuture, Future<DogFuture> motherFuture) {
        this.dog = dog;
        this.fatherFuture = fatherFuture;
        this.motherFuture = motherFuture;
    }

    Node getDog() {
        return dog;
    }

    private Future<Node>[] getPuppyFutures() {
        return puppyFuturesRef.get();
    }

    private Node getFather() {
        return getParent(fatherFuture);
    }

    private Node getMother() {
        return getParent(motherFuture);
    }

    private Node getParent(Future<DogFuture> parentFuture) {
        if (parentFuture == null) {
            return null;
        }
        try {
            DogFuture nodeFuture = parentFuture.get();
            if (nodeFuture == null) {
                return null;
            }
            nodeFuture.getFather(); // wait recursively
            nodeFuture.getMother(); // wait recursively
            nodeFuture.waitOnPuppies();
            return nodeFuture.getDog();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(e);
        }
    }

    private void waitOnPuppies() {
        for (Future<Node> puppyFuture : getPuppyFutures()) {
            try {
                puppyFuture.get(); // wait on all puppies
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw new RuntimeException(e);
            }
        }
    }

    public void waitForPedigreeImportToComplete() {
        getFather();
        getMother();
        waitOnPuppies();
    }
}
