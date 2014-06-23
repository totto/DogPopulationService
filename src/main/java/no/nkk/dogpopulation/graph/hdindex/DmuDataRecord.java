package no.nkk.dogpopulation.graph.hdindex;

import java.io.PrintWriter;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DmuDataRecord implements DmuWritableRecord {

    // Manglende værdier kodes -9999999
    public static int UNKNOWN = -9999999;

    private final String uuid;

    // ID = Unik identifikation af individet
    private final int id;

    // Race = Indvidets race
    private final int breedCode;

    // Reg_år = Registreringsår
    private final int hdXrayYear;

    // Køn = 1 for female og 2 for male
    private final int gender;

    // RÅK = Race*Reg_år*Køn vekselvirkning ( Race*100000+Reg_år*10+Køn)
    private final int breedHdXrayYearGender;

    // Kuld = Unik identifikation af det enkelte kuld
    private final int litterId;

    // Mor = Unik identifikation af moderen
    private final int motherId;

    // HD = HD-score
    private final int hdScore;

    public DmuDataRecord(String uuid, int id, int breedCode, int hdXrayYear, int gender, int breedHdXrayYearGender, int litterId, int motherId, int hdScore) {
        this.uuid = uuid;
        this.id = id;
        this.breedCode = breedCode;
        this.hdXrayYear = hdXrayYear;
        this.gender = gender;
        this.breedHdXrayYearGender = breedHdXrayYearGender;
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
        out.print(getHdXrayYear());
        out.print(" ");
        out.print(getGender());
        out.print(" ");
        out.print(getBreedHdXrayYearGender());
        out.print(" ");
        out.print(getLitterId());
        out.print(" ");
        out.print(getMotherId());
        out.print(" ");
        out.print(getHdScore());
        out.print(NEWLINE);
    }


    public String getUuid() {
        return uuid;
    }

    public int getId() {
        return id;
    }

    public int getBreedCode() {
        return breedCode;
    }

    public int getHdXrayYear() {
        return hdXrayYear;
    }

    public int getGender() {
        return gender;
    }

    public int getBreedHdXrayYearGender() {
        return breedHdXrayYearGender;
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
