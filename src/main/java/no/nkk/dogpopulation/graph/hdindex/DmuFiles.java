package no.nkk.dogpopulation.graph.hdindex;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuFiles {
    private final DmuDataFile dataFile;
    private final DmuPedigreeFile pedigreeFile;

    public DmuFiles(DmuDataFile dataFile, DmuPedigreeFile pedigreeFile) {
        this.dataFile = dataFile;
        this.pedigreeFile = pedigreeFile;
    }

    public DmuDataFile getDataFile() {
        return dataFile;
    }

    public DmuPedigreeFile getPedigreeFile() {
        return pedigreeFile;
    }
}
