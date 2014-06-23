package no.nkk.dogpopulation.graph.hdindex;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Writes a collection of data-records to file
 */
public class DmuDatasetWriter {

    private final File dataFile;
    private final File pedigreeFile;
    private final File uuidMappingFile;
    private final File breedCodeMappingFile;
    private final File dataErrorFile;


    public DmuDatasetWriter(File dataFile, File pedigreeFile, File uuidMappingFile, File breedCodeMappingFile, File dataErrorFile) {
        this.dataFile = dataFile;
        this.pedigreeFile = pedigreeFile;
        this.uuidMappingFile = uuidMappingFile;
        this.breedCodeMappingFile = breedCodeMappingFile;
        this.dataErrorFile = dataErrorFile;
    }


    public void writeFiles(DmuDataset dmuDataset) {
        boolean success = false;
        try {
            writeRecordsToFile(dmuDataset.getBreedCodeRecords(), breedCodeMappingFile);
            writeRecordsToFile(dmuDataset.getUuidRecords().values(), uuidMappingFile);
            writeRecordsToFile(dmuDataset.getDataErrorRecords().values(), dataErrorFile);
            writeRecordsToFile(dmuDataset.getDataRecords().values(), dataFile);
            writeRecordsToFile(dmuDataset.getPedigreeRecords().values(), pedigreeFile);
            success = true;
        } finally {
            if (!success) {
                final File folder = dataFile.getParentFile();
                breedCodeMappingFile.delete();
                uuidMappingFile.delete();
                dataErrorFile.delete();
                dataFile.delete();
                pedigreeFile.delete();
                folder.delete();
            }
        }
    }


    void writeRecordsToFile(Iterable<? extends DmuWritableRecord> records, File file) {
        boolean success = false;
        try(PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Charset.forName("ISO-8859-1")))) {
            for (DmuWritableRecord record : records) {
                record.writeTo(out);
            }
            out.flush();
            success = true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (!success) {
                file.delete();
            }
        }
    }

}
