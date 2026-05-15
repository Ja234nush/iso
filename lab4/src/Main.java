package lab4.src;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String filepath = "TSPA.csv";

        try {
            Instance instance = new Instance(filepath);
            Solver solver = new Solver(instance);

            int numRuns = 20;
            int numAlgorithms = 4;

            String[] algoNames = {
                    "Heurystyka 2-Zal",
                    "LS Standard Steepest",
                    "LS Kandydackie",
                    "LS Lista LM"
            };

            int[][] scores = new int[numAlgorithms][numRuns];
            long[][] timesNs = new long[numAlgorithms][numRuns];

            Solution[] bestSolutions = new Solution[numAlgorithms];
            Solution[] worstSolutions = new Solution[numAlgorithms];

            int[] maxScores = new int[numAlgorithms];
            int[] minScoresTracker = new int[numAlgorithms];

            for (int i = 0; i < numAlgorithms; i++) {
                maxScores[i] = Integer.MIN_VALUE;
                minScoresTracker[i] = Integer.MAX_VALUE;
            }

            System.out.println("⏳ Trwa przeprowadzanie algorytmow podstawowych (" + numRuns + " iteracji)...");

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

                if (iter % 2 == 0) System.out.print(".");
            }

            System.out.println("\n✅ Zakończono obliczenia podstawowe!");
            printStats(algoNames, scores, timesNs);

            // =========================================================================
            // EKSPERYMENTY DLA ZADANIA 4 (MSLS, ILS, LNS)
            // =========================================================================
            System.out.println("\n⏳ Rozpoczynam eksperymenty dla METAHEURYSTYK (Zadanie 4)...");

            // Zmienne statystyk metaheurystyk
            double mslsAvgObj = 0, ilsAvgObj = 0, lnsAvgObj = 0;
            int mslsMaxObj = Integer.MIN_VALUE, ilsMaxObj = Integer.MIN_VALUE, lnsMaxObj = Integer.MIN_VALUE;
            long totalMslsTime = 0;
            long totalIlsPerturbations = 0, totalLnsPerturbations = 0;

            Solution bestMslsSolution = null;
            Solution bestIlsSolution = null;
            Solution bestLnsSolution = null;

            // 1. Uruchamiamy MSLS i liczymy czas do warunku stopu
            System.out.println("   [MSLS] Trwa liczenie...");
            for(int i = 0; i < numRuns; i++) {
                long startMs = System.currentTimeMillis();
                Solution sol = solver.solveMSLS();
                totalMslsTime += (System.currentTimeMillis() - startMs);

                int obj = sol.getObjectiveValue();
                mslsAvgObj += obj;
                if (obj > mslsMaxObj) { mslsMaxObj = obj; bestMslsSolution = sol; }
            }
            long mslsAvgTimeLimitMs = totalMslsTime / numRuns;

            // 2. Uruchamiamy ILS oparty na limicie czasowym
            System.out.println("   [ILS] Trwa liczenie (Limit: " + mslsAvgTimeLimitMs + " ms)...");
            for(int i = 0; i < numRuns; i++) {
                Solution sol = solver.solveILS(mslsAvgTimeLimitMs);
                totalIlsPerturbations += solver.ilsPerturbationsCount;

                int obj = sol.getObjectiveValue();
                ilsAvgObj += obj;
                if (obj > ilsMaxObj) { ilsMaxObj = obj; bestIlsSolution = sol; }
            }

            // 3. Uruchamiamy LNS oparty na limicie czasowym
            System.out.println("   [LNS] Trwa liczenie (Limit: " + mslsAvgTimeLimitMs + " ms)...");
            for(int i = 0; i < numRuns; i++) {
                Solution sol = solver.solveLNS(mslsAvgTimeLimitMs);
                totalLnsPerturbations += solver.lnsPerturbationsCount;

                int obj = sol.getObjectiveValue();
                lnsAvgObj += obj;
                if (obj > lnsMaxObj) { lnsMaxObj = obj; bestLnsSolution = sol; }
            }

            System.out.println("\n====================== WYNIKI METAHEURYSTYK (Zadanie 4) ======================");
            System.out.printf("%-20s | Srednia: %7.2f | Max: %5d | Śr. Czas/Limit: %d ms\n", "MSLS", mslsAvgObj/numRuns, mslsMaxObj, mslsAvgTimeLimitMs);
            System.out.printf("%-20s | Srednia: %7.2f | Max: %5d | Śr. perturbacji: %d\n", "ILS", ilsAvgObj/numRuns, ilsMaxObj, totalIlsPerturbations/numRuns);
            System.out.printf("%-20s | Srednia: %7.2f | Max: %5d | Śr. iteracji D/R: %d\n", "LNS", lnsAvgObj/numRuns, lnsMaxObj, totalLnsPerturbations/numRuns);

            // =========================================================================
            // ZAPIS WYNIKÓW
            // =========================================================================
            File directory = new File("lab4/wyniki");
            if (!directory.exists()) directory.mkdirs();

            for (int i = 0; i < numAlgorithms; i++) {
                String safeName = algoNames[i].replaceAll("[\\s,\\(\\)-]+", "_").toLowerCase();
                saveRouteToFile(bestSolutions[i], "lab4/wyniki/route_najlepsza_" + safeName + ".txt");
                saveRouteToFile(worstSolutions[i], "lab4/wyniki/route_najgorsza_" + safeName + ".txt");
            }

            // Zapis tras dla metaheurystyk
            saveRouteToFile(bestMslsSolution, "lab4/wyniki/route_najlepsza_msls.txt");
            saveRouteToFile(bestIlsSolution, "lab4/wyniki/route_najlepsza_ils.txt");
            saveRouteToFile(bestLnsSolution, "lab4/wyniki/route_najlepsza_lns.txt");

            System.out.println("\n✅ Zapisano pomyślnie wszystkie trasy w folderze 'lab4/wyniki/'.");

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
                out.println(solution.getPath().get(0));
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