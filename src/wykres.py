import matplotlib.pyplot as plt
import pandas as pd
import os

# 1. Wczytanie oryginalnych danych - upewnij się że nazwa to Twój plik z danymi
df = pd.read_csv('TSPA.csv', sep=';', header=None, names=['x', 'y', 'cost'])
sizes = df['cost'] / df['cost'].max() * 200

# Słownik ze zaktualizowanymi ścieżkami do folderu lab1/wyniki/
algorithms = {
    'lab1/wyniki/route_random.txt': 'Rozwiązanie Losowe',
    'lab1/wyniki/route_nn_no_profit.txt': 'Najbliższy Sąsiad (NNa - bez zysku)',
    'lab1/wyniki/route_nn_profit.txt': 'Najbliższy Sąsiad (NN - z zyskiem)',
    'lab1/wyniki/route_gc_no_profit.txt': 'Zachłanny Cykl (GCa - bez zysku)',
    'lab1/wyniki/route_gc_profit.txt': 'Zachłanny Cykl (GC - z zyskiem)',
    'lab1/wyniki/route_regret.txt': 'Algorytm 2-Żal (z zyskiem)',
    'lab1/wyniki/route_weighted_regret.txt': 'Ważony Algorytm 2-Żal (z zyskiem)'
}

# Pętla po wszystkich algorytmach
for filepath, title in algorithms.items():
    if not os.path.exists(filepath):
        print(f"Brak pliku {filepath}, pomijam...")
        continue

    # Wczytanie trasy
    with open(filepath, 'r') as f:
        route = [int(line.strip()) for line in f.readlines()]

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

    # Zapis obrazka do tego samego folderu (lab1/wyniki/)
    safe_title = title.replace(' ', '_').replace('(', '').replace(')', '').replace('-', '_')
    output_filename = f'lab1/wyniki/wykres_{safe_title}.png'

    plt.savefig(output_filename, dpi=300, bbox_inches='tight')
    print(f"✅ Zapisano wykres: {output_filename}")

    plt.close()

print("Gotowe! Wszystkie wykresy i pliki tras są w folderze lab1/wyniki/")