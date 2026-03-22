package lab1.src;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;

public class Main {
    public static void main(String[] args) {
        // Upewnij się, że nazwa pliku zgadza się z tą na dysku
        String filepath = "TSPA.csv";

        try {
            Instance instance = new Instance(filepath);
            int n = instance.getSize();
            System.out.println(" Wczytano wierzchołków: " + n);


            Solver solver = new Solver(instance);

            // 1. Tablice do statystyk - ZBIERAJĄ WYNIKI ZE WSZYSTKICH 200 URUCHOMIEŃ
            double[] randomResults = new double[n];
            double[] nnWithoutProfitResults = new double[n];
            double[] nnWithProfitResults = new double[n];
            double[] gcWithoutProfitResults = new double[n];
            double[] gcWithProfitResults = new double[n];
            double[] regretWithProfitResults = new double[n];
            double[] weightedRegretWithProfitResults = new double[n];

            // 2. Zmienne do zapamiętania TYLKO NAJLEPSZYCH tras dla każdego algorytmu (do rysowania wykresów)
            Solution bestRandom = null; double maxRandom = -Double.MAX_VALUE;
            Solution bestNnWithoutProfit = null; double maxNnWithoutProfit = -Double.MAX_VALUE;
            Solution bestNnWithProfit = null; double maxNnWithProfit = -Double.MAX_VALUE;
            Solution bestGcWithoutProfit = null; double maxGcWithoutProfit = -Double.MAX_VALUE;
            Solution bestGcWithProfit = null; double maxGcWithProfit = -Double.MAX_VALUE;
            Solution bestRegret = null; double maxRegret = -Double.MAX_VALUE;
            Solution bestWeightedRegret = null; double maxWeightedRegret = -Double.MAX_VALUE;

            // Główna pętla eksperymentu
            for (int i = 0; i < n; i++) {

                // --- 0. Algorytm Losowy ---
                Solution randomSolution = solver.randomSolution();
                randomResults[i] = randomSolution.getObjectiveValue(); // Zapis do statystyk
                if (randomResults[i] > maxRandom) {  // Sprawdzenie czy to nowy rekord
                    maxRandom = randomResults[i];
                    bestRandom = new Solution(randomSolution);
                }

                // --- 1. NN (bez zysku) ---
                Solution nnWithoutProfit = solver.nearestNeighbor(i, false);
                nnWithoutProfitResults[i] = nnWithoutProfit.getObjectiveValue();
                if (nnWithoutProfitResults[i] > maxNnWithoutProfit) {
                    maxNnWithoutProfit = nnWithoutProfitResults[i];
                    bestNnWithoutProfit = new Solution(nnWithoutProfit);
                }

                // --- 2. NN (z zyskiem) ---
                Solution nnWithProfit = solver.nearestNeighbor(i, true);
                nnWithProfitResults[i] = nnWithProfit.getObjectiveValue();
                if (nnWithProfitResults[i] > maxNnWithProfit) {
                    maxNnWithProfit = nnWithProfitResults[i];
                    bestNnWithProfit = new Solution(nnWithProfit);
                }

                // --- 3. GC (bez zysku) ---
                Solution gcWithoutProfit = solver.greedyCycle(i, false);
                gcWithoutProfitResults[i] = gcWithoutProfit.getObjectiveValue();
                if (gcWithoutProfitResults[i] > maxGcWithoutProfit) {
                    maxGcWithoutProfit = gcWithoutProfitResults[i];
                    bestGcWithoutProfit = new Solution(gcWithoutProfit);
                }

                // --- 4. GC (z zyskiem) ---
                Solution gcWithProfit = solver.greedyCycle(i, true);
                gcWithProfitResults[i] = gcWithProfit.getObjectiveValue();
                if (gcWithProfitResults[i] > maxGcWithProfit) {
                    maxGcWithProfit = gcWithProfitResults[i];
                    bestGcWithProfit = new Solution(gcWithProfit);
                }

                // --- 5. 2-Żal ---
                Solution regretWithProfit = solver.regretCycle(i, true, false);
                regretWithProfitResults[i] = regretWithProfit.getObjectiveValue();
                if (regretWithProfitResults[i] > maxRegret) {
                    maxRegret = regretWithProfitResults[i];
                    bestRegret = new Solution(regretWithProfit);
                }

                // --- 6. Ważony 2-Żal ---
                Solution weightedRegretWithProfit = solver.regretCycle(i, true, true);
                weightedRegretWithProfitResults[i] = weightedRegretWithProfit.getObjectiveValue();
                if (weightedRegretWithProfitResults[i] > maxWeightedRegret) {
                    maxWeightedRegret = weightedRegretWithProfitResults[i];
                    bestWeightedRegret = new Solution(weightedRegretWithProfit);
                }

            }

            java.io.File directory = new java.io.File("lab1/wyniki");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Zapis do folderu
            saveRouteToFile(bestRandom, "lab1/wyniki/route_random.txt");
            saveRouteToFile(bestNnWithoutProfit, "lab1/wyniki/route_nn_no_profit.txt");
            saveRouteToFile(bestNnWithProfit, "lab1/wyniki/route_nn_profit.txt");
            saveRouteToFile(bestGcWithoutProfit, "lab1/wyniki/route_gc_no_profit.txt");
            saveRouteToFile(bestGcWithProfit, "lab1/wyniki/route_gc_profit.txt");
            saveRouteToFile(bestRegret, "lab1/wyniki/route_regret.txt");
            saveRouteToFile(bestWeightedRegret, "lab1/wyniki/route_weighted_regret.txt");

            // Wypisuwanie statystyk kolejnych algorytmów
            System.out.println("\n=================== WYNIKI EKSPERYMENTÓW ===================");
            printStats("Algorytm Losowy", randomResults);
            printStats("Najbliższy Sąsiad (Faza I ignoruje zysk)", nnWithoutProfitResults);
            printStats("Najbliższy Sąsiad (Faza I uwzględnia zysk)", nnWithProfitResults);
            printStats("Zachłanny Cykl (Faza I ignoruje zysk)", gcWithoutProfitResults);
            printStats("Zachłanny Cykl (Faza I uwzględnia zysk)", gcWithProfitResults);
            printStats("Algorytm 2-Żal (uwzględnia zysk)", regretWithProfitResults);
            printStats("Ważony Algorytm 2-Żal (uwzględnia zysk)", weightedRegretWithProfitResults);
            System.out.println("============================================================");

        } catch (IOException e) {
            System.err.println(" Błąd! Nie udało się wczytać pliku: " + e.getMessage());
        }
    }


    private static void saveRouteToFile(Solution solution, String filename) {
        if (solution == null) return;
        try (java.io.PrintWriter out = new java.io.PrintWriter(filename)) {
            for (int nodeId : solution.getPath()) {
                out.println(nodeId);
            }
            // Zamykamy cykl dodając z powrotem punkt startowy
            if (!solution.getPath().isEmpty()) {
                out.println(solution.getPath().get(0));
            }
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Błąd zapisu pliku: " + e.getMessage());
        }
    }


     // Metoda do wypisywania statystyk ze wszystkich 200 przebiegów

    private static void printStats(String algorithmName, double[] results) {
        DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
        for (double result : results) {
            stats.accept(result);
        }

        System.out.println("👉 " + algorithmName + ":");
        System.out.printf("   Najlepszy wynik (MAX) : %.2f\n", stats.getMax());
        System.out.printf("   Najgorszy wynik (MIN) : %.2f\n", stats.getMin());
        System.out.printf("   Średni wynik (AVG)    : %.2f\n\n", stats.getAverage());
    }
}