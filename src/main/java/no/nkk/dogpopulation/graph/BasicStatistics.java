package no.nkk.dogpopulation.graph;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * To be used for JSON serialization of a DescriptiveStatistics instance.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class BasicStatistics {
    private final DescriptiveStatistics descriptiveStatistics;

    public BasicStatistics(DescriptiveStatistics descriptiveStatistics) {
        this.descriptiveStatistics = descriptiveStatistics;
    }

    public long getN() {
        return descriptiveStatistics.getN();
    }

    public double getSum() {
        return descriptiveStatistics.getSum();
    }

    public double getMin() {
        return descriptiveStatistics.getMin();
    }

    public double getMax() {
        return descriptiveStatistics.getMax();
    }

    public double getMean() {
        return descriptiveStatistics.getMean();
    }

    public double getStandardDeviation() {
        return descriptiveStatistics.getStandardDeviation();
    }

    public double getPercentile50() {
        return descriptiveStatistics.getPercentile(50);
    }

    public double getPercentile90() {
        return descriptiveStatistics.getPercentile(90);
    }
}
