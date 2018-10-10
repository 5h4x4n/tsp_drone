package de.hbrs.inf.tsp;

import de.hbrs.inf.tsp.json.TspLibJson;
import gurobi.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Stack;

public abstract class TspModel{

	protected String name;
	protected String comment;
	protected String type;
	protected int dimension;
	protected double[][] nodes;
	protected transient int[][] distances;
	protected transient GRBModel grbModel;
	protected transient GRBEnv grbEnv;
	protected transient GRBVar[][] grbTruckEdgeVars;
	protected transient GRBCallback grbCallback;
	protected int additionalConstraintsCounter = 0;
	protected int calculatedConstraintsCounter = 0;
	protected int maxOptimizationSeconds = -1;

	private static final double EARTH_RADIUS = 6378.388;
	protected static Logger log = Logger.getLogger( TspModel.class.getName() );

	public TspModel() {}

	public TspModel( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances ){
		this.name = name;
		this.comment = comment;
		this.type = type;
		this.dimension = dimension;
		this.nodes = nodes;
		this.distances = distances;
	}

	public static double[][] calculateNodes( TspLibJson tspLibJson ){

		if( tspLibJson.getNode_coordinates() != null && tspLibJson.getNode_coordinates().length > 0 ){
			return tspLibJson.getNode_coordinates();
		}

		if( tspLibJson.getDisplay_data() != null && tspLibJson.getDisplay_data().length > 0 ){
			return tspLibJson.getDisplay_data();
		}

		return null;
	}

