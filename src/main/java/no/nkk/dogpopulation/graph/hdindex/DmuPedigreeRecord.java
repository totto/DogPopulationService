package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuPedigreeRecord implements DmuWritableRecord {

    public static int UNKNOWN = -9999999;

    // ID = Unik identifikation af individet (samme som i datafilen)
    private final long id;

    // Far_id = Unik identifikation af faderen
    // Ukendte forældre kodes med negativ racekode (Racekode*(-1))
    private final long fatherId;

    // Mor_id = Unik identifikation af moderen (samme som i datafilen)
    // Ukendte forældre kodes med negativ racekode (Racekode*(-1))
    private final long motherId;

    // F_dato = Fødselsdato (YYYYMMDD)
    // Ukendte fødselsdatoer kodes med -9999999. --> UNKNOWN
    private final int birthDate;

    // Race = Race kode (samme som i datafilen)
    private final int breedCode;

    public DmuPedigreeRecord(long id, long fatherId, long motherId, int birthDate, int breedCode) {
        this.id = id;
        this.fatherId = fatherId;
        this.motherId = motherId;
        this.birthDate = birthDate;
        this.breedCode = breedCode;
    }


    public void writeTo(PrintWriter out) {
        final String NEWLINE = "\r\n";
        out.print(getId());
        out.print(" ");
        out.print(getFatherId());
        out.print(" ");
        out.print(getMotherId());
        out.print(" ");
        out.print(getBirthDate());
        out.print(" ");
        out.print(getBreedCode());
        out.print(NEWLINE);
    }


    public long getId() {
        return id;
    }

    public long getFatherId() {
        return fatherId;
    }

    public long getMotherId() {
        return motherId;
    }

    public int getBirthDate() {
        return birthDate;
    }

    public int getBreedCode() {
        return breedCode;
    }
}
