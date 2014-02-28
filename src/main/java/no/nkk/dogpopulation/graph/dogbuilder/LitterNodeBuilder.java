package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class LitterNodeBuilder extends AbstractNodeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(LitterNodeBuilder.class);

    private String parentId; // for debug purposes only
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

        Node litterNode;
        if (id.trim().isEmpty()) {
            // create a new litter node as we don't know the id of the litter
            litterNode = graphDb.createNode(DogGraphLabel.LITTER);
            litterNode.setProperty(DogGraphConstants.LITTER_ID, "");
        } else {
            // assume id is a globally unique (within the context of the graph) litter-id.
            litterNode = GraphUtils.getSingleNode(graphDb, DogGraphLabel.LITTER, DogGraphConstants.LITTER_ID, id);
            if (litterNode == null) {
                litterNode = graphDb.createNode(DogGraphLabel.LITTER);
                litterNode.setProperty(DogGraphConstants.LITTER_ID, id);
            } else {
                // possible conflict
                detectAndLogConflict(litterNode);
            }
        }

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

    private void detectAndLogConflict(Node alreadyExistingLitterNode) {
        boolean conflict = false;
        if (count != null && alreadyExistingLitterNode.hasProperty(DogGraphConstants.LITTER_COUNT)) {
            int existingCount = (Integer) alreadyExistingLitterNode.getProperty(DogGraphConstants.LITTER_COUNT);
            if (existingCount != count) {
                conflict = true;
            }
        }
        if (born != null && alreadyExistingLitterNode.hasProperty(DogGraphConstants.LITTER_YEAR)
                && alreadyExistingLitterNode.hasProperty(DogGraphConstants.LITTER_MONTH)
                && alreadyExistingLitterNode.hasProperty(DogGraphConstants.LITTER_DAY)) {
            int existingYear = (Integer) alreadyExistingLitterNode.getProperty(DogGraphConstants.LITTER_YEAR);
            if (existingYear != born.getYear()) {
                conflict = true;
            }
            int existingMonth = (Integer) alreadyExistingLitterNode.getProperty(DogGraphConstants.LITTER_MONTH);
            if (existingMonth != born.getMonthOfYear()) {
                conflict = true;
            }
            int existingDay = (Integer) alreadyExistingLitterNode.getProperty(DogGraphConstants.LITTER_DAY);
            if (existingDay!= born.getDayOfMonth()) {
                conflict = true;
            }
        }
        if (conflict) {
            LOGGER.warn("LITTER conflict on id {}", id);
        }
    }


    public LitterNodeBuilder born(String born) {
        if (born == null) {
            return this;
        }
        try {
            DateTime dateTime = DateTime.parse(born);
            return born(dateTime);
        } catch (Exception e) {
            LOGGER.warn("Unable to parse born as DateTime: parentId={}, litterId={}, born={}", parentId, id, born);
        }
        return this;
    }

    public LitterNodeBuilder parentId(String parentId) {
        this.parentId = parentId;
        return this;
    }
    public LitterNodeBuilder id(String id) {
        this.id = id;
        return this;
    }
    public LitterNodeBuilder born(DateTime born) {
        this.born = born.toLocalDate();
        return this;
    }
    public LitterNodeBuilder count(Integer count) {
        this.count = count;
        return this;
    }

}
