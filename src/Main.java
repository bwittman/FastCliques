import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner in = new Scanner(System.in);
        String name = "data/brock200_1.clq";
        Graph graph;
        try (Scanner file = new Scanner(new File(name))) {
            graph = new Graph(file);
        }

        System.out.println("Upper bound: " + graph.cliqueUpperBound());
        // The .clq files use 1-based numbering for nodes and edges but report cliques with 0-based numbering
        int[] reportedClique = {133, 17, 92, 177, 38, 19, 84, 134, 107, 89, 185, 91, 67, 141, 149, 72, 101, 135, 86, 93, 80};
        System.out.println("Has clique: " + graph.isClique(reportedClique));


        // Testing of random clique generation
        // Issue: There are large numbers of triangular cliques,
        // so randomly merging them generates a small number of size 6 cliques.
        // Since the number of size 6 cliques is small relative to the size 3,
        // even a large number of iterations fails to merge a size 6 with anything else.
        Set<BitSet> cliques = randomlyFindCliques(graph, 10000000);
    }

    /**
     * Builds cliques by merging existing cliques. Initially, it creates all cliques with three elements.
     * @param graph input graph
     * @param iterations number of iterations to run to find cliques
     */
    public static Set<BitSet> randomlyFindCliques(Graph graph, int iterations) {
        Set<BitSet> cliques = new HashSet<>(); // Set for seeing if a clique already exists
        List<BitSet> listOfCliques = new ArrayList<>(); // List for randomly selecting cliques

        int nodes = graph.nodes();
        for (int i = 0; i < nodes - 1; ++i)
            for (int j = i + 1; j < nodes; ++j) {
                if (graph.hasEdge(i,j)) {
                    for (int k = 0; k < nodes; ++k) {
                        if (k != i && k != j && graph.hasEdge(i,k) && graph.hasEdge(j,k)) {
                            BitSet clique = new BitSet(nodes);
                            clique.set(i);
                            clique.set(j);
                            clique.set(k);
                            cliques.add(clique);
                            listOfCliques.add(clique);
                        }
                    }
                }
            }

        if (cliques.size() < 2)
            return cliques;

        int largest = 3;

        Random random = new Random();
        for (int iteration = 1; iteration <= iterations; ++iteration) {
            int first = random.nextInt(cliques.size());
            int second;
            do {
                second = random.nextInt(cliques.size());
            } while (second == first);

            BitSet candidate = graph.mergeCliques(listOfCliques.get(first), listOfCliques.get(second));
            if (candidate != null && !cliques.contains(candidate)) {
                // System.out.println("Found new clique!");
                cliques.add(candidate);
                listOfCliques.add(candidate);
                if (candidate.cardinality() > largest)
                    largest = candidate.cardinality();
            }

            if (iteration % 1000 == 0) {
                System.out.format("%10d: %d cliques, largest: %d%n", iteration, cliques.size(), largest);
            }
        }

        System.out.format("%10s: %d cliques, largest: %d%n", "Final", cliques.size(), largest);
        return cliques;
    }
}
