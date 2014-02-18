package no.nkk.dogpopulation.graph.hdindex;

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
    private final int hdXrayYear;

    // RÅK = Race*Reg_år*Køn vekselvirkning ( Race*100000+Reg_år*10+Køn)
    private final int breedHdXrayYearGender;

    // Kuld = Unik identifikation af det enkelte kuld
    private final int litterId;

    // Mor = Unik identifikation af moderen
    private final int motherId;

    // HD = HD-score
    private final double hdScore;

    public DmuDataRecord(int id, int breedCode, int hdXrayYear, int breedHdXrayYearGender, int litterId, int motherId, double hdScore) {
        this.id = id;
        this.breedCode = breedCode;
        this.hdXrayYear = hdXrayYear;
        this.breedHdXrayYearGender = breedHdXrayYearGender;
        this.litterId = litterId;
        this.motherId = motherId;
        this.hdScore = hdScore;
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

    public int getBreedHdXrayYearGender() {
        return breedHdXrayYearGender;
    }

    public int getLitterId() {
        return litterId;
    }

    public int getMotherId() {
        return motherId;
    }

    public double getHdScore() {
        return hdScore;
    }
}
