package de.hbrs.inf.graphics;

public class Node {

    public enum NodeType {
        DEPOT,
        DRONE_DELIVERY_POSSIBLE_BUT_NOT_IN_FLIGHT_RANGE,
        DRONE_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE,
        DRONE_DELIVERY_NOT_POSSIBLE
    }

    private double x;
    private double y;
    private NodeType type;

    public Node( double x, double y, NodeType type ) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }
}
