package de.hbrs.inf;

public class TSPLibJson{

	private String name;
	private String type;
	private String comment;
	private int dimension;
	private double truck_speed;
	private String edge_weight_type;
	private String edge_weight_format;
	private String display_data_type;
	private double[][] node_coordinates;

	public String getName(){
		return name;
	}

	public void setName( String name ){
		this.name = name;
	}

	public String getType(){
		return type;
	}

	public void setType( String type ){
		this.type = type;
	}

	public String getComment(){
		return comment;
	}

	public void setComment( String comment ){
		this.comment = comment;
	}

	public int getDimension(){
		return dimension;
	}

	public void setDimension( int dimension ){
		this.dimension = dimension;
	}

	public double getTruck_speed(){
		return truck_speed;
	}

	public void setTruck_speed( double truck_speed ){
		this.truck_speed = truck_speed;
	}

	public String getEdge_weight_type(){
		return edge_weight_type;
	}

	public void setEdge_weight_type( String edge_weight_type ){
		this.edge_weight_type = edge_weight_type;
	}

	public String getEdge_weight_format(){
		return edge_weight_format;
	}

	public void setEdge_weight_format( String edge_weight_format ){
		this.edge_weight_format = edge_weight_format;
	}

	public String getDisplay_data_type(){
		return display_data_type;
	}

	public void setDisplay_data_type( String display_data_type ){
		this.display_data_type = display_data_type;
	}

	public double[][] getNode_coordinates(){
		return node_coordinates;
	}

	public void setNode_coordinates( double[][] node_coordinates ){
		this.node_coordinates = node_coordinates;
	}

	public String toString() {

		String toString;

		toString = "Name: " + name + "\n";
		toString += "Type: " + type + "\n";
		toString += "Comment: " + comment + "\n";
		toString += "Dimension: " + dimension + "\n";
		toString += "Truck Speed: " + truck_speed + "\n";
		toString += "Edge Weight Type: " + edge_weight_type + "\n";
		toString += "Edge Weight Format: " + edge_weight_format + "\n";
		toString += "Display Data Type: " + display_data_type + "\n";
		toString += "Node Coordinates: \n";
		int i = 0;
		for( double[] nodeCoordinates : node_coordinates ) {
			toString += "\t" + i++ + ": [ " + nodeCoordinates[0] + ", " + nodeCoordinates[1]  + " ]\n";
		}

		return toString;
	}

}
