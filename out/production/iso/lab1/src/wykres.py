import matplotlib.pyplot as plt
import pandas as pd
import os

# 1. Wczytanie oryginalnych danych
# ../ oznacza "wyjdź z folderu src wyżej", a potem wchodzimy do "wyniki"
csv_path = '../../TSPA.csv'

if not os.path.exists(csv_path):
    print(f"❌ Błąd: Nie znaleziono pliku {csv_path}. Upewnij się, że nazwa się zgadza!")
else:
    df = pd.read_csv(csv_path, sep=';', header=None, names=['x', 'y', 'cost'])
    sizes = df['cost'] / df['cost'].max() * 200

    # Słownik ze ścieżkami wychodzącymi do ../wyniki/
    algorithms = {
        '../wyniki/route_random.txt': 'Rozwiązanie Losowe',
        '../wyniki/route_nn_no_profit.txt': 'Najbliższy Sąsiad (NNa - bez zysku)',
        '../wyniki/route_nn_profit.txt': 'Najbliższy Sąsiad (NN - z zyskiem)',
        '../wyniki/route_gc_no_profit.txt': 'Zachłanny Cykl (GCa - bez zysku)',
        '../wyniki/route_gc_profit.txt': 'Zachłanny Cykl (GC - z zyskiem)',
        '../wyniki/route_regret.txt': 'Algorytm 2-Żal (z zyskiem)',
        '../wyniki/route_weighted_regret.txt': 'Ważony Algorytm 2-Żal (z zyskiem)'
    }

    # Pętla po wszystkich algorytmach
    for filepath, title in algorithms.items():
        if not os.path.exists(filepath):
            print(f"Brak pliku {filepath}, pomijam...")
            continue

        # UWAGA: Wczytanie trasy z ominięciem nagłówka dla Excela (NodeID;X;Y)
        with open(filepath, 'r') as f:
            lines = f.readlines()
            route = []
            for line in lines[1:]:  # Omijamy pierwszą linijkę (nagłówek)
                if line.strip():    # Jeśli linia nie jest pusta
                    # Rozdzielamy po średniku i bierzemy tylko pierwszy element (ID wierzchołka)
                    node_id = int(line.strip().split(';')[0])
                    route.append(node_id)

        route_x = [df.iloc[node_id]['x'] for node_id in route]
        route_y = [df.iloc[node_id]['y'] for node_id in route]

        # Tworzenie wykresu
        plt.figure(figsize=(12, 8))

        # Rysowanie tła (ominięte punkty)
        plt.scatter(df['x'], df['y'], c='lightgray', s=sizes, label='Ominięte wierzchołki')

        # Rysowanie trasy
        plt.plot(route_x, route_y, c='red', linewidth=2, zorder=1, label='Trasa')
        plt.scatter(route_x, route_y, c='blue', s=[sizes[i] for i in route], zorder=2, label='Odwiedzone')
        plt.scatter(route_x[0], route_y[0], c='green', marker='*', s=300, zorder=3, label='Start')

        # Estetyka
        plt.title(f'{title}\nProblem komiwojażera z zyskami', fontsize=16)
        plt.xlabel('Współrzędna X')
        plt.ylabel('Współrzędna Y')
        plt.legend()
        plt.grid(True, linestyle='--', alpha=0.5)

        # Zapis obrazka do tego samego folderu (../wyniki/)
        safe_title = title.replace(' ', '_').replace('(', '').replace(')', '').replace('-', '_')
        output_filename = f'../wyniki/wykres_{safe_title}.png'

        plt.savefig(output_filename, dpi=300, bbox_inches='tight')
        print(f"✅ Zapisano wykres: {output_filename}")

        plt.close()

    print("\nGotowe! Wszystkie wykresy powędrowały do folderu lab1/wyniki/")