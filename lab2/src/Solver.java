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
    public Solution randomSolution() {
        Solution solution = new Solution(instance);
        int totalNodes = instance.getSize();
        java.util.Random random = new java.util.Random();

        // Skoro nie mamy limitu 50%, losujemy dowolny początkowy rozmiar trasy (minimum 3 wierzchołki)
        int numNodesToSelect = random.nextInt(totalNodes - 2) + 3;
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

        // Zgodnie z Twoim starym kodem, budujemy cykl z wszystkich węzłów, a Local Search go "odchudzi" usuwając najsłabsze
        int targetSize = instance.getSize();
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
                int score = regret; // Jeśli chcesz wagi, dodaj parametr

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
    // LOCAL SEARCH (LAB 2) - WERSJA DYNAMICZNA (ADD / REMOVE)
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

            // Budowanie połączonego sąsiedztwa
            // Kody: 1=Swap, 2=2-Opt, 3=Add, 4=Remove
            List<int[]> neighborhood = new ArrayList<>();

            // 1. Dodawanie wierzchołków (ADD)
            for (int u : unvisited) {
                for (int i = 0; i < currentSolution.getPath().size(); i++) {
                    neighborhood.add(new int[]{3, u, i}); // 3 = Dodaj węzeł u na pozycję i
                }
            }

            // 2. Usuwanie wierzchołków (REMOVE)
            // Zabezpieczenie: trasa musi mieć więcej niż 3 węzły, żeby usunięcie nie zepsuło geometrii cyklu
            if (currentSolution.getPath().size() > 3) {
                for (int i = 0; i < currentSolution.getPath().size(); i++) {
                    neighborhood.add(new int[]{4, i, -1}); // 4 = Usuń węzeł z indeksu i
                }
            }

            // 3. Ruchy wewnątrztrasowe (SWAP / 2-OPT)
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

                if (type == 1) currentDelta = currentSolution.getSwapDelta(i, j);
                else if (type == 2) currentDelta = currentSolution.getTwoOptDelta(i, j);
                else if (type == 3) currentDelta = currentSolution.getAddDelta(i, j); // i = newNode, j = insertIndex
                else if (type == 4) currentDelta = currentSolution.getRemoveDelta(i); // i = routeIndex

                // Znalazł się ruch na plus!
                if (currentDelta > bestDelta) {
                    bestMove = move;
                    bestDelta = currentDelta;
                    if (!isSteepest) break; // Greedy przerywa od razu
                }
            }

            // Aplikacja ruchu
            if (bestMove != null) {
                int type = bestMove[0];
                int i = bestMove[1];
                int j = bestMove[2];

                if (type == 1) {
                    currentSolution.applySwap(i, j);
                } else if (type == 2) {
                    currentSolution.applyTwoOpt(i, j);
                } else if (type == 3) {
                    currentSolution.applyAdd(i, j);
                    unvisited.remove(Integer.valueOf(i)); // Usuwamy z nieodwiedzonych, bo dodaliśmy do trasy
                } else if (type == 4) {
                    int removedNode = currentSolution.getPath().get(i);
                    currentSolution.applyRemove(i);
                    unvisited.add(removedNode); // Wyrzucony z trasy węzeł wraca do nieodwiedzonych
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
            int routeSize = currentSolution.getPath().size();

            // Losujemy, co robić: 0 = Wewnętrzny ruch, 1 = Dodaj, 2 = Usuń
            int action = random.nextInt(3);

            // Zabezpieczenia: nie usuwaj, jak mało węzłów; nie dodawaj, jak nie ma wolnych
            if (action == 2 && routeSize <= 3) action = 0;
            if (action == 1 && unvisited.isEmpty()) action = 0;

            if (action == 1) { // DODAJ
                int u = unvisited.get(random.nextInt(unvisited.size()));
                int insertIndex = random.nextInt(routeSize + 1); // Od 0 do końca
                currentSolution.applyAdd(u, insertIndex);
                unvisited.remove(Integer.valueOf(u));

            } else if (action == 2) { // USUŃ
                int indexToRemove = random.nextInt(routeSize);
                int removedNode = currentSolution.getPath().get(indexToRemove);
                currentSolution.applyRemove(indexToRemove);
                unvisited.add(removedNode);

            } else { // RUCHY WEWNĄTRZ (SWAP / 2-OPT)
                int i = random.nextInt(routeSize - 1);
                int j = i + 1 + random.nextInt(routeSize - i - 1);

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