package org.shawn.games.Serendipity.Search;

public class Limits {
    private long time;
    private long increment;
    private int movesToGo;

    private long nodes;
    private int depth;

    public Limits() {
        this(100, 0, 0, -1, 256);
    }

    public Limits(long time, long increment, int movesToGo, long nodes, int depth) {
        this.time = time;
        this.increment = increment;
        this.movesToGo = movesToGo;
        this.nodes = nodes;
        this.depth = depth;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getIncrement() {
        return increment;
    }

    public void setIncrement(long increment) {
        this.increment = increment;
    }

    public int getMovesToGo() {
        return movesToGo;
    }

    public void setMovesToGo(int movesToGo) {
        this.movesToGo = movesToGo;
    }

    public long getNodes() {
        return nodes;
    }

    public void setNodes(long nodes) {
        this.nodes = nodes;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Limits clone() {
        return new Limits(time, increment, movesToGo, nodes, depth);
    }
}
