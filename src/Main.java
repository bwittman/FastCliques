import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Scanner in = new Scanner(System.in);
        String name = "data/brock.dat";
        Map<String, Integer> graphs = new TreeMap<>();
        try (Scanner file = new Scanner(new File(name))) {
            while (file.hasNext()) {
                String graphName = file.next();
                int bestKnownClique = file.nextInt();
                graphs.put(graphName, bestKnownClique);
            }
        }

        System.out.format("%-20s %5s %5s %s%n", "Graph File", "Known", "Found", "Time");

        for (Map.Entry<String, Integer> graphEntry : graphs.entrySet()) {
            Graph graph;
            String graphName = graphEntry.getKey();
            try (Scanner file = new Scanner(new File("data/" + graphName))) {
                graph = new Graph(file);
            }

            // System.out.println("Upper bound: " + graph.cliqueUpperBound());
            // The .clq files use 1-based numbering for nodes and edges but report cliques with 0-based numbering
            //int[] reportedClique = {133, 17, 92, 177, 38, 19, 84, 134, 107, 89, 185, 91, 67, 141, 149, 72, 101, 135, 86, 93, 80};
            //System.out.println("Has clique: " + graph.isClique(reportedClique));


            // Testing of random clique generation
            // Issue: There are large numbers of triangular cliques,
            // so randomly merging them generates a small number of size 6 cliques.
            // Since the number of size 6 cliques is small relative to the size 3,
            // even a large number of iterations fails to merge a size 6 with anything else.
            // Set<BitSet> cliques = randomlyFindCliques(graph, 10000000);

            // Try by sorting by degree
            long start = System.nanoTime();
            int largest = buildCliquesByDescendingDegree(graph);
            long end = System.nanoTime();
            System.out.format("%-20s %5d %5d %.3f seconds%n", graphName, graphEntry.getValue(), largest, (end - start) / 1000000000.0);
        }
    }


    /**
     * Builds cliques by sorting graph by degree.
     * Hopefully, large-degree nodes will be in the same clique as their neighbors.
     *
     * @param graph input graph
     */
    public static int buildCliquesByDescendingDegree(Graph graph) {
        Set<BitSet> cliques = new HashSet<>();
        List<BitSet> listOfCliques = new ArrayList<>(); // List for randomly selecting cliques
        int[][] result = graph.getNodesSortedByDegree();
        int[] sortedNodes = result[0];
        int[] lastIndexWithDegree = result[1];
        int largest = 1;
        for (int i = 0; i < sortedNodes.length; ++i) {
            boolean connected = true;
            BitSet clique = new BitSet(sortedNodes.length);
            clique.set(sortedNodes[i]);
            for (int j = i + 1; connected && j < sortedNodes.length; ++j) {
                for (int k = i; connected && k < j; ++k) {
                    if (!graph.hasEdge(sortedNodes[k], sortedNodes[j]))
                        connected = false;
                }
                if (connected)
                    clique.set(sortedNodes[j]);
            }
            cliques.add(clique);
            listOfCliques.add(clique);
            /*
            if (clique.cardinality() > 1) { // Put the singleton clique in as well
                BitSet singleton = new BitSet(sortedNodes.length);
                singleton.set(sortedNodes[i]);
                cliques.add(clique);
                listOfCliques.add(clique);
            }
            */
            if (clique.cardinality() > largest)
                largest = clique.cardinality();
            //System.out.println("Added clique with size: " + clique.cardinality());
        }

        if (cliques.size() < 2)
            return largest;

        // All pairs
        int initialSize = listOfCliques.size();
        for (int i = 0; i < initialSize - 1; ++i)
            for (int j = i + 1; j < initialSize; ++j) {
                BitSet candidate = graph.mergeCliques(listOfCliques.get(i), listOfCliques.get(j));
                if (candidate != null && !cliques.contains(candidate)) {
                    // System.out.println("Found new clique!");
                    cliques.add(candidate);
                    listOfCliques.add(candidate);
                    if (candidate.cardinality() > largest)
                        largest = candidate.cardinality();
                }
            }

        listOfCliques.sort(Comparator.comparingInt(BitSet::cardinality));

        Random random = new Random();
        int power = 2;
        final int EDGES = graph.edges();
        final int ITERATIONS = EDGES;
        final int NODES = graph.nodes();
        int window = Math.min(10, cliques.size());
        for (int iteration = 1; iteration <= ITERATIONS; ++iteration) {

            int first = (int) Math.pow((Math.pow(cliques.size(), power + 1)) * random.nextDouble(), 1.0 / (power + 1));
            int second;
            do {
                second = (int) Math.pow((Math.pow(cliques.size(), power + 1)) * random.nextDouble(), 1.0 / (power + 1));
            } while (second == first);
/*

            int first = random.nextInt(cliques.size());
            int second;
            do {
                second = random.nextInt(cliques.size());
            } while (second == first);

            */
            BitSet candidate = graph.mergeCliques(listOfCliques.get(first), listOfCliques.get(second));
            if (candidate != null && !cliques.contains(candidate)) {
                // System.out.println("Found new clique!");
                cliques.add(candidate);
                listOfCliques.add(candidate);
                if (candidate.cardinality() > largest)
                    largest = candidate.cardinality();
            }

            if (iteration % NODES == 0) {
                listOfCliques.sort(Comparator.comparingInt(BitSet::cardinality));
                //System.out.format("%10d: %d cliques, largest: %d%n", iteration, cliques.size(), largest);

                int oldLargest = largest;
                int oldSize = listOfCliques.size();
                BitSet singleton = new BitSet();

                for (int i = 0; i < window; ++i) {
                    boolean found = false;
                    int lastNode = Math.max(0, lastIndexWithDegree[largest - 1]);
                    for (int node = NODES - 1; node >= lastNode && !found; --node) {
                        singleton.clear();
                        singleton.set(sortedNodes[node]);
                        candidate = graph.mergeCliques(listOfCliques.get(oldSize - 1 - i), singleton);
                        if (candidate != null && !cliques.contains(candidate)) {
                            // System.out.println("Found new clique!");
                            cliques.add(candidate);
                            listOfCliques.add(candidate);
                            if (candidate.cardinality() > largest)
                                largest = candidate.cardinality();
                            found = true;
                        }
                    }
                }

                // No larger clique found, grow window
                if (largest == oldLargest)
                    window = Math.min(window * 2, Math.min(cliques.size(), NODES));
                else
                    window = Math.max(window / 2, Math.min(10, cliques.size()));
                //System.out.println("Window size: " + window);
            }
        }

        //System.out.format("%10s: %d cliques, largest: %d%n", "Final", cliques.size(), largest);

        return largest;
    }


    /**
     * Builds cliques by merging existing cliques. Initially, it creates all cliques with three elements.
     *
     * @param graph      input graph
     * @param iterations number of iterations to run to find cliques
     */
    public static Set<BitSet> randomlyFindCliques(Graph graph, int iterations) {
        Set<BitSet> cliques = new HashSet<>(); // Set for seeing if a clique already exists
        List<BitSet> listOfCliques = new ArrayList<>(); // List for randomly selecting cliques

        int nodes = graph.nodes();
        for (int i = 0; i < nodes - 1; ++i)
            for (int j = i + 1; j < nodes; ++j) {
                if (graph.hasEdge(i, j)) {
                    for (int k = 0; k < nodes; ++k) {
                        if (k != i && k != j && graph.hasEdge(i, k) && graph.hasEdge(j, k)) {
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
        int power = 5;
        for (int iteration = 1; iteration <= iterations; ++iteration) {
            int first = (int) Math.pow((Math.pow(cliques.size(), power + 1)) * random.nextDouble(), 1.0 / (power + 1));
            int second;
            do {
                second = (int) Math.pow((Math.pow(cliques.size(), power + 1)) * random.nextDouble(), 1.0 / (power + 1));
            } while (second == first);

            /*
            int first = random.nextInt(cliques.size());
            int second;
            do {
                second = random.nextInt(cliques.size());
            } while (second == first);
             */
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
