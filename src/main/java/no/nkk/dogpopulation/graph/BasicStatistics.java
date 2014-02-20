package no.nkk.dogpopulation.graph;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * To be used for JSON serialization of a DescriptiveStatistics instance.
 *
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
@JsonPropertyOrder({"n", "mean", "min", "max", "standardDeviation", "sum", "percentile1",
        "percentile5", "percentile10", "percentile15", "percentile20", "percentile25",
        "percentile30", "percentile35", "percentile40", "percentile45","percentile50",
        "percentile55", "percentile60", "percentile65", "percentile70", "percentile75",
        "percentile80", "percentile85", "percentile90", "percentile95", "percentile99"})
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

    public double getPercentile1() {
        return descriptiveStatistics.getPercentile(1);
    }

    public double getPercentile5() {
        return descriptiveStatistics.getPercentile(5);
    }

    public double getPercentile10() {
        return descriptiveStatistics.getPercentile(10);
    }

    public double getPercentile15() {
        return descriptiveStatistics.getPercentile(15);
    }

    public double getPercentile20() {
        return descriptiveStatistics.getPercentile(20);
    }

    public double getPercentile25() {
        return descriptiveStatistics.getPercentile(25);
    }

    public double getPercentile30() {
        return descriptiveStatistics.getPercentile(30);
    }

    public double getPercentile35() {
        return descriptiveStatistics.getPercentile(35);
    }

    public double getPercentile40() {
        return descriptiveStatistics.getPercentile(40);
    }

    public double getPercentile45() {
        return descriptiveStatistics.getPercentile(45);
    }

    public double getPercentile50() {
        return descriptiveStatistics.getPercentile(50);
    }

    public double getPercentile55() {
        return descriptiveStatistics.getPercentile(55);
    }

    public double getPercentile60() {
        return descriptiveStatistics.getPercentile(60);
    }

    public double getPercentile65() {
        return descriptiveStatistics.getPercentile(65);
    }

    public double getPercentile70() {
        return descriptiveStatistics.getPercentile(70);
    }

    public double getPercentile75() {
        return descriptiveStatistics.getPercentile(75);
    }

    public double getPercentile80() {
        return descriptiveStatistics.getPercentile(80);
    }

    public double getPercentile85() {
        return descriptiveStatistics.getPercentile(85);
    }

    public double getPercentile90() {
        return descriptiveStatistics.getPercentile(90);
    }

    public double getPercentile95() {
        return descriptiveStatistics.getPercentile(95);
    }

    public double getPercentile99() {
        return descriptiveStatistics.getPercentile(99);
    }
}
