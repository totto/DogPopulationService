package no.nkk.dogpopulation.graph.hdindex;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuDataFile {

    private final List<DmuDataRecord> records = new ArrayList<>();

    public List<DmuDataRecord> getRecords() {
        return records;
    }
}
