import matplotlib.pyplot as plt
import csv
import sys
import os
import glob

def wczytaj_instancje(sciezka_csv):
    """Wczytuje współrzędne wierzchołków oraz ich zyski z pliku CSV."""
    wspolrzedne = {}
    try:
        with open(sciezka_csv, 'r') as plik:
            reader = csv.reader(plik, delimiter=';')
            for i, wiersz in enumerate(reader):
                if len(wiersz) >= 3:
                    # x, y oraz zysk (cost)
                    x = int(wiersz[0])
                    y = int(wiersz[1])
                    cost = int(wiersz[2])
                    wspolrzedne[i] = (x, y, cost)
    except Exception as e:
        print(f"Błąd podczas wczytywania instancji: {e}")
        sys.exit(1)
    return wspolrzedne

def wczytaj_trase(sciezka_txt):
    """Wczytuje kolejność wierzchołków z pliku TXT."""
    trasa = []
    try:
        with open(sciezka_txt, 'r') as plik:
            for linia in plik:
                if linia.strip():
                    trasa.append(int(linia.strip()))
    except Exception as e:
        print(f"Błąd podczas wczytywania trasy {sciezka_txt}: {e}")
        return []
    return trasa

def rysuj_wykres(plik_csv, plik_txt, tytul="Wizualizacja Trasy"):
    wspolrzedne = wczytaj_instancje(plik_csv)
    trasa = wczytaj_trase(plik_txt)

    if not wspolrzedne or not trasa:
        print(f"Pomijam pusty lub uszkodzony plik: {plik_txt}")
        return

    # Wyliczanie rozmiarów kropek na podstawie zysku (jak w Lab 1)
    koszty = [pos[2] for pos in wspolrzedne.values()]
    max_koszt = max(koszty) if koszty else 1

    # Przygotowanie danych tła
    wszystkie_x = [pos[0] for pos in wspolrzedne.values()]
    wszystkie_y = [pos[1] for pos in wspolrzedne.values()]
    rozmiary_tlo = [(k / max_koszt) * 200 for k in koszty]

    # Przygotowanie danych trasy
    trasa_x = [wspolrzedne[w][0] for w in trasa]
    trasa_y = [wspolrzedne[w][1] for w in trasa]
    rozmiary_trasa = [(wspolrzedne[w][2] / max_koszt) * 200 for w in trasa]

    plt.figure(figsize=(12, 8))

    # 1. Rysowanie wszystkich wierzchołków z pliku (tło)
    plt.scatter(wszystkie_x, wszystkie_y, c='lightgray', s=rozmiary_tlo, label='Ominięte wierzchołki', zorder=1)

    # 2. Rysowanie krawędzi trasy (linie)
    plt.plot(trasa_x, trasa_y, c='red', linewidth=1.5, zorder=2, label='Trasa')

    # 3. Rysowanie wierzchołków należących do trasy (kropki)
    plt.scatter(trasa_x, trasa_y, c='blue', s=rozmiary_trasa, zorder=3, label='Odwiedzone')

    # 4. Wyróżnienie punktu startowego
    if trasa:
        start_x, start_y, start_cost = wspolrzedne[trasa[0]]
        plt.scatter([start_x], [start_y], c='lime', marker='*', s=300, edgecolors='black', zorder=4, label='Start')

    # Ustawienia wyglądu wykresu
    plt.title(tytul + '\nProblem komiwojażera z zyskami', fontsize=16)
    plt.xlabel('Współrzędna X', fontsize=12)
    plt.ylabel('Współrzędna Y', fontsize=12)
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.5)

    # Zapis
    nazwa_pliku_wyjsciowego = plik_txt.replace('.txt', '.png')
    plt.savefig(nazwa_pliku_wyjsciowego, dpi=300, bbox_inches='tight')
    print(f"Zapisano wykres: {nazwa_pliku_wyjsciowego}")

    # UWAGA: Zamykamy wykres zamiast go wyświetlać
    plt.close()

if __name__ == "__main__":
    # Zakładam, że skrypt pythona leży w folderze "lab3", a TSPA.csv jest folder wyżej
    plik_instancji = "../TSPA.csv"
    folder_wynikow = "wyniki"

    print(f"Szukam plików z trasami w folderze: {folder_wynikow}...")

    # Znalezienie wszystkich plików TXT
    sciezka_wyszukiwania = os.path.join(folder_wynikow, "route_*.txt")
    pliki_tras = glob.glob(sciezka_wyszukiwania)

    if not pliki_tras:
        print(f"Brak plików do przetworzenia. Sprawdź czy ścieżka '{folder_wynikow}' jest poprawna.")
    else:
        print(f"Znaleziono {len(pliki_tras)} plików. Rozpoczynam generowanie wykresów...")

        for plik_trasy in pliki_tras:
            # Tworzymy ładny tytuł na podstawie nazwy pliku
            nazwa = os.path.basename(plik_trasy)
            czysty_tytul = nazwa.replace("route_", "").replace(".txt", "").replace("_", " ").strip().title()

            # Poprawienie formatowania tytułu, jeśli to Local Search
            czysty_tytul = czysty_tytul.replace("Ls ", "Local Search ")

            rysuj_wykres(plik_instancji, plik_trasy, f"{czysty_tytul}")

        print("\n✅ Wszystkie wykresy zostały wygenerowane i zapisane w folderze!")