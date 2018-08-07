package de.hbrs.inf;

import gurobi.*;
import org.apache.log4j.Logger;

public abstract class TspModel{

	protected String name;
	protected String comment;
	protected String type;
	protected int dimension;
	protected double[][] nodes;
	protected int[][] distances;
	protected GRBModel grbModel;
	protected GRBEnv grbEnv;
	protected GRBVar[][] grbTruckEdgeVars;
	protected int additionalConstraintsCounter = 0;

	protected static Logger log = Logger.getLogger( de.hbrs.inf.TspModel.class.getName() );

	public TspModel( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances ){
		this.name = name;
		this.comment = comment;
		this.type = type;
		this.dimension = dimension;
		this.nodes = nodes;
		this.distances = distances;
	}

	public static double[][] calculateNodes( TspLibJson tspLibJson ){

		int dimension = tspLibJson.getDimension();
		double[][] nodes = new double[dimension][dimension];

		if( tspLibJson.getNode_coordinates() != null && tspLibJson.getNode_coordinates().length > 0 ){
			return tspLibJson.getNode_coordinates();
		}

		if( tspLibJson.getDisplay_data() != null && tspLibJson.getDisplay_data().length > 0 ){
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

			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
					if( i == j ) {
						distances[i][j] = 0;
					} else {
						double q1 = Math.cos( longitude[i] - longitude[j] );
						double q2 = Math.cos( latitude[i] - latitude[j] );
						double q3 = Math.cos( latitude[i] + latitude[j] );
						distances[i][j] = (int)(Defines.EARTH_RADIUS * Math.acos( 0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3) ) + 1.0);
					}
				}
			}
			return distances;

		} else if( edge_weight_type.equals( "EUC_2D" ) ){
			int[][] distances = new int[dimension][dimension];

			for(int i = 0; i < dimension; i++){
				for(int j = 0; j < dimension; j++){
					if( i == j ) {
						distances[i][j] = 0;
					} else {
						double deltaX = node_coordinates[i][0] - node_coordinates[j][0];
						double deltaY = node_coordinates[i][1] - node_coordinates[j][1];
						distances[i][j] = (int)(Math.sqrt( deltaX * deltaX + deltaY * deltaY ) + 0.5);
					}
				}
			}
			return distances;

		} else if( edge_weight_type.equals( "EXPLICIT" ) ){
			if( edge_weight_format.equals( "LOWER_DIAG_ROW" ) ){
				int[][] distances = new int[dimension][dimension];
				for(int i = 0; i < dimension; i++){
					for(int j = i; j < dimension; j++){
						if( i == j ) {
							distances[i][j] = 0;
						} else {
							distances[j][i] = edge_weights[j][i];
							distances[i][j] = edge_weights[j][i];
						}
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

		return toString;
	}

	protected abstract GRBModel calcGrbModel() throws GRBException;

	protected abstract boolean addViolatedConstraints() throws GRBException;

	public void grbOptimize(){
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
			long currentIterationRuntime;
			do{
				currentIterationRuntime = System.nanoTime();
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
					int objval = (int) ( grbModel.get( GRB.DoubleAttr.ObjVal ) + 0.5d );
					log.info( "Found objective: " + objval );

					GRBVar[] vars = grbModel.getVars();
					String[] varNames = grbModel.get( GRB.StringAttr.VarName, vars );
					double[] x = grbModel.get( GRB.DoubleAttr.X, vars );
					String solutionString = "";

					for(int i = 0; i < vars.length; i++){
						if( (int)(x[i] + 0.5d) != 0 ){
							solutionString += varNames[i] + ", ";
						}
					}
					solutionString = solutionString.substring( 0, solutionString.length() - 2 );
					log.debug( "Decision variables in solution: " + solutionString );

					if( !addViolatedConstraints() ){
						isSolutionOptimal = true;

						currentIterationRuntime = System.nanoTime() - currentIterationRuntime;
						double cuurentIterationRuntimeMilliseconds = currentIterationRuntime / 1e6;
						log.info( "Last iteration runtime: " + cuurentIterationRuntimeMilliseconds + "ms" );

						log.info( "Found solution for '" + name + "' is optimal!" );
						log.info( "Optimal objective: " + objval );

						runtimeOptimization = System.nanoTime() - runtimeOptimization;
						double runtimeOptimizationMilliseconds = runtimeOptimization / 1e6;
						double runtimeCalcGrbModelMilliseconds = runtimeCalcGrbModel / 1e6;
						log.info( "Total optimization runtime: " + runtimeOptimizationMilliseconds + "ms" );
						log.debug( "Runtime of GRB Model calculation: " + runtimeCalcGrbModelMilliseconds + "ms" );

						//TODO generate solution
						//log.info( "Tour: " + getSolutionString( solution ) );
						//TODO show runtime from starting solve-algorithm and maybe also from parts like finding subtours (also percentage)

						//TODO create and return result object
					} else {
						currentIterationRuntime = System.nanoTime() - currentIterationRuntime;
						double cuurentIterationRuntimeMilliseconds = currentIterationRuntime / 1e6;
						long currentRuntimeOptimization = System.nanoTime() - runtimeOptimization;
						double currentRuntimeOptimizationMilliseconds = currentRuntimeOptimization / 1e6;
						log.info( "Last iteration runtime: " + cuurentIterationRuntimeMilliseconds + "ms" );
						log.info( "Current total optimization runtime: " + currentRuntimeOptimizationMilliseconds + "ms" );
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
			} while(!isSolutionOptimal);

			// Dispose of model and environment
			grbModel.dispose();
			grbEnv.dispose();

		} catch(GRBException e){
			log.error( "Error code: " + e.getErrorCode() + ". " + e.getMessage() );
		}
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
}


