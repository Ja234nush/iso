package lab4.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Solver {
    private final Instance instance;
    private final Random random = new Random();

    // Liczniki do analizy eksperymentu
    public int ilsPerturbationsCount = 0;
    public int lnsPerturbationsCount = 0;

    public Solver(Instance instance) {
        this.instance = instance;
    }

    // =========================================================================
    // METODY POMOCNICZE (z Lab 2 i 3)
    // =========================================================================
    public Solution randomSolution() {
        Solution solution = new Solution(instance);
        int totalNodes = instance.getSize();
        int numNodesToSelect = (int)Math.ceil(totalNodes / 2.0);
        List<Integer> availableNodes = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) availableNodes.add(i);
        java.util.Collections.shuffle(availableNodes, random);
        for (int i = 0; i < numNodesToSelect; i++) solution.addNodeLast(availableNodes.get(i));
        return solution;
    }

    public Solution regretCycle(int startNode) {
        Solution solution = new Solution(instance);
        int n = instance.getSize();
        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < n; i++) if (i != startNode) unvisited.add(i);

        solution.addNodeLast(startNode);
        int secondNode = -1, bestDist = Integer.MIN_VALUE;
        for (int candidate : unvisited) {
            int distScore = instance.getCost(candidate) - instance.getDistance(startNode, candidate);
            if (distScore > bestDist) { bestDist = distScore; secondNode = candidate; }
        }
        solution.addNodeLast(secondNode);
        unvisited.remove(Integer.valueOf(secondNode));

        int targetSize = (int)Math.ceil(instance.getSize() / 2.0);
        while (solution.getPath().size() < targetSize && !unvisited.isEmpty()) {
            int bestNodeToInsert = -1, bestInsertionIndex = -1, maxScore = Integer.MIN_VALUE;
            for (int candidate : unvisited) {
                int b1 = Integer.MIN_VALUE, b2 = Integer.MIN_VALUE, bestLocalIndex = -1;
                for (int i = 0; i < solution.getPath().size(); i++) {
                    int deltaDist = solution.getInsertionDeltaDistance(candidate, i);
                    int benefit = instance.getCost(candidate) - deltaDist;
                    if (benefit > b1) { b2 = b1; b1 = benefit; bestLocalIndex = i; }
                    else if (benefit > b2) { b2 = benefit; }
                }
                int score = b1 - b2;
                if (score > maxScore) { maxScore = score; bestNodeToInsert = candidate; bestInsertionIndex = bestLocalIndex; }
            }
            solution.insertNode(bestNodeToInsert, bestInsertionIndex);
            unvisited.remove(Integer.valueOf(bestNodeToInsert));
        }
        return solution;
    }

    // =========================================================================
    // BAZOWY LOCAL SEARCH (Steepest, referencyjny z Lab 2)
    // =========================================================================
    public Solution localSearchBaseSteepest(Solution startSolution) {
        Solution currentSolution = new Solution(startSolution);
        boolean improvement = true;

        while (improvement) {
            improvement = false;
            int bestType = -1, bestI = -1, bestJ = -1, bestDelta = 0;
            List<Integer> path = currentSolution.getPath();

            for (int i = 0; i < path.size() - 1; i++) {
                for (int j = i + 1; j < path.size(); j++) {
                    int delta = currentSolution.getTwoOptDelta(i, j);
                    if (delta > bestDelta) { bestDelta = delta; bestType = 2; bestI = i; bestJ = j; }
                }
            }

            for (int u = 0; u < instance.getSize(); u++) {
                if (!path.contains(u)) {
                    for (int i = 0; i < path.size(); i++) {
                        int delta = currentSolution.getAddDelta(u, i);
                        if (delta > bestDelta) { bestDelta = delta; bestType = 3; bestI = u; bestJ = i; }
                    }
                }
            }

            if (path.size() > 3) {
                for (int i = 0; i < path.size(); i++) {
                    int delta = currentSolution.getRemoveDelta(i);
                    if (delta > bestDelta) { bestDelta = delta; bestType = 4; bestI = i; }
                }
            }

            if (bestDelta > 0) {
                if (bestType == 2) currentSolution.applyTwoOpt(bestI, bestJ);
                else if (bestType == 3) currentSolution.applyAdd(bestI, bestJ);
                else if (bestType == 4) currentSolution.applyRemove(bestI);
                improvement = true;
            }
        }
        return currentSolution;
    }

    // =========================================================================
    // LOCAL SEARCH - RUCHY KANDYDACKIE
    // =========================================================================
    public Solution localSearchCandidateMoves(Solution startSolution) {
        Solution currentSolution = new Solution(startSolution);
        boolean improvement = true;

        while (improvement) {
            improvement = false;
            int bestType = -1, bestI = -1, bestJ = -1, bestDelta = 0;
            List<Integer> path = currentSolution.getPath();

            int[] pos = new int[instance.getSize()];
            java.util.Arrays.fill(pos, -1);
            for (int i = 0; i < path.size(); i++) {
                pos[path.get(i)] = i;
            }

            for (int i = 0; i < path.size(); i++) {
                int nodeI = path.get(i);
                int prevI = path.get((i - 1 + path.size()) % path.size());

                for (int neighborJ : instance.getNearestNeighbors(prevI)) {
                    int j = pos[neighborJ];
                    if (j != -1) {
                        int minIdx = Math.min(i, j);
                        int maxIdx = Math.max(i, j);
                        int delta = currentSolution.getTwoOptDelta(minIdx, maxIdx);
                        if (delta > bestDelta) { bestDelta = delta; bestType = 2; bestI = minIdx; bestJ = maxIdx; }
                    }
                }

                for (int neighborNextJ : instance.getNearestNeighbors(nodeI)) {
                    int nextJPos = pos[neighborNextJ];
                    if (nextJPos != -1) {
                        int j = (nextJPos - 1 + path.size()) % path.size();
                        int minIdx = Math.min(i, j);
                        int maxIdx = Math.max(i, j);
                        int delta = currentSolution.getTwoOptDelta(minIdx, maxIdx);
                        if (delta > bestDelta) { bestDelta = delta; bestType = 2; bestI = minIdx; bestJ = maxIdx; }
                    }
                }
            }

            for (int u = 0; u < instance.getSize(); u++) {
                if (pos[u] == -1) {
                    for (int neighbor : instance.getNearestNeighbors(u)) {
                        int neighborPos = pos[neighbor];
                        if (neighborPos != -1) {
                            int delta1 = currentSolution.getAddDelta(u, neighborPos);
                            if (delta1 > bestDelta) { bestDelta = delta1; bestType = 3; bestI = u; bestJ = neighborPos; }

                            int insertAfter = neighborPos + 1;
                            int delta2 = currentSolution.getAddDelta(u, insertAfter);
                            if (delta2 > bestDelta) { bestDelta = delta2; bestType = 3; bestI = u; bestJ = insertAfter; }
                        }
                    }
                }
            }

            if (path.size() > 3) {
                for (int i = 0; i < path.size(); i++) {
                    int delta = currentSolution.getRemoveDelta(i);
                    if (delta > bestDelta) { bestDelta = delta; bestType = 4; bestI = i; }
                }
            }

            if (bestDelta > 0) {
                if (bestType == 2) currentSolution.applyTwoOpt(bestI, bestJ);
                else if (bestType == 3) currentSolution.applyAdd(bestI, bestJ);
                else if (bestType == 4) currentSolution.applyRemove(bestI);
                improvement = true;
            }
        }
        return currentSolution;
    }

    // =========================================================================
    // LOCAL SEARCH - LISTA RUCHÓW (LM)
    // =========================================================================
    public Solution localSearchWithLM(Solution startSolution) {
        Solution currentSolution = new Solution(startSolution);
        List<Move> LM = new ArrayList<>();

        evaluateAllMoves(currentSolution, LM);

        while (!LM.isEmpty()) {
            boolean moveFound = false;
            Iterator<Move> iterator = LM.iterator();
            while (iterator.hasNext()) {
                Move m = iterator.next();
                int currentDelta = checkApplicabilityDelta(currentSolution, m);

                if (currentDelta <= 0) {
                    iterator.remove();
                } else {
                    applyMove(currentSolution, m);
                    moveFound = true;
                    break;
                }
            }

            if (!moveFound) {
                LM.clear();
                evaluateAllMoves(currentSolution, LM);
            }
        }
        return currentSolution;
    }

    private void evaluateAllMoves(Solution currentSolution, List<Move> LM) {
        List<Integer> path = currentSolution.getPath();

        for (int i = 0; i < path.size() - 1; i++) {
            for (int j = i + 1; j < path.size(); j++) {
                int delta = currentSolution.getTwoOptDelta(i, j);
                if (delta > 0) LM.add(new Move(2, path.get(i), path.get(j), delta));
            }
        }

        for (int u = 0; u < instance.getSize(); u++) {
            if (!path.contains(u)) {
                for (int i = 0; i < path.size(); i++) {
                    int delta = currentSolution.getAddDelta(u, i);
                    if (delta > 0) LM.add(new Move(3, u, path.get(i), delta));
                }
            }
        }

        if (path.size() > 3) {
            for (int i = 0; i < path.size(); i++) {
                int delta = currentSolution.getRemoveDelta(i);
                if (delta > 0) LM.add(new Move(4, path.get(i), -1, delta));
            }
        }

        Collections.sort(LM);
    }

    private int checkApplicabilityDelta(Solution s, Move m) {
        List<Integer> path = s.getPath();
        if (m.type == 2) {
            int i = path.indexOf(m.node1);
            int j = path.indexOf(m.node2);
            if (i != -1 && j != -1) {
                if (i > j) { int temp = i; i = j; j = temp; }
                return s.getTwoOptDelta(i, j);
            }
        } else if (m.type == 3) {
            int u = m.node1;
            int insertIdx = path.indexOf(m.node2);
            if (!path.contains(u) && insertIdx != -1) return s.getAddDelta(u, insertIdx);
        } else if (m.type == 4) {
            int routeIdx = path.indexOf(m.node1);
            if (routeIdx != -1 && path.size() > 3) return s.getRemoveDelta(routeIdx);
        }
        return -1;
    }

    private void applyMove(Solution s, Move m) {
        List<Integer> path = s.getPath();
        if (m.type == 2) {
            int i = path.indexOf(m.node1);
            int j = path.indexOf(m.node2);
            if (i > j) { int temp = i; i = j; j = temp; }
            s.applyTwoOpt(i, j);
        } else if (m.type == 3) {
            int insertIdx = path.indexOf(m.node2);
            s.applyAdd(m.node1, insertIdx);
        } else if (m.type == 4) {
            int routeIdx = path.indexOf(m.node1);
            s.applyRemove(routeIdx);
        }
    }

    // =========================================================================
    // ZADANIE 4: METAHEURYSTYKI
    // =========================================================================

    public Solution solveMSLS() {
        Solution bestOverall = null;
        int iterations = 200;

        for (int i = 0; i < iterations; i++) {
            Solution x = randomSolution();
            Solution localOptimum = localSearchCandidateMoves(x);

            if (bestOverall == null || localOptimum.getObjectiveValue() > bestOverall.getObjectiveValue()) {
                bestOverall = localOptimum;
            }
        }
        return bestOverall;
    }

    public Solution solveILS(long timeLimitMs) {
        long startTime = System.currentTimeMillis();
        ilsPerturbationsCount = 0;

        Solution x = randomSolution();
        x = localSearchCandidateMoves(x);

        while ((System.currentTimeMillis() - startTime) < timeLimitMs) {
            Solution y = new Solution(x);

            // SILNIEJSZA PERTURBACJA: Zmieniono z 3 wymian na 10
            perturbILS(y);
            ilsPerturbationsCount++;

            y = localSearchCandidateMoves(y);

            if (y.getObjectiveValue() > x.getObjectiveValue()) {
                x = y;
            }
        }
        return x;
    }

    private void perturbILS(Solution s) {
        List<Integer> path = s.getPath();
        if (path.size() < 4) return;

        // Zmieniono na 10 uderzeń szumu (swapów)
        for (int i = 0; i < 10; i++) {
            int idx1 = random.nextInt(path.size());
            int idx2 = random.nextInt(path.size());
            Collections.swap(path, idx1, idx2);
        }
        s.recalculateObjective();
    }

    public Solution solveLNS(long timeLimitMs) {
        long startTime = System.currentTimeMillis();
        lnsPerturbationsCount = 0;

        Solution x = randomSolution();
        x = localSearchCandidateMoves(x);

        while ((System.currentTimeMillis() - startTime) < timeLimitMs) {
            Solution y = new Solution(x);

            // RÓŻNORODNOŚĆ W DESTROY:
            // Przekazujemy lnsPerturbationsCount, aby sterować logiką (heurystyka vs losowość)
            destroyLNS(y, 0.3, lnsPerturbationsCount);

            repairLNS(y);
            lnsPerturbationsCount++;



            if (y.getObjectiveValue() > x.getObjectiveValue()) {
                x = y;
            }
        }
        return x;
    }

    private void destroyLNS(Solution s, double percentage, int iterationCount) {
        List<Integer> path = s.getPath();
        int toRemove = (int) (path.size() * percentage);

        // Raz na 5 wywołań (gdy reszta z dzielenia przez 5 równa się 0) używamy metody opartej na usuwaniu najgorszych.
        // W pozostałych 80% przypadków usuwamy wierzchołki w sposób całkowicie losowy.
        boolean removeWorst = (iterationCount % 5 == 0);

        for (int k = 0; k < toRemove; k++) {
            if (path.size() < 3) break;

            int indexToRemove = -1;

            if (removeWorst) {
                // Strategia deterministyczna: Usuń najgorszy wierzchołek
                double worstScore = Double.MAX_VALUE;
                for (int i = 0; i < path.size(); i++) {
                    int curr = path.get(i);
                    int prev = path.get((i - 1 + path.size()) % path.size());
                    int next = path.get((i + 1) % path.size());

                    int currentObjective = instance.getCost(curr) - (instance.getDistance(prev, curr) + instance.getDistance(curr, next));

                    if (currentObjective < worstScore) {
                        worstScore = currentObjective;
                        indexToRemove = i;
                    }
                }
            } else {
                // Strategia losowa: Zwiększa eksplorację (Exploration) zapobiegając stagnacji
                indexToRemove = random.nextInt(path.size());
            }

            s.applyRemove(indexToRemove);
        }
    }

    private void repairLNS(Solution solution) {
        int n = instance.getSize();
        int targetSize = (int)Math.ceil(n / 2.0);

        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!solution.getPath().contains(i)) {
                unvisited.add(i);
            }
        }

        while (solution.getPath().size() < targetSize && !unvisited.isEmpty()) {
            int bestNodeToInsert = -1, bestInsertionIndex = -1, maxScore = Integer.MIN_VALUE;

            for (int candidate : unvisited) {
                int b1 = Integer.MIN_VALUE, b2 = Integer.MIN_VALUE, bestLocalIndex = -1;
                for (int i = 0; i < solution.getPath().size(); i++) {
                    int deltaDist = solution.getInsertionDeltaDistance(candidate, i);
                    int benefit = instance.getCost(candidate) - deltaDist;

                    if (benefit > b1) {
                        b2 = b1;
                        b1 = benefit;
                        bestLocalIndex = i;
                    } else if (benefit > b2) {
                        b2 = benefit;
                    }
                }
                int score = b1 - b2;
                if (score > maxScore) {
                    maxScore = score;
                    bestNodeToInsert = candidate;
                    bestInsertionIndex = bestLocalIndex;
                }
            }
            solution.insertNode(bestNodeToInsert, bestInsertionIndex);
            unvisited.remove(Integer.valueOf(bestNodeToInsert));
        }
    }
}