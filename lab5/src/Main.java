package lab5.src;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String filepath = "TSPB.csv"; // Możesz zmienić na TSPB.csv w przyszłości

        try {
            Instance instance = new Instance(filepath);
            Solver solver = new Solver(instance);
            long timeLimitMs = 2000;

            System.out.println("=========================================================================");
            System.out.println("🚀 ROZPOCZYNAMY REALIZACJĘ ZADANIA 5: TESTY GLOBALNEJ WYPUKŁOŚCI");
            System.out.println("=========================================================================");

            System.out.println("⏳ 1. Wyznaczanie bardzo dobrego rozwiązania referencyjnego przy użyciu ILS...");
            Solution bestKnownSolution = solver.solveILS(5000);
            System.out.println("   [OK] Referencyjna funkcja celu: " + bestKnownSolution.getObjectiveValue());

            System.out.println("⏳ 2. Generowanie 1000 losowych optimów lokalnych...");
            List<Solution> localOptima = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                Solution startRandom = solver.randomSolution();
                Solution localOpt = solver.localSearchCandidateMoves(startRandom);
                localOptima.add(localOpt);
                if ((i + 1) % 200 == 0) System.out.print((i + 1) + "... ");
            }
            System.out.println("\n   [OK] Wygenerowano całą populację.");

            System.out.println("⏳ 3. Obliczanie miar podobieństwa i zapis danych do FDC...");
            File directory = new File("lab5/wyniki");
            if (!directory.exists()) directory.mkdirs();

            try (PrintWriter out = new PrintWriter("lab5/wyniki/wykresy_sciezek/wypuklosc_dane.csv")) {
                out.println("cost;sim_best_nodes;sim_best_edges;sim_nodes_avg;sim_edges_avg");

                for (int i = 0; i < 1000; i++) {
                    Solution optI = localOptima.get(i);
                    int fCelu = optI.getObjectiveValue();

                    int simBestNodes = optI.getCommonNodes(bestKnownSolution);
                    int simBestEdges = optI.getCommonEdges(bestKnownSolution);

                    long totalSimNodes = 0;
                    long totalSimEdges = 0;
                    for (int j = 0; j < 1000; j++) {
                        if (i == j) continue;
                        Solution optJ = localOptima.get(j);
                        totalSimNodes += optI.getCommonNodes(optJ);
                        totalSimEdges += optI.getCommonEdges(optJ);
                    }

                    double simAvgNodes = totalSimNodes / 999.0;
                    double simAvgEdges = totalSimEdges / 999.0;

                    out.printf(java.util.Locale.US, "%d;%d;%d;%.4f;%.4f\n",
                            fCelu, simBestNodes, simBestEdges, simAvgNodes, simAvgEdges);
                }
            }
            System.out.println("   [OK] Zapisano plik: lab5/wyniki/wypuklosc_dane.csv");


            System.out.println("\n=========================================================================");
            System.out.println("🚀 ROZPOCZYNAMY REALIZACJĘ ZADANIA 6: PORÓWNANIE METAHEURYSTYK I HAE");
            System.out.println("=========================================================================");

            int numRuns = 10;
            System.out.println("⏳ Trwa uruchamianie algorytmów (każdy testowany przez " + numRuns + " powtórzeń, limit " + timeLimitMs + "ms)...");

            // Mapa do zapisywania najlepszych znalezionych ścieżek
            Map<String, List<Integer>> bestPaths = new LinkedHashMap<>();

            runExperiment("Heurystyka Zachlanna", numRuns, () -> solver.solveGreedyHeuristic(), solver, bestPaths);
            runExperiment("Bazowe Lokalne Przeszk.", numRuns, () -> solver.solveBaseLocalSearch(), solver, bestPaths);

            runExperiment("MSLS", numRuns, () -> solver.solveMSLS(timeLimitMs), solver, bestPaths);
            runExperiment("ILS", numRuns, () -> solver.solveILS(timeLimitMs), solver, bestPaths);
            runExperiment("LNS", numRuns, () -> solver.solveLNS(timeLimitMs), solver, bestPaths);

            runExperiment("HAE (Op1 + LS)", numRuns, () -> solver.solveHAE(timeLimitMs, 1, true), solver, bestPaths);
            runExperiment("HAE (Op2 + LS)", numRuns, () -> solver.solveHAE(timeLimitMs, 2, true), solver, bestPaths);
            runExperiment("HAE (Op2 bez LS)", numRuns, () -> solver.solveHAE(timeLimitMs, 2, false), solver, bestPaths);
            runExperiment("HAE (Op3 + LS)", numRuns, () -> solver.solveHAE(timeLimitMs, 3, true), solver, bestPaths);
            runExperiment("HAE (Op3 bez LS)", numRuns, () -> solver.solveHAE(timeLimitMs, 3, false), solver, bestPaths);

            System.out.println("=========================================================================");

            // ZAPIS ŚCIEŻEK DO PLIKU
            try (PrintWriter out = new PrintWriter("lab5/wyniki/najlepsze_sciezki.txt")) {
                for (Map.Entry<String, List<Integer>> entry : bestPaths.entrySet()) {
                    out.print(entry.getKey() + ";");
                    List<Integer> path = entry.getValue();
                    for (int i = 0; i < path.size(); i++) {
                        out.print(path.get(i) + (i == path.size() - 1 ? "" : ","));
                    }
                    out.println();
                }
            }
            System.out.println("✅ Pomyślnie zapisano plik z trasami: lab5/wyniki/najlepsze_sciezki.txt");

        } catch (Exception e) {
            System.err.println("❌ Wystąpił błąd krytyczny: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runExperiment(String name, int runs, java.util.function.Supplier<Solution> algorithm,
                                      Solver solver, Map<String, List<Integer>> bestPathsMap) {
        double avgObj = 0;
        int maxObj = Integer.MIN_VALUE;
        long totalIterations = 0;
        Solution bestSolutionFound = null;

        System.out.print(String.format("%-23s", name) + " [");
        for (int i = 0; i < runs; i++) {
            Solution sol = algorithm.get();
            int obj = sol.getObjectiveValue();
            avgObj += obj;
            totalIterations += solver.getLatestIterations();

            if (obj > maxObj) {
                maxObj = obj;
                bestSolutionFound = new Solution(sol);
            }
            System.out.print("=");
        }
        System.out.print("] ");
        System.out.printf("| Średnia: %7.2f | Max: %5d | Śr. Iteracji: %6.1f\n",
                avgObj / runs, maxObj, (double) totalIterations / runs);

        if (bestSolutionFound != null) {

            bestPathsMap.put(name, bestSolutionFound.getPath());
        }
    }
}