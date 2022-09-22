import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

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
    }
}
