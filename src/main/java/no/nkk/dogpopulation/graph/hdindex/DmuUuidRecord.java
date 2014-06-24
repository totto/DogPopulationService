package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

public class DmuUuidRecord implements DmuWritableRecord {

    private final long id;
    private final String uuid;
    private final String regno;
    private final String chip;

    public DmuUuidRecord(long id, String uuid, String regno, String chip) {
        this.id = id;
        this.uuid = uuid;
        this.regno = regno;
        this.chip = chip;
    }

    @Override
    public void writeTo(PrintWriter out) {
        final String NEWLINE = "\r\n";
        out.print(id);
        out.print(" ");
        out.print(uuid);
        out.print(" ");
        out.print(getRegno());
        out.print(" ");
        out.print(getChip());
        out.print(NEWLINE);
    }

    public long getId() {
        return id;
    }

    public String getRegno() {
        if (regno == null || regno.isEmpty()) {
            return "-";
        }
        return regno;
    }

    public String getChip() {
        if (chip == null || chip.isEmpty()) {
            return "-";
        }
        return chip;
    }
}
