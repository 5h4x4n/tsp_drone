package de.hbrs.inf.tsp;

import de.hbrs.inf.tsp.json.TspLibJson;
import gurobi.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Stack;

public abstract class TspModel extends GRBCallback{

	protected String name;
	protected String comment;
	protected String testDescription = "";
	protected String type;
	protected int dimension;
	protected double[][] nodes;
	protected int[][] distances;
	protected transient GRBModel grbModel;
	protected transient GRBEnv grbEnv;
	protected transient GRBVar[][] grbTruckEdgeVars;
	protected transient double[][] truckEdgeVars;
	protected transient double[][] grbTruckEdgeVarsStartValues = null;
	protected int additionalConstraintsCounter = 0;
	protected int calculatedConstraintsCounter = 0;
	protected int maxOptimizationSeconds = -1;
	protected transient long startOptimizationTime = -1;
	protected boolean isLazyActive = true;
	protected boolean isGurobiHeuristicsActive = true;
	protected Defines.PresolveHeuristicType presolveHeuristicType = Defines.PresolveHeuristicType.NONE;
	protected String hostname;
	protected int threadCount = 0;
	protected double heuristicValue = -1.0;
	protected int errorCode = 0;

	private final transient static Object lock = new Object();
	private transient final static double currentBestObjectiveSynced_DEFAULT_VALUE = 999999999;
	protected transient static double currentBestObjectiveSynced = currentBestObjectiveSynced_DEFAULT_VALUE;

	private static final double EARTH_RADIUS = 6378.388;
	protected static Logger log = Logger.getLogger( TspModel.class.getName() );

	public TspModel(){
	}

	public TspModel( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances ){
		this.name = name;
		this.comment = comment;
		this.type = type;
		this.dimension = dimension;
		this.nodes = nodes;
		this.distances = distances;
		setDefaultValueForCurrentBestObjectiveSynced();
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

	public static int[][] calculateTravelDistances( double[][] node_coordinates, int[][] edge_weights, int dimension,
					String edge_weight_type, String edge_weight_format ){

		//calculate travel distances dependent on the distance type
		switch( edge_weight_type ){
			case "GEO":{
				double[] latitude = new double[dimension];
				double[] longitude = new double[dimension];
				int[][] distances = new int[dimension][dimension];

				for( int i = 0; i < dimension; i++ ){
					int degX = (int)node_coordinates[i][0];
					int degY = (int)node_coordinates[i][1];
					double minX = node_coordinates[i][0] - degX;
					double minY = node_coordinates[i][1] - degY;
					latitude[i] = Math.PI * ( degX + 5.0 * minX / 3.0 ) / 180.0;
					longitude[i] = Math.PI * ( degY + 5.0 * minY / 3.0 ) / 180.0;
				}

				for( int i = 0; i < dimension; i++ ){
					for( int j = 0; j < dimension; j++ ){
						if( i == j ){
							distances[i][j] = 0;
						} else {
							double q1 = Math.cos( longitude[i] - longitude[j] );
							double q2 = Math.cos( latitude[i] - latitude[j] );
							double q3 = Math.cos( latitude[i] + latitude[j] );
							distances[i][j] = (int)( EARTH_RADIUS * Math.acos( 0.5 * ( ( 1.0 + q1 ) * q2 - ( 1.0 - q1 ) * q3 ) ) + 1.0 );
						}
					}
				}
				return distances;

			}
			case "EUC_2D":{
				int[][] distances = new int[dimension][dimension];

				for( int i = 0; i < dimension; i++ ){
					for( int j = 0; j < dimension; j++ ){
						if( i == j ){
							distances[i][j] = 0;
						} else {
							double deltaX = node_coordinates[i][0] - node_coordinates[j][0];
							double deltaY = node_coordinates[i][1] - node_coordinates[j][1];
							distances[i][j] = (int)( Math.sqrt( deltaX * deltaX + deltaY * deltaY ) + 0.5 );
						}
					}
				}
				return distances;

			}
			case "EXPLICIT":
				switch( edge_weight_format ){
					case "LOWER_DIAG_ROW":{
						int[][] distances = new int[dimension][dimension];
						for( int i = 0; i < dimension; i++ ){
							for( int j = i; j < dimension; j++ ){
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
						for( int i = 0; i < dimension; i++ ){
							distances[i][i] = 0;
							for( int j = 0; j < dimension - i - 1; j++ ){
								distances[i][j + i + 1] = edge_weights[i][j];
								distances[j + i + 1][i] = edge_weights[i][j];
							}
						}
						return distances;
					}
					case "FULL_MATRIX":{
						int[][] distances = new int[dimension][dimension];
						for( int i = 0; i < dimension; i++ ){
							for( int j = 0; j < dimension; j++ ){
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

	public static int[][] calculateTravelTimes( double speed, int[][] distances ){

		int dimension = distances.length;

		//calculate times with distances and speed
		int[][] travelTimes = new int[dimension][dimension];
		for( int i = 0; i < dimension; i++ ){
			for( int j = 0; j < dimension; j++ ){
				travelTimes[i][j] = (int)( ( distances[i][j] / speed ) + 0.5d );
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

	protected void setStartValues() throws GRBException{
		if( grbTruckEdgeVarsStartValues != null ) {
			log.info( "Set start values for grbTruckEdgeVars!" );
			for( int i = 0; i < dimension; i++ ){
				for( int j = i; j < dimension; j++ ){
					if( grbTruckEdgeVarsStartValues[i][j] >= 0 ){
						log.debug( "Set start value for x" + i + "_" + j + ": " + (int)(grbTruckEdgeVarsStartValues[i][j] + 0.5d) );
						grbTruckEdgeVars[i][j].set( GRB.DoubleAttr.Start, (int)(grbTruckEdgeVarsStartValues[i][j] + 0.5d) );
					} else {
						grbTruckEdgeVars[i][j].set( GRB.DoubleAttr.Start, GRB.UNDEFINED );
					}
				}
			}
		} else {
			log.info( "Could not set start values for grbTruckEdgeVars, because grbTruckEdgeVarsStartValues is null!" );
		}
	}

	public abstract boolean presolveHeuristic( Defines.PresolveHeuristicType presolveHeuristicType );

	public TspModelResult grbOptimize(){
		try{
			long runtimeCalcGrbModel = System.nanoTime();
			grbEnv = new GRBEnv();
			grbEnv.set( GRB.IntParam.LogToConsole, 1 );
			if( log.isDebugEnabled() ){
				grbEnv.set( GRB.StringParam.LogFile, "out.log" );
			}

			if( !isGurobiHeuristicsActive ){
				grbEnv.set( GRB.DoubleParam.Heuristics, 0.0 );
			}

			grbModel = calcGrbModel();

			if( isLazyActive ){
				grbModel.set( GRB.IntParam.LazyConstraints, 1 );
			}
			grbModel.set( GRB.IntParam.Threads, threadCount );

			if( maxOptimizationSeconds > 0 ){
				grbModel.set( GRB.DoubleParam.TimeLimit, maxOptimizationSeconds );
			}

			runtimeCalcGrbModel = System.nanoTime() - runtimeCalcGrbModel;
			getResult().setRuntimeGrbModelCalculation( runtimeCalcGrbModel / 1e9 );

			long runtimePresolveHeuristic = 0;
			if( presolveHeuristicType != Defines.PresolveHeuristicType.NONE ){
				runtimePresolveHeuristic = System.nanoTime();
				log.info( "Start presolve process with heuristic (" + presolveHeuristicType + ")!" );
				if( !presolveHeuristic( presolveHeuristicType ) ){
					log.info( "Something went wrong in the presolve heuristic calculation!" );
					return null;
				}
				setStartValues();
				runtimePresolveHeuristic = System.nanoTime() - runtimePresolveHeuristic;
				log.info( "End presolve process with heuristic!" );
			}
			getResult().setRuntimePresolveHeuristic( runtimePresolveHeuristic / 1e9 );

			log.info( "Start optimization process" );
			boolean isSolutionOptimal = false;
			int iterationCounter = 1;
			long runtimeOptimization = System.nanoTime();
			startOptimizationTime = runtimeOptimization;
			grbModel.setCallback( this );

			long currentIterationRuntime;
			do{
				currentIterationRuntime = System.nanoTime();
				/* TODO Add option for resetting grb model
				if( iterationCounter > 1 ) {
					grbModel.reset();
					log.info( "Reset old optimization status/infos for clean optimization without warm start!" );
				}
				*/
				log.info( "IterationCounter: " + iterationCounter++ );
				grbModel.optimize();
				int optimizationStatus = grbModel.get( GRB.IntAttr.Status );

				if( optimizationStatus == GRB.Status.INF_OR_UNBD ){
					grbModel.set( GRB.IntParam.Presolve, 0 );
					grbModel.optimize();
					optimizationStatus = grbModel.get( GRB.IntAttr.Status );
				}

				if( optimizationStatus == GRB.Status.OPTIMAL ){
					double objval = (int)( grbModel.get( GRB.DoubleAttr.ObjVal ) + 0.5d );
					log.info( "Found objective: " + objval );

					GRBVar[] vars = grbModel.getVars();
					String[] varNames = grbModel.get( GRB.StringAttr.VarName, vars );
					double[] x = grbModel.get( GRB.DoubleAttr.X, vars );
					StringBuilder solutionString = new StringBuilder();
					StringBuilder solutionWaitTimesString = new StringBuilder();

					for( int i = 0; i < vars.length; i++ ){
						if( !varNames[i].contains( "w" ) && (int)(x[i] + 0.5d) != 0 ){
							solutionString.append( varNames[i] ).append( ", " );
						}
						if( varNames[i].contains( "w" ) && (int)(x[i] + 0.5d) != 0 ){
							solutionWaitTimesString.append( varNames[i] ).append( " = " ).append( (int)(x[i] + 0.5d) ).append( ", " );
						}
					}
					solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) );
					log.info( "Decision variables for edges in solution: " + solutionString );

					if( solutionWaitTimesString.length() > 0 ){
						solutionWaitTimesString = new StringBuilder( solutionWaitTimesString.substring( 0, solutionWaitTimesString.length() - 2 ) );
						log.info( "Wait times in solution: " + solutionWaitTimesString );
					}

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
						getResult().setRuntimeOptimization( runtimeOptimization / 1e9 );
						getResult().setObjective( objval );
						getResult().setObjectiveBound( objval );
						getResult().setOptimal( true );

						log.info( "Found solution for '" + name + "' with dimension '" + dimension + "' is optimal!" );
						log.info( currentTspIterationResult.getSolutionString() );
						log.info( "Optimal objective: " + objval );

						log.info( "Runtime (total): " + getResult().getRuntimeTotal() + "s" );
						log.info( "Runtime of GRB Model calculation: " + getResult().getRuntimeGrbModelCalculation() + "s" );
						log.info( "Runtime of Presolve Heuristic: " + getResult().getRuntimePresolveHeuristic() + "s" );
						log.info( "Runtime of Optimization: " + getResult().getRuntimeOptimization() + "s" );

						if( log.isDebugEnabled() ){
							grbModel.write( "out.lp" );
							grbModel.write( "out.mps" );
							grbModel.write( "out.sol" );
						}

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
						getResult().setRuntimeOptimization( currentRuntimeOptimizationSeconds );

					}
				} else if( optimizationStatus == GRB.Status.INTERRUPTED ){
					log.info( "Optimization process interrupted, cause of an error!" );
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
				} else if( optimizationStatus == GRB.Status.TIME_LIMIT ){
					log.info( "Optimization process cancelled, cause the runtime exceeds the maximumOptimizationSeconds!" );
					break;
				} else {
					log.info( "Optimization was stopped with status = " + optimizationStatus );
					break;
				}
			} while( !isSolutionOptimal );

			// Dispose of model and environment
			grbModel.dispose();
			grbEnv.dispose();

		} catch( GRBException e ){
			e.printStackTrace();
			errorCode = e.getErrorCode();
			log.error( "Error code: " + e.getErrorCode() + ". " + e.getMessage() );
		}

		setDefaultValueForCurrentBestObjectiveSynced();
		return getResult();
	}

	@Override
	protected void callback(){

		//log.debug( "#Test - where: " + where );

		try{
			if( where == GRB.CB_MIP ){
				double currentRuntimeSeconds = (System.nanoTime() - startOptimizationTime) / 1e9;
				getResult().setRuntimeOptimization( currentRuntimeSeconds );

			} else if( where == GRB.CB_MIPSOL ){
				log.debug( "MIPSOL Callback called." );
				//grbModel.getEnv().message( "GRB.CB_MIPSOL" );
				if( isLazyActive ){

					log.info( "MIP Solution found." );

					double objValue = (int)( getDoubleInfo( GRB.CB_MIPSOL_OBJ ) + 0.5d );
					double bestObjValue = (int)( getDoubleInfo( GRB.CB_MIPSOL_OBJBST ) + 0.5d );
					double bestObjBound = getDoubleInfo( GRB.CB_MIPSOL_OBJBND );
					double exploredNodeCount = getDoubleInfo( GRB.CB_MIPSOL_NODCNT );
					int feasableSolutionsFoundCount = getIntInfo( GRB.CB_MIPSOL_SOLCNT );

					log.info( "Objective value for new solution: " + objValue );
					log.info( "Current best objective: " + bestObjValue );
					log.info( "Current best objective (synced): " + getCurrentBestObjectiveSynced() );
					log.info( "Current best objective bound: " + bestObjBound );
					log.info( "Current explored node count: " + exploredNodeCount );
					log.info( "Current count of feasible solutions found: " + feasableSolutionsFoundCount );

					getResult().setObjectiveBound( bestObjBound );
					double currentRuntimeSeconds = (System.nanoTime() - startOptimizationTime) / 1e9;
					getResult().setRuntimeOptimization( currentRuntimeSeconds );

					//TODO Remove debug message here?!
					if( objValue > getCurrentBestObjectiveSynced() ){
						log.info( "############# WARNING: objValue > currentBestObjectiveSynced ###############" );
					}

					//only add lazy constraints if current objective value is lower-equals than the given heuristic (maybe optimal) value
					//cause an other branch will find a better solution or the according solution for the given value
					if( heuristicValue <= 0.0 || objValue <= heuristicValue ){
						if( !addViolatedLazyConstraints() ){
							synchronized( lock ){
								log.info( "No violated constraints found! Current solution is feasible!" );
								if( objValue < getCurrentBestObjectiveSynced() ){
									setCurrentBestObjectiveSynced( objValue );
									getResult().setObjective( objValue );
									//TODO getSolution and add it as iterationResult?!
									log.info( "New best feasible solution found (objective: " + objValue + ")." );
									log.info( logSolution() );
								} else {
									log.info( "Current solution is feasible, but no new best one!" );
								}
							}
						} else {
							log.info( "Added violated lazy constraints!" );
						}
					} else {
						log.info( "Do not look for violated constraints here, cause current solution " + objValue + " is higher than heuristic value " + heuristicValue );
					}
				}
			}

		} catch( GRBException e ){
			e.printStackTrace();
			errorCode = e.getErrorCode();
			log.error( "GRBException while looking for violated constraints and adding lazy constraints in MIPSOL callback! Error Code: " + errorCode + ", Message: " + e
							.getMessage() );
			grbModel.terminate();
		}
	}

	protected String logSolution() throws GRBException{
		ArrayList<ArrayList<Integer>> truckTours = findSubtours( getSolution( grbTruckEdgeVars ) );
		StringBuilder solutionString = new StringBuilder( "\nTruck_Tours_Size: " ).append( truckTours.size() );
		if( truckTours.size() > 0 ){
			for( int i = 0; i < truckTours.size(); i++ ){
				solutionString.append( "\nTruck_Tour_" ).append( i ).append( "_Size: " ).append( truckTours.get( i ).size() );
				solutionString.append( "\nTruck_Tour_" ).append( i ).append( ": " );
				for( int j = 0; j < truckTours.get( i ).size(); j++ ){
					solutionString.append( truckTours.get( i ).get( j ) ).append( ", " );
				}
				solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) );
			}
		}
		return solutionString.toString();
	}

	protected abstract boolean addViolatedLazyConstraints() throws GRBException;

	protected ArrayList<ArrayList<Integer>> findSubtours( double[][] edgeVars ){
		log.debug( "Starting find subtours" );

		ArrayList<ArrayList<Integer>> subtours = new ArrayList<>();
		Stack<Integer> unvisitedVertices = new Stack<>();

		//this prevents to search subtours for vertices which are not currently in the solution
		//needed for e.g. pdstsp
		for( int i = dimension - 1; i >= 0; i-- ){
			for( int j = dimension - 1; j >= 0; j-- ){
				if( i != j ){
					if( ( (int)( edgeVars[i][j] + 0.5d ) ) != 0 ){
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
				for( int i = 0; i < dimension; i++ ){
					if( i != currentSubtourVertex ){
						//log.debug( "Check x" + currentSubtourVertex + "_" + i + " = " + (int) grbTruckEdgeVars[currentSubtourVertex][i].get( GRB.DoubleAttr.X ) );
						if( ( (int)( edgeVars[currentSubtourVertex][i] + 0.5d ) ) != 0 && !subtour.contains( i )
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
		for( int i = 0; i < dimension; i++ ){
			StringBuilder rowString = new StringBuilder();
			for( int j = 0; j < dimension; j++ ){
				if( i == j ){
					rowString.append( "-, " );
				} else {
					rowString.append( (int)grbTruckEdgeVars[i][j].get( GRB.DoubleAttr.X ) ).append( ", " );
				}
			}
			log.debug( rowString.substring( 0, rowString.length() - 2 ) );
		}
	}

	protected ArrayList<int[]> createEdgesForSubtourEliminationConstraint( ArrayList<Integer> subtour ){
		ArrayList<int[]> edges = new ArrayList<>();
		for( int i = 0; i < subtour.size() - 1; i++ ){
			for( int j = i + 1; j < subtour.size(); j++ ){
				int[] edge = new int[2];
				edge[0] = subtour.get( i );
				edge[1] = subtour.get( j );
				edges.add( edge );

			}
		}
		return edges;
	}

	protected void log2DimIntArray( int[][] array, String title ){
		/*
		StringBuilder outputString = new StringBuilder( "\n" ).append( title ).append( ":\n" );
		for( int i = 0; i < dimension; i++ ){
			StringBuilder rowString = new StringBuilder();
			for( int j = 0; j < dimension; j++ ){
				if( i == j ){
					rowString.append( "-, " );
				} else {
					rowString.append( array[i][j] ).append( ", " );
				}
			}
			outputString.append( rowString.substring( 0, rowString.length() - 2 ) ).append( "\n" );
		}
		log.debug( outputString );
		 */

		System.out.println( title + ":" );
		for( int row = 0; row < array.length; row++ ){
			for( int col = 0; col < array[row].length; col++ ){
				System.out.printf( "%6d", array[row][col] );
			}
			System.out.println();
		}
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

	public int getTotalConstraintsCounter(){
		return calculatedConstraintsCounter + additionalConstraintsCounter;
	}

	public void setMaxOptimizationSeconds( int maxOptimizationSeconds ){
		this.maxOptimizationSeconds = maxOptimizationSeconds;
	}

	public int getMaxOptimizationSeconds(){
		return maxOptimizationSeconds;
	}

	public boolean isLazyActive(){
		return isLazyActive;
	}

	public void setLazyActive( boolean lazyActive ){
		isLazyActive = lazyActive;
	}

	public String getHostname(){
		return hostname;
	}

	public void setHostname( String hostname ){
		this.hostname = hostname;
	}

	public int getThreadCount(){
		return threadCount;
	}

	public void setThreadCount( int threadCount ){
		this.threadCount = threadCount;
	}

	public double getHeuristicValue(){
		return heuristicValue;
	}

	public void setHeuristicValue( double heuristicValue ){
		this.heuristicValue = heuristicValue;
		getResult().setUsedHeuristicValue( heuristicValue );
	}

	public int getErrorCode(){
		return errorCode;
	}

	public void setErrorCode( int errorCode ){
		this.errorCode = errorCode;
	}

	public String getTestDescription(){
		return testDescription;
	}

	public void setTestDescription( String testDescription ){
		this.testDescription = testDescription;
	}

	public Defines.PresolveHeuristicType getPresolveHeuristicType(){
		return presolveHeuristicType;
	}

	public void setPresolveHeuristicType( Defines.PresolveHeuristicType presolveHeuristicType ){
		this.presolveHeuristicType = presolveHeuristicType;
	}

	public double[][] getGrbTruckEdgeVarsStartValues(){
		return grbTruckEdgeVarsStartValues;
	}

	public void setGrbTruckEdgeVarsStartValues( double[][] grbTruckEdgeVarsStartValues ){
		this.grbTruckEdgeVarsStartValues = grbTruckEdgeVarsStartValues;
	}

	public static void setCurrentBestObjectiveSynced( double value ){
		synchronized( lock ){
			if( value < currentBestObjectiveSynced ){
				currentBestObjectiveSynced = value;
			}
		}
	}

	public static double getCurrentBestObjectiveSynced(){
		synchronized( lock ){
			return currentBestObjectiveSynced;
		}
	}

	public static void setDefaultValueForCurrentBestObjectiveSynced(){
		synchronized( lock ){
			currentBestObjectiveSynced = currentBestObjectiveSynced_DEFAULT_VALUE;
		}
	}

	public boolean isGurobiHeuristicsActive(){
		return isGurobiHeuristicsActive;
	}

	public void setGurobiHeuristicsActive( boolean gurobiHeuristicsActive ){
		isGurobiHeuristicsActive = gurobiHeuristicsActive;
	}
}


