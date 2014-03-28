package no.nkk.dogpopulation.graph;

import org.joda.time.LocalDateTime;

/**
 * Constants match either: LABEL_PROPERTY or LABEL_PROPERTY_VALUE
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class DogGraphConstants {

    // A timestamp before the very first timestamp of an entry on dogsearch
    public static final LocalDateTime BEGINNING_OF_TIME = new LocalDateTime(2013, 12, 19, 0, 0, 0);

    public static final String BREEDGROUP_FCIBREEDGROUP = "FCIBreedGroup";

    public static final String BREED_BREED_NAME = "BreedName";
    public static final String BREED_FCI_BREED_ID = "FCIBreedId";
    public static final String BREED_NKK_BREED_ID = "NKKBreedId";
    public static final String BREED_CLUB_ID = "ClubId";

    public static final String BREEDSYNONYM_SYNONYM = "synonym";
    public static final String BREEDSYNONYM_UPDATEDTO = "updatedTo";

    public static final String CATEGORY_CATEGORY = "category";

    public static final String CATEGORY_CATEGORY_ROOT = "Root";
    public static final String CATEGORY_CATEGORY_BREEDGROUPS = "BreedGroups";
    // public static final String CATEGORY_CATEGORY_LITTER = "Litter";

    // public static final String CATEGORY_CATEGORY_COUNTRY = "Country";

    // public static final String COUNTRY_COUNTRY = "country";

    public static final String DOG_JSON = "json";
    public static final String DOG_UUID = "uuid";
    public static final String DOG_NAME = "name";
    public static final String DOG_GENDER = "gender";
    public static final String DOG_REGNO = "RegNo";
    public static final String DOG_CHIPNO = "Chip";
    public static final String DOG_BORN_YEAR = "b_year";
    public static final String DOG_BORN_MONTH = "b_month";
    public static final String DOG_BORN_DAY = "b_day";
    public static final String DOG_HDDIAG = "hd_diag";
    public static final String DOG_HDYEAR = "hd_year";

    public static final String HASPARENT_ROLE = "role";

    public static final String HASLITTER_ROLE = "role";

    public static final String LITTER_ID = "id";
    public static final String LITTER_COUNT = "count";
    public static final String LITTER_YEAR = "year";
    public static final String LITTER_MONTH = "month";
    public static final String LITTER_DAY = "day";
}
