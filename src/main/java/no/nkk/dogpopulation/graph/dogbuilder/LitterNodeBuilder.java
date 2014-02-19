package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class LitterNodeBuilder extends AbstractNodeBuilder {

    private String id;
    private LocalDate born;
    private Integer count;

    LitterNodeBuilder() {
    }

    @Override
    protected Node doBuild(GraphDatabaseService graphDb) {
        if (id == null) {
            throw new MissingFieldException("id");
        }

        Node litterNode = findOrCreateLitterNode(graphDb, id);
        if (count != null) {
            litterNode.setProperty(DogGraphConstants.LITTER_COUNT, count);
        }
        if (born != null) {
            litterNode.setProperty(DogGraphConstants.LITTER_YEAR, born.getYear());
            litterNode.setProperty(DogGraphConstants.LITTER_MONTH, born.getMonthOfYear());
            litterNode.setProperty(DogGraphConstants.LITTER_DAY, born.getDayOfMonth());
        }
        return litterNode;

    }

    private Node findOrCreateLitterNode(GraphDatabaseService graphDb, String litterId) {
        Node litter = GraphUtils.getSingleNode(graphDb, DogGraphLabel.LITTER, DogGraphConstants.LITTER_ID, litterId);
        if (litter != null) {
            return litter;
        }
        litter = graphDb.createNode(DogGraphLabel.LITTER);
        litter.setProperty(DogGraphConstants.LITTER_ID, litterId);
        return litter;
    }


    public LitterNodeBuilder id(String id) {
        this.id = id;
        return this;
    }
    public LitterNodeBuilder born(LocalDate born) {
        this.born = born;
        return this;
    }
    public LitterNodeBuilder count(int count) {
        this.count = count;
        return this;
    }

}
