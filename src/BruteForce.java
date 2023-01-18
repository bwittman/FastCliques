import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class BruteForce {
    public static void main(String[] args) throws FileNotFoundException, ExecutionException, InterruptedException {

        Graph graph;
        //String graphName = "johnson16-2-4.clq";
        //String graphName = "johnson8-4-4.clq";
        //String graphName = "hamming6-2.clq";
        //String graphName = "hamming6-4.clq";
        //String graphName = "hamming8-2.clq"; // *
        //String graphName = "keller4.clq";
        //String graphName = "MANN_a9.clq";
        //String graphName = "p_hat1500-3.clq"; // *
        //String graphName = "p_hat300-1.clq";
        //String graphName = "p_hat300-3.clq";
        //String graphName = "p_hat500-1.clq";
        //String graphName = "p_hat500-2.clq";
        //String graphName = "p_hat500-3.clq";
        //String graphName = "p_hat700-1.clq";
        //String graphName = "p_hat700-2.clq";
        //String graphName = "p_hat700-3.clq";
        //String graphName = "san200_0.7_1.clq";
        String graphName = "brock200_1.clq";
        //String graphName = "C125.9.clq";


        try (Scanner file = new Scanner(new File("data/" + graphName))) {
            graph = new Graph(file);
        }

        long start = System.nanoTime();
        //int[] nodes = graph.findLargestClique();
        //int[] nodes = graph.findLargestCliqueStack();
        //int[] nodes = graph.findLargestCliqueStackThreaded();
        int[] nodes = graph.newFindLargestCliqueStackThreaded();
        long end = System.nanoTime();
        System.out.format("Clique size: %d %.3f seconds%n", nodes.length, (end - start) / 1000000000.0);
        if (graph.isClique(nodes))
            System.out.println("It's a clique!");
        else
            System.out.println("It's not a clique!");
    }
}
