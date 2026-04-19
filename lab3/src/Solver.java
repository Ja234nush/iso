package lab3.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Solver {
    private final Instance instance;

    public Solver(Instance instance) {
        this.instance = instance;
    }

    // =========================================================================
    // METODY POMOCNICZE (z Lab 2)
    // =========================================================================
    public Solution randomSolution() {
        Solution solution = new Solution(instance);
        int totalNodes = instance.getSize();
        java.util.Random random = new java.util.Random();
        int numNodesToSelect = random.nextInt(totalNodes - 2) + 3;
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

        while (!unvisited.isEmpty()) {
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
    // 0. BAZOWY LOCAL SEARCH (Steepest, referencyjny z Lab 2)
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
    // 1. LOCAL SEARCH - RUCHY KANDYDACKIE (PRAWDZIWA OPTYMALIZACJA)
    // =========================================================================
    public Solution localSearchCandidateMoves(Solution startSolution) {
        Solution currentSolution = new Solution(startSolution);
        boolean improvement = true;

        while (improvement) {
            improvement = false;
            int bestType = -1, bestI = -1, bestJ = -1, bestDelta = 0;
            List<Integer> path = currentSolution.getPath();

            // SZYBKA TABLICA LOKALIZACJI: pos[id_wierzcholka] = indeks_na_trasie
            // Pozwala sprawdzić w czasie O(1), gdzie leży kandydat
            int[] pos = new int[instance.getSize()];
            java.util.Arrays.fill(pos, -1);
            for (int i = 0; i < path.size(); i++) {
                pos[path.get(i)] = i;
            }

            // -------------------------------------------------------------
            // RUCH 2-OPT (Kandydackie)
            // Ruch 2-opt usuwa krawędzie (i-1, i) oraz (j, j+1)
            // a wstawia nowe krawędzie: (i-1, j) oraz (i, j+1).
            // -------------------------------------------------------------
            for (int i = 0; i < path.size(); i++) {
                int nodeI = path.get(i);
                int prevI = path.get((i - 1 + path.size()) % path.size());

                // Chcemy, aby NOWA krawędź (prevI, j) była kandydacka
                // Więc sprawdzamy tylko j, które są sąsiadami prevI
                for (int neighborJ : instance.getNearestNeighbors(prevI)) {
                    int j = pos[neighborJ];
                    if (j != -1) { // Sąsiad jest na trasie
                        int minIdx = Math.min(i, j);
                        int maxIdx = Math.max(i, j);
                        int delta = currentSolution.getTwoOptDelta(minIdx, maxIdx);
                        if (delta > bestDelta) { bestDelta = delta; bestType = 2; bestI = minIdx; bestJ = maxIdx; }
                    }
                }

                // Chcemy też sprawdzić przypadek, gdzie (nodeI, j+1) jest kandydacka
                for (int neighborNextJ : instance.getNearestNeighbors(nodeI)) {
                    int nextJPos = pos[neighborNextJ];
                    if (nextJPos != -1) {
                        // Skoro neighborNextJ to indeks (j+1), to j jest o jedno miejsce wcześniej
                        int j = (nextJPos - 1 + path.size()) % path.size();
                        int minIdx = Math.min(i, j);
                        int maxIdx = Math.max(i, j);
                        int delta = currentSolution.getTwoOptDelta(minIdx, maxIdx);
                        if (delta > bestDelta) { bestDelta = delta; bestType = 2; bestI = minIdx; bestJ = maxIdx; }
                    }
                }
            }

            // -------------------------------------------------------------
            // RUCH ADD (Dodawanie wierzchołków - Kandydackie)
            // Dodajemy węzeł `u` obok jego sąsiadów, którzy są na trasie
            // -------------------------------------------------------------
            for (int u = 0; u < instance.getSize(); u++) {
                if (pos[u] == -1) { // Jeżeli 'u' nie ma na trasie
                    // Sprawdzamy wstawienie go tylko w pobliżu jego 10 najbliższych sąsiadów
                    for (int neighbor : instance.getNearestNeighbors(u)) {
                        int neighborPos = pos[neighbor];
                        if (neighborPos != -1) {
                            // Opcja 1: Wstawienie bezpośrednio PRZED sąsiadem
                            int delta1 = currentSolution.getAddDelta(u, neighborPos);
                            if (delta1 > bestDelta) { bestDelta = delta1; bestType = 3; bestI = u; bestJ = neighborPos; }

                            // Opcja 2: Wstawienie bezpośrednio ZA sąsiadem
                            int insertAfter = neighborPos + 1;
                            int delta2 = currentSolution.getAddDelta(u, insertAfter);
                            if (delta2 > bestDelta) { bestDelta = delta2; bestType = 3; bestI = u; bestJ = insertAfter; }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // RUCH REMOVE (Wszystkie)
            // (Tutaj trasa się skraca, więc przeglądamy wszystkie możliwości)
            // -------------------------------------------------------------
            if (path.size() > 3) {
                for (int i = 0; i < path.size(); i++) {
                    int delta = currentSolution.getRemoveDelta(i);
                    if (delta > bestDelta) { bestDelta = delta; bestType = 4; bestI = i; }
                }
            }

            // ZASTOSOWANIE NAJLEPSZEGO RUCHU
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
    // 2. LOCAL SEARCH - LISTA RUCHÓW (LM) - METODA ALTERNATYWNA
    // =========================================================================
    public Solution localSearchWithLM(Solution startSolution) {
        Solution currentSolution = new Solution(startSolution);
        List<Move> LM = new ArrayList<>();

        // 1. Oceń wszystkie ruchy aplikowalne do x i dodaj ruchy przynoszące poprawę do LM
        evaluateAllMoves(currentSolution, LM);

        // 2. Powtarzaj dopóki LM nie jest pusta [cite: 81, 89]
        while (!LM.isEmpty()) {
            boolean moveFound = false;

            // Dla wszystkich ruchów m w LM zaczynając od najlepszego [cite: 82]
            Iterator<Move> iterator = LM.iterator();
            while (iterator.hasNext()) {
                Move m = iterator.next();

                // Sprawdź czy m jest aplikowalny (w naszym przypadku: czy nadal przynosi dodatnią deltę)
                int currentDelta = checkApplicabilityDelta(currentSolution, m);

                if (currentDelta <= 0) {
                    // Jeżeli nie, to usuń m z LM
                    iterator.remove();
                } else {
                    // Jeżeli ruch m został znaleziony, zaakceptuj go [cite: 84, 85]
                    applyMove(currentSolution, m);
                    moveFound = true;
                    // Przerywamy wewnętrzną pętlę, by znowu zacząć przeglądanie LM od góry (najlepszych)
                    break;
                }
            }

            // W przeciwnym wypadku (gdy przejrzymy całe LM i nic nie ma) [cite: 86]
            if (!moveFound) {
                // Oceń wszystkie ruchy aplikowalne do x i dodaj ruchy przynoszące poprawę do LM [cite: 87, 88]
                LM.clear();
                evaluateAllMoves(currentSolution, LM);
            }
        }
        return currentSolution;
    }

    // --- Metody pomocnicze dla LM ---

    private void evaluateAllMoves(Solution currentSolution, List<Move> LM) {
        List<Integer> path = currentSolution.getPath();

        // 2-Opt
        for (int i = 0; i < path.size() - 1; i++) {
            for (int j = i + 1; j < path.size(); j++) {
                int delta = currentSolution.getTwoOptDelta(i, j);
                if (delta > 0) LM.add(new Move(2, path.get(i), path.get(j), delta));
            }
        }

        // Add
        for (int u = 0; u < instance.getSize(); u++) {
            if (!path.contains(u)) {
                for (int i = 0; i < path.size(); i++) {
                    int delta = currentSolution.getAddDelta(u, i);
                    if (delta > 0) LM.add(new Move(3, u, path.get(i), delta));
                }
            }
        }

        // Remove
        if (path.size() > 3) {
            for (int i = 0; i < path.size(); i++) {
                int delta = currentSolution.getRemoveDelta(i);
                if (delta > 0) LM.add(new Move(4, path.get(i), -1, delta));
            }
        }

        // Zainicjuj LM uporządkowaną od najlepszego do najgorszego [cite: 79]
        Collections.sort(LM);
    }

    private int checkApplicabilityDelta(Solution s, Move m) {
        List<Integer> path = s.getPath();
        if (m.type == 2) {
            int i = path.indexOf(m.node1);
            int j = path.indexOf(m.node2);
            // Upewniamy się, że oba węzły są w trasie
            if (i != -1 && j != -1) {
                // Wymuszamy, by i < j (2-opt zależy od kierunku)
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
        return -1; // Nieaplikowalne (zwróci ujemną wartość by wyrzucić ruch z LM)
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
}