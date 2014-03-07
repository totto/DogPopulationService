package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuDataRecord {

    // Manglende værdier kodes -9999999
    public static int UNKNOWN = -9999999;

    // ID = Unik identifikation af individet
    private final int id;

    // Race = Indvidets race
    private final int breedCode;

    // Reg_år = Registreringsår
    private final int bornYear;

    // Køn = 1 for female og 2 for male
    private final int gender;

    // RÅK = Race*Reg_år*Køn vekselvirkning ( Race*100000+Reg_år*10+Køn)
    private final int breedBornYearGender;

    // Kuld = Unik identifikation af det enkelte kuld
    private final int litterId;

    // Mor = Unik identifikation af moderen
    private final int motherId;

    // HD = HD-score
    private final int hdScore;

    public DmuDataRecord(int id, int breedCode, int bornYear, int gender, int breedBornYearGender, int litterId, int motherId, int hdScore) {
        this.id = id;
        this.breedCode = breedCode;
        this.bornYear = bornYear;
        this.gender = gender;
        this.breedBornYearGender = breedBornYearGender;
        this.litterId = litterId;
        this.motherId = motherId;
        this.hdScore = hdScore;
    }


    public void writeTo(PrintWriter out) {
        final String NEWLINE = "\r\n";
        out.print(getId());
        out.print(" ");
        out.print(getBreedCode());
        out.print(" ");
        out.print(getBornYear());
        out.print(" ");
        out.print(getGender());
        out.print(" ");
        out.print(getBreedBornYearGender());
        out.print(" ");
        out.print(getLitterId());
        out.print(" ");
        out.print(getMotherId());
        out.print(" ");
        out.print(getHdScore());
        out.print(NEWLINE);
    }


    public int getId() {
        return id;
    }

    public int getBreedCode() {
        return breedCode;
    }

    public int getBornYear() {
        return bornYear;
    }

    public int getGender() {
        return gender;
    }

    public int getBreedBornYearGender() {
        return breedBornYearGender;
    }

    public int getLitterId() {
        return litterId;
    }

    public int getMotherId() {
        return motherId;
    }

    public int getHdScore() {
        return hdScore;
    }
}
