package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.DogGraphConstants;
import no.nkk.dogpopulation.graph.DogGraphLabel;
import no.nkk.dogpopulation.graph.DogGraphRelationshipType;
import no.nkk.dogpopulation.graph.GraphUtils;
import no.nkk.dogpopulation.importer.dogsearch.*;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogNodeBuilder extends AbstractNodeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogNodeBuilder.class);

    private final CommonNodes commonNodes;

    private String uuid;
    private String regNo;
    private Node breedNode;
    private String name;
    private LocalDate bornLocalDate;
    private String hdDiag;
    private LocalDate hdXray;

    DogNodeBuilder(CommonNodes commonNodes) {
        this.commonNodes = commonNodes;
    }

    @Override
    protected Node doBuild(GraphDatabaseService graphDb) {
        Node dogNode = addDog(graphDb);
        connectToBreed(dogNode);
        return dogNode;
    }

    private Node addDog(GraphDatabaseService graphDb) {
        if (uuid == null) {
            throw new MissingFieldException("uuid");
        }

        Node dogNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);

        if (dogNode.hasProperty(DogGraphConstants.DOG_NAME)) {
            LOGGER.trace("DOG already exists, properties will not be updated");
            return dogNode;
        }

        // new dog

        dogNode.setProperty(DogGraphConstants.DOG_NAME, name);
        if (regNo != null) {
            dogNode.setProperty(DogGraphConstants.DOG_REGNO, regNo);
        }
        if (bornLocalDate != null) {
            dogNode.setProperty(DogGraphConstants.DOG_BORN_YEAR, bornLocalDate.getYear());
            dogNode.setProperty(DogGraphConstants.DOG_BORN_MONTH, bornLocalDate.getMonthOfYear());
            dogNode.setProperty(DogGraphConstants.DOG_BORN_DAY, bornLocalDate.getDayOfMonth());
        }
        if (hdDiag != null) {
            dogNode.setProperty(DogGraphConstants.DOG_HDDIAG, hdDiag);
        }
        if (hdXray != null) {
            dogNode.setProperty(DogGraphConstants.DOG_HDYEAR, hdXray.getYear());
        }

        LOGGER.trace("Added DOG to graph {}", uuid);

        return dogNode;
    }

    private Relationship connectToBreed(Node dogNode) {

        if (dogNode.hasRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING)) {
            // relationship already exists
            Relationship relationship = dogNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
            Node existingBreedNode = relationship.getEndNode();
            if (existingBreedNode.equals(breedNode)) {
                return relationship; // breed is correct - do nothing
            }

            String existingBreed = (String) existingBreedNode.getProperty(DogGraphConstants.BREED_BREED);
            LOGGER.warn("BREED of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingBreed, breedNode);
            relationship.delete();
        }

        // establish new relationship to breed
        Relationship relationship = dogNode.createRelationshipTo(breedNode, DogGraphRelationshipType.IS_BREED);
        return relationship;
    }

    public DogNodeBuilder all(DogDetails dogDetails) {
        return uuid(dogDetails).name(dogDetails).breed(dogDetails).regNo(dogDetails).born(dogDetails).health(dogDetails.getHealth());
    }

    public DogNodeBuilder uuid(DogDetails dogDetails) {
        return uuid(dogDetails.getId());
    }

    public DogNodeBuilder name(DogDetails dogDetails) {
        String name = dogDetails.getName();
        if (name == null) {
            name = "";
            LOGGER.warn("UNKNOWN name of dog, using empty name. {}.", dogDetails.getId());
        }
        return name(name);
    }

    public DogNodeBuilder breed(DogDetails dogDetails) {
        String uuid = dogDetails.getId();
        DogBreed dogBreed = dogDetails.getBreed();
        String breedName;
        String breedId = null;
        if (dogBreed == null) {
            LOGGER.warn("UNKNOWN breed of dog {}.", uuid);
            breedName = "UNKNOWN";
        } else {
            breedId = dogBreed.getId();
            if (dogBreed.getName().trim().isEmpty()) {
                LOGGER.warn("UNKNOWN breed of dog {}.", uuid);
                breedName = "UNKNOWN";
            } else {
                breedName = dogBreed.getName();
            }
        }
        return breed(commonNodes.getBreed(breedName, breedId));
    }

    public DogNodeBuilder regNo(DogDetails dogDetails) {
        for (DogId dogId : dogDetails.getIds()) {
            if (DogGraphConstants.DOG_REGNO.equalsIgnoreCase(dogId.getType())) {
                return regNo(dogId.getValue());
            }
        }
        return this;
    }

    public DogNodeBuilder born(DogDetails dogDetails) {
        String born = dogDetails.getBorn();
        if (born == null) {
            return this;
        }
        try {
            LocalDate localDate = LocalDate.parse(born.substring(0, 10));
            return born(localDate);
        } catch (RuntimeException ignore) {
        }
        return this;
    }

    public DogNodeBuilder health(DogHealth health) {
        if (health == null) {
            return this;
        }
        DogHealthHD[] hdArr = health.getHd();
        if (hdArr != null && hdArr.length > 0) {
            DogHealthHD hd = hdArr[0]; // TODO choose HD diagnosis more wisely than just picking the first one.
            hdDiag(hd.getDiagnosis());
            try {
                LocalDate localDate = LocalDate.parse(hd.getXray().substring(0, 10));
                hdXray(localDate);
            } catch (RuntimeException ignore) {
            }
        }
        return this;
    }


    public DogNodeBuilder uuid(String uuid) {
        this.uuid = uuid;
        return this;
    }
    public DogNodeBuilder regNo(String regNo) {
        this.regNo = regNo;
        return this;
    }
    public DogNodeBuilder breed(Node breedNode) {
        this.breedNode = breedNode;
        return this;
    }
    public DogNodeBuilder name(String name) {
        this.name = name;
        return this;
    }
    public DogNodeBuilder born(LocalDate bornLocalDate) {
        this.bornLocalDate = bornLocalDate;
        return this;
    }
    public DogNodeBuilder hdDiag(String hdDiag) {
        this.hdDiag = hdDiag;
        return this;
    }
    public DogNodeBuilder hdXray(LocalDate hdXray) {
        this.hdXray = hdXray;
        return this;
    }

}
