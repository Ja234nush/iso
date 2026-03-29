import matplotlib.pyplot as plt
import csv
import sys
import os
import glob

def wczytaj_instancje(sciezka_csv):
    """Wczytuje współrzędne wierzchołków z pliku CSV."""
    wspolrzedne = {}
    try:
        with open(sciezka_csv, 'r') as plik:
            reader = csv.reader(plik, delimiter=';')
            for i, wiersz in enumerate(reader):
                if len(wiersz) >= 2:
                    # Zakładamy, że x jest w pierwszej kolumnie, y w drugiej
                    x = int(wiersz[0])
                    y = int(wiersz[1])
                    wspolrzedne[i] = (x, y)
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
        return [] # Zwracamy pustą listę zamiast przerywać cały skrypt
    return trasa

def rysuj_wykres(plik_csv, plik_txt, tytul="Wizualizacja Trasy"):
    wspolrzedne = wczytaj_instancje(plik_csv)
    trasa = wczytaj_trase(plik_txt)

    if not wspolrzedne or not trasa:
        print(f"Pomijam pusty lub uszkodzony plik: {plik_txt}")
        return

    # Przygotowanie danych do rysowania
    wszystkie_x = [pos[0] for pos in wspolrzedne.values()]
    wszystkie_y = [pos[1] for pos in wspolrzedne.values()]

    trasa_x = [wspolrzedne[w][0] for w in trasa]
    trasa_y = [wspolrzedne[w][1] for w in trasa]

    plt.figure(figsize=(10, 8))

    # 1. Rysowanie wszystkich wierzchołków z pliku (tło)
    plt.scatter(wszystkie_x, wszystkie_y, c='lightgray', label='Wszystkie wierzchołki', zorder=1)

    # 2. Rysowanie krawędzi trasy (linie)
    plt.plot(trasa_x, trasa_y, c='blue', linewidth=1.5, zorder=2, label='Trasa')

    # 3. Rysowanie wierzchołków należących do trasy (kropki)
    plt.scatter(trasa_x, trasa_y, c='blue', s=30, zorder=3)

    # 4. Wyróżnienie punktu startowego
    if trasa:
        start_x, start_y = wspolrzedne[trasa[0]]
        plt.scatter([start_x], [start_y], c='lime', s=100, edgecolors='black', zorder=4, label='Start')

    # Ustawienia wyglądu wykresu
    plt.title(tytul, fontsize=14, fontweight='bold')
    plt.xlabel('Współrzędna X', fontsize=12)
    plt.ylabel('Współrzędna Y', fontsize=12)
    plt.legend()
    plt.grid(True, linestyle='--', alpha=0.6)

    # Zapis
    nazwa_pliku_wyjsciowego = plik_txt.replace('.txt', '.png')
    plt.savefig(nazwa_pliku_wyjsciowego, dpi=300, bbox_inches='tight')
    print(f"Zapisano wykres: {nazwa_pliku_wyjsciowego}")

    # UWAGA: Zamykamy wykres zamiast go wyświetlać, żeby skrypt przetworzył wszystkie pliki z automatu
    plt.close()

if __name__ == "__main__":
    # 1. Konfiguracja pliku z instancją
    plik_instancji = "../TSPA.csv"
    folder_wynikow = "wyniki"

    print(f"Szukam plików z trasami w folderze: {folder_wynikow}...")

    # 2. Znalezienie wszystkich plików TXT zaczynających się od "route_"
    sciezka_wyszukiwania = os.path.join(folder_wynikow, "route_*.txt")
    pliki_tras = glob.glob(sciezka_wyszukiwania)

    if not pliki_tras:
        print(f"Brak plików do przetworzenia. Sprawdź czy ścieżka '{folder_wynikow}' jest poprawna.")
    else:
        print(f"Znaleziono {len(pliki_tras)} plików. Rozpoczynam generowanie wykresów...")

        # 3. Pętla przetwarzająca każdy plik
        for plik_trasy in pliki_tras:
            # Tworzymy ładny tytuł na podstawie nazwy pliku
            # (np. "route_ls_rand_steepest_swap.txt" -> "Ls Rand Steepest Swap")
            nazwa = os.path.basename(plik_trasy)
            czysty_tytul = nazwa.replace("route_", "").replace(".txt", "").replace("_", " ").strip().title()

            rysuj_wykres(plik_instancji, plik_trasy, f"Trasa: {czysty_tytul}")

        print("\n✅ Wszystkie wykresy zostały wygenerowane i zapisane w folderze!")