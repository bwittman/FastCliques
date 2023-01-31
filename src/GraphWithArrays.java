import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;

public class GraphWithArrays {

    private boolean[][] edges;
    private int totalEdges;
    private int[] degrees;

    public GraphWithArrays(Scanner in) {
        int nodes = 0;
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
        return edges[from][to];
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

    private static int cardinality(boolean[] set) {
        int size = 0;
        for (boolean value : set) {
            if (value)
                ++size;
        }

        return size;
    }

    private static int cardinality(boolean[] set, int starting) {
        int size = 0;
        for (int i = starting; i < set.length; ++i) {
            if (set[i])
                ++size;
        }

        return size;
    }

    public static int[] booleansToArray(boolean[] set) {
        int[] nodes = new int[cardinality(set)];
        int index = 0;

        for (int i = 0; i < set.length; ++i) {
            if (set[i]) {
                nodes[index] = i;
                ++index;
            }

            /* // Unnecessary safety check
            if (i == Integer.MAX_VALUE) {
                break; // or (i+1) would overflow
            }
            */
        }
        return nodes;
    }

    public boolean isClique(boolean[] set) {
        return isClique(booleansToArray(set));
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

    public boolean[] addNode(boolean[] clique, int node) {
        //if (clique.get(node))
        //return null;

        boolean[] nodeEdges = edges[node];

        for (int i = 0; i < nodeEdges.length; ++i) {
            if (clique[i]) {
                if (!nodeEdges[i])
                    return null;
            }
        }

        boolean[] newClique = clique.clone();
        newClique[node] = true;
        return newClique;
    }

    public boolean makeClique(boolean[] clique, int node) {
        //if (clique.get(node))
        //return null;

        boolean[] nodeEdges = edges[node];

        for (int i = 0; i < nodeEdges.length; ++i) {
            if (clique[i]) {
                if (!nodeEdges[i])
                    return false;
            }
        }

        clique[node] = true;
        return true;
    }


    public int[] findLargestCliqueStackThreaded() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newWorkStealingPool();

        LongAccumulator largest = new LongAccumulator(Long::max, 0);
        final int NODES = edges.length;
        List<Callable<boolean[]>> callableTasks = new ArrayList<>();
        // Go backwards since high-numbered nodes finish faster
        for (int i = NODES - 1; i >= 0; --i) {
            final int finalI = i;
            callableTasks.add(() -> findLargestCliqueStack(finalI, largest));
        }

        List<Future<boolean[]>> futures = executor.invokeAll(callableTasks);
        int largestCardinality = 0;
        boolean[] largestClique = null;

        for (Future<boolean[]> future : futures) {
            boolean[] clique = future.get();
            int cardinality = cardinality(clique);
            if (cardinality > largestCardinality) {
                largestClique = clique;
                largestCardinality = cardinality;
            }
        }

        executor.shutdown();
        return booleansToArray(largestClique);
    }


    public int[] newFindLargestCliqueStackThreaded() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newWorkStealingPool();

        LongAccumulator largest = new LongAccumulator(Long::max, 1); // A non-empty graph will always have a size at least 1
        final int NODES = edges.length;
        List<Future<boolean[]>> futures = new ArrayList<>();

        final int PREBRANCHING  = 20;

        // Go backwards since high-numbered nodes finish faster
        for (int i = NODES - 1; i >= 0; --i) {
            final int finalI = i;
            if (i >= NODES - PREBRANCHING )
                futures.add(executor.submit(() -> findLargestCliqueStack(finalI, largest)));
            else
                futures.add(executor.submit(() -> findLargestCliqueStack(finalI, largest, executor)));
        }

        int largestCardinality = 0;
        boolean[] largestClique = null;

        for (Future<boolean[]> future : futures) {
            boolean[] clique = future.get();
            int cardinality = cardinality(clique);
            if (cardinality > largestCardinality) {
                largestClique = clique;
                largestCardinality = cardinality;
            }
        }

        executor.shutdown();
        return booleansToArray(largestClique);
    }

    private boolean[] findLargestCliqueStackWorker(int node, boolean[] startingClique, LongAccumulator largest) {
        final int NODES = edges.length;

        boolean[] largestClique = startingClique.clone();
        int largestCardinality = cardinality(largestClique);

        Deque<GraphWithArrays.CliqueData> stack = new ArrayDeque<>();

        boolean[] neighbors = edges[node];
        int next = node + 1;
        while (next < NODES && !neighbors[next])
            ++next;

        GraphWithArrays.CliqueData startingData = new GraphWithArrays.CliqueData();
        startingData.clique = startingClique;
        startingData.cardinality = largestCardinality; // Actually the same as startingClique cardinality
        startingData.neighbors = neighbors;
        startingData.nextNeighbor = next;
        startingData.remainingNeighbors = cardinality(neighbors, next);

        stack.push(startingData);

        while (!stack.isEmpty()) {
            GraphWithArrays.CliqueData currentData = stack.peek();
            if (currentData.remainingNeighbors + currentData.cardinality > largest.get()) {
                --currentData.remainingNeighbors;
                int neighbor = currentData.nextNeighbor;
                boolean[] newClique = addNode(currentData.clique, neighbor);
                if (newClique != null) {
                    neighbors = edges[neighbor];
                    GraphWithArrays.CliqueData newData = new GraphWithArrays.CliqueData();
                    newData.clique = newClique;
                    newData.cardinality = currentData.cardinality + 1;
                    newData.neighbors = neighbors;
                    newData.nextNeighbor = neighbor + 1;
                    while (newData.nextNeighbor < NODES && !neighbors[newData.nextNeighbor])
                        ++newData.nextNeighbor;
                    newData.remainingNeighbors = cardinality(neighbors, newData.nextNeighbor);

                    stack.push(newData);

                    if (newData.cardinality > largestCardinality) {
                        System.arraycopy(newClique, 0, largestClique, 0, NODES);
                        largestCardinality = newData.cardinality;
                        largest.accumulate(largestCardinality);
                    }
                }

                do {
                    ++currentData.nextNeighbor;
                } while (currentData.nextNeighbor < NODES && !currentData.neighbors[currentData.nextNeighbor]);
            }
            else
                stack.pop();
        }
        return largestClique;
    }

    private boolean[] findLargestCliqueStack(int node, LongAccumulator largest, ExecutorService executor) throws ExecutionException, InterruptedException {
        final int NODES = edges.length;
        boolean[] startingClique = new boolean[NODES];
        startingClique[node] = true;
        boolean[] largestClique = startingClique.clone();
        int largestCardinality = 1;

        boolean[] neighbors = edges[node].clone();
        int neighbor = node + 1;
        while (neighbor < NODES && !neighbors[neighbor])
            ++neighbor;

        List<Future<boolean[]>> futures = new ArrayList<>();

        while (neighbor < NODES) {
            if (neighbors[neighbor]) {
                boolean[] newClique = addNode(startingClique, neighbor);
                if (newClique != null) {
                    int finalNeighbor = neighbor;
                    futures.add(executor.submit(() -> findLargestCliqueStackWorker(finalNeighbor, newClique, largest)));
                }
            }

            ++neighbor;
        }

        for (Future<boolean[]> future : futures) {
            boolean[] clique = future.get();
            int cardinality = cardinality(clique);
            if (cardinality > largestCardinality) {
                largestClique = clique;
                largestCardinality = cardinality;
            }
        }

        System.out.println("[Finished starting at node " + node  + " Best: " + largestCardinality + "]");
        return largestClique;
    }

    public int[] findLargestCliqueThreaded() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newWorkStealingPool();

            LongAccumulator largest = new LongAccumulator(Long::max, 1); // A non-empty graph will always have a size at least 1
            final int NODES = edges.length;
            List<Future<boolean[]>> futures = new ArrayList<>();

            final int PREBRANCHING = 40;

