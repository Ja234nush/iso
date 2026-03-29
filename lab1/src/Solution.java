package lab1.src;

import java.util.ArrayList;
import java.util.List;

public class Solution {
    private final Instance instance;
    private final List<Integer> path;
    private double totalDistance; // Zmienione na double
    private int totalProfit;

    public Solution(Instance instance) {
        this.instance = instance;
        this.path = new ArrayList<>();
        this.totalDistance = 0.0;
        this.totalProfit = 0;
    }

    public Solution(Solution other) {
        this.instance = other.instance;
        this.path = new ArrayList<>(other.path);
        this.totalDistance = other.totalDistance;
        this.totalProfit = other.totalProfit;
    }

    public double getInsertionDeltaDistance(int nodeId, int insertIndex) {
        if (path.isEmpty()) return 0.0;
        if (path.size() == 1) {
            return 2.0 * instance.getDistance(path.get(0), nodeId);
        }

        int prevNode = path.get((insertIndex - 1 + path.size()) % path.size());
        int nextNode = path.get(insertIndex % path.size());

        // Efektywne liczenie lokalnej zmiany (delty)
        return instance.getDistance(prevNode, nodeId)
                + instance.getDistance(nodeId, nextNode)
                - instance.getDistance(prevNode, nextNode);
    }

    public void insertNode(int nodeId, int insertIndex) {
        double deltaDist = getInsertionDeltaDistance(nodeId, insertIndex);

        if (insertIndex >= path.size()) {
            path.add(nodeId);
        } else {
            path.add(insertIndex, nodeId);
        }

        this.totalDistance += deltaDist;
        this.totalProfit += instance.getCost(nodeId);
    }

    public void addNodeLast(int nodeId) {
        insertNode(nodeId, path.size());
    }

    public List<Integer> getPath() { return path; }
    public double getTotalDistance() { return totalDistance; }
    public int getTotalProfit() { return totalProfit; }

    public double getObjectiveValue() {
        return totalProfit - totalDistance;
    }

    // Liczy zmianę dystansu po usunięciu wierzchołka pod danym indeksem
    public double getRemovalDeltaDistance(int index) {
        if (path.size() < 3) return 0.0; // Nie możemy usunąć, jeśli zniszczy to cykl

        int prevNode = path.get((index - 1 + path.size()) % path.size());
        int currNode = path.get(index);
        int nextNode = path.get((index + 1) % path.size());

        // Usuwamy dwie krawędzie i dodajemy jedną bezpośrednią
        return instance.getDistance(prevNode, nextNode)
                - instance.getDistance(prevNode, currNode)
                - instance.getDistance(currNode, nextNode);
    }

    // Usuwa wierzchołek i aktualizuje koszty
    public void removeNode(int index) {
        double deltaDist = getRemovalDeltaDistance(index);
        int nodeId = path.get(index);

        this.totalDistance += deltaDist;
        this.totalProfit -= instance.getCost(nodeId); // Odejmujemy zysk usuniętego miasta
        path.remove(index);
    }

    @Override
    public String toString() {
        // Skracamy wyświetlanie double do 2 miejsc po przecinku dla czytelności
        return String.format("Rozwiązanie | Dystans: %.2f | Zysk: %d | Cel: %.2f | Rozmiar: %d",
                totalDistance, totalProfit, getObjectiveValue(), path.size());
    }
}