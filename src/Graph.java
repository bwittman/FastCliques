import java.util.Arrays;
import java.util.Scanner;

public class Graph {
    private int nodes = 0;
    private boolean[][] edges;
    private int[] degrees;

    public Graph(Scanner in) {
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.startsWith("p ")) {
                if (nodes != 0)
                    throw new IllegalArgumentException("Number of nodes defined multiple times in graph file input!");
                String[] parts = line.split("\\s+");
                nodes = Integer.parseInt(parts[2]);
                edges = new boolean[nodes][nodes];
                degrees = new int[nodes];
            }
            else if (line.startsWith("e ")) {
                if (nodes == 0)
                    throw new IllegalArgumentException("Trying to add an edge before number of nodes is known!");

                String[] parts = line.split("\\s+");
                int from = Integer.parseInt(parts[1]) - 1;
                int to = Integer.parseInt(parts[2]) - 1;
                // In case edges are listed from both directions
                if (!edges[from][to]) {
                    edges[from][to] = true;
                    edges[to][from] = true;
                    ++degrees[to];
                    ++degrees[from];
                }
            }
        }
    }

    public int cliqueUpperBound() {
        int[] degreesCopy = degrees.clone();
        Arrays.sort(degreesCopy);
        int upperBound = 1;

        // 1 2 3 4 4 4 5 5 total: 8
        // 0 1 2 3 4 5 6 7
        for (int i = 0; i < degreesCopy.length; ++i) {
            if ((degreesCopy.length - 1 - i) > upperBound && degreesCopy[i] > upperBound)
                upperBound = degreesCopy[i];
        }

        return upperBound;
    }

    public boolean isClique(int[] nodes) {
        return isClique(nodes, false);
    }

    public boolean isClique(int[] nodes, boolean oneBasedIndexing) {
        int offset = oneBasedIndexing ? 1 : 0;
        for (int i = 0; i < nodes.length - 1; ++i)
            for (int j = i + 1; j < nodes.length; ++j)
                if (!edges[nodes[i]-offset][nodes[j]-offset])
                    return false;

        return true;
    }
}
