package lab1.src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Instance {
    private final List<Node> nodes;
    private final double[][] distanceMatrix;

    public Instance(String filepath) throws IOException {
        this.nodes = loadNodes(filepath);
        this.distanceMatrix = calculateDistanceMatrix(this.nodes);
    }

    private List<Node> loadNodes(String filepath) throws IOException {
        List<Node> loadedNodes = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(filepath));

        int currentId = 0; // Zgodnie z umową, zaczynamy ID od 0

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Twoje pliki używają średników jako separatorów
            String[] parts = line.split(";");
            if (parts.length >= 3) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int cost = Integer.parseInt(parts[2].trim());

                    loadedNodes.add(new Node(currentId, x, y, cost));
                    currentId++;
                } catch (NumberFormatException e) {
                    // Ignoruje ewentualne nagłówki literowe i idzie dalej
                }
            }
        }
        return loadedNodes;
    }

    private double[][] calculateDistanceMatrix(List<Node> nodes) {
        int n = nodes.size();
        double[][] matrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0.0;
                } else {
                    Node n1 = nodes.get(i);
                    Node n2 = nodes.get(j);

                    double dx = n1.x() - n2.x();
                    double dy = n1.y() - n2.y();
                    // Dokładna wartość zmiennoprzecinkowa (double) bez zaokrąglania do int
                    matrix[i][j] = Math.sqrt(dx * dx + dy * dy);
                }
            }
        }
        return matrix;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public double getDistance(int id1, int id2) {
        // Skoro ID zaczyna się od 0, możemy bezpośrednio odpytać tablicę
        return distanceMatrix[id1][id2];
    }

    public int getCost(int id) {
        return nodes.get(id).cost();
    }

    public int getSize() {
        return nodes.size();
    }

}