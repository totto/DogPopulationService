package no.nkk.dogpopulation.graph.hdindex;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import no.nkk.dogpopulation.AbstractGraphTest;
import no.nkk.dogpopulation.graph.DogGender;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuHdIndexAlgorithmTest extends AbstractGraphTest {

    File dataFile;
    File pedigreeFile;
    File uuidMappingFile;
    File breedCodeMappingFile;

    @Inject
    @Named("hdindex-folder")
    String hdIndexFolderPath;

    @BeforeMethod
    public void setup() {
        new File(hdIndexFolderPath).mkdirs();
        dataFile = new File(hdIndexFolderPath, "data.txt");
        pedigreeFile = new File(hdIndexFolderPath, "pedigree.txt");
        uuidMappingFile = new File(hdIndexFolderPath, "uuidmapping.txt");
        breedCodeMappingFile = new File(hdIndexFolderPath, "breedcodemapping.txt");
    }

    @Test
    public void thatNonEmptyFilesAreProduced() throws IOException {
        // given
        String breed1 = "Breed1";
        addBreed(breed1, "1");
        addDog("A", "NO/00777/95", "2008-02-26", DogGender.MALE, breed1, "A2", "2010-06-10");
        addDog("B", "NO/00777/94", "2005-10-02", DogGender.MALE, breed1, "A2", "2007-06-11");
        addDog("C", "NO/00777/93", "2005-07-11", DogGender.FEMALE, breed1, "B2", "2007-06-12");
        addDog("D", "NO/00777/92", "2002-02-19", DogGender.MALE, breed1, "B1", "2004-06-13");
        addDog("E", "NO/00777/91", "2002-03-14", DogGender.FEMALE, breed1, "A2", "2004-06-14");
        addDog("F", "NO/00777/90", "2002-02-21", DogGender.MALE, breed1, "C2", "2004-06-15");
        addDog("G", "NO/00777/89", "2002-03-01", DogGender.FEMALE, breed1, "A2", "2004-06-16");
        connectChildToFather("A", "B");
        connectChildToMother("A", "C");
        connectChildToFather("B", "D");
        connectChildToMother("B", "E");
        connectChildToFather("C", "F");
        connectChildToMother("C", "G");

        Set<String> breedSet = new LinkedHashSet<>();
        breedSet.add(breed1);

        // when
        graphQueryService.writeDmuFiles(dataFile, pedigreeFile, uuidMappingFile, breedCodeMappingFile, breedSet);

        // then
       Assert.assertTrue(dataFile.isFile());
       Assert.assertTrue(dataFile.length() > 0);
       Assert.assertTrue(pedigreeFile.isFile());
       Assert.assertTrue(pedigreeFile.length() > 0);
       Assert.assertTrue(uuidMappingFile.isFile());
       Assert.assertTrue(uuidMappingFile.length() > 0);
       Assert.assertTrue(breedCodeMappingFile.isFile());
       Assert.assertTrue(breedCodeMappingFile.length() > 0);
    }

}
