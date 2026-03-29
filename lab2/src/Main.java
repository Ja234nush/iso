package lab2.src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        String filepath = "TSPA.csv"; // lub TSPB.csv

        try {
            Instance instance = new Instance(filepath);
            System.out.println("✅ Wczytano wierzchołków: " + instance.getSize());

            Solver solver = new Solver(instance);
            Random random = new Random();

            int numRuns = 100;
            int numAlgorithms = 9; // 8 wariantów LS + 1 Random Walk

            // Nazwy algorytmów do tabeli
            String[] algoNames = {
                    "LS (Rand, Steepest, Swap)",
                    "LS (Rand, Steepest, 2-Opt)",
                    "LS (Rand, Greedy, Swap)",
                    "LS (Rand, Greedy, 2-Opt)",
                    "LS (Regret, Steepest, Swap)",
                    "LS (Regret, Steepest, 2-Opt)",
                    "LS (Regret, Greedy, Swap)",
                    "LS (Regret, Greedy, 2-Opt)",
                    "Random Walk (Baseline)"
            };

            // Tablice do przechowywania statystyk
            int[][] scores = new int[numAlgorithms][numRuns];
            long[][] timesNs = new long[numAlgorithms][numRuns];

            Solution[] bestSolutions = new Solution[numAlgorithms];
            int[] maxScores = new int[numAlgorithms];
            for (int i = 0; i < numAlgorithms; i++) {
                maxScores[i] = Integer.MIN_VALUE;
            }

            System.out.println("⏳ Trwa przeprowadzanie 100 iteracji dla 8 wariantów Local Search...");

            // =========================================================
            // ETAP 1: Testowanie 8 wariantów Local Search
            // =========================================================
            for (int iter = 0; iter < numRuns; iter++) {

                // Generowanie rozwiązań startowych
                Solution startRandom = solver.randomSolution();
                int startNode = random.nextInt(instance.getSize());
                Solution startRegret = solver.regretCycle(startNode, true, false);

                // --- 0: LS (Rand, Steepest, Swap) ---
                long t0 = System.nanoTime();
                Solution s0 = solver.localSearch(startRandom, true, false);
                timesNs[0][iter] = System.nanoTime() - t0;
                scores[0][iter] = s0.getObjectiveValue();
                if (scores[0][iter] > maxScores[0]) { maxScores[0] = scores[0][iter]; bestSolutions[0] = s0; }

                // --- 1: LS (Rand, Steepest, 2-Opt) ---
                long t1 = System.nanoTime();
                Solution s1 = solver.localSearch(startRandom, true, true);
                timesNs[1][iter] = System.nanoTime() - t1;
                scores[1][iter] = s1.getObjectiveValue();
                if (scores[1][iter] > maxScores[1]) { maxScores[1] = scores[1][iter]; bestSolutions[1] = s1; }

                // --- 2: LS (Rand, Greedy, Swap) ---
                long t2 = System.nanoTime();
                Solution s2 = solver.localSearch(startRandom, false, false);
                timesNs[2][iter] = System.nanoTime() - t2;
                scores[2][iter] = s2.getObjectiveValue();
                if (scores[2][iter] > maxScores[2]) { maxScores[2] = scores[2][iter]; bestSolutions[2] = s2; }

                // --- 3: LS (Rand, Greedy, 2-Opt) ---
                long t3 = System.nanoTime();
                Solution s3 = solver.localSearch(startRandom, false, true);
                timesNs[3][iter] = System.nanoTime() - t3;
                scores[3][iter] = s3.getObjectiveValue();
                if (scores[3][iter] > maxScores[3]) { maxScores[3] = scores[3][iter]; bestSolutions[3] = s3; }

                // --- 4: LS (Regret, Steepest, Swap) ---
                long t4 = System.nanoTime();
                Solution s4 = solver.localSearch(startRegret, true, false);
                timesNs[4][iter] = System.nanoTime() - t4;
                scores[4][iter] = s4.getObjectiveValue();
                if (scores[4][iter] > maxScores[4]) { maxScores[4] = scores[4][iter]; bestSolutions[4] = s4; }

                // --- 5: LS (Regret, Steepest, 2-Opt) ---
                long t5 = System.nanoTime();
                Solution s5 = solver.localSearch(startRegret, true, true);
                timesNs[5][iter] = System.nanoTime() - t5;
                scores[5][iter] = s5.getObjectiveValue();
                if (scores[5][iter] > maxScores[5]) { maxScores[5] = scores[5][iter]; bestSolutions[5] = s5; }

                // --- 6: LS (Regret, Greedy, Swap) ---
                long t6 = System.nanoTime();
                Solution s6 = solver.localSearch(startRegret, false, false);
                timesNs[6][iter] = System.nanoTime() - t6;
                scores[6][iter] = s6.getObjectiveValue();
                if (scores[6][iter] > maxScores[6]) { maxScores[6] = scores[6][iter]; bestSolutions[6] = s6; }

                // --- 7: LS (Regret, Greedy, 2-Opt) ---
                long t7 = System.nanoTime();
                Solution s7 = solver.localSearch(startRegret, false, true);
                timesNs[7][iter] = System.nanoTime() - t7;
                scores[7][iter] = s7.getObjectiveValue();
                if (scores[7][iter] > maxScores[7]) { maxScores[7] = scores[7][iter]; bestSolutions[7] = s7; }

                if (iter % 10 == 0) System.out.print("."); // Progress bar
            }
            System.out.println("\n✅ Zakończono Local Search.");

            // =========================================================
            // ETAP 2: Obliczanie czasu najwolniejszego wariantu LS
            // =========================================================
            long maxAvgTimeNs = 0;
            for (int algo = 0; algo < 8; algo++) {
                long sumTimeNs = 0;
                for (int iter = 0; iter < numRuns; iter++) {
                    sumTimeNs += timesNs[algo][iter];
                }
                long avgTimeNs = sumTimeNs / numRuns;
                if (avgTimeNs > maxAvgTimeNs) {
                    maxAvgTimeNs = avgTimeNs;
                }
            }

            System.out.printf("⏳ Średni czas najwolniejszego LS to: %.2f ms. Odpalam Random Walk...\n", maxAvgTimeNs / 1_000_000.0);

            // =========================================================
            // ETAP 3: Testowanie Random Walk
            // =========================================================
            for (int iter = 0; iter < numRuns; iter++) {
                Solution startRandom = solver.randomSolution();

                long t8 = System.nanoTime();
                Solution s8 = solver.randomWalk(startRandom, maxAvgTimeNs, true); // Zezwalamy na 2-opt w Random Walk
                timesNs[8][iter] = System.nanoTime() - t8;
                scores[8][iter] = s8.getObjectiveValue();

                if (scores[8][iter] > maxScores[8]) {
                    maxScores[8] = scores[8][iter];
                    bestSolutions[8] = s8;
                }
            }
            System.out.println("✅ Zakończono Random Walk.");

            // =========================================================
            // ETAP 4: Zapis plików tras (do wizualizacji)
            // =========================================================
            File directory = new File("2/wyniki");
            if (!directory.exists()) directory.mkdirs();

            for (int i = 0; i < numAlgorithms; i++) {
                String safeName = algoNames[i].replaceAll("[\\s,\\(\\)-]+", "_").toLowerCase();
                saveRouteToFile(bestSolutions[i], "lab2/wyniki/route_" + safeName + ".txt");
            }

            // =========================================================
            // ETAP 5: Drukowanie wyników
            // =========================================================
            printObjectiveTable(algoNames, scores);
            printTimeTable(algoNames, timesNs);

        } catch (IOException e) {
            System.err.println("❌ Błąd! Nie udało się wczytać pliku: " + e.getMessage());
        }
    }

    private static void saveRouteToFile(Solution solution, String filename) {
        if (solution == null) return;
        try (PrintWriter out = new PrintWriter(filename)) {
            for (int nodeId : solution.getPath()) {
                out.println(nodeId);
            }
            if (!solution.getPath().isEmpty()) {
                out.println(solution.getPath().get(0)); // Zamknięcie cyklu
            }
        } catch (FileNotFoundException e) {
            System.err.println("Błąd zapisu pliku: " + e.getMessage());
        }
    }

    // =========================================================
    // METODY WYPISUJĄCE TABELE
    // =========================================================

    private static void printObjectiveTable(String[] algoNames, int[][] scores) {
        System.out.println("\n=========================================================================");
        System.out.println("                         WYNIKI FUNKCJI CELU (Zysk - Dystans)            ");
        System.out.println("=========================================================================");
        System.out.printf("%-30s | %10s | %10s | %10s\n", "Algorytm", "Średnia", "Min", "Max");
        System.out.println("-------------------------------------------------------------------------");

        for (int i = 0; i < algoNames.length; i++) {
            long sum = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            for (int s : scores[i]) {
                sum += s;
                if (s < min) min = s;
                if (s > max) max = s;
            }
            double avg = (double) sum / scores[i].length;
            System.out.printf("%-30s | %10.2f | %10d | %10d\n", algoNames[i], avg, min, max);
        }
        System.out.println("=========================================================================");
    }

    private static void printTimeTable(String[] algoNames, long[][] timesNs) {
        System.out.println("\n=========================================================================");
        System.out.println("                         CZASY WYKONANIA [ms]                            ");
        System.out.println("=========================================================================");
        System.out.printf("%-30s | %10s | %10s | %10s\n", "Algorytm", "Średnia", "Min", "Max");
        System.out.println("-------------------------------------------------------------------------");

        for (int i = 0; i < algoNames.length; i++) {
            long sumNs = 0;
            long minNs = Long.MAX_VALUE;
            long maxNs = Long.MIN_VALUE;

            for (long t : timesNs[i]) {
                sumNs += t;
                if (t < minNs) minNs = t;
                if (t > maxNs) maxNs = t;
            }

            // Konwersja na milisekundy z nanosekund (1 ms = 1 000 000 ns)
            double avgMs = (sumNs / (double) timesNs[i].length) / 1_000_000.0;
            double minMs = minNs / 1_000_000.0;
            double maxMs = maxNs / 1_000_000.0;

            System.out.printf("%-30s | %10.2f | %10.2f | %10.2f\n", algoNames[i], avgMs, minMs, maxMs);
        }
        System.out.println("=========================================================================\n");
    }
}