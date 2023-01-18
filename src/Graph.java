import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

public class Graph {
    private BitSet[] edges;
    private int totalEdges;
    private int[] degrees;

    public Graph(Scanner in) {
        int nodes = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.startsWith("p ")) {
                if (nodes != 0)
                    throw new IllegalArgumentException("Number of nodes defined multiple times in graph file input!");
                String[] parts = line.split("\\s+");
                nodes = Integer.parseInt(parts[2]);
                edges = new BitSet[nodes];
                for (int i = 0; i < nodes; ++i)
                    edges[i] = new BitSet(nodes);
                degrees = new int[nodes];
            }
            else if (line.startsWith("e ")) {
                if (nodes == 0)
                    throw new IllegalArgumentException("Trying to add an edge before number of nodes is known!");

                String[] parts = line.split("\\s+");
                int from = Integer.parseInt(parts[1]) - 1;
                int to = Integer.parseInt(parts[2]) - 1;
                // In case edges are listed from both directions
                if (!edges[from].get(to)) {
                    edges[from].set(to);
                    edges[to].set(from);
                    ++degrees[to];
                    ++degrees[from];
                    ++totalEdges;
                }
            }
        }
    }

    public int nodes() {
        return edges.length;
    }

    public int edges() { return totalEdges; }

    public boolean hasEdge(int from, int to) {
        return edges[from].get(to);
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

    public static int[] bitSetToArray(BitSet set) {
        int[] nodes = new int[set.cardinality()];
        int index = 0;

        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i+1)) {
            nodes[index] = i;
            ++index;

            /* // Unnecessary safety check
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
            */
        }
        return nodes;
    }

    public boolean isClique(BitSet nodes) {
        return isClique(bitSetToArray(nodes));
    }

    public boolean isClique(int[] nodes) {
        return isClique(nodes, false);
    }

    public boolean isClique(int[] nodes, boolean oneBasedIndexing) {
        int offset = oneBasedIndexing ? 1 : 0;
        for (int i = 0; i < nodes.length - 1; ++i)
            for (int j = i + 1; j < nodes.length; ++j)
                if (!edges[nodes[i]-offset].get(nodes[j]-offset))
                    return false;

        return true;
    }

    public int[] getNodesSortedByDensity() {
        double[] degrees = new double[this.degrees.length];
        for (int i = 0; i < degrees.length; ++i)
            degrees[i] = this.degrees[i];
        double[] scratch = new double[degrees.length];

        for (int iteration = 0; iteration < Math.log(edges.length); ++iteration) {
            for (int i = 0; i < edges.length; ++i) {
                scratch[i] = 0;
                BitSet currentEdges = edges[i];
                for (int j = 0; j < edges[i].length(); ++j) {
                    if (currentEdges.get(j)) {
                        scratch[i] += degrees[j]/2.0;
                    }
                }
                scratch[i] /= this.degrees[i];
                scratch[i] += degrees[i];
            }
            double[] temp = degrees;
            degrees = scratch;
            scratch = temp;
        }

        Integer[] nodes = new Integer[edges.length];
        for (int i = 0; i < nodes.length; ++i)
            nodes[i] = i;

        double[] finalDegrees = degrees;
        Arrays.sort(nodes, Comparator.comparingDouble(integer -> finalDegrees[integer]));

        int[] sorted = new int[nodes.length];
        for (int i = 0; i < sorted.length; ++i)
            sorted[i] = nodes[i];

        return sorted;
    }

    public int[] getNodesSortedByDegree() {
        Integer[] nodes = new Integer[edges.length];
        for (int i = 0; i < nodes.length; ++i)
            nodes[i] = i;

        Arrays.sort(nodes, Comparator.comparingInt(integer -> degrees[integer]));

        int[] sorted = new int[nodes.length];
        for (int i = 0; i < sorted.length; ++i)
            sorted[i] = nodes[i];

        return sorted;
        /*
        // Hoo-ha that returns the last index in the sorted list with a given degree
        // It's probably not very useful since, in the hard clique problems, most nodes
        // have a high degree.
        int[][] result = new int[2][];
        result[0] = sorted;

        int[] lastIndexWithDegree = new int[nodes.length];
        int index = 0;
        while (index < degrees[sorted[0]]) {
            lastIndexWithDegree[index] = -1;
            ++index;
        }
        for (int i = 0; i < nodes.length; ++i) {
            while (degrees[sorted[i]] > index) {
                lastIndexWithDegree[index] = i - 1;
                ++index;
            }
        }
        while (index < nodes.length) {
            lastIndexWithDegree[index] = -1;
            ++index;
        }

        result[1] = lastIndexWithDegree;

        return result;
        */
    }

    public BitSet addNode(BitSet clique, int node) {
        //if (clique.get(node))
            //return null;

        BitSet nodeEdges = edges[node];

        for (int i = clique.nextSetBit(0); i >= 0; i = clique.nextSetBit(i+1)) {
            if (!nodeEdges.get(i))
                return null;
        }

        BitSet newClique = (BitSet) clique.clone();
        newClique.set(node);
        return newClique;
    }

    /**
     * Unions two cliques together to make a new clique. This method will return null if the resulting union
     * is not a clique. Output is undefined if input cliques are not, in fact, cliques.
     * @param first First pre-existing clique
     * @param second Second pre-existing clique
     * @return New clique made by unioning together nodes in existing cliques or null if the result is not a clique
     */
    public BitSet mergeCliques(BitSet first, BitSet second) {
        BitSet firstMinusSecond = (BitSet) first.clone();
        firstMinusSecond.andNot(second);
        BitSet secondMinusFirst = (BitSet) second.clone();
        secondMinusFirst.andNot(first);

        if (firstMinusSecond.isEmpty() || secondMinusFirst.isEmpty())
            return null;

        int[] remainingFirstNodes = bitSetToArray(firstMinusSecond);
        int[] remainingSecondNodes = bitSetToArray(secondMinusFirst);
        for (int firstNode : remainingFirstNodes)
            for (int secondNode : remainingSecondNodes)
                if (!edges[firstNode].get(secondNode))
                    return null;

        BitSet union = (BitSet)first.clone();
        union.or(second);
        return union;
    }

    /**
     * Combines two cliques together, keeping those elements of the first that are completely connected to the second.
     * This method will return null unless the resulting clique is larger than both of the input cliques.
     * Output is undefined if input cliques are not, in fact, cliques.
     * @param first First pre-existing clique
     * @param second Second pre-existing clique
     * @return New clique made by combining together nodes in existing cliques or null if the result is not a larger clique
     */
    public BitSet combineCliques(BitSet first, BitSet second) {
        BitSet firstMinusSecond = (BitSet) first.clone();
        firstMinusSecond.andNot(second);
        BitSet secondMinusFirst = (BitSet) second.clone();
        secondMinusFirst.andNot(first);

        if (firstMinusSecond.isEmpty() || secondMinusFirst.isEmpty())
            return null;

        int[] remainingFirstNodes = bitSetToArray(firstMinusSecond);
        int[] remainingSecondNodes = bitSetToArray(secondMinusFirst);

        BitSet intersection = (BitSet) first.clone();
        intersection.and(second);
        for (int firstNode : remainingFirstNodes) {
            boolean keep = true;
            for (int secondNode : remainingSecondNodes)
                if (!edges[firstNode].get(secondNode)) {
                    keep = false;
                    break;
                }

            if (keep)
                intersection.set(firstNode);
        }

        for (int secondNode : remainingSecondNodes) {
            boolean keep = true;
            for (int firstNode : remainingFirstNodes)
                if (!edges[secondNode].get(firstNode)) {
                    keep = false;
                    break;
                }

            if (keep)
                intersection.set(secondNode);
        }

        int newSize = intersection.cardinality();
        if (newSize > first.cardinality() && newSize > second.cardinality())
            return intersection;
        else
            return null;
    }

    public static double[][] power(double[][] matrix, int power) {
        if (power < 1)
            throw new IllegalArgumentException("Invalid power: " + power);

        if (power == 1)
            return matrix;
        else if (power % 2 == 0) {
            double[][] result = power(matrix, power/2);
            return multiply(result, result);
        }
        else {
            double[][] result = power(matrix, (power - 1)/2);
            return multiply(matrix,multiply(result, result));
        }
    }

    public static double[][] multiply(double[][] a, double[][] b) {
        double[][] c = new double[a.length][b[0].length];
        for (int i = 0; i < c.length; ++i)
            for (int j = 0; j < c[0].length; ++j)
                for (int k = 0; k < a[0].length; ++k)
                    c[i][j] += a[i][k] * b[k][j];
        return c;
    }

    public int[] getNodesSortedByWalkSize() {
        final int NODES = edges.length;

        double[][] matrix = new double[NODES][NODES];
        for (int i = 0; i < NODES - 1; ++i)
            for (int j = i + 1; j < NODES; ++j)
                if (edges[i].get(j)) {
                    matrix[i][j] = 1;
                    matrix[j][i] = 1;
                }

        // How big of a walk? Square root of nodes to start with
         int walkLength = (int)Math.round(Math.log(NODES));
        //int walkLength = NODES;
        double[][] walkMatrix = power(matrix, walkLength);

        double[] totalWalks = new double[NODES];
        for (int i = 0; i < NODES; ++i)
            for (int j = 0; j < NODES; ++j)
                totalWalks[i] += walkMatrix[i][j];

        Integer[] nodes = new Integer[NODES];
        for (int i = 0; i < NODES; ++i)
            nodes[i] = i;

        Arrays.sort(nodes, Comparator.comparingDouble(integer -> totalWalks[integer]));

        int[] sorted = new int[NODES];
        for (int i = 0; i < NODES; ++i)
            sorted[i] = nodes[i];

        return sorted;
    }

    public int[] findLargestClique() {
        final int NODES = edges.length;
        BitSet currentClique = new BitSet(NODES);
        BitSet largestClique = new BitSet(NODES);
        for (int i = 0; i < NODES; ++i) {
            System.out.println("Starting at: " + i);
            currentClique.clear();
            currentClique.set(i);
            findLargestClique(i, currentClique, largestClique);
        }
        return bitSetToArray(largestClique);
    }


    /*
    public int[] findLargestCliqueStack() {
        final int NODES = edges.length;
        BitSet largestClique = new BitSet(NODES);
        for (int i = 0; i < NODES; ++i) {
            //System.out.println("Starting at: " + i);
            BitSet clique = findLargestCliqueStack(i);
            if (clique.cardinality() > largestClique.cardinality())
                largestClique = clique;
        }
        return bitSetToArray(largestClique);
    }
    */


    public int[] findLargestCliqueStackThreaded() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(16);

        LongAccumulator largest = new LongAccumulator(Long::max, 0);
        final int NODES = edges.length;
        List<Callable<BitSet>> callableTasks = new ArrayList<>();
        // Go backwards since high-numbered nodes finish faster
        for (int i = NODES - 1; i >= 0; --i) {
            final int finalI = i;
            callableTasks.add(() -> findLargestCliqueStack(finalI, largest));
        }

        List<Future<BitSet>> futures = executor.invokeAll(callableTasks);
        int largestCardinality = 0;
        BitSet largestClique = null;

        for (Future<BitSet> future : futures) {
            BitSet clique = future.get();
            int cardinality = clique.cardinality();
            if (cardinality > largestCardinality) {
                largestClique = clique;
                largestCardinality = cardinality;
            }
        }

        executor.shutdown();
        return bitSetToArray(largestClique);
    }


    public int[] newFindLargestCliqueStackThreaded() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newWorkStealingPool();

        LongAccumulator largest = new LongAccumulator(Long::max, 0);
        final int NODES = edges.length;
        List<Future<BitSet>> futures = new ArrayList<>();

        final int PREBRANCHING  = 10;

        // Go backwards since high-numbered nodes finish faster
        for (int i = NODES - 1; i >= 0; --i) {
            final int finalI = i;
            if (i >= NODES - PREBRANCHING )
                futures.add(executor.submit(() -> findLargestCliqueStack(finalI, largest)));
            else
                futures.add(executor.submit(() -> findLargestCliqueStack(finalI, largest, executor)));
        }

        int largestCardinality = 0;
        BitSet largestClique = null;

        for (Future<BitSet> future : futures) {
            BitSet clique = future.get();
            int cardinality = clique.cardinality();
            if (cardinality > largestCardinality) {
                largestClique = clique;
                largestCardinality = cardinality;
            }
        }

        executor.shutdown();
        return bitSetToArray(largestClique);
    }

    private BitSet findLargestCliqueStackWorker(int node, BitSet startingClique, LongAccumulator largest) {
        final int NODES = edges.length;

        BitSet largestClique = (BitSet) startingClique.clone();
        int largestCardinality = largestClique.cardinality();

        Deque<CliqueData> stack = new ArrayDeque<>();

        BitSet neighbors = (BitSet) edges[node].clone();
        BitSet mask = new BitSet(edges.length);
        mask.flip(0, node + 1); // Only look at neighbors after the current node
        neighbors.andNot(mask);

        CliqueData startingData = new CliqueData();
        startingData.clique = startingClique;
        startingData.cardinality = largestCardinality; // Actually the same as startingClique cardinality
        startingData.neighbors = neighbors;
        startingData.nextNeighbor = neighbors.nextSetBit(node + 1);
        startingData.remainingNeighbors = neighbors.cardinality();

        stack.push(startingData);

        while (!stack.isEmpty()) {
            CliqueData currentData = stack.peek();
            if (currentData.nextNeighbor != -1 && currentData.remainingNeighbors + currentData.cardinality > largest.get()) {
                int neighbor = currentData.nextNeighbor;
                currentData.nextNeighbor = currentData.neighbors.nextSetBit(neighbor + 1);
                --currentData.remainingNeighbors;
                BitSet newClique = addNode(currentData.clique, neighbor);
                if (newClique != null) {

                    neighbors = (BitSet) edges[neighbor].clone();
                    mask = new BitSet(edges.length);
                    mask.flip(0, neighbor + 1);
                    neighbors.andNot(mask);

                    CliqueData newData = new CliqueData();
                    newData.clique = newClique;
                    newData.cardinality = currentData.cardinality + 1;
                    newData.neighbors = neighbors;
                    newData.nextNeighbor = neighbors.nextSetBit(neighbor + 1);
                    newData.remainingNeighbors = neighbors.cardinality();

                    stack.push (newData);

                    if (newData.cardinality > largestCardinality) {
                        largestClique.clear();
                        largestClique.or(newClique);
                        largestCardinality = newData.cardinality;
                        largest.accumulate(largestCardinality);
                    }
                }
            }
            else
                stack.pop();
        }
        return largestClique;
    }

    private BitSet findLargestCliqueStack(int node, LongAccumulator largest, ExecutorService executor) throws ExecutionException, InterruptedException {
        final int NODES = edges.length;
        BitSet startingClique = new BitSet(NODES);
        startingClique.set(node);
        BitSet largestClique = (BitSet) startingClique.clone();
        int largestCardinality = largestClique.cardinality();

        BitSet neighbors = (BitSet) edges[node].clone();
        BitSet mask = new BitSet(edges.length);
        mask.flip(0, node + 1); // Only look at neighbors after the current node
        neighbors.andNot(mask);

        List<Future<BitSet>> futures = new ArrayList<>();

        for (int neighbor = neighbors.nextSetBit(node + 1); neighbor != -1; neighbor = neighbors.nextSetBit(neighbor + 1)) {
            BitSet newClique = addNode(startingClique, neighbor);
            if (newClique != null) {
                int finalNeighbor = neighbor;
                futures.add(executor.submit(() -> findLargestCliqueStackWorker(finalNeighbor, newClique, largest)));
            }
        }

        for (Future<BitSet> future : futures) {
            BitSet clique = future.get();
            int cardinality = clique.cardinality();
            if (cardinality > largestCardinality) {
                largestClique = clique;
                largestCardinality = cardinality;
            }
        }

        System.out.println("[Finished starting at node " + node  + " Best: " + largestCardinality + "]");
        return largestClique;
    }


    private int findLargestClique(int node, BitSet currentClique, BitSet largestClique) {
        int largestCardinality = largestClique.cardinality();
        int currentCardinality = currentClique.cardinality();

        if (currentCardinality > largestCardinality) {
            largestClique.clear();
            largestClique.or(currentClique);
            largestCardinality = currentCardinality;
        }

        // Only look at neighbors with larger indexes than current node
        // (To avoid repetition)
        BitSet neighbors = (BitSet) edges[node].clone();
        BitSet mask = new BitSet(edges.length);
        mask.flip(0, node + 1);
        neighbors.andNot(mask);
        int nextNeighbor = neighbors.nextSetBit(node + 1);
        int remainingNeighbors = neighbors.cardinality();

        while (nextNeighbor != -1 && remainingNeighbors + currentCardinality > largestCardinality) {
            BitSet newClique = addNode(currentClique, nextNeighbor);
            if (newClique != null)
                largestCardinality = findLargestClique(nextNeighbor, newClique, largestClique);

            nextNeighbor = neighbors.nextSetBit(nextNeighbor + 1);
            --remainingNeighbors;
        }

        return largestCardinality;
    }

    private BitSet findLargestCliqueStack(int node, LongAccumulator largest) {
        final int NODES = edges.length;
        BitSet startingClique = new BitSet(NODES);
        startingClique.set(node);

        BitSet largestClique = (BitSet) startingClique.clone();
        int largestCardinality = largestClique.cardinality();

        Deque<CliqueData> stack = new ArrayDeque<>();

        BitSet neighbors = (BitSet) edges[node].clone();
        BitSet mask = new BitSet(edges.length);
        mask.flip(0, node + 1); // Only look at neighbors after the current node
        neighbors.andNot(mask);

        CliqueData startingData = new CliqueData();
        startingData.clique = startingClique;
        startingData.cardinality = largestCardinality; // Actually the same as startingClique cardinality (1)
        startingData.neighbors = neighbors;
        startingData.nextNeighbor = neighbors.nextSetBit(node + 1);
        startingData.remainingNeighbors = neighbors.cardinality();

        stack.push(startingData);

        while (!stack.isEmpty()) {
            CliqueData currentData = stack.peek();
            if (currentData.nextNeighbor != -1 && currentData.remainingNeighbors + currentData.cardinality > largest.get()) {
                int neighbor = currentData.nextNeighbor;
                currentData.nextNeighbor = currentData.neighbors.nextSetBit(neighbor + 1);
                --currentData.remainingNeighbors;
                BitSet newClique = addNode(currentData.clique, neighbor);
                if (newClique != null) {
                    neighbors = (BitSet) edges[neighbor].clone();
                    mask = new BitSet(edges.length);
                    mask.flip(0, neighbor + 1);
                    neighbors.andNot(mask);

                    CliqueData newData = new CliqueData();
                    newData.clique = newClique;
                    newData.cardinality = currentData.cardinality + 1;
                    newData.neighbors = neighbors;
                    newData.nextNeighbor = neighbors.nextSetBit(neighbor + 1);
                    newData.remainingNeighbors = neighbors.cardinality();

                    stack.push (newData);

                    if (newData.cardinality > largestCardinality) {
                        largestClique.clear();
                        largestClique.or(newClique);
                        largestCardinality = newData.cardinality;
                        largest.accumulate(largestCardinality);
                    }
                }
            }
            else
                stack.pop();
        }

        System.out.println("[Finished starting at node " + node  + " Best: " + largestCardinality + "]");
        return largestClique;
    }

    private static class CliqueData {
        public BitSet clique;
        public int cardinality;
        public int nextNeighbor;
        public BitSet neighbors;
        public int remainingNeighbors;
    }

}
