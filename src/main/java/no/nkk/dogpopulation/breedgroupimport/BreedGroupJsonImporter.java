package no.nkk.dogpopulation.breedgroupimport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.net.URL;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedGroupJsonImporter {

    private final URL url;
    private final GraphDatabaseService graphDb;

    @Inject
    public BreedGroupJsonImporter(@Named("breedJsonUrl") URL url, GraphDatabaseService graphDb) {
        this.url = url;
        this.graphDb = graphDb;
    }

    public void importBreedGroup() {
        BreedGroupFile breedGroupFile = parseBreedGroupFile();

        try (Transaction tx = graphDb.beginTx()) {
            Node breedGroupsCategory = createBreedGroupsCategoryNode();

            for (BreedDefinition breedDefinition : breedGroupFile.getBreed()) {
                importBreedDefinition(breedGroupsCategory, breedDefinition);
            }

            tx.success();
        }
    }

    private void importBreedDefinition(Node breedGroupsCategory, BreedDefinition breedDefinition) {
        Node breedGroupNode = createFCIBreedGroupNode(breedGroupsCategory, breedDefinition.getGroup());
        Node breedNode = createBreedNode(breedGroupNode, breedDefinition.getName());
        for (BreedId breedId : breedDefinition.getIds()) {
            assignBreedId(breedNode, breedId);
        }
        for (String breedSynonym : breedDefinition.getThesaurus()) {
            assignSynonym(breedNode, breedSynonym);
        }
    }

    private Node createBreedNode(Node breedGroupNode, String breedName) {
        Node breedNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED, DogGraphConstants.BREED_BREED_NAME, breedName);
        breedNode.setProperty(DogGraphConstants.BREED_BREED_NAME, breedName);
        if (!breedNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breedNode.createRelationshipTo(breedGroupNode, DogGraphRelationshipType.MEMBER_OF);
        }
        return breedNode;
    }

    private void assignBreedId(Node breedGroupNode, BreedId breedId) {
        switch (breedId.getType()) {
            case "NKK":
                breedGroupNode.setProperty(DogGraphConstants.BREED_NKK_BREED_ID, breedId.getValue());
                break;
            case "FCI":
                breedGroupNode.setProperty(DogGraphConstants.BREED_FCI_BREED_ID, breedId.getValue());
                break;
            case "club":
                breedGroupNode.setProperty(DogGraphConstants.BREED_CLUB_ID, breedId.getValue());
                break;
        }
    }

    private void assignSynonym(Node breedGroupNode, String breedSynonym) {
        Node breedSynonymNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED_SYNONYM, DogGraphConstants.BREEDSYNONYM_SYNONYM, breedSynonym);
        if (!breedSynonymNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breedSynonymNode.createRelationshipTo(breedGroupNode, DogGraphRelationshipType.MEMBER_OF);
        }
    }

    private Node createFCIBreedGroupNode(Node breedGroupsCategory, String fciBreedGroup) {
        Node breedGroupNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.BREED_GROUP, DogGraphConstants.BREEDGROUP_FCIBREEDGROUP, fciBreedGroup);
        if (!breedGroupNode.hasRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING)) {
            breedGroupNode.createRelationshipTo(breedGroupsCategory, DogGraphRelationshipType.MEMBER_OF);
        }
        return breedGroupNode;
    }

    private Node createBreedGroupsCategoryNode() {
        Node rootCategory = createRootCategoryNode();
        Node breedGroupsCategory = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_BREEDGROUPS);
        if (!breedGroupsCategory.hasRelationship(DogGraphRelationshipType.MEMBER_OF)) {
            breedGroupsCategory.createRelationshipTo(rootCategory, DogGraphRelationshipType.MEMBER_OF);
        }
        return breedGroupsCategory;
    }

    private Node createRootCategoryNode() {
        return GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.CATEGORY, DogGraphConstants.CATEGORY_CATEGORY, DogGraphConstants.CATEGORY_CATEGORY_ROOT);
    }

    private BreedGroupFile parseBreedGroupFile() {
        BreedGroupFile breedGroupFile;
        try {
            breedGroupFile = new ObjectMapper().readValue(url, BreedGroupFile.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return breedGroupFile;
    }
}
