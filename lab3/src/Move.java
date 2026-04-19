package lab3.src;

public class Move implements Comparable<Move> {
    public int type; // 1: Swap, 2: 2-Opt, 3: Add, 4: Remove
    public int node1;
    public int node2;
    public int delta;

    public Move(int type, int node1, int node2, int delta) {
        this.type = type;
        this.node1 = node1;
        this.node2 = node2;
        this.delta = delta;
    }

    @Override
    public int compareTo(Move other) {
        // Sortowanie malejąco (największa delta na początku)
        return Integer.compare(other.delta, this.delta);
    }
}