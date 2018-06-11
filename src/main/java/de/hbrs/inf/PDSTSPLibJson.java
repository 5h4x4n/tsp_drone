package de.hbrs.inf;

public class PDSTSPLibJson extends TSPLibJson{

	private double drone_flight_range;
	private int drone_fleet_size;
	private double drone_speed;
	private int[] drone_delivery_possible;

	public double getDrone_flight_range(){
		return drone_flight_range;
	}

	public void setDrone_flight_range( double drone_flight_range ){
		this.drone_flight_range = drone_flight_range;
	}

	public int getDrone_fleet_size(){
		return drone_fleet_size;
	}

	public void setDrone_fleet_size( int drone_fleet_size ){
		this.drone_fleet_size = drone_fleet_size;
	}

	public double getDrone_speed(){
		return drone_speed;
	}

	public void setDrone_speed( double drone_speed ){
		this.drone_speed = drone_speed;
	}


	public int[] getDrone_delivery_possible(){
		return drone_delivery_possible;
	}

	public void setDrone_delivery_possible( int[] drone_delivery_possible ){
		this.drone_delivery_possible = drone_delivery_possible;
	}

	public String toString() {

		String toString;

		toString = "Name: " + getName() + "\n";
		toString += "Type: " + getType() + "\n";
		toString += "Comment: " + getComment() + "\n";
		toString += "Dimension: " + getDimension() + "\n";
		toString += "Drone Flight Range: " + drone_flight_range + "\n";
		toString += "Drone Fleet Size: " + drone_fleet_size + "\n";
		toString += "Drone Speed: " + drone_speed + "\n";
		toString += "Truck Speed: " + getTruck_speed() + "\n";
		toString += "Edge Weight Type: " + getEdge_weight_type() + "\n";
		toString += "Edge Weight Format: " + getEdge_weight_format() + "\n";
		toString += "Display Data Type: " + getDisplay_data_type() + "\n";
		toString += "Node Coordinates: \n";
		int i = 0;
		for( double[] nodeCoordinates : getNode_coordinates() ) {
			toString += "\t" + i++ + ": [ " + nodeCoordinates[0] + ", " + nodeCoordinates[1]  + " ]\n";
		}
		toString += "Drone Delivery Possible: [ ";

		for( int index : drone_delivery_possible ) {
			toString += index + ", ";
		}
		toString = toString.substring( 0, toString.length() - 2 ) + " ]";

		return toString;
	}

}
