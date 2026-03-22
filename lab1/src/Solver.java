package lab1.src;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Solver {
    private final Instance instance;

    public Solver(Instance instance) {
        this.instance = instance;
    }

    // =========================================================
    // 0. ALGORYTM LOSOWY
    // =========================================================
    public Solution randomSolution() {
        Solution solution = new Solution(instance);
        int totalNodes = instance.getSize();
        java.util.Random random = new java.util.Random();

        int numNodesToSelect = random.nextInt(totalNodes - 1) + 2;
        List<Integer> availableNodes = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) availableNodes.add(i);

        java.util.Collections.shuffle(availableNodes, random);
        for (int i = 0; i < numNodesToSelect; i++) {
            solution.addNodeLast(availableNodes.get(i));
        }
        return solution;
    }

    // =========================================================
    // 1. NAJBLIŻSZY SĄSIAD NN
    // =========================================================
    public Solution nearestNeighbor(int startNode, boolean useProfit) {
        Solution solution = new Solution(instance);
        int n = instance.getSize();
        Set<Integer> unvisited = new HashSet<>();
        for (int i = 0; i < n; i++) if (i != startNode) unvisited.add(i);

        solution.addNodeLast(startNode);
        int currentNode = startNode;

        while (!unvisited.isEmpty()) {
            int bestNextNode = -1;
            // Szukamy maksimum, więc startujemy od najmniejszej możliwej liczby
            double bestScore = -Double.MAX_VALUE;

            for (int candidate : unvisited) {
                double distance = instance.getDistance(currentNode, candidate);
                // Ocena = Zysk - Dystans
                double score = useProfit ? (instance.getCost(candidate) - distance) : -distance;

                if (score > bestScore) {
                    bestScore = score;
                    bestNextNode = candidate;
                }
            }
            solution.addNodeLast(bestNextNode);
            unvisited.remove(bestNextNode);
            currentNode = bestNextNode;
        }

        optimizePhaseTwo(solution);
        return solution;
    }

    // =========================================================
    // 2. ZACHŁANNY CYKL (GC)
    // =========================================================
    public Solution greedyCycle(int startNode, boolean useProfit) {
        Solution solution = new Solution(instance);
        int n = instance.getSize();
        Set<Integer> unvisited = new HashSet<>();
        for (int i = 0; i < n; i++) if (i != startNode) unvisited.add(i);

        solution.addNodeLast(startNode);

        int secondNode = -1;
        double bestDist = -Double.MAX_VALUE;
        for (int candidate : unvisited) {
            double distScore = -instance.getDistance(startNode, candidate);
            if (distScore > bestDist) {
                bestDist = distScore;
                secondNode = candidate;
            }
        }
        solution.addNodeLast(secondNode);
        unvisited.remove(secondNode);

        while (!unvisited.isEmpty()) {
            int bestNodeToInsert = -1;
            int bestInsertionIndex = -1;
            double bestScore = -Double.MAX_VALUE; // MAKSYMALIZUJEMY

            for (int candidate : unvisited) {
                for (int i = 0; i < solution.getPath().size(); i++) {
                    double deltaDist = solution.getInsertionDeltaDistance(candidate, i);
                    double score = useProfit ? (instance.getCost(candidate) - deltaDist) : -deltaDist;

                    if (score > bestScore) { // Chcemy największy score
                        bestScore = score;
                        bestNodeToInsert = candidate;
                        bestInsertionIndex = i;
                    }
                }
            }
            solution.insertNode(bestNodeToInsert, bestInsertionIndex);
            unvisited.remove(bestNodeToInsert);
        }

        optimizePhaseTwo(solution);
        return solution;
    }

    // =========================================================
    // 3. ALGORYTM 2-ŻAL
    // =========================================================
    public Solution regretCycle(int startNode, boolean useProfit, boolean weighted) {
        Solution solution = new Solution(instance);
        int n = instance.getSize();
        Set<Integer> unvisited = new HashSet<>();
        for (int i = 0; i < n; i++) if (i != startNode) unvisited.add(i);

        solution.addNodeLast(startNode);
        int secondNode = -1;
        double bestDist = -Double.MAX_VALUE;
        for (int candidate : unvisited) {
            double distScore = -instance.getDistance(startNode, candidate);
            if (distScore > bestDist) {
                bestDist = distScore;
                secondNode = candidate;
            }
        }
        solution.addNodeLast(secondNode);
        unvisited.remove(secondNode);

        while (!unvisited.isEmpty()) {
            int bestNodeToInsert = -1;
            int bestInsertionIndex = -1;
            double maxScore = -Double.MAX_VALUE;
            double bestAssociatedBenefit = -Double.MAX_VALUE;

            for (int candidate : unvisited) {
                double b1 = -Double.MAX_VALUE;
                double b2 = -Double.MAX_VALUE;
                int bestLocalIndex = -1;

                for (int i = 0; i < solution.getPath().size(); i++) {
                    double deltaDist = solution.getInsertionDeltaDistance(candidate, i);
                    double benefit = useProfit ? (instance.getCost(candidate) - deltaDist) : -deltaDist;

                    if (benefit > b1) {
                        b2 = b1;
                        b1 = benefit;
                        bestLocalIndex = i;
                    } else if (benefit > b2) {
                        b2 = benefit;
                    }
                }


                double regret = b1 - b2;
                // Jeśli ważony, bierzemy pod uwagę też to, jak dobra ogólnie jest ta wstawka
                double score = weighted ? (regret + b1) : regret;

                boolean isBetter = false;
                if (score > maxScore) {
                    isBetter = true;
                } else if (Math.abs(score - maxScore) < 0.0001 && b1 > bestAssociatedBenefit) {
                    isBetter = true;
                }

                if (isBetter) {
                    maxScore = score;
                    bestAssociatedBenefit = b1;
                    bestNodeToInsert = candidate;
                    bestInsertionIndex = bestLocalIndex;
                }
            }

            solution.insertNode(bestNodeToInsert, bestInsertionIndex);
            unvisited.remove(bestNodeToInsert);
        }

        return solution;
    }

    // =========================================================
    // FAZA II - OPTYMALIZACJA
    // =========================================================
    private void optimizePhaseTwo(Solution solution) {
        boolean improvement = true;

        while (improvement && solution.getPath().size() > 3) {
            improvement = false;
            double bestObjectiveDelta = 0.0;
            int bestIndexToRemove = -1;

            for (int i = 0; i < solution.getPath().size(); i++) {
                int nodeId = solution.getPath().get(i);
                double deltaDist = solution.getRemovalDeltaDistance(i);
                int profit = instance.getCost(nodeId);

                // Zmiana funkcji celu przy usunięciu:
                // Tracimy zysk z tego miasta (-profit) ale zyskujemy dystans (odejmujemy ujemną deltę)
                double objectiveDelta = -profit - deltaDist;

                // Szukamy takiego usunięcia, które podbije nasz wynik celu na PLUS (największa zmiana dodatnia)
                if (objectiveDelta > bestObjectiveDelta) {
                    bestObjectiveDelta = objectiveDelta;
                    bestIndexToRemove = i;
                }
            }

            if (bestIndexToRemove != -1 && bestObjectiveDelta > 0.0001) {
                solution.removeNode(bestIndexToRemove);
                improvement = true;
            }
        }
    }
}