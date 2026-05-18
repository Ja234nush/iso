package lab5.src;

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

    public int getInsertionDeltaDistance(int nodeId, int insertIndex) {
        if (path.isEmpty()) return 0;
        if (path.size() == 1) return 2 * instance.getDistance(path.get(0), nodeId);
        int prevNode = path.get((insertIndex - 1 + path.size()) % path.size());
        int nextNode = path.get(insertIndex % path.size());
        return instance.getDistance(prevNode, nodeId) + instance.getDistance(nodeId, nextNode) - instance.getDistance(prevNode, nextNode);
    }

    public void insertNode(int nodeId, int insertIndex) {
        int deltaDist = getInsertionDeltaDistance(nodeId, insertIndex);
        if (insertIndex >= path.size()) path.add(nodeId);
        else path.add(insertIndex, nodeId);
        this.totalDistance += deltaDist;
        this.totalProfit += instance.getCost(nodeId);
    }

    public void addNodeLast(int nodeId) {
        insertNode(nodeId, path.size());
    }

    public int getAddDelta(int newNodeId, int insertIndex) {
        int prevNode = path.get((insertIndex - 1 + path.size()) % path.size());
        int nextNode = path.get(insertIndex % path.size());
        int deltaDist = instance.getDistance(prevNode, newNodeId) + instance.getDistance(newNodeId, nextNode) - instance.getDistance(prevNode, nextNode);
        int deltaProfit = instance.getCost(newNodeId);
        return deltaProfit - deltaDist;
    }

    public void applyAdd(int newNodeId, int insertIndex) {
        path.add(insertIndex, newNodeId);
        recalculateObjective();
    }

    public int getRemoveDelta(int routeIndex) {
        if (path.size() <= 3) return Integer.MIN_VALUE;
        int nodeToRemove = path.get(routeIndex);
        int prevNode = path.get((routeIndex - 1 + path.size()) % path.size());
        int nextNode = path.get((routeIndex + 1) % path.size());
        int oldDist = instance.getDistance(prevNode, nodeToRemove) + instance.getDistance(nodeToRemove, nextNode);
        int newDist = instance.getDistance(prevNode, nextNode);
        int distSaved = oldDist - newDist;
        int profitLost = instance.getCost(nodeToRemove);
        return distSaved - profitLost;
    }

    public void applyRemove(int routeIndex) {
        path.remove(routeIndex);
        recalculateObjective();
    }

    public int getTwoOptDelta(int i, int j) {
        if (i >= j || (i == 0 && j == path.size() - 1)) return 0;
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
    public int getObjectiveValue() { return totalProfit - totalDistance; }

    // 1. Liczba wspólnych wierzchołków
    public int getCommonNodes(Solution other) {
        java.util.Set<Integer> myNodes = new java.util.HashSet<>(this.getPath());
        java.util.Set<Integer> otherNodes = new java.util.HashSet<>(other.getPath());
        myNodes.retainAll(otherNodes); // Część wspólna
        return myNodes.size();
    }

    // 2. Liczba wspólnych krawędzi
    public int getCommonEdges(Solution other) {
        java.util.Set<String> myEdges = this.getEdgesSet();
        java.util.Set<String> otherEdges = other.getEdgesSet();
        myEdges.retainAll(otherEdges); // Część wspólna krawędzi
        return myEdges.size();
    }

    // Metoda pomocnicza: generuje zbiór krawędzi w postaci "mniejszy-wiekszy"
    private java.util.Set<String> getEdgesSet() {
        java.util.Set<String> edges = new java.util.HashSet<>();
        if (path.isEmpty()) return edges;

        for (int i = 0; i < path.size(); i++) {
            int u = path.get(i);
            int v = path.get((i + 1) % path.size());
            // Zapewniamy, że krawędź u-v to to samo co v-u
            int min = Math.min(u, v);
            int max = Math.max(u, v);
            edges.add(min + "-" + max);
        }
        return edges;
    }
}