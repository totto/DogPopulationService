package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

public class DmuDataErrorRecord implements DmuWritableRecord {

    private final long id;
    private final String message;

    public DmuDataErrorRecord(long id, String message) {
        this.id = id;
        this.message = message;
    }

    @Override
    public void writeTo(PrintWriter out) {
        out.println(message);
    }

    public long getId() {
        return id;
    }
}
