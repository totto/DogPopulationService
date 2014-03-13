package no.nkk.dogpopulation.graph.inbreeding;

import org.neo4j.graphdb.Path;

/**
 * @author <a href="mailto:kim.christian.swenson@gmail.com">Kim Christian Swenson</a>
 */
public class CommonAncestorPath {
    private final Path path1;
    private final Path path2;

    public CommonAncestorPath(Path path1, Path path2) {
        this.path1 = path1;
        this.path2 = path2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommonAncestorPath that = (CommonAncestorPath) o;

        if (!path1.equals(that.path1)) return false;
        if (!path2.equals(that.path2)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = path1.hashCode();
        result = 31 * result + path2.hashCode();
        return result;
    }
}
