package no.nkk.dogpopulation.graph.hdindex;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuPedigreeFile {

    private final List<DmuPedigreeRecord> records = new ArrayList<>();

    public List<DmuPedigreeRecord> getRecords() {
        return records;
    }
}
