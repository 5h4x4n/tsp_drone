package de.hbrs.inf;

public class TspLibJson{

	private String name;
	private String type;
	private String comment;
	private int dimension;
	private String edge_weight_type;
	private String edge_weight_format;
	private int[][] edge_weights;
	private String display_data_type;
	private double[][] node_coordinates;
	private double[][] display_data;

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

	public int[][] getEdge_weights(){
		return edge_weights;
	}

	public void setEdge_weights( int[][] edge_weights ){
		this.edge_weights = edge_weights;
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

	public double[][] getDisplay_data(){
		return display_data;
	}

	public void setDisplay_data( double[][] display_data ){
		this.display_data = display_data;
	}

	public String toString() {

		String toString;

		toString = "Name: " + name + "\n";
		toString += "Type: " + type + "\n";
		toString += "Comment: " + comment + "\n";
		toString += "Dimension: " + dimension + "\n";
		toString += "Edge Weight Type: " + edge_weight_type + "\n";
		toString += "Edge Weight Format: " + edge_weight_format + "\n";
		toString += "Display Data Type: " + display_data_type + "\n";
		if( node_coordinates != null ){
			int i = 1;
			toString += "Node Coordinates: \n";
			for( double[] nodeCoordinates : node_coordinates ){
				toString += "\t" + i++ + ": [ " + nodeCoordinates[0] + ", " + nodeCoordinates[1] + " ]\n";
			}
		}
		if( edge_weights != null ){
			toString += "Edge Weights: \n";
			for( int i = 0; i < edge_weights.length; i++ ){
				for( int j = 0; j < edge_weights[i].length; j++){
					toString += edge_weights[i][j] + ", ";
				}
				toString = toString.substring( 0, toString.length() - 2 );
				toString += "\n";
			}
		}
		return toString;
	}

}
