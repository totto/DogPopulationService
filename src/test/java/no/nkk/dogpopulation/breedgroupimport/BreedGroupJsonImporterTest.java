package no.nkk.dogpopulation.breedgroupimport;

import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BreedGroupJsonImporterTest extends AbstractGraphTest {

    private static final int GROUPS_IN_FILE = 10;
    private static final int BREED_IN_FILE = 411;
    private static final int SYNONYMS_IN_FILE = 923;

    @Test(groups = "fast")
    public void thatJsonCanBeImportedIntoEmptyDatabase() throws IOException {
        //URL url = new URL("https://raw.githubusercontent.com/NKK-IT-Utvikling/Breedmapping/master/Raser.json");
         URL url = new File("src/test/resources/breedimport/Raser.json").toURI().toURL();
        BreedGroupJsonImporter importer = new BreedGroupJsonImporter(url, graphDb);
        importer.importBreedGroup();

        validateBreedSynonymCount(SYNONYMS_IN_FILE);
        validateBreedCount(BREED_IN_FILE);
        validateFCIBreedGroupCount(GROUPS_IN_FILE);
    }

    @Test(groups = "fast")
    public void thatJsonCanBeImportedWithExistingBreedSynonyms() throws IOException {
        Node australianCattleDogSynonymNode;
        try (Transaction tx = graphDb.beginTx()) {
            australianCattleDogSynonymNode = graphDb.createNode(DogGraphLabel.BREED_SYNONYM);
            australianCattleDogSynonymNode.setProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM, "australian cattledog"); // will also be in the imported json file
            Node bullshitBreedSynonym = graphDb.createNode(DogGraphLabel.BREED_SYNONYM);
            bullshitBreedSynonym.setProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM, "BullShitter deluxe");
            addDog("MyCattledog", australianCattleDogSynonymNode);
            addDog("MyBullShitter", bullshitBreedSynonym);
            tx.success();
        }

        validateBreedSynonymCount(2); // australian cattledog and BullShitter deluxe
        validateBreedCount(0);
        validateFCIBreedGroupCount(0);

        URL url = new File("src/test/resources/breedimport/Raser.json").toURI().toURL();
        BreedGroupJsonImporter importer = new BreedGroupJsonImporter(url, graphDb);
        importer.importBreedGroup();

        validateBreedSynonymCount(1 + SYNONYMS_IN_FILE); // australian cattledog was added both programatically and through file import and should not be counted twice
        validateBreedCount(BREED_IN_FILE);
        validateFCIBreedGroupCount(GROUPS_IN_FILE);

        try (Transaction tx = graphDb.beginTx()) {
            Relationship memberOfBreed = australianCattleDogSynonymNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            Assert.assertNotNull(memberOfBreed);
            Node breedNode = memberOfBreed.getEndNode();
            Assert.assertEquals((String) breedNode.getProperty(DogGraphConstants.BREED_BREED_NAME), "Australian Cattledog");
            Assert.assertEquals((String) breedNode.getProperty(DogGraphConstants.BREED_FCI_BREED_ID), "287");
            Assert.assertEquals((String) breedNode.getProperty(DogGraphConstants.BREED_NKK_BREED_ID), "402");
            Assert.assertEquals((String) breedNode.getProperty(DogGraphConstants.BREED_CLUB_ID), "318000");
            Relationship memberOfBreedGroup = breedNode.getSingleRelationship(DogGraphRelationshipType.MEMBER_OF, Direction.OUTGOING);
            Assert.assertNotNull(memberOfBreedGroup);
            Node breedGroupNode = memberOfBreedGroup.getEndNode();
            Assert.assertEquals((String) breedGroupNode.getProperty(DogGraphConstants.BREEDGROUP_FCIBREEDGROUP), "1");
            tx.success();
        }
    }

    private void validateBreedSynonymCount(int expected) {
        ExecutionResult synonymCount = executionEngine.execute("MATCH (n:BREED_SYNONYM) RETURN count(n)");
        int n = 0;
        for (Map<String, Object> row : IteratorUtil.asIterable(synonymCount.iterator())) {
            Assert.assertEquals(((Long) row.get("count(n)")).intValue(), expected);
            n++;
        }
        Assert.assertEquals(n, 1);
    }

    private void validateBreedCount(int expected) {
        ExecutionResult breedCount = executionEngine.execute("MATCH (n:BREED) RETURN count(n)");
        int n = 0;
        for (Map<String, Object> row : IteratorUtil.asIterable(breedCount.iterator())) {
            Assert.assertEquals(((Long) row.get("count(n)")).intValue(), expected);
            n++;
        }
        Assert.assertEquals(n, 1);
    }

    private void validateFCIBreedGroupCount(int expected) {
        ExecutionResult breedGroupCount = executionEngine.execute("MATCH (n:BREED_GROUP) RETURN count(n)");
        int n = 0;
        for (Map<String, Object> row : IteratorUtil.asIterable(breedGroupCount.iterator())) {
            Assert.assertEquals(((Long) row.get("count(n)")).intValue(), expected);
            n++;
        }
        Assert.assertEquals(n, 1);
    }
}
