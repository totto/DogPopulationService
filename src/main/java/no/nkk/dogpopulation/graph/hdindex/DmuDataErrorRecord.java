package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

public class DmuDataErrorRecord implements DmuWritableRecord {

    private final String uuid;
    private final String message;

    public DmuDataErrorRecord(String uuid, String message) {
        this.uuid = uuid;
        this.message = message;
    }

    @Override
    public void writeTo(PrintWriter out) {
        out.println(message);
    }

    public String getUuid() {
        return uuid;
    }
}
