package de.hbrs.inf.tsp.graphics;

public class Edge {

    public enum EdgeType {
        TRUCK,
        DRONE
    }

    private Node node1;
    private Node node2;
    private EdgeType type;

    public Edge( Node node1, Node node2, EdgeType type ) {
        this.node1 = node1;
        this.node2 = node2;
        this.type = type;
    }

    public Node getNode1() {
        return node1;
    }

    public void setNode1(Node node1) {
        this.node1 = node1;
    }

    public Node getNode2() {
        return node2;
    }

    public void setNode2(Node node2) {
        this.node2 = node2;
    }

    public EdgeType getType() {
        return type;
    }

    public void setType(EdgeType type) {
        this.type = type;
    }
}
