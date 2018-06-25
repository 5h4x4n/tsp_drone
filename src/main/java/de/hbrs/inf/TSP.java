package de.hbrs.inf;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import gurobi.*;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class TSP{

	String name;
	String comment;
	private double[][] nodes;
	private int[][] distances;
	private double[][] truckTimes;
	private int secCounter = 0;
	private GRBModel grbModel;
	private GRBEnv grbEnv;
	private GRBVar[][] grbVars;
	private static Logger log = Logger.getLogger( TSP.class.getName() );

	public TSP(){
	}

	public TSP( String name, String comment, double[][] nodes, int[][] distances, double[][] truckTimes ){
		this.name = name;
		this.comment = comment;
		this.nodes = nodes;
		this.distances = distances;
		this.truckTimes = truckTimes;
	}

	public static TSP getObjectFromJson( String fileName ){
		TSPLibJson tspLibJson;
		log.info( "Try to read TSPJson file '" + fileName + "' and convert it to corresponding java object." );
		try{
			Gson gson = new Gson();
			JsonReader reader = new JsonReader( new FileReader( fileName ) );
			tspLibJson = gson.fromJson( reader, TSPLibJson.class );
			log.debug( "TSPLibJson successfully read: \n" + tspLibJson );

		} catch(FileNotFoundException e){
			log.error( "File not found '" + fileName + "'." );
			return null;
		} catch(Exception e){
			log.error( "Something went wrong while reading TSPLibJson File! Error message: " + e.getMessage() );
			return null;
		}

		//convert TSPLibJson to TSP object
		double[][] nodes = calculateNodes( tspLibJson );

		log.info( "Calculate distances with edge_weight_type '" + tspLibJson.getEdge_weight_type() + "'." );
		int[][] distances = calculateTravelDistances( tspLibJson.getNode_coordinates(), tspLibJson.getEdge_weights(),
						tspLibJson.getDimension(), tspLibJson.getEdge_weight_type(), tspLibJson.getEdge_weight_format() );

		log.info( "Calculate truckTimes with speed '" + tspLibJson.getTruck_speed() + "'." );
		double[][] truckTimes = calculateTravelTimes( tspLibJson.getTruck_speed(), distances );

		TSP tsp = new TSP( tspLibJson.getName(), tspLibJson.getComment(), nodes, distances, truckTimes );
		log.info( "Created TSP model from JSON file." );
		log.debug( tsp );

		return tsp;
	}

	public static double[][] calculateNodes( TSPLibJson tspLibJson ) {

		int dimension = tspLibJson.getDimension();
		double[][] nodes = new double[dimension][dimension];

		if( tspLibJson.getNode_coordinates() != null && tspLibJson.getNode_coordinates().length > 0 ) {
			return tspLibJson.getNode_coordinates();
		}

		if( tspLibJson.getDisplay_data() != null && tspLibJson.getDisplay_data().length > 0 ) {
			return tspLibJson.getDisplay_data();
		}

		return null;
	}

	public static int[][] calculateTravelDistances( double[][] node_coordinates, int[][] edge_weights, int dimension,
					String edge_weight_type, String edge_weight_format ){

		//calculate travel distances dependent on the distance type
		if( edge_weight_type.equals( "GEO" ) ){
			double[] latitude = new double[dimension];
			double[] longitude = new double[dimension];
			int[][] distances = new int[dimension][dimension];


			for(int i = 0; i < dimension; i++){
				int degX = (int)node_coordinates[i][0];
				int degY = (int)node_coordinates[i][1];
				double minX = node_coordinates[i][0] - degX;
				double minY = node_coordinates[i][1] - degY;
				latitude[i] = Math.PI * (degX + 5.0 * minX / 3.0) / 180.0;
				longitude[i] = Math.PI * (degY + 5.0 * minY / 3.0) / 180.0;
			}

			for( int i = 0; i < dimension; i++ ){
				for( int j = 0; j < dimension; j++ ){
					double q1 = Math.cos( longitude[i] - longitude[j] );
					double q2 = Math.cos( latitude[i] - latitude[j] );
					double q3 = Math.cos( latitude[i] + latitude[j] );
					distances[i][j] = (int) (Defines.EARTH_RADIUS * Math.acos( 0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3) ) + 1.0);
				}
			}
			return distances;

		} else if( edge_weight_type.equals( "EUC_2D" ) ){
			int[][] distances = new int[dimension][dimension];

			for( int i = 0; i < dimension; i++ ){
				for( int j = 0; j < dimension; j++ ){
					double deltaX = node_coordinates[i][0] - node_coordinates[j][0];
					double deltaY = node_coordinates[i][1] - node_coordinates[j][1];
					distances[i][j] = (int)( Math.sqrt( deltaX * deltaX + deltaY * deltaY ) + 0.5 );
				}
			}
			return distances;

		} else if( edge_weight_type.equals( "EXPLICIT" ) ) {
			if( edge_weight_format.equals( "LOWER_DIAG_ROW" ) ) {
				int[][] distances = new int[dimension][dimension];
				for( int i = 0; i < dimension; i++ ){
					for( int j = i; j < dimension; j++ ){
						distances[j][i] = edge_weights[j][i];
						distances[i][j] = edge_weights[j][i];
					}
				}
				return distances;
			} else {
				log.error( "edge_weight_format '" + edge_weight_format + "' not supported." );
				return null;
			}
		} else {
			log.error( "edge_weight_type '" + edge_weight_type + "' not supported." );
			return null;
		}
	}

	public static double[][] calculateTravelTimes( double speed, int[][] distances ){

		int dimension = distances.length;

		//calculate times with distances and speed
		double[][] travelTimes = new double[dimension][dimension];
		for(int i = 0; i < dimension; i++){
			for(int j = 0; j < dimension; j++){
				travelTimes[i][j] = distances[i][j] / speed;
			}
		}

		return travelTimes;
	}

	public static Double[] convertNodeArrayToPoint2dArray( double[][] nodesCoordinates ){
		Double[] nodes = new Double[nodesCoordinates.length];
		for(int i = 0; i < nodes.length; i++){
			nodes[i] = new Double( nodesCoordinates[i][0], nodesCoordinates[i][1] );
		}
		return nodes;
	}

	public String toString(){
		String toString = "Nodes: \n";
		for(int i = 0; i < nodes.length; i++){
			toString += "[ " + nodes[i][0] + ", " + nodes[i][1] + " ], \n";
		}
		toString += "Distances: \n";
		for(int i = 0; i < distances.length; i++){
			for(int j = 0; j < distances[0].length; j++){
				toString += distances[i][j] + ", ";
			}
			toString = toString.substring( 0, toString.length() - 2 ) + "\n";
		}
		toString += "Truck Times: \n";
		for(int i = 0; i < truckTimes.length; i++){
			for(int j = 0; j < truckTimes[0].length; j++){
				toString += truckTimes[i][j] + ", ";
			}
			toString = toString.substring( 0, toString.length() - 2 ) + "\n";
		}

		return toString;
	}

	private void calcGrbModel() throws GRBException{
		log.info( "Start calculation of gurobi model for the TSP without subtour elimination constraints" );

		grbEnv = new GRBEnv();
		grbEnv.set( GRB.IntParam.LogToConsole, 0 );
		grbModel = new GRBModel( grbEnv );

		int n = nodes.length;

		// create decision variables
		grbVars = new GRBVar[n][n];
		//TODO change indexes (begin by 1 to match known solutions)
		for(int i = 0; i < n; i++){
			for(int j = i + 1; j < n; j++){
				log.debug( "Add decision var x" + i + "_" + j + " with factor " + distances[i][j] );
				grbVars[i][j] = grbModel.addVar( 0.0, 1.0, distances[i][j], GRB.BINARY, "x" + String.valueOf( i ) + "_" + String.valueOf( j ) );
				grbVars[j][i] = grbVars[i][j];
			}
		}

		// create degree-2 constraints
		for(int i = 0; i < n; i++){
			GRBLinExpr grbExpr = new GRBLinExpr();
			String logString = "";
			for(int j = 0; j < n; j++){
				if( i != j ){
					logString += "x" + i + "_" + j + " + ";
					grbExpr.addTerm( 1.0, grbVars[i][j] );
				}
			}
			logString = logString.substring( 0, logString.length() - 2 );
			log.debug( "Add degree-2 constraint deg2_" + i + ": " + logString + " = 2" );
			grbModel.addConstr( grbExpr, GRB.EQUAL, 2.0, "deg2_" + String.valueOf( i ) );
		}

		log.info( "End calculation of gurobi model for the TSP without subtour elimination constraints" );
	}

	public void grbOptimize(){
		try{
			long runtimeCalcGrbModel = System.nanoTime();
			calcGrbModel();
			runtimeCalcGrbModel = System.nanoTime() - runtimeCalcGrbModel;
			boolean isSolutionOptimal = false;
			int iterationCounter = 1;

			log.info( "Start optimization process" );
			long runtimeOptimization = System.nanoTime();
			do{
				log.info( "IterationCounter: " + iterationCounter++ );
				grbModel.optimize();
				int optimizationStatus = grbModel.get( GRB.IntAttr.Status );

				//TODO check what this part does
				if( optimizationStatus == GRB.Status.INF_OR_UNBD ){
					grbModel.set( GRB.IntParam.Presolve, 0 );
					grbModel.optimize();
					optimizationStatus = grbModel.get( GRB.IntAttr.Status );
				}

				if( optimizationStatus == GRB.Status.OPTIMAL ){
					double objval = grbModel.get( GRB.DoubleAttr.ObjVal );
					log.info( "Found objective: " + objval );

					GRBVar[] vars = grbModel.getVars();
					String[] varNames = grbModel.get( GRB.StringAttr.VarName, vars );
					double[] x = grbModel.get( GRB.DoubleAttr.X, vars );
					String solutionString = "";
					ArrayList<String> solutionEdges = new ArrayList<String>();
					for(int i = 0; i < vars.length; i++){
						if( x[i] != 0.0 ){
							solutionString += varNames[i] + ", ";
							solutionEdges.add( varNames[i].substring( 1 ) );
						}
					}
					solutionString = solutionString.substring( 0, solutionString.length() - 2 );
					log.debug( "Edges in solution: " + solutionString );

					ArrayList<HashSet<Integer>> subtours = findSubtours( solutionEdges );
					if( subtours.size() > 1 ){
						log.info( "Found subtours: " + subtours.size() );
						log.debug( "Subtours: " + subtours );

						log.info( "Add violated subtour elimination constraints" );
						for( HashSet<Integer> subtour : subtours ){
							double subtourVertexCounter = subtour.size();

							//skip subtours with bigger size than half of the dimension, cause it is not needed
							if( subtourVertexCounter > distances.length / 2 ) {
								log.debug( "Skip subtour cause it's bigger than half the dimension: " + subtour );
								continue;
							}
							ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
							String subtourEliminationConstraintString = "";
							String subtourEliminationConstraintName = "sec_";
							GRBLinExpr grbExpr = new GRBLinExpr();
							for(int[] edge : edges){
								String currentEdgeString = "x" + edge[0] + "_" + edge[1];
								//TODO change?!
								//subtourEliminationConstraintName += currentEdgeString + "-";
								subtourEliminationConstraintString += currentEdgeString + " + ";
								grbExpr.addTerm( 1.0, grbVars[edge[0]][edge[1]] );
							}
							//TODO change?!
							//subtourEliminationConstraintName = subtourEliminationConstraintString
							//				.substring( 0, subtourEliminationConstraintName.length() - 2 );
							subtourEliminationConstraintName += secCounter++;
							log.debug( "Add subtour elimination constraint: " + subtourEliminationConstraintString
											.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + (subtour.size() - 1) );
							grbModel.addConstr( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, subtourEliminationConstraintName );
						}

					} else {
						isSolutionOptimal = true;
						log.info( "Found solution for '" + name + "' is optimal!" );
						log.info( "Optimal objective: " + objval );
						runtimeOptimization = System.nanoTime() - runtimeOptimization;
						double runtimeOptimizationMilliseconds = runtimeOptimization / 1e6;
						double runtimeCalcGrbModelMilliseconds = runtimeCalcGrbModel / 1e6;
						log.info( "Calc GRB Model runtime: " + runtimeCalcGrbModelMilliseconds + "ms" );
						log.info( "Total optimization runtime: " + runtimeOptimizationMilliseconds + "ms" );
						//TODO generate tour form solution edges
						//log.info( "Tour: " + getTourFromSolutionEdges( solutionEdges ));
						//TODO show runtime from starting solve-algorithm and maybe also from parts like finding subtours (also percentage)
					}

				} else if( optimizationStatus == GRB.Status.INFEASIBLE ){
					log.info( "Model is infeasible" );
					// Compute and write out IIS
					grbModel.computeIIS();
					grbModel.write( "model.ilp" );
					break;
				} else if( optimizationStatus == GRB.Status.UNBOUNDED ){
					log.info( "Model is unbounded" );
					break;
				} else {
					log.info( "Optimization was stopped with status = " + optimizationStatus );
					break;
				}
			} while( !isSolutionOptimal );

			// Dispose of model and environment
			grbModel.dispose();
			grbEnv.dispose();

		} catch(GRBException e){
			log.error( "Error code: " + e.getErrorCode() + ". " + e.getMessage() );
		}
	}

	private ArrayList<int[]> createEdgesForSubtourEliminationConstraint( HashSet<Integer> subtour ) {
		ArrayList<Integer> subtourList = new ArrayList<Integer>( subtour );
		ArrayList<int[]> edges = new ArrayList<int[]>();
		for( int i = 0; i < subtourList.size() - 1; i++ ) {
			for( int j = i + 1; j < subtourList.size(); j++ ) {
				int[] edge = new int[2];
				edge[0] = subtourList.get( i );
				edge[1] = subtourList.get( j );
				edges.add( edge );

			}
		}
		return edges;
	}

	public ArrayList<HashSet<Integer>> findSubtours( ArrayList<String> solutionEdges ) {
		log.debug( "Starting find subtours" );
		log.debug( "Solution Edges: " + solutionEdges );

		ArrayList<HashSet<Integer>> subtours = new ArrayList<HashSet<Integer>>();
		Stack<Integer> unhandledVerticesForSubtour = new Stack<Integer>();

		while( !solutionEdges.isEmpty() ) {

			HashSet<Integer> subtour = new HashSet<Integer>();
			subtours.add( subtour );
			String currentEdge = solutionEdges.remove( 0 );
			int[] currentEdgeVertices = getVerticesFromEdge( currentEdge );
			subtour.add( currentEdgeVertices[0] );
			subtour.add( currentEdgeVertices[1] );
			unhandledVerticesForSubtour.push( currentEdgeVertices[0] );
			unhandledVerticesForSubtour.push( currentEdgeVertices[1] );

			while( !unhandledVerticesForSubtour.empty() ) {

				int currentVertex = unhandledVerticesForSubtour.pop();
				ArrayList<Integer> edgesToRemove = new ArrayList<Integer>();
				for( int i = 0; i < solutionEdges.size(); i++ ) {
					currentEdgeVertices = getVerticesFromEdge( solutionEdges.get( i ) );
					if( currentEdgeVertices[0] == currentVertex ){
						subtour.add( currentEdgeVertices[1] );
						unhandledVerticesForSubtour.add( currentEdgeVertices[1] );
						edgesToRemove.add( i );
					} else if( currentEdgeVertices[1] == currentVertex ) {
						subtour.add( currentEdgeVertices[0] );
						unhandledVerticesForSubtour.add( currentEdgeVertices[0] );
						edgesToRemove.add( i );
					}
				}
				//Remove edgesToRemove from solutionEdges
				for( int i : edgesToRemove ) {
					solutionEdges.remove( i );
				}
			}
		}
		log.debug( "Ending find subtours" );
		return subtours;
	}

	private int[] getVerticesFromEdge( String edge ) {
		int[] vertices = new int[2];
		String[] edgeVertices = edge.split( "_" );
		vertices[0] = Integer.parseInt( edgeVertices[0] );
		vertices[1] = Integer.parseInt( edgeVertices[1] );
		return vertices;
	}

	private ArrayList<Integer> getTourFromSolutionEdges( ArrayList<String> solutionEdges ) {
		ArrayList<Integer> tour = new ArrayList<Integer>();
		//TODO implementation
		tour.add( 0 );
		for( int i=0; i < nodes.length; i++ ) {
			int currentNode = tour.get( i );


		}

		return tour;
	}

	public double[][] getNodes(){
		return nodes;
	}

	public void setNodes( double[][] nodes ){
		this.nodes = nodes;
	}

	public int[][] getDistances(){
		return distances;
	}

	public void setDistances( int[][] distances ){
		this.distances = distances;
	}

	public double[][] getTruckTimes(){
		return truckTimes;
	}

	public void setTruckTimes( double[][] truckTimes ){
		this.truckTimes = truckTimes;
	}
}
