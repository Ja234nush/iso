package lab2.src;

import java.util.ArrayList;
import java.util.List;

public class Solution {
    private final Instance instance;
    private final List<Integer> path;
    private int totalDistance;
    private int totalProfit;

    public Solution(Instance instance) {
        this.instance = instance;
        this.path = new ArrayList<>();
        this.totalDistance = 0;
        this.totalProfit = 0;
    }

    public Solution(Solution other) {
        this.instance = other.instance;
        this.path = new ArrayList<>(other.path);
        this.totalDistance = other.totalDistance;
        this.totalProfit = other.totalProfit;
    }

    // =================================================================
    // METODY DO KONSTRUKCJI (LAB 1)
    // =================================================================

    public int getInsertionDeltaDistance(int nodeId, int insertIndex) {
        if (path.isEmpty()) return 0;
        if (path.size() == 1) {
            return 2 * instance.getDistance(path.get(0), nodeId);
        }

        int prevNode = path.get((insertIndex - 1 + path.size()) % path.size());
        int nextNode = path.get(insertIndex % path.size());

        return instance.getDistance(prevNode, nodeId)
                + instance.getDistance(nodeId, nextNode)
                - instance.getDistance(prevNode, nextNode);
    }

    public void insertNode(int nodeId, int insertIndex) {
        int deltaDist = getInsertionDeltaDistance(nodeId, insertIndex);

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

    // =================================================================
    // METODY DO LOCAL SEARCH (LAB 2)
    // =================================================================

    // 1. WYMIANA ZEWNĘTRZNA (EXCHANGE)
    // =================================================================
    // RUCH: DODAJ WIERZCHOŁEK (ADD)
    // =================================================================
    public int getAddDelta(int newNodeId, int insertIndex) {
        int prevNode = path.get((insertIndex - 1 + path.size()) % path.size());
        int nextNode = path.get(insertIndex % path.size());

        // O ile wydłuży się trasa?
        int deltaDist = instance.getDistance(prevNode, newNodeId)
                + instance.getDistance(newNodeId, nextNode)
                - instance.getDistance(prevNode, nextNode);

        int deltaProfit = instance.getCost(newNodeId);

        // Zmiana funkcji celu: Zysk - Koszt dystansu
        return deltaProfit - deltaDist;
    }

    public void applyAdd(int newNodeId, int insertIndex) {
        path.add(insertIndex, newNodeId);
        recalculateObjective();
    }

    // =================================================================
    // RUCH: USUŃ WIERZCHOŁEK (REMOVE)
    // =================================================================
    public int getRemoveDelta(int routeIndex) {
        if (path.size() <= 3) return Integer.MIN_VALUE; // Zabezpieczenie przed zniszczeniem cyklu

        int nodeToRemove = path.get(routeIndex);
        int prevNode = path.get((routeIndex - 1 + path.size()) % path.size());
        int nextNode = path.get((routeIndex + 1) % path.size());

        // O ile skróci się trasa? (Stary dystans minus nowy bezpośredni skrót)
        int oldDist = instance.getDistance(prevNode, nodeToRemove) + instance.getDistance(nodeToRemove, nextNode);
        int newDist = instance.getDistance(prevNode, nextNode);

        int distSaved = oldDist - newDist; // To jest na plus (oszczędność paliwa)
        int profitLost = instance.getCost(nodeToRemove); // To jest na minus (tracimy zysk)

        // Zmiana funkcji celu: Zyskany dystans - Utracony profit
        return distSaved - profitLost;
    }

    public void applyRemove(int routeIndex) {
        path.remove(routeIndex);
        recalculateObjective();
    }

    // 2. ZAMIANA WIERZCHOŁKÓW (SWAP)
    public int getSwapDelta(int i, int j) {
        if (i == j) return 0;
        if (i > j) { int temp = i; i = j; j = temp; } // Wymuszamy i < j

        int prevI = path.get((i - 1 + path.size()) % path.size());
        int nodeI = path.get(i);
        int nextI = path.get((i + 1) % path.size());

        int prevJ = path.get((j - 1 + path.size()) % path.size());
        int nodeJ = path.get(j);
        int nextJ = path.get((j + 1) % path.size());

        int oldDist, newDist;
        if (j == i + 1) { // Sąsiedzi w środku trasy
            oldDist = instance.getDistance(prevI, nodeI) + instance.getDistance(nodeJ, nextJ);
            newDist = instance.getDistance(prevI, nodeJ) + instance.getDistance(nodeI, nextJ);
        } else if (i == 0 && j == path.size() - 1) { // Sąsiedzi na zawinięciu (pierwszy z ostatnim)
            oldDist = instance.getDistance(prevJ, nodeJ) + instance.getDistance(nodeI, nextI);
            newDist = instance.getDistance(prevJ, nodeI) + instance.getDistance(nodeJ, nextI);
        } else { // Normalna zamiana
            oldDist = instance.getDistance(prevI, nodeI) + instance.getDistance(nodeI, nextI)
                    + instance.getDistance(prevJ, nodeJ) + instance.getDistance(nodeJ, nextJ);
            newDist = instance.getDistance(prevI, nodeJ) + instance.getDistance(nodeJ, nextI)
                    + instance.getDistance(prevJ, nodeI) + instance.getDistance(nodeI, nextJ);
        }
        return oldDist - newDist;
    }

    public void applySwap(int i, int j) {
        int temp = path.get(i);
        path.set(i, path.get(j));
        path.set(j, temp);
        recalculateObjective();
    }

    // 3. WYMIANA KRAWĘDZI (2-OPT)
    public int getTwoOptDelta(int i, int j) {
        if (i >= j) return 0;
        // Zabezpieczenie przed nieskończoną pętlą - odwrócenie całego cyklu to tak naprawdę brak zmiany!
        if (i == 0 && j == path.size() - 1) return 0;

        int prevI = path.get((i - 1 + path.size()) % path.size());
        int nodeI = path.get(i);
        int nodeJ = path.get(j);
        int nextJ = path.get((j + 1) % path.size());

        int oldDist = instance.getDistance(prevI, nodeI) + instance.getDistance(nodeJ, nextJ);
        int newDist = instance.getDistance(prevI, nodeJ) + instance.getDistance(nodeI, nextJ);
        return oldDist - newDist;
    }

    public void applyTwoOpt(int i, int j) {
        if (i >= j || (i == 0 && j == path.size() - 1)) return;
        java.util.Collections.reverse(path.subList(i, j + 1));
        recalculateObjective();
    }

    // =================================================================
    // METODY POMOCNICZE
    // =================================================================

    public void recalculateObjective() {
        this.totalDistance = 0;
        this.totalProfit = 0;
        for (int i = 0; i < path.size(); i++) {
            int current = path.get(i);
            int next = path.get((i + 1) % path.size());
            this.totalDistance += instance.getDistance(current, next);
            this.totalProfit += instance.getCost(current);
        }
    }

    public List<Integer> getPath() { return path; }
    public int getTotalDistance() { return totalDistance; }
    public int getObjectiveValue() { return totalProfit - totalDistance; }

    @Override
    public String toString() {
        return String.format("Rozwiązanie | Dystans: %d | Zysk: %d | Cel: %d | Rozmiar: %d",
                totalDistance, totalProfit, getObjectiveValue(), path.size());
    }
}