package lab2.src;

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
    // ROZWIĄZANIE STARTOWE 1: LOSOWE
    // =========================================================
    // =========================================================
    // ROZWIĄZANIE STARTOWE 1: LOSOWE
    // =========================================================
    public Solution randomSolution() {
        Solution solution = new Solution(instance);
        int totalNodes = instance.getSize();
        java.util.Random random = new java.util.Random();

        // TUTAJ ZMIANA: Wybieramy zawsze równo 50% wierzchołków
        int numNodesToSelect = (int) Math.round(totalNodes / 2.0);
        List<Integer> availableNodes = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) availableNodes.add(i);

        java.util.Collections.shuffle(availableNodes, random);
        for (int i = 0; i < numNodesToSelect; i++) {
            solution.addNodeLast(availableNodes.get(i));
        }
        return solution;
    }

    // =========================================================
    // ROZWIĄZANIE STARTOWE 2: ALGORYTM 2-ŻAL (Z ZYSKIEM)
    // =========================================================
    public Solution regretCycle(int startNode, boolean useProfit, boolean weighted) {
        Solution solution = new Solution(instance);
        int n = instance.getSize();
        Set<Integer> unvisited = new HashSet<>();
        for (int i = 0; i < n; i++) if (i != startNode) unvisited.add(i);

        solution.addNodeLast(startNode);
        int secondNode = -1;
        int bestDist = Integer.MIN_VALUE;
        for (int candidate : unvisited) {
            int distScore = useProfit ?
                    (instance.getCost(candidate) - instance.getDistance(startNode, candidate)) :
                    -instance.getDistance(startNode, candidate);
            if (distScore > bestDist) {
                bestDist = distScore;
                secondNode = candidate;
            }
        }
        solution.addNodeLast(secondNode);
        unvisited.remove(secondNode);

        int targetSize = (int) Math.round(instance.getSize() / 2.0); // Chcemy 50% wierzchołków
        while (solution.getPath().size() < targetSize) {
            int bestNodeToInsert = -1;
            int bestInsertionIndex = -1;
            int maxScore = Integer.MIN_VALUE;
            int bestAssociatedBenefit = Integer.MIN_VALUE;

            for (int candidate : unvisited) {
                int b1 = Integer.MIN_VALUE;
                int b2 = Integer.MIN_VALUE;
                int bestLocalIndex = -1;

                for (int i = 0; i < solution.getPath().size(); i++) {
                    int deltaDist = solution.getInsertionDeltaDistance(candidate, i);
                    int benefit = useProfit ? (instance.getCost(candidate) - deltaDist) : -deltaDist;

                    if (benefit > b1) {
                        b2 = b1;
                        b1 = benefit;
                        bestLocalIndex = i;
                    } else if (benefit > b2) {
                        b2 = benefit;
                    }
                }

                int regret = b1 - b2;
                int score = regret;

                boolean isBetter = false;
                if (score > maxScore) {
                    isBetter = true;
                } else if (score == maxScore && b1 > bestAssociatedBenefit) {
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
    // LOCAL SEARCH (LAB 2)
    // =========================================================
    public Solution localSearch(Solution startSolution, boolean isSteepest, boolean useTwoOpt) {
        Solution currentSolution = new Solution(startSolution);

        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < instance.getSize(); i++) {
            if (!currentSolution.getPath().contains(i)) unvisited.add(i);
        }

        boolean improvement = true;
        java.util.Random random = new java.util.Random();

        while (improvement) {
            improvement = false;

            // Budowanie połączonego sąsiedztwa (0=Exchange, 1=Swap, 2=2-Opt)
            List<int[]> neighborhood = new ArrayList<>();

            for (int i = 0; i < currentSolution.getPath().size(); i++) {
                for (int u : unvisited) {
                    neighborhood.add(new int[]{0, i, u});
                }
            }

            int internalMoveType = useTwoOpt ? 2 : 1;
            for (int i = 0; i < currentSolution.getPath().size() - 1; i++) {
                for (int j = i + 1; j < currentSolution.getPath().size(); j++) {
                    neighborhood.add(new int[]{internalMoveType, i, j});
                }
            }

            // Randomizacja dla wersji zachłannej (Greedy)
            if (!isSteepest) {
                java.util.Collections.shuffle(neighborhood, random);
            }

            int[] bestMove = null;
            int bestDelta = 0;

            for (int[] move : neighborhood) {
                int type = move[0];
                int i = move[1];
                int j = move[2];
                int currentDelta = 0;

                if (type == 0) currentDelta = currentSolution.getExchangeDelta(i, j);
                else if (type == 1) currentDelta = currentSolution.getSwapDelta(i, j);
                else if (type == 2) currentDelta = currentSolution.getTwoOptDelta(i, j);

                if (currentDelta > bestDelta) {
                    bestMove = move;
                    bestDelta = currentDelta;
                    if (!isSteepest) break;
                }
            }

            // Aplikacja ruchu
            if (bestMove != null) {
                int type = bestMove[0];
                int i = bestMove[1];
                int j = bestMove[2];

                if (type == 0) {
                    int oldNode = currentSolution.getPath().get(i);
                    currentSolution.applyExchange(i, j);
                    unvisited.remove(Integer.valueOf(j));
                    unvisited.add(oldNode);
                } else if (type == 1) {
                    currentSolution.applySwap(i, j);
                } else if (type == 2) {
                    currentSolution.applyTwoOpt(i, j);
                }
                improvement = true;
            }
        }
        return currentSolution;
    }

    // =========================================================
    // RANDOM WALK (Baseline dla Lab 2)
    // =========================================================
    public Solution randomWalk(Solution startSolution, long maxTimeNanos, boolean useTwoOpt) {
        Solution currentSolution = new Solution(startSolution);
        Solution bestSolution = new Solution(startSolution);
        int bestObjective = bestSolution.getObjectiveValue();

        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < instance.getSize(); i++) {
            if (!currentSolution.getPath().contains(i)) unvisited.add(i);
        }

        java.util.Random random = new java.util.Random();
        long startTime = System.nanoTime();

        while (System.nanoTime() - startTime < maxTimeNanos) {
            boolean doExchange = random.nextBoolean();

            if (doExchange) {
                int i = random.nextInt(currentSolution.getPath().size());
                int u = unvisited.get(random.nextInt(unvisited.size()));
                int oldNode = currentSolution.getPath().get(i);
                currentSolution.applyExchange(i, u);
                unvisited.remove(Integer.valueOf(u));
                unvisited.add(oldNode);
            } else {
                int i = random.nextInt(currentSolution.getPath().size() - 1);
                int j = i + 1 + random.nextInt(currentSolution.getPath().size() - i - 1);

                if (useTwoOpt) currentSolution.applyTwoOpt(i, j);
                else currentSolution.applySwap(i, j);
            }

            if (currentSolution.getObjectiveValue() > bestObjective) {
                bestObjective = currentSolution.getObjectiveValue();
                bestSolution = new Solution(currentSolution);
            }
        }
        return bestSolution;
    }
}