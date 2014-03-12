package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedNodeBuilder extends AbstractNodeBuilder {

    private String fci;
    private String nkk;
    private String club;
    private String name;

    BreedNodeBuilder() {
    }

    @Override
    protected Node doBuild(GraphDatabaseService graphDb) {
        if (name == null) {
            throw new MissingFieldException("name");
        }
        if (fci == null) {
            throw new MissingFieldException("fci");
        }

        Node breedNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED, DogGraphConstants.BREED_FCI_BREED_ID, fci);
        breedNode.setProperty(DogGraphConstants.BREED_BREED_NAME, name);
        if (nkk != null) {
            breedNode.setProperty(DogGraphConstants.BREED_NKK_BREED_ID, nkk);
        } else if (breedNode.hasProperty(DogGraphConstants.BREED_NKK_BREED_ID)) {
            breedNode.removeProperty(DogGraphConstants.BREED_NKK_BREED_ID);
        }
        if (club != null) {
            breedNode.setProperty(DogGraphConstants.BREED_CLUB_ID, club);
        } else if (breedNode.hasProperty(DogGraphConstants.BREED_CLUB_ID)) {
            breedNode.removeProperty(DogGraphConstants.BREED_CLUB_ID);
        }
        return breedNode;
    }


    public BreedNodeBuilder club(String club) {
        if (club != null) {
            this.club = club.trim();
        }
        return this;
    }
    public BreedNodeBuilder nkk(String nkk) {
        if (nkk != null) {
            this.nkk = nkk.trim();
        }
        return this;
    }
    public BreedNodeBuilder fci(String fci) {
        if (fci != null) {
            this.fci = fci.trim();
        }
        return this;
    }
    public BreedNodeBuilder name(String name) {
        if (name != null) {
            this.name = name.trim();
        }
        return this;
    }

}
