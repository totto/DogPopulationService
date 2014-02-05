package no.nkk.dogpopulation.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nkk.dogpopulation.Main;
import no.nkk.dogpopulation.dogsearch.*;
import no.nkk.dogpopulation.graph.GraphAdminService;
import no.nkk.dogpopulation.graph.ParentRole;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogImporter {

    private final DogSearchClient dogSearchClient;
    private final GraphAdminService graphAdminService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DogImporter(DogSearchClient dogSearchClient, GraphAdminService graphAdminService) {
        this.dogSearchClient = dogSearchClient;
        this.graphAdminService = graphAdminService;
    }

    public void importDogPedigree(String query) {
        query = "S38454/2006";
        DogSearchResponse response = dogSearchClient.findDog(query);
        DogSearchResponseBody body = response.getResponse();
        for (DogDocument dogDocument : body.getDocs()) {
            DogDetails dogDetails = dogDocument.toDogDetails(objectMapper);
            String uuid = dogDetails.getId();
            String name = dogDetails.getName();
            String breed = dogDetails.getBreed().getName();
            graphAdminService.addDog(uuid, name, breed);
            DogAncestry dogAncestry = dogDetails.getAncestry();
            DogParent father = dogAncestry.getFather();
            // TODO - recursively search for father and add to graph
            DogParent mother = dogAncestry.getMother();
            // TODO - recursively search for mother and add to graph
        }
    }

    public static void main(String... args) {
        GraphDatabaseService graphDb = Main.createGraphDb("data/dogdb");
        GraphAdminService graphAdminService = new GraphAdminService(graphDb);

        // TODO - Use dogsearch for import rather than this static test data

        String breed = "Rottweiler";
        String childUuid = "uuid-1234567890";
        String childName = "Wicked teeth Jr. III";
        String fatherUuid = "uuid-1234567891";
        String fatherName = "Wicked teeth Sr. II";
        String fathersFatherUuid = "uuid-1234567892";
        String fathersFatherName = "Wicked teeth I";
        String fathersMotherUuid = "uuid-1234567893";
        String fathersMotherName = "Daisy";
        String motherUuid = "uuid-1234567894";
        String motherName = "Tigerclaws";

        graphAdminService.addDog(childUuid, childName, breed);
        graphAdminService.addDog(fatherUuid, fatherName, breed);
        graphAdminService.connectChildToParent(childUuid, fatherUuid, ParentRole.FATHER);
        graphAdminService.addDog(fathersFatherUuid, fathersFatherName, breed);
        graphAdminService.connectChildToParent(fatherUuid, fathersFatherUuid, ParentRole.FATHER);
        graphAdminService.addDog(fathersMotherUuid, fathersMotherName, breed);
        graphAdminService.connectChildToParent(fatherUuid, fathersMotherUuid, ParentRole.MOTHER);
        graphAdminService.addDog(motherUuid, motherName, breed);
        graphAdminService.connectChildToParent(childUuid, motherUuid, ParentRole.MOTHER);

        graphDb.shutdown();
    }
}
