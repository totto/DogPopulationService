package no.nkk.dogpopulation.importer.dogsearch;

import no.nkk.dogpopulation.graph.Builder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogFuture {

    private final Builder<Node> dogBuilder;
    private final Future<Node> dog;

    private final Future<DogFuture> fatherFuture;
    private final Future<DogFuture> motherFuture;

    private final List<Future<Future<Relationship>>> puppyFutures;

    DogFuture(Builder<Node> dogBuilder, Future<Node> dog) {
        this(dogBuilder, dog, null, null, null);
    }

    DogFuture(Builder<Node> dogBuilder, Future<Node> dog, Future<DogFuture> fatherFuture, Future<DogFuture> motherFuture, List<Future<Future<Relationship>>> puppyFutures) {
        this.dogBuilder = dogBuilder;
        this.dog = dog;
        this.fatherFuture = fatherFuture;
        this.motherFuture = motherFuture;
        this.puppyFutures = puppyFutures;
    }

    public Builder<Node> getDogBuilder() {
        return dogBuilder;
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
            return nodeFuture.waitOnDog();
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
        if (puppyFutures == null) {
            return;
        }
        for (Future<Future<Relationship>> puppyFuture : puppyFutures) {
            if (puppyFuture == null) {
                continue;
            }
            try {
                Future<Relationship> nodeFuture = puppyFuture.get(); // wait on external-search
                if (nodeFuture == null) {
                    continue;
                }
                nodeFuture.get(); // wait on graph bulk-write operation
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

    public Node waitForPedigreeImportToComplete() {
        getFather();
        getMother();
        waitOnPuppies();
        return waitOnDog();
    }

    private Node waitOnDog() {
        try {
            return dog.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
