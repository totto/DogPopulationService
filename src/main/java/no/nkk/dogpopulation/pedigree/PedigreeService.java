package no.nkk.dogpopulation.pedigree;

import no.nkk.dogpopulation.graph.Dog;
import no.nkk.dogpopulation.graph.GraphQueryService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class PedigreeService {

    private final GraphQueryService graphQueryService;

    public PedigreeService(GraphQueryService graphQueryService) {
        this.graphQueryService = graphQueryService;
    }

    public Dog getPedigree(String uuid) {
        Dog dog = graphQueryService.getPedigree(uuid);
        return dog;
    }
}