            // Go backwards since high-numbered nodes finish faster
            for (int i = NODES - 1; i >= 0; --i) {
                final int finalI = i;
                if (i >= NODES - PREBRANCHING)
                    futures.add(executor.submit(() -> findLargestCliqueThreaded(finalI, largest)));
                else
                    futures.add(executor.submit(() -> findLargestCliqueThreaded(finalI, largest, executor)));
            }

            int largestCardinality = 0;
            boolean[] largestClique = null;

            for (Future<boolean[]> future : futures) {
                boolean[] clique = future.get();
                int cardinality = cardinality(clique);
                if (cardinality > largestCardinality) {
                    largestClique = clique;
                    largestCardinality = cardinality;
                }
            }

        executor.shutdown();
            return booleansToArray(largestClique);

    }


    private boolean[] findLargestCliqueThreaded(int node, LongAccumulator largest) {
        final int NODES = edges.length;
        boolean[] clique = new boolean[NODES];
        clique[node] = true;
        boolean[] largestClique = clique.clone();
        boolean[] neighbors = edges[node];
        AtomicInteger largestCardinality = new AtomicInteger(1);

        for (int i = node + 1; i < NODES; ++i) {
            if (neighbors[i]) {
                clique[i] = true;
                findLargestCliqueThreaded(i, clique, 1, largestClique, largestCardinality, largest);
                clique[i] = false;
            }
        }

        System.out.println("[Finished starting at node " + node  + " Best: " + largestCardinality + "]");

        return largestClique;
    }

    private boolean[] findLargestCliqueThreaded(int node, LongAccumulator largest, ExecutorService executor) throws ExecutionException, InterruptedException {
        final int NODES = edges.length;
        boolean[] clique = new boolean[NODES];
        clique[node] = true;

        boolean[] neighbors = edges[node];

        List<Future<boolean[]>> futures = new ArrayList<>();
        for (int i = node + 1; i < NODES; ++i) {
            if (neighbors[i]) {
                boolean[] largestClique = clique.clone();
                boolean[] currentClique = clique.clone();
                AtomicInteger largestCardinality = new AtomicInteger(1);
                currentClique[i] = true;
                int finalI = i;
                futures.add(executor.submit(() -> findLargestCliqueThreaded(finalI, currentClique, 1, largestClique, largestCardinality, largest)));
            }
        }

        int largestCardinality = 0;
        boolean[] largestClique = null;

        for (Future<boolean[]> future : futures) {
            clique = future.get();
            if (clique != null) {
                int cardinality = cardinality(clique);
                if (cardinality > largestCardinality) {
                    largestClique = clique;
                    largestCardinality = cardinality;
                }
            }
        }

        System.out.println("[Finished starting at node " + node  + " Best: " + largestCardinality + "]");
        return largestClique;
    }

    private boolean[]  findLargestCliqueThreaded(int node, boolean[] clique, int cardinality, boolean[] largestClique, AtomicInteger largestCardinality, LongAccumulator largest) {
        final int NODES = edges.length;
        boolean[] neighbors = edges[node];
        for (int j = node - 1; j >= 0; --j) {
            if (clique[j] && !neighbors[j])
                return null;
        }

        ++cardinality; // Yuck

        if (cardinality > largestCardinality.get()) {
            largestCardinality.set(cardinality);
            System.arraycopy(clique, 0, largestClique, 0, NODES);
            largest.accumulate(cardinality);
        }

        int remainingNeighbors = 0;
        for (int i = node + 1; i < NODES; ++i)
            if (neighbors[i])
                ++remainingNeighbors;

        for (int i = node + 1; i < NODES; ++i) {
            if (neighbors[i] ) {
                if (remainingNeighbors + cardinality > largest.get()) {
                    clique[i] = true;
                    findLargestCliqueThreaded(i, clique, cardinality, largestClique, largestCardinality, largest);
                    clique[i] = false;
                }
                --remainingNeighbors;
            }
        }

        return largestClique;
    }

    private int findLargestCliqueThreadedWorker(int node, boolean[] currentClique, int currentSize, boolean[] largestClique, int largestSize, LongAccumulator globalLargest) {
        final int NODES = edges.length;

        if (currentSize > largestSize) {
            System.arraycopy(currentClique, 0, largestClique, 0, NODES);
            globalLargest.accumulate(currentSize);
            largestSize = currentSize;
        }

        // Only look at neighbors with larger indexes than current node
        // (To avoid repetition)
        boolean[] neighbors = edges[node];
        int nextNeighbor = node + 1;
        while (nextNeighbor < NODES && !neighbors[nextNeighbor])
            ++nextNeighbor;

        int remainingNeighbors = cardinality(neighbors, nextNeighbor);

        while (remainingNeighbors + currentSize > globalLargest.get()) {
            if (neighbors[nextNeighbor]) {
                if (makeClique(currentClique, nextNeighbor)) {
                    largestSize = Math.max(largestSize, findLargestCliqueThreadedWorker(nextNeighbor, currentClique, currentSize + 1, largestClique, largestSize, globalLargest));
                    currentClique[nextNeighbor] = false;
                }

                --remainingNeighbors;
            }

            ++nextNeighbor;
        }

        return largestSize;
    }

    private boolean[] findLargestCliqueStack(int node, LongAccumulator largest) {
        final int NODES = edges.length;
        boolean[] startingClique = new boolean[NODES];
        startingClique[node] = true;

        boolean[] largestClique = startingClique.clone();
        int largestCardinality = 1;

        Deque<GraphWithArrays.CliqueData> stack = new ArrayDeque<>();

        boolean[] neighbors = edges[node];
        int nextNeighbor = node + 1;
        while (nextNeighbor < NODES && !neighbors[nextNeighbor])
            ++nextNeighbor;

        GraphWithArrays.CliqueData startingData = new GraphWithArrays.CliqueData();
        startingData.clique = startingClique;
        startingData.cardinality = largestCardinality; // Actually the same as startingClique cardinality (1)
        startingData.neighbors = neighbors;
        startingData.nextNeighbor = nextNeighbor;
        startingData.remainingNeighbors = cardinality(neighbors, nextNeighbor);

        stack.push(startingData);

        while (!stack.isEmpty()) {
            GraphWithArrays.CliqueData currentData = stack.peek();
            if (currentData.remainingNeighbors + currentData.cardinality > largest.get()) {
                --currentData.remainingNeighbors;
                int neighbor = currentData.nextNeighbor;
                boolean[] newClique = addNode(currentData.clique, neighbor);
                if (newClique != null) {
                    neighbors = edges[neighbor];
                    GraphWithArrays.CliqueData newData = new GraphWithArrays.CliqueData();
                    newData.clique = newClique;
                    newData.cardinality = currentData.cardinality + 1;
                    newData.neighbors = neighbors;
                    newData.nextNeighbor = neighbor + 1;
                    while (newData.nextNeighbor < NODES && !neighbors[newData.nextNeighbor])
                        ++newData.nextNeighbor;
                    newData.remainingNeighbors = cardinality(neighbors, newData.nextNeighbor);

                    stack.push(newData);

                    if (newData.cardinality > largestCardinality) {
                        System.arraycopy(newClique, 0, largestClique, 0, NODES);
                        largestCardinality = newData.cardinality;
                        largest.accumulate(largestCardinality);
                    }
                }

                do {
                    ++currentData.nextNeighbor;
                } while (currentData.nextNeighbor < NODES && !currentData.neighbors[currentData.nextNeighbor]);
            }
            else
                stack.pop();
        }

        System.out.println("[Finished starting at node " + node  + " Best: " + largestCardinality + "]");
        return largestClique;
    }

    private static class CliqueData {
        public boolean[] clique;
        public int cardinality;
        public int nextNeighbor;
        public boolean[] neighbors;
        public int remainingNeighbors;
    }

}
