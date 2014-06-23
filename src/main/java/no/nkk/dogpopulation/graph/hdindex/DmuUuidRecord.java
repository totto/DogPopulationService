package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

public class DmuUuidRecord implements DmuWritableRecord {

    private final long id;
    private final String uuid;

    public DmuUuidRecord(long id, String uuid) {
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

    public long getId() {
        return id;
    }
}
