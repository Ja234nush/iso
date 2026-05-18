package lab5.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Solver {
    private final Instance instance;
    private final Random random = new Random();

    // Pole do śledzenia liczby iteracji ostatnio uruchomionego algorytmu
    private int latestIterations = 0;

    public Solver(Instance instance) {
        this.instance = instance;
    }

    public int getLatestIterations() {
        return latestIterations;
    }

    // =========================================================================
    // METODY BAZOWE
    // =========================================================================
    public Solution randomSolution() {
        Solution solution = new Solution(instance);
        int totalNodes = instance.getSize();
        int numNodesToSelect = (int) Math.ceil(totalNodes / 2.0);
        List<Integer> availableNodes = new ArrayList<>();
        for (int i = 0; i < totalNodes; i++) availableNodes.add(i);
        Collections.shuffle(availableNodes, random);
        for (int i = 0; i < numNodesToSelect; i++) solution.addNodeLast(availableNodes.get(i));
        return solution;
    }

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

    public Solution solveMSLS(long timeLimitMs) {
        long startTime = System.currentTimeMillis();
        Solution bestGlobal = null;
        int iterations = 0;

        while ((System.currentTimeMillis() - startTime) < timeLimitMs) {
            Solution randomStart = randomSolution();
            Solution localOptimum = localSearchCandidateMoves(randomStart);
            iterations++;

            if (bestGlobal == null || localOptimum.getObjectiveValue() > bestGlobal.getObjectiveValue()) {
                bestGlobal = localOptimum;
            }
        }
        this.latestIterations = iterations;
        return bestGlobal;
    }

    // =========================================================================
    // METODY ILS (Iterated Local Search)
    // =========================================================================
    public Solution solveILS(long timeLimitMs) {
        long startTime = System.currentTimeMillis();
        Solution x = randomSolution();
        x = localSearchCandidateMoves(x);
        int iterations = 0;

        while ((System.currentTimeMillis() - startTime) < timeLimitMs) {
            Solution y = new Solution(x);
            perturbILS(y);
            y = localSearchCandidateMoves(y);
            iterations++;
            if (y.getObjectiveValue() > x.getObjectiveValue()) {
                x = y;
            }
        }
        this.latestIterations = iterations;
        return x;
    }

    private void perturbILS(Solution s) {
        List<Integer> path = s.getPath();
        if (path.size() < 4) return;
        for (int i = 0; i < 10; i++) {
            int idx1 = random.nextInt(path.size());
            int idx2 = random.nextInt(path.size());
            Collections.swap(path, idx1, idx2);
        }
        s.recalculateObjective();
    }

    // =========================================================================
    // METODY LNS (Large Neighborhood Search)
    // =========================================================================
    public Solution solveLNS(long timeLimitMs) {
        long startTime = System.currentTimeMillis();
        Solution x = randomSolution();
        x = localSearchCandidateMoves(x);
        int iterations = 0;

        while ((System.currentTimeMillis() - startTime) < timeLimitMs) {
            Solution y = new Solution(x);
            destroyLNS(y, 0.3, iterations++);
            repairLNS(y);
            if (y.getObjectiveValue() > x.getObjectiveValue()) {
                x = y;
            }
        }
        this.latestIterations = iterations;
        return x;
    }

    private void destroyLNS(Solution s, double percentage, int iterationCount) {
        int toRemove = (int) (s.getPath().size() * percentage);
        boolean removeWorst = (iterationCount % 5 == 0);

        for (int k = 0; k < toRemove; k++) {
            if (s.getPath().size() <= 3) break;
            int indexToRemove = -1;

            if (removeWorst) {
                int bestDelta = Integer.MIN_VALUE;
                for (int i = 0; i < s.getPath().size(); i++) {
                    int delta = s.getRemoveDelta(i);
                    if (delta > bestDelta) {
                        bestDelta = delta;
                        indexToRemove = i;
                    }
                }
            } else {
                indexToRemove = random.nextInt(s.getPath().size());
            }

            if (indexToRemove != -1) {
                s.applyRemove(indexToRemove);
            }
        }
    }

    public void repairLNS(Solution solution) {
        int n = instance.getSize();
        List<Integer> unvisited = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!solution.getPath().contains(i)) unvisited.add(i);
        }

        // FAZA 1: Dopełnianie trasy (dopóki są opłacalne miasta)
        boolean improvement = true;
        while (improvement && !unvisited.isEmpty()) {
            improvement = false;
            int bestNodeToInsert = -1, bestInsertionIndex = -1;
            double bestScore = Double.NEGATIVE_INFINITY;
            double bestActualBenefit = Double.NEGATIVE_INFINITY;

            for (int candidate : unvisited) {
                double b1 = Double.NEGATIVE_INFINITY;
                double b2 = Double.NEGATIVE_INFINITY;
                int bestLocalIndex = -1;

                for (int i = 0; i < solution.getPath().size(); i++) {
                    int deltaDist = solution.getInsertionDeltaDistance(candidate, i);
                    // Zysk to nagroda minus koszt objazdu
                    double benefit = instance.getCost(candidate) - deltaDist;

                    if (benefit > b1) {
                        b2 = b1;
                        b1 = benefit;
                        bestLocalIndex = i;
                    } else if (benefit > b2) {
                        b2 = benefit;
                    }
                }

                if (b2 == Double.NEGATIVE_INFINITY) b2 = b1;

                double regret = b1 - b2;
                double score = 0.5 * regret + 0.5 * b1;

                if (score > bestScore) {
                    bestScore = score;
                    bestNodeToInsert = candidate;
                    bestInsertionIndex = bestLocalIndex;
                    bestActualBenefit = b1; // Zapisujemy czysty zysk z tego węzła
                }
            }

            // DODAJEMY TYLKO JEŚLI ZYSK JEST DODATNI! (Opłaca się)
            if (bestNodeToInsert != -1 && bestActualBenefit > 0) {
                solution.insertNode(bestNodeToInsert, bestInsertionIndex);
                unvisited.remove(Integer.valueOf(bestNodeToInsert));
                improvement = true;
            }
        }

        // FAZA 2: Usuwanie nieopłacalnych wierzchołków
        // (jeśli po rekombinacji odziedziczyliśmy śmieciowe węzły, które przynoszą straty)
        improvement = true;
        while (improvement && solution.getPath().size() > 3) { // Min. 3 węzły dla TSP
            improvement = false;
            int worstIdx = -1;
            int bestDelta = 0; // Musi dawać oszczędność > 0 (Dystans zaoszczędzony > Utracony profit)

            for (int i = 0; i < solution.getPath().size(); i++) {
                int delta = solution.getRemoveDelta(i);
                if (delta > bestDelta) {
                    bestDelta = delta;
                    worstIdx = i;
                }
            }

            if (worstIdx != -1) {
                solution.applyRemove(worstIdx);
                improvement = true; // Usunęliśmy węzeł z sukcesem, sprawdzamy dalej
            }
        }
    }

    // =========================================================================
    // METODY DLA ZADANIA 6 (HAE)
    // =========================================================================
    public Solution solveHAE(long timeLimitMs, int operatorType, boolean useLocalSearchAfterRecombination) {
        long startTime = System.currentTimeMillis();
        int popSize = 20;
        int iterations = 0;

        List<Solution> population = new ArrayList<>();
        while (population.size() < popSize) {
            Solution s = randomSolution();
            s = localSearchCandidateMoves(s);

            boolean isUnique = true;
            for (Solution existing : population) {
                if (existing.getObjectiveValue() == s.getObjectiveValue()) {
                    isUnique = false; break;
                }
            }
            if (isUnique) population.add(s);
        }

        while ((System.currentTimeMillis() - startTime) < timeLimitMs) {
            int p1Idx = random.nextInt(popSize);
            int p2Idx = random.nextInt(popSize);
            while (p1Idx == p2Idx) p2Idx = random.nextInt(popSize);

            Solution p1 = population.get(p1Idx);
            Solution p2 = population.get(p2Idx);

            Solution offspring = null;
            if (operatorType == 1) offspring = crossoverOp1(p1, p2);
            else if (operatorType == 2) offspring = crossoverOp2(p1, p2);
            else if (operatorType == 3) offspring = crossoverOp3(p1, p2);

            iterations++;

            if (useLocalSearchAfterRecombination && offspring != null) {
                offspring = localSearchCandidateMoves(offspring);
            }

            if (offspring != null) {
                int worstIdx = 0;
                for (int i = 1; i < popSize; i++) {
                    if (population.get(i).getObjectiveValue() < population.get(worstIdx).getObjectiveValue()) {
                        worstIdx = i;
                    }
                }

                if (offspring.getObjectiveValue() > population.get(worstIdx).getObjectiveValue()) {
                    boolean isUnique = true;
                    for (Solution existing : population) {
                        if (existing.getObjectiveValue() == offspring.getObjectiveValue()) {
                            isUnique = false; break;
                        }
                    }
                    if (isUnique) population.set(worstIdx, offspring);
                }
            }
        }

        Solution best = population.get(0);
        for (Solution s : population) {
            if (s.getObjectiveValue() > best.getObjectiveValue()) best = s;
        }
        this.latestIterations = iterations;
        return best;
    }

    // =========================================================================
    // OPERATOR 1: Części wspólne + losowe łączenie
    // =========================================================================
    private Solution crossoverOp1(Solution p1, Solution p2) {
        Solution child = new Solution(instance);
        List<Integer> path1 = p1.getPath();
        List<Integer> path2 = p2.getPath();

        List<Integer> currentSubpath = new ArrayList<>();
        List<List<Integer>> subpaths = new ArrayList<>();

        for (int i = 0; i < path1.size(); i++) {
            int curr = path1.get(i);
            int next = path1.get((i + 1) % path1.size());

            if (path2.contains(curr)) {
                currentSubpath.add(curr);
                if (!hasEdge(p2, curr, next)) {
                    subpaths.add(new ArrayList<>(currentSubpath));
                    currentSubpath.clear();
                }
            }
        }
        if (!currentSubpath.isEmpty()) subpaths.add(currentSubpath);

        Collections.shuffle(subpaths, random);
        for (List<Integer> subpath : subpaths) {
            if (random.nextBoolean()) Collections.reverse(subpath); // Losowe odwracanie wg instrukcji
            for (int n : subpath) child.addNodeLast(n);
        }

        repairRecombinedSolution(child);
        return child;
    }

    // =========================================================================
    // OPERATOR 2: Redukcja (Bypass) -> Usuwanie krawędzi -> Losowe łączenie
    // =========================================================================
    private Solution crossoverOp2(Solution p1, Solution p2) {
        Solution child = new Solution(instance);
        List<Integer> path1 = p1.getPath();
        List<Integer> path2 = p2.getPath();

        // Krok 1: Usunięcie wierzchołków i automatyczne dodanie krawędzi (Bypass)
        List<Integer> reducedP1 = new ArrayList<>();
        for (int node : path1) {
            if (path2.contains(node)) {
                reducedP1.add(node);
            }
        }

        // Krok 2: Usuwanie niepasujących krawędzi z już zredukowanego cyklu
        List<Integer> currentSubpath = new ArrayList<>();
        List<List<Integer>> subpaths = new ArrayList<>();

        if (!reducedP1.isEmpty()) {
            for (int i = 0; i < reducedP1.size(); i++) {
                int curr = reducedP1.get(i);
                int next = reducedP1.get((i + 1) % reducedP1.size());

                currentSubpath.add(curr);
                if (!hasEdge(p2, curr, next)) {
                    subpaths.add(new ArrayList<>(currentSubpath));
                    currentSubpath.clear();
                }
            }
            if (!currentSubpath.isEmpty()) subpaths.add(currentSubpath);
        }

        // Krok 3: Rozłączne podścieżki łączymy "losowo jak w operatorze 1"
        Collections.shuffle(subpaths, random);
        for (List<Integer> subpath : subpaths) {
            if (random.nextBoolean()) Collections.reverse(subpath);
            for (int n : subpath) child.addNodeLast(n);
        }

        repairRecombinedSolution(child);
        return child;
    }

    // =========================================================================
    // OPERATOR 3: Tylko zachowanie wspólnych wierzchołków
    // =========================================================================
    private Solution crossoverOp3(Solution p1, Solution p2) {
        Solution child = new Solution(instance);
        for (int node : p1.getPath()) {
            if (p2.getPath().contains(node)) {
                child.addNodeLast(node); // Naturalny kierunek P1 zszywa usunięte luki
            }
        }
        repairRecombinedSolution(child);
        return child;
    }

    private boolean hasEdge(Solution s, int u, int v) {
        List<Integer> p = s.getPath();
        for(int i = 0; i < p.size(); i++){
            int c = p.get(i);
            int n = p.get((i + 1) % p.size());
            if ((c == u && n == v) || (c == v && n == u)) return true;
        }
        return false;
    }

    private void repairRecombinedSolution(Solution s) {
        int targetSize = (int) Math.ceil(instance.getSize() / 2.0);
        repairLNS(s);

        while (s.getPath().size() > targetSize) {
            int worstIdx = -1;
            int bestDelta = Integer.MIN_VALUE;
            for (int i = 0; i < s.getPath().size(); i++) {
                int delta = s.getRemoveDelta(i);
                if (delta > bestDelta) { bestDelta = delta; worstIdx = i; }
            }
            if (worstIdx != -1) s.applyRemove(worstIdx);
            else break;
        }
    }

    // =========================================================================
    // METODY PODSTAWOWE DO PORÓWNAŃ BAZOWYCH (WYMAGANE W INSTRUKCJI LAB 6)
    // =========================================================================

    // Wyniki czystej metody zachłannej (tej samej, co w LNS i HAE)
    public Solution solveGreedyHeuristic() {
        Solution s = new Solution(instance);
        // Zaczynamy od losowego wierzchołka, aby rozbudować ścieżkę
        s.addNodeLast(random.nextInt(instance.getSize()));
        repairLNS(s);
        this.latestIterations = 1; // Wykonuje się dokładnie raz
        return s;
    }

    // Wyniki bazowego lokalnego przeszukiwania (wylosuj raz i popraw)
    public Solution solveBaseLocalSearch() {
        Solution randomStart = randomSolution();
        Solution result = localSearchCandidateMoves(randomStart);
        this.latestIterations = 1; // Jedno wywołanie algorytmu poprawy
        return result;
    }
}