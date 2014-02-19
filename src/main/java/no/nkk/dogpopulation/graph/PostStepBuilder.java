package no.nkk.dogpopulation.graph;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public interface PostStepBuilder {

    void setPostBuildTask(Runnable task);
    Runnable getPostBuildTask();
}
