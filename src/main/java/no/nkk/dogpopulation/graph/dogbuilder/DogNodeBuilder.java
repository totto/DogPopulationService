package no.nkk.dogpopulation.graph.dogbuilder;

import no.nkk.dogpopulation.graph.*;
import no.nkk.dogpopulation.importer.dogsearch.*;
import org.joda.time.DateTime;
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
public class DogNodeBuilder extends AbstractNodeBuilder implements PostStepBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DogNodeBuilder.class);

    private final BreedSynonymNodeCache breedSynonymNodeCache;

    private final String uuid;
    private String regNo;
    private Node breedNode;
    private String name;
    private DogGender gender;
    private LocalDate bornLocalDate;
    private String hdDiag;
    private LocalDate hdXray;
    private Runnable task;
    private String json;

    DogNodeBuilder(BreedSynonymNodeCache breedSynonymNodeCache, String uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("uuid cannot be null");
        }
        this.breedSynonymNodeCache = breedSynonymNodeCache;
        this.uuid = uuid;
    }

    @Override
    public void setPostBuildTask(Runnable task) {
        this.task = task;
    }

    @Override
    public Runnable getPostBuildTask() {
        return task;
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
        if (name == null) {
            throw new MissingFieldException("name");
        }

        Node dogNode = GraphUtils.findOrCreateNode(graphDb, DogGraphLabel.DOG, DogGraphConstants.DOG_UUID, uuid);

        boolean dogExists = false;

        if (dogNode.hasProperty(DogGraphConstants.DOG_NAME)) {
            dogExists = true;
        }

        // new dog

        dogNode.setProperty(DogGraphConstants.DOG_NAME, name);
        if (json != null) {
            dogNode.setProperty(DogGraphConstants.DOG_JSON, json);
        }
        if (gender != null) {
            dogNode.setProperty(DogGraphConstants.DOG_GENDER, gender.name().toLowerCase());
        }
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

        if (dogExists) {
            LOGGER.trace("Updated DOG properties of {}", uuid);
        } else {
            LOGGER.trace("Added DOG to graph {}", uuid);
        }

        return dogNode;
    }

    private Relationship connectToBreed(Node dogNode) {
        if (breedNode == null) {
            return null;
        }

        if (dogNode.hasRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING)) {
            // relationship already exists
            Relationship relationship = dogNode.getSingleRelationship(DogGraphRelationshipType.IS_BREED, Direction.OUTGOING);
            Node existingBreedNode = relationship.getEndNode();
            if (existingBreedNode.equals(breedNode)) {
                return relationship; // breed is correct - do nothing
            }

            String existingBreed = (String) existingBreedNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM);
            String breed = (String) breedNode.getProperty(DogGraphConstants.BREEDSYNONYM_SYNONYM);
            LOGGER.warn("BREED of dog \"{}\" changed from \"{}\" to \"{}\".", uuid, existingBreed, breed);
            relationship.delete();
        }

        // establish new relationship to breed
        Relationship relationship = dogNode.createRelationshipTo(breedNode, DogGraphRelationshipType.IS_BREED);
        return relationship;
    }

    public DogNodeBuilder all(DogDetails dogDetails) {
        return json(dogDetails).name(dogDetails).gender(dogDetails).breed(dogDetails).regNo(dogDetails).born(dogDetails).health(dogDetails.getHealth());
    }

    public DogNodeBuilder json(DogDetails dogDetails) {
        return json(dogDetails.getJson());
    }

    public DogNodeBuilder name(DogDetails dogDetails) {
        String name = dogDetails.getName();
        if (name == null) {
            name = "";
            LOGGER.warn("UNKNOWN name of dog, using empty name. {}.", dogDetails.getId());
        }
        return name(name);
    }

    public DogNodeBuilder gender(DogDetails dogDetails) {
        String genderStr = dogDetails.getGender();
        if (genderStr == null) {
            return this;
        }
        try {
            return gender(DogGender.valueOf(genderStr.toUpperCase()));
        } catch (RuntimeException ignore) {
            LOGGER.warn("Dog {} is neither male nor female, but {}", dogDetails.getId(), genderStr);
        }
        return this;
    }

    public DogNodeBuilder breed(DogDetails dogDetails) {
        String uuid = dogDetails.getId();
        DogBreed dogBreed = dogDetails.getBreed();
        String breedName;
        if (dogBreed == null) {
            LOGGER.debug("UNKNOWN breed of dog {}.", uuid);
            breedName = "UNKNOWN";
        } else {
            if (dogBreed.getName().trim().isEmpty()) {
                LOGGER.debug("Empty breed name, using UNKNOWN as breed for dog {}.", uuid);
                breedName = "UNKNOWN";
            } else {
                breedName = dogBreed.getName();
            }
        }
        return breed(breedSynonymNodeCache.getBreed(breedName));
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
        if (born.trim().isEmpty()) {
            return this;
        }
        try {
            DateTime dateTime = DateTime.parse(born);
            return born(dateTime);
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to parse born as DateTime: uuid={}, born={}", uuid, born);
        }
        return this;
    }

    public DogNodeBuilder health(DogHealth health) {
        if (health == null) {
            return this;
        }
        DogHealthHD[] hdArr = health.getHd();
        if (hdArr == null || hdArr.length == 0) {
            return this;
        }
        DogHealthHD hd = hdArr[0]; // TODO choose HD diagnosis more wisely than just picking the first one.
        hdDiag(hd.getDiagnosis());
        String xray = hd.getXray();
        if (xray == null) {
            return this;
        }
        if (xray.trim().isEmpty()) {
            return this;
        }
        try {
            DateTime dateTime = DateTime.parse(xray);
            hdXray(dateTime);
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to parse xRay as DateTime: uuid={}, xRay={}", uuid, xray);
        }
        return this;
    }


    public DogNodeBuilder json(String json) {
        this.json = json;
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
        if (name == null) {
            name = "";
            LOGGER.warn("UNKNOWN name of dog, using empty name. {}.", uuid);
        }
        this.name = name;
        return this;
    }
    public DogNodeBuilder gender(DogGender gender) {
        this.gender = gender;
        return this;
    }
    public DogNodeBuilder born(DateTime born) {
        this.bornLocalDate = born.toLocalDate();
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
    public DogNodeBuilder hdXray(DateTime hdXray) {
        this.hdXray = hdXray.toLocalDate();
        return this;
    }

    public String uuid() {
        return uuid;
    }
}