	public static int[][] calculateTravelDistances( double[][] node_coordinates, int[][] edge_weights, int dimension, String edge_weight_type,
					String edge_weight_format ){

		//calculate travel distances dependent on the distance type
		switch(edge_weight_type){
			case "GEO":{
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

				for(int i = 0; i < dimension; i++){
					for(int j = 0; j < dimension; j++){
						if( i == j ){
							distances[i][j] = 0;
						} else {
							double q1 = Math.cos( longitude[i] - longitude[j] );
							double q2 = Math.cos( latitude[i] - latitude[j] );
							double q3 = Math.cos( latitude[i] + latitude[j] );
							distances[i][j] = (int)( EARTH_RADIUS * Math.acos( 0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3) ) + 1.0 );
						}
					}
				}
				return distances;

			}
			case "EUC_2D":{
				int[][] distances = new int[dimension][dimension];

				for(int i = 0; i < dimension; i++){
					for(int j = 0; j < dimension; j++){
						if( i == j ){
							distances[i][j] = 0;
						} else {
							double deltaX = node_coordinates[i][0] - node_coordinates[j][0];
							double deltaY = node_coordinates[i][1] - node_coordinates[j][1];
							distances[i][j] = (int)(Math.sqrt( deltaX * deltaX + deltaY * deltaY ) + 0.5);
						}
					}
				}
				return distances;

			}
			case "EXPLICIT":
				switch(edge_weight_format){
					case "LOWER_DIAG_ROW":{
						int[][] distances = new int[dimension][dimension];
						for(int i = 0; i < dimension; i++){
							for(int j = i; j < dimension; j++){
								if( i == j ){
									distances[i][j] = 0;
								} else {
									distances[j][i] = edge_weights[j][i];
									distances[i][j] = edge_weights[j][i];
								}
							}
						}
						return distances;
					}
					case "UPPER_ROW":{
						int[][] distances = new int[dimension][dimension];
						for(int i = 0; i < dimension; i++){
							distances[i][i] = 0;
							for(int j = 0; j < dimension - i - 1; j++){
								distances[i][j + i + 1] = edge_weights[i][j];
								distances[j + i + 1][i] = edge_weights[i][j];
							}
						}
						return distances;
					}
					case "FULL_MATRIX":{
						int[][] distances = new int[dimension][dimension];
						for(int i = 0; i < dimension; i++){
							for(int j = 0; j < dimension; j++){
								distances[i][j] = edge_weights[i][j];
							}
						}
						return distances;
					}
					default:
						log.error( "edge_weight_format '" + edge_weight_format + "' not supported." );
						return null;
				}
			default:
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

	public String toString(){
		StringBuilder toString = new StringBuilder( "Nodes: \n" );
		for( double[] node : nodes ){
			toString.append( "[ " ).append( node[0] ).append( ", " ).append( node[1] ).append( " ], \n" );
		}
		toString.append( "Distances: \n" );
		for( int[] distance : distances ){
			for( int j = 0; j < distances[0].length; j++ ){
				toString.append( distance[j] ).append( ", " );
			}
			toString = new StringBuilder( toString.substring( 0, toString.length() - 2 ) ).append( "\n" );
		}

		return toString.toString();
	}

	protected abstract GRBModel calcGrbModel() throws GRBException;

	protected abstract boolean addViolatedConstraints() throws GRBException;

	public TspModelResult grbOptimize(){
		try{
			long runtimeCalcGrbModel = System.nanoTime();
			grbEnv = new GRBEnv();
			grbEnv.set( GRB.IntParam.LogToConsole, 0 );
			grbModel = calcGrbModel();
			runtimeCalcGrbModel = System.nanoTime() - runtimeCalcGrbModel;
			boolean isSolutionOptimal = false;
			int iterationCounter = 1;

			log.info( "Start optimization process" );
			long runtimeOptimization = System.nanoTime();
			grbCallback = new TspGrbCallback( grbModel, runtimeOptimization, maxOptimizationSeconds );
			grbModel.setCallback( grbCallback );

			long currentIterationRuntime;
			do{
				currentIterationRuntime = System.nanoTime();
				log.info( "IterationCounter: " + iterationCounter++ );
				grbModel.optimize();
				int optimizationStatus = grbModel.get( GRB.IntAttr.Status );

				if( optimizationStatus == GRB.Status.INF_OR_UNBD ){
					grbModel.set( GRB.IntParam.Presolve, 0 );
					grbModel.optimize();
					optimizationStatus = grbModel.get( GRB.IntAttr.Status );
				}

				if( optimizationStatus == GRB.Status.OPTIMAL ){
					int objval = (int)(grbModel.get( GRB.DoubleAttr.ObjVal ) + 0.5d);
					log.info( "Found objective: " + objval );

					GRBVar[] vars = grbModel.getVars();
					String[] varNames = grbModel.get( GRB.StringAttr.VarName, vars );
					double[] x = grbModel.get( GRB.DoubleAttr.X, vars );
					StringBuilder solutionString = new StringBuilder();

					for(int i = 0; i < vars.length; i++){
						if( (int)(x[i] + 0.5d) != 0 ){
							solutionString.append( varNames[i] ).append( ", " );
						}
					}
					solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) );
					log.debug( "Decision variables in solution: " + solutionString );

					if( log.isDebugEnabled() ){
						logIterationDebug();
					}
					TspModelIterationResult currentTspIterationResult = calculateAndAddIterationResult();
					currentTspIterationResult.setObjective( objval );

					if( !addViolatedConstraints() ){
						isSolutionOptimal = true;

						currentIterationRuntime = System.nanoTime() - currentIterationRuntime;
						double currentIterationRuntimeSeconds = currentIterationRuntime / 1e9;
						log.info( "Last iteration runtime: " + currentIterationRuntimeSeconds + "s" );

						runtimeOptimization = System.nanoTime() - runtimeOptimization;

						currentTspIterationResult.setIterationRuntime( currentIterationRuntimeSeconds );
						getResult().setRuntime( runtimeOptimization / 1e9 );
						getResult().setRuntimeGrbModelCalculation( runtimeCalcGrbModel / 1e9 );
						getResult().setObjective( objval );
						getResult().setOptimal( true );

						log.info( "Found solution for '" + name + "' with dimension '" + dimension + "' is optimal!" );
						log.info( currentTspIterationResult.getSolutionString() );
						log.info( "Optimal objective: " + objval );

						log.info( "Total optimization runtime: " + getResult().getRuntime() + "s" );
						log.debug( "Runtime of GRB Model calculation: " + getResult().getRuntimeGrbModelCalculation() + "s" );

						//TODO show runtime from parts like finding subtours (also percentage)

					} else {
						currentIterationRuntime = System.nanoTime() - currentIterationRuntime;
						double currentIterationRuntimeSeconds = currentIterationRuntime / 1e9;
						long currentRuntimeOptimization = System.nanoTime() - runtimeOptimization;
						double currentRuntimeOptimizationSeconds = currentRuntimeOptimization / 1e9;

						log.debug( currentTspIterationResult.getSolutionString() );
						log.info( "Last iteration runtime: " + currentIterationRuntimeSeconds + "s" );
						log.info( "Current total optimization runtime: " + currentRuntimeOptimizationSeconds + "s" );

						currentTspIterationResult.setIterationRuntime( currentIterationRuntimeSeconds );
						getResult().setRuntime( currentRuntimeOptimizationSeconds );

					}
				} else if( optimizationStatus == GRB.Status.INTERRUPTED ) {
					log.info( "Optimization process cancelled, cause the runtime exceeds the maximumOptimizationSeconds!" );
					break;
				} else if( optimizationStatus == GRB.Status.INFEASIBLE ){
					//TODO change filename specific for input
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
			} while(!isSolutionOptimal);

			// Dispose of model and environment
			grbModel.dispose();
			grbEnv.dispose();

		} catch(GRBException e){
			log.error( "Error code: " + e.getErrorCode() + ". " + e.getMessage() );
		}

		return getResult();
	}

