package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

public class DmuUuidRecord implements DmuWritableRecord {

    private final int id;
    private final String uuid;

    public DmuUuidRecord(int id, String uuid) {
        this.id = id;
        this.uuid = uuid;
    }

    @Override
    public void writeTo(PrintWriter out) {
        final String NEWLINE = "\r\n";
        out.print(id);
        out.print(" ");
        out.print(uuid);
        out.print(NEWLINE);
    }

    public String getUuid() {
        return uuid;
    }
}
