package no.nkk.dogpopulation.graph.hdindex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DmuDataset {

    private final Map<Long, DmuDataRecord> dataRecords = new LinkedHashMap<>();
    private final Map<Long, DmuPedigreeRecord> pedigreeRecords = new LinkedHashMap<>();
    private final Map<Long, DmuDataErrorRecord> dataErrorRecords = new LinkedHashMap<>();
    private final Map<Long, DmuUuidRecord> uuidRecords = new LinkedHashMap<>();
    private final List<DmuBreedCodeRecord> breedCodeRecords = new ArrayList<>();

    void add(DmuDataRecord record) {
        dataRecords.put(record.getId(), record);
    }

    void add(DmuPedigreeRecord record) {
        pedigreeRecords.put(record.getId(), record);
    }

    void add(DmuDataErrorRecord record) {
        dataErrorRecords.put(record.getId(), record);
    }

    void add(DmuUuidRecord record) {
        uuidRecords.put(record.getId(), record);
    }

    void add(DmuBreedCodeRecord record) {
        breedCodeRecords.add(record);
    }

    Map<Long, DmuDataRecord> getDataRecords() {
        return dataRecords;
    }

    Map<Long, DmuPedigreeRecord> getPedigreeRecords() {
        return pedigreeRecords;
    }

    Map<Long, DmuDataErrorRecord> getDataErrorRecords() {
        return dataErrorRecords;
    }

    Map<Long, DmuUuidRecord> getUuidRecords() {
        return uuidRecords;
    }

    List<DmuBreedCodeRecord> getBreedCodeRecords() {
        return breedCodeRecords;
    }

}
