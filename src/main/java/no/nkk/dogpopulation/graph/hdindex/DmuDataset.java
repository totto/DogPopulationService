package no.nkk.dogpopulation.graph.hdindex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


    void filterByRelationToDogWithHDxrayImageTaken() {
        Set<Long> includeFilter = new HashSet<>();
        for (Map.Entry<Long, DmuPedigreeRecord> e : pedigreeRecords.entrySet()) {
            Set<Long> pedigree = new HashSet<>();
            if (includePedigreeIfRelatedToDogWithXray(pedigree, e.getValue())) {
                includeFilter.addAll(pedigree);
            }
        }
        filterPedigreeRecords(includeFilter);
    }

    private boolean includePedigreeIfRelatedToDogWithXray(Set<Long> pedigree, DmuPedigreeRecord dmuPedigreeRecord) {
        pedigree.add(dmuPedigreeRecord.getId());
        boolean include = false;
        if (dataRecords.containsKey(dmuPedigreeRecord.getId())) {
            include = true;
        }
        include |= includeParent(pedigree, dmuPedigreeRecord.getFatherId());
        include |= includeParent(pedigree, dmuPedigreeRecord.getMotherId());
        return include;
    }

    private boolean includeParent(Set<Long> pedigree, long parentId) {
        if (parentId < 0) {
            return false;
        }
        DmuPedigreeRecord parentRecord = pedigreeRecords.get(parentId);
        if (parentRecord == null) {
            return false;
        }
        return includePedigreeIfRelatedToDogWithXray(pedigree, parentRecord);
    }

    private void filterPedigreeRecords(Set<Long> includeFilter) {
        Iterator<Map.Entry<Long, DmuPedigreeRecord>> iterator = pedigreeRecords.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, DmuPedigreeRecord> entry = iterator.next();
            if (!includeFilter.contains(entry.getKey())) {
                iterator.remove();
            }
        }
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