	protected ArrayList<ArrayList<Integer>> findSubtours() throws GRBException{
		log.debug( "Starting find subtours" );

		ArrayList<ArrayList<Integer>> subtours = new ArrayList<>();
		Stack<Integer> unvisitedVertices = new Stack<>();

		//this prevents to search subtours for vertices which are not currently in the solution
		//needed for e.g. pdstsp
		for( int i = dimension - 1; i >= 0; i-- ){
			for( int j = dimension - 1; j >= 0; j-- ){
				if( i != j ){
					if( ( (int)grbTruckEdgeVars[i][j].get( GRB.DoubleAttr.X ) ) == 1 ){
						unvisitedVertices.add( i );
						break;
					}
				}
			}
		}

		while( !unvisitedVertices.isEmpty() ){
			int currentVertex = unvisitedVertices.pop();
			log.debug( "currentVertex: " + currentVertex );
			log.debug( "unvisitedVertices: " + unvisitedVertices );
			ArrayList<Integer> subtour = new ArrayList<>();
			subtours.add( subtour );
			Stack<Integer> unvisitedVerticesForSubtour = new Stack<>();
			unvisitedVerticesForSubtour.add( currentVertex );
			log.debug( "unvisitedVerticesForSubtour: " + unvisitedVerticesForSubtour );

			while( !unvisitedVerticesForSubtour.isEmpty() ){
				Integer currentSubtourVertex = unvisitedVerticesForSubtour.pop();
				log.debug( "currentSubtourVertex: " + currentSubtourVertex );
				log.debug( "unvisitedVerticesForSubtour: " + unvisitedVerticesForSubtour );
				subtour.add( currentSubtourVertex );
				log.debug( "subtour: " + subtour );
				unvisitedVertices.remove( currentSubtourVertex );
				for(int i = 0; i < dimension; i++){
					if( i != currentSubtourVertex ){
						//log.debug( "Check x" + currentSubtourVertex + "_" + i + " = " + (int) grbTruckEdgeVars[currentSubtourVertex][i].get( GRB.DoubleAttr.X ) );
						if( ( (int)( grbTruckEdgeVars[currentSubtourVertex][i].get( GRB.DoubleAttr.X ) + 0.5d ) ) == 1
										&& !subtour.contains( i )
										&& !unvisitedVerticesForSubtour.contains( i ) ){
							unvisitedVerticesForSubtour.add( i );
							log.debug( "unvisitedVerticesForSubtour: " + unvisitedVerticesForSubtour );
						}
					}
				}
			}
			log.debug( "subtour: " + subtour );
		}
		log.debug( "Ending find subtours" );
		return subtours;

	}

	protected void logIterationDebug() throws GRBException{
		log.debug( "Adjacency matrix of solution:" );
		for(int i = 0; i < dimension; i++){
			StringBuilder rowString = new StringBuilder();
			for(int j = 0; j < dimension; j++){
				if( i == j ){
					rowString.append( "-, " );
				} else {
					rowString.append( (int)grbTruckEdgeVars[i][j].get( GRB.DoubleAttr.X ) ).append( ", " );
				}
			}
			log.debug( rowString.substring( 0, rowString.length() - 2 ) );
		}
	}

	protected ArrayList<int[]> createEdgesForSubtourEliminationConstraint(  ArrayList<Integer> subtour ) {
		ArrayList<int[]> edges = new ArrayList<>();
		for( int i = 0; i < subtour.size() - 1; i++ ) {
			for( int j = i + 1; j < subtour.size(); j++ ) {
				int[] edge = new int[2];
				edge[0] = subtour.get( i );
				edge[1] = subtour.get( j );
				edges.add( edge );

			}
		}
		return edges;
	}

	protected abstract TspModelIterationResult calculateAndAddIterationResult() throws GRBException;

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

	public String getName(){
		return name;
	}

	public String getType(){
		return type;
	}

	public int getDimension(){
		return dimension;
	}

	public GRBModel getGrbModel(){
		return grbModel;
	}

	public int getAdditionalConstraintsCounter(){
		return additionalConstraintsCounter;
	}

	public abstract TspModelResult getResult();

	public int getDecisionVariablesCounter(){
		return grbModel.getVars().length;
	}

	public int getTotalConstraintsCounter() {
		return calculatedConstraintsCounter + additionalConstraintsCounter;
	}

	public void setMaxOptimizationSeconds( int maxOptimizationSeconds ){
		this.maxOptimizationSeconds = maxOptimizationSeconds;
	}
}


