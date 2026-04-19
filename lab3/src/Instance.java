package lab3.src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Instance {
    private final List<Node> nodes;
    private final int[][] distanceMatrix;
    private final int[][] nearestNeighbors; // Tablica 10 najbliższych sąsiadów

    public Instance(String filepath) throws IOException {
        this.nodes = loadNodes(filepath);
        this.distanceMatrix = calculateDistanceMatrix(this.nodes);
        this.nearestNeighbors = calculateNearestNeighbors(10); // Dla ruchów kandydackich
    }

    private List<Node> loadNodes(String filepath) throws IOException {
        List<Node> loadedNodes = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(filepath));
        int currentId = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(";");
            if (parts.length >= 3) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int cost = Integer.parseInt(parts[2].trim());
                    loadedNodes.add(new Node(currentId, x, y, cost));
                    currentId++;
                } catch (NumberFormatException ignored) {}
            }
        }
        return loadedNodes;
    }

    private int[][] calculateDistanceMatrix(List<Node> nodes) {
        int n = nodes.size();
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    Node n1 = nodes.get(i);
                    Node n2 = nodes.get(j);
                    double dx = n1.x() - n2.x();
                    double dy = n1.y() - n2.y();
                    matrix[i][j] = (int) Math.round(Math.sqrt(dx * dx + dy * dy));
                }
            }
        }
        return matrix;
    }

    // Nowa metoda do wyznaczania najbliższych sąsiadów
    private int[][] calculateNearestNeighbors(int k) {
        int n = nodes.size();
        int[][] neighbors = new int[n][k];

        for (int i = 0; i < n; i++) {
            final int currNode = i;
            List<Integer> others = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i != j) others.add(j);
            }
            // Sortujemy po dystansie od wierzchołka 'i'
            others.sort(Comparator.comparingInt(a -> getDistance(currNode, a)));

            for (int j = 0; j < Math.min(k, others.size()); j++) {
                neighbors[i][j] = others.get(j);
            }
        }
        return neighbors;
    }

    // Weryfikacja czy krawędź jest kandydacka
    public boolean isCandidateEdge(int n1, int n2) {
        for (int neighbor : nearestNeighbors[n1]) {
            if (neighbor == n2) return true;
        }
        for (int neighbor : nearestNeighbors[n2]) {
            if (neighbor == n1) return true;
        }
        return false;
    }

    public List<Node> getNodes() { return nodes; }
    public int getDistance(int id1, int id2) { return distanceMatrix[id1][id2]; }
    public int getCost(int id) { return nodes.get(id).cost(); }
    public int getSize() { return nodes.size(); }
    public int[] getNearestNeighbors(int id) { return nearestNeighbors[id]; }
}