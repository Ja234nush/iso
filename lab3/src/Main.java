package lab3.src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        // Pamiętaj o zmianie ścieżki, jeśli pliki CSV trzymasz wyżej lub w innym miejscu
        String filepath = "TSPA.csv";

        try {
            Instance instance = new Instance(filepath);
            Solver solver = new Solver(instance);
            int numRuns = 100;
            int numAlgorithms = 4;

            String[] algoNames = {
                    "Heurystyka 2-Zal",
                    "LS Standard Steepest",
                    "LS Kandydackie",
                    "LS Lista LM"
            };

            int[][] scores = new int[numAlgorithms][numRuns];
            long[][] timesNs = new long[numAlgorithms][numRuns];

            // Tablice do trzymania najlepszych i najgorszych rozwiązań dla każdego algorytmu
            Solution[] bestSolutions = new Solution[numAlgorithms];
            Solution[] worstSolutions = new Solution[numAlgorithms];

            int[] maxScores = new int[numAlgorithms];
            int[] minScoresTracker = new int[numAlgorithms];

            for (int i = 0; i < numAlgorithms; i++) {
                maxScores[i] = Integer.MIN_VALUE;
                minScoresTracker[i] = Integer.MAX_VALUE;
            }

            System.out.println("⏳ Trwa przeprowadzanie eksperymentow (100 iteracji)...");

            for (int iter = 0; iter < numRuns; iter++) {
                Solution startRandom = solver.randomSolution();
                java.util.Random rand = new java.util.Random();

                // 1. Heurystyka 2-Żal jako Baseline
                long t0 = System.nanoTime();
                Solution s0 = solver.regretCycle(rand.nextInt(instance.getSize()));
                timesNs[0][iter] = System.nanoTime() - t0;
                scores[0][iter] = s0.getObjectiveValue();
                if (scores[0][iter] > maxScores[0]) { maxScores[0] = scores[0][iter]; bestSolutions[0] = s0; }
                if (scores[0][iter] < minScoresTracker[0]) { minScoresTracker[0] = scores[0][iter]; worstSolutions[0] = s0; }

                // 2. Base Local Search
                long t1 = System.nanoTime();
                Solution s1 = solver.localSearchBaseSteepest(startRandom);
                timesNs[1][iter] = System.nanoTime() - t1;
                scores[1][iter] = s1.getObjectiveValue();
                if (scores[1][iter] > maxScores[1]) { maxScores[1] = scores[1][iter]; bestSolutions[1] = s1; }
                if (scores[1][iter] < minScoresTracker[1]) { minScoresTracker[1] = scores[1][iter]; worstSolutions[1] = s1; }

                // 3. Local Search - Ruchy Kandydackie
                long t2 = System.nanoTime();
                Solution s2 = solver.localSearchCandidateMoves(startRandom);
                timesNs[2][iter] = System.nanoTime() - t2;
                scores[2][iter] = s2.getObjectiveValue();
                if (scores[2][iter] > maxScores[2]) { maxScores[2] = scores[2][iter]; bestSolutions[2] = s2; }
                if (scores[2][iter] < minScoresTracker[2]) { minScoresTracker[2] = scores[2][iter]; worstSolutions[2] = s2; }

                // 4. Local Search - Lista Ruchów (LM)
                long t3 = System.nanoTime();
                Solution s3 = solver.localSearchWithLM(startRandom);
                timesNs[3][iter] = System.nanoTime() - t3;
                scores[3][iter] = s3.getObjectiveValue();
                if (scores[3][iter] > maxScores[3]) { maxScores[3] = scores[3][iter]; bestSolutions[3] = s3; }
                if (scores[3][iter] < minScoresTracker[3]) { minScoresTracker[3] = scores[3][iter]; worstSolutions[3] = s3; }

                if (iter % 10 == 0) System.out.print(".");
            }

            System.out.println("\n✅ Zakończono obliczenia!");

            // =========================================================
            // ZAPIS WYNIKÓW DO PLIKÓW DLA SKRYPTU PYTHON
            // =========================================================
            File directory = new File("lab3/wyniki");
            if (!directory.exists()) directory.mkdirs();

            for (int i = 0; i < numAlgorithms; i++) {
                // Tworzymy bezpieczną nazwę pliku (usuwamy spacje, zmieniamy na małe litery)
                String safeName = algoNames[i].replaceAll("[\\s,\\(\\)-]+", "_").toLowerCase();

                // Zapisujemy zarówno najlepsze, jak i najgorsze rozwiązania
                saveRouteToFile(bestSolutions[i], "lab3/wyniki/route_najlepsza_" + safeName + ".txt");
                saveRouteToFile(worstSolutions[i], "lab3/wyniki/route_najgorsza_" + safeName + ".txt");
            }
            System.out.println("✅ Zapisano najlepsze ORAZ najgorsze trasy w folderze 'lab3/wyniki/'.");

            printStats(algoNames, scores, timesNs);

        } catch (IOException e) {
            System.err.println("Blad odczytu pliku: " + e.getMessage());
        }
    }

    private static void saveRouteToFile(Solution solution, String filename) {
        if (solution == null) return;
        try (PrintWriter out = new PrintWriter(filename)) {
            for (int nodeId : solution.getPath()) {
                out.println(nodeId);
            }
            if (!solution.getPath().isEmpty()) {
                out.println(solution.getPath().get(0)); // Zamknięcie cyklu dla ładnego wykresu
            }
        } catch (FileNotFoundException e) {
            System.err.println("Błąd zapisu pliku: " + e.getMessage());
        }
    }

    private static void printStats(String[] algoNames, int[][] scores, long[][] timesNs) {
        System.out.println("\n====================== WYNIKI CELU (Zysk - Koszt) ======================");
        for (int i = 0; i < algoNames.length; i++) {
            double avgScore = 0;
            int maxScore = Integer.MIN_VALUE;
            int minScore = Integer.MAX_VALUE;
            for (int s : scores[i]) {
                avgScore += s;
                if(s > maxScore) maxScore = s;
                if(s < minScore) minScore = s;
            }
            System.out.printf("%-40s | Srednia: %7.2f | Max: %5d | Min: %5d\n", algoNames[i], avgScore / scores[i].length, maxScore, minScore);
        }

        System.out.println("\n====================== CZASY WYKONANIA (W MILISEKUNDACH) ======================");
        for (int i = 0; i < algoNames.length; i++) {
            double avgTimeMs = 0;
            for (long t : timesNs[i]) {
                avgTimeMs += (t / 1_000_000.0);
            }
            System.out.printf("%-40s | Sredni czas: %7.2f ms\n", algoNames[i], avgTimeMs / timesNs[i].length);
        }
    }
}