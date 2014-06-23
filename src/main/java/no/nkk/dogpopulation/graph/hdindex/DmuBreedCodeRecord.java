package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

public class DmuBreedCodeRecord implements DmuWritableRecord {
    private final int breedCode;
    private final String breedName;

    public DmuBreedCodeRecord(int breedCode, String breedName) {
        this.breedCode = breedCode;
        this.breedName = breedName;
    }

    @Override
    public void writeTo(PrintWriter out) {
        final String NEWLINE = "\r\n";
        out.print(breedCode);
        out.print(" ");
        out.print(breedName);
        out.print(NEWLINE);
    }
}
