package no.nkk.dogpopulation.graph.inbreeding;

import java.util.Map;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class InbreedingResult {
    private final double coi;
    private final Map<String, Double> coiByContributingAncestor;

    public InbreedingResult(double coi, Map<String, Double> coiByContributingAncestor) {
        this.coi = coi;
        this.coiByContributingAncestor = coiByContributingAncestor;
    }

    public double getCoi() {
        return coi;
    }

    public Map<String, Double> getCoiByContributingAncestor() {
        return coiByContributingAncestor;
    }
}
