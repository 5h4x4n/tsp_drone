package de.hbrs.inf.tsp;

import gurobi.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

public class Fstsp extends TspModel{

	private double truckSpeed;
	private transient int[][] truckTimes;
	private double droneSpeed;
	private double droneFlightTime;
	private transient int[][] droneTimes;
	private int[] droneDeliveryPossible;
	private transient boolean[][][] possibleDroneFlights;
	private int possibleDroneFlightsSize = 0;
	private transient GRBVar[][] grbTruckEdgeWaitVars;
	private transient double[][] grbTruckEdgeWaitVarsStartValues = null;
	private transient GRBVar[][][] grbDroneFlightsVars;
	private transient double[][][] grbDroneFlightsVarsStartValues = null;
	private transient GRBVar grbObjectiveVar;
	private FstspResult result;

	private static Logger log = Logger.getLogger( Fstsp.class.getName() );

	public Fstsp(){
	}

	public Fstsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances, double truckSpeed, int[][] truckTimes, double droneSpeed,
					double droneFlightTime, int[][] droneTimes, int[] droneDeliveryPossible ){
		super( name, comment, type, dimension, nodes, distances );
		this.truckSpeed = truckSpeed;
		this.truckTimes = truckTimes;
		this.droneSpeed = droneSpeed;
		this.droneFlightTime = droneFlightTime;
		this.droneTimes = droneTimes;
		this.droneDeliveryPossible = droneDeliveryPossible;

		// [start][customer][end]
		this.possibleDroneFlights = new boolean[dimension+1][dimension][dimension+1];
		for( int start = 0; start < dimension + 1; start++ ){
			for( int customer : droneDeliveryPossible ){
				for( int end = 0; end < dimension + 1; end++ ){

					if( start != end && start != customer && end != customer ){

						//TODO depot only 0 or add column/row in times arrays
						int tmpStart = start == dimension ? 0 : start;
						int tmpEnd = end == dimension ? 0 : end;

						if( droneTimes[tmpStart][customer] + droneTimes[customer][tmpEnd] <= this.droneFlightTime ){
							possibleDroneFlights[start][customer][end] = true;
							possibleDroneFlightsSize++;
						}

					}
				}
			}
		}

		//TODO Remove debug log?!
		/*
		log.debug( "possibleDroneFlightsSize: " + possibleDroneFlightsSize );
		for( int customer = 1; customer < dimension; customer++ ){
			log.debug( "possibleDroneFlights for customer " + customer + ":" );
			for( int start = 0; start < dimension + 1; start++ ){
				for( int end = 0; end < dimension + 1; end++ ){
					if( start != end && start != customer && end != customer ){
						if( possibleDroneFlights[start][customer][end] ) {
							log.debug( "start: " + start + ", end " + end );
						}
					}
				}
			}
		}
		 */

		this.result = new FstspResult( name );
	}

	public String toString(){
		return super.toString();

		//TODO implement toString
		/*
		String toString = "Nodes: \n";
		for( int i = 0; i < getNodes().length; i++ ) {
			toString += "[ " + getNodes()[i][0] + ", " + getNodes()[i][1] + " ]\n";
		}
		toString += "Drone Flight Time: " + droneFlightTime + "\n";
		toString += "Truck Times: \n";
		for( int i = 0; i < getTruckTimes().length; i++ ){
			for( int j = 0; j < getTruckTimes()[0].length; j++ ){
				toString += getTruckTimes()[i][j] + ", ";
			}
			toString = toString.substring( 0, toString.length() - 2 ) + "\n";
		}
		toString += "Drone Times: \n";
		for( int i = 0; i < droneTimes.length; i++ ){
			for( int j = 0; j < droneTimes[0].length; j++ ){
				toString += droneTimes[i][j] + ", ";
			}
			toString = toString.substring( 0, toString.length() - 2 ) + "\n";
		}
		toString += "Possible Drone Flights: \n;
		...

		return toString;
		*/
	}

	@Override protected GRBModel calcGrbModel() throws GRBException{

		log.info( "Start calculation of gurobi model for the FSTSP without all special constraints" );

		GRBModel grbModel = new GRBModel( grbEnv );

		//init arrays for decision variables
		grbTruckEdgeVars = new GRBVar[dimension+1][dimension+1];
		grbTruckEdgeWaitVars = new GRBVar[dimension+1][dimension+1];
		grbDroneFlightsVars = new GRBVar[dimension+1][dimension][dimension+1];

		GRBLinExpr grbLinExpr;
		StringBuilder logString;

		//create decision variables for the truck edges and the according wait times
		for( int i = 0; i < dimension + 1; i++ ){
			for( int j = i; j < dimension + 1; j++ ){
				if( i == j ){
					grbTruckEdgeVars[i][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "x" + i + "_" + i );
					grbTruckEdgeWaitVars[i][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.CONTINUOUS, "w" + i + "_" + i );
				} else {
					// this is necessary cause the node with dimension as index is also the depot, but as end-node
					int tmpJ = j;
					if( j == dimension ) {
						tmpJ = 0;
					}
					log.debug( "Add decision var x" + i + "_" + j + " with factor " + distances[i][tmpJ] );
					grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, distances[i][tmpJ], GRB.BINARY, "x" + i + "_" + j );
					grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];

					log.debug( "Add decision var w" + i + "_" + j + " with factor 1.0 and upper bound " + droneFlightTime );
					grbTruckEdgeWaitVars[i][j] = grbModel.addVar( 0.0, droneFlightTime, 1.0, GRB.CONTINUOUS, "w" + i + "_" + j );
					grbTruckEdgeWaitVars[j][i] = grbTruckEdgeWaitVars[i][j];
				}
			}
		}

		//create decision variables for the drone flights
		for( int customer = 0; customer < dimension; customer++ ){
			for( int i = 0; i < dimension + 1; i++ ){
				for( int j = i; j < dimension + 1; j++ ){
					if( possibleDroneFlights[i][customer][j] ) {
						log.debug( "Add decision var y" + i + "_" + customer + "_" + j + " with factor 0.0 " );
						grbDroneFlightsVars[i][customer][j] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "y" + i + "_" + customer + "_" + j );
					} else {
						grbDroneFlightsVars[i][customer][j] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "y" + i + "_" + customer + "_" + j );
					}
					grbDroneFlightsVars[j][customer][i] = grbDroneFlightsVars[i][customer][j];
				}
			}
		}

		//create constraints that each customer is served by truck or drone exactly once
		//the constraint implicit contains degree-2 constraints for customers
		for( int customer = 1; customer < dimension; customer++ ) {
			grbLinExpr = new GRBLinExpr();
			logString = new StringBuilder( "0.5 * (" );
			for( int i = 0; i < dimension; i++ ) {
				if( i != customer ){
					logString.append( "x" ).append( i ).append( "_" ).append( customer ).append( " + " );
					grbLinExpr.addTerm( 0.5, grbTruckEdgeVars[i][customer] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) ).append( ") + " );

			for( int start = 0; start < dimension; start++ ) {
				for( int end = 1; end < dimension + 1; end++ ) {
					if( possibleDroneFlights[start][customer][end] ) {
						logString.append( "y" ).append( start ).append( "_" ).append( customer ).append( "_" ).append( end ).append( " + " );
						grbLinExpr.addTerm( 1.0, grbDroneFlightsVars[start][customer][end] );
					}
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );

			log.debug( "Add constraint customer_served_once_" + customer + ": " + logString + " = 1" );
			grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "customer_served_once_" + customer );
			calculatedConstraintsCounter++;
		}

		//create constraints that the truck leaves the start-depot-node
		grbLinExpr = new GRBLinExpr();
		logString = new StringBuilder();
		for( int j = 1; j < dimension + 1; j++ ) {
			logString.append( "x0_" ).append( j ).append( " + " );
			grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[0][j] );
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add constraint truck_leave_depot: " + logString + " = 1" );
		grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "truck_leave_depot" );
		calculatedConstraintsCounter++;

		//create constraints that the truck stops at the end-depot-node
		grbLinExpr = new GRBLinExpr();
		logString = new StringBuilder();
		for( int i = 0; i < dimension; i++ ) {
			logString.append( "x" ).append( i ).append( "_" ).append( dimension ).append( " + " );
			grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[i][dimension] );
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add constraint truck_stop_depot: " + logString + " = 1" );
		grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "truck_stop_depot" );
		calculatedConstraintsCounter++;

		//create constraints that the truck visits start and end nodes of a drone flight
		for( int customer = 1; customer < dimension; customer++ ) {
			for( int i = 0; i < dimension; i++ ) {
				for( int j = 1; j < dimension + 1; j++ ){
					if( possibleDroneFlights[i][customer][j] ){
						grbLinExpr = new GRBLinExpr();
						logString = new StringBuilder();
						GRBLinExpr grbLinExpr2 = new GRBLinExpr();
						StringBuilder logString2 = new StringBuilder();
						for( int h = 0; h < dimension + 1; h++ ) {
							if( i != h ){
								logString.append( "x" ).append( i ).append( "_" ).append( h ).append( " + " );
								grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[i][h] );
							}
							if( j != h ){
								logString2.append( "x" ).append( j ).append( "_" ).append( h ).append( " + " );
								grbLinExpr2.addTerm( 1.0, grbTruckEdgeVars[j][h] );
							}
						}

						logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
						log.debug( "Add constraint truck_visits_drone_nodes: y" + i + "_" + customer + "_" + j + " <= " + logString );
						grbModel.addConstr( grbLinExpr, GRB.GREATER_EQUAL, grbDroneFlightsVars[i][customer][j], "truck_visits_drone_nodes" );
						calculatedConstraintsCounter++;

						logString2 = new StringBuilder( logString2.substring( 0, logString2.length() - 3 ) );
						log.debug( "Add constraint truck_visits_drone_nodes: y" + i + "_" + customer + "_" + j + " <= " + logString2 );
						grbModel.addConstr( grbLinExpr2, GRB.GREATER_EQUAL, grbDroneFlightsVars[i][customer][j], "truck_visits_drone_nodes" );
						calculatedConstraintsCounter++;
					}
				}
			}
		}


		//TODO just for testing - adding constraints - each node max 2 drone edges
		for( int i = 0; i < dimension + 1; i++ ) {
			grbLinExpr = new GRBLinExpr();
			logString = new StringBuilder();
			for( int c = 1; c < dimension; c++ ) {
				for( int j = 0; j < dimension + 1; j++ ) {
					if( i != c && c != j && i != j ) {
						logString.append( "y" ).append( i ).append( "_" ).append( c ).append( "_" ).append( j ).append( " + " );
						grbLinExpr.addTerm( 1.0, grbDroneFlightsVars[i][c][j] );
					}
				}
			}

			int rhs = 2;
			if( i == 0 || i == dimension ) {
				rhs = 1;
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
			log.debug( "Add constraint max_drone_edge_constraint_" + i + ":  rhs >= " + logString );
			grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, rhs, "max_drone_edge_constraint_" );
			calculatedConstraintsCounter++;
		}

		//TODO just for testing



		//TODO Remove debug info
		log.debug( "calculatedConstraintsCounter: " + calculatedConstraintsCounter );

		log.info( "End calculation of gurobi model for the FSTSP without all special constraints" );

		return grbModel;
	}

	@Override public boolean presolveHeuristic( Defines.PresolveHeuristicType presolveHeuristicType ){
/*
		//TODO FSTSP has two variables for the depot (start and end node)
		switch( presolveHeuristicType ){
			case TSP:
				//tsp as heuristic solution
				Tsp tsp = new Tsp( this.name, this.comment, Defines.TSP, this.dimension, this.nodes, this.distances );
				if( tsp.grbOptimize() != null ){
					if( tsp.getResult().isOptimal() ){
						grbTruckEdgeVarsStartValues = tsp.truckEdgeVars.clone();
						log.info( "Set heuristicValue: " + tsp.getResult().getObjective() );
						setHeuristicValue( tsp.getResult().getObjective() / truckSpeed );
						// set the grbDronesCustomerStartValues to 0 (doubles are 0 by default)
						grbDronesCustomersVarsStartValues = new double[droneFleetSize][dimension];
						return true;
					}
				}
				return false;

			default:
				log.info( "PresolveHeuristicType + '" + presolveHeuristicType.toString() + "' not supported for TSP!" );
				return false;
		}

 */
		return false;
	}

	@Override protected void setStartValues() throws GRBException{
		/*
		super.setStartValues();
		if( grbDronesCustomersVarsStartValues != null ){
			log.info( "Set start values for grbDronesCustomersVars!" );
			for( int v = 0; v < droneFleetSize; v++ ){
				for( int i : droneDeliveryPossibleAndInFlightRange ){
					if( grbDronesCustomersVarsStartValues[v][i] >= 0 ){
						log.debug( "Set start value for y" + v + "_" + i + ": " + (int)(grbDronesCustomersVarsStartValues[v][i] + 0.5d) );
						grbDronesCustomersVars[v][i].set( GRB.DoubleAttr.Start, (int)(grbDronesCustomersVarsStartValues[v][i] + 0.5d) );
					} else {
						grbDronesCustomersVars[v][i].set( GRB.DoubleAttr.Start, GRB.UNDEFINED );
					}
				}
			}
		} else {
			log.info( "Could not set start values for grbDronesCustomersVars, because grbDronesCustomersVarsStartValues is null!" );
		}

		 */
	}

	@Override protected TspModelIterationResult calculateAndAddIterationResult() throws GRBException{

		FstspIterationResult fstspIterationResult = new FstspIterationResult();
		double[][] truckEdgeVars = grbModel.get( GRB.DoubleAttr.X, grbTruckEdgeVars );
		fstspIterationResult.setTruckTours( findSubtours( truckEdgeVars ) );
		double[][][] droneFlightsVars = grbModel.get( GRB.DoubleAttr.X, grbDroneFlightsVars );
		fstspIterationResult.setDroneFlights( findDroneFlights( droneFlightsVars ) );
		result.getFstspIterationResults().add( fstspIterationResult );

		return fstspIterationResult;
	}

	private ArrayList<Integer[]> findDroneFlights( double[][][] droneFlightsVars ) throws GRBException{
		ArrayList<Integer[]> droneFlights = new ArrayList<>();
		for( int i = 0; i < droneFlightsVars.length; i++ ) {
			for( int c = 0; c < droneFlightsVars[i].length; c++ ) {
				for( int j = i; j < droneFlightsVars[i][c].length; j++ ) {
					if( (int)(droneFlightsVars[i][c][j] + 0.5d) != 0 ){
						droneFlights.add( new Integer[] { i, c, j } );
					}
				}
			}
		}
		return droneFlights;
	}

	protected ArrayList<ArrayList<Integer>> findSubtours( double[][] edgeVars ){
		log.debug( "Starting find subtours" );

		ArrayList<ArrayList<Integer>> subtours = new ArrayList<>();
		Stack<Integer> unvisitedVertices = new Stack<>();

		//this prevents to search subtours for vertices which are not currently in the solution
		for( int i = dimension; i >= 0; i-- ){
			for( int j = dimension; j >= 0; j-- ){
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
				for( int i = 0; i < dimension + 1; i++ ){
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

	protected String logSolution() throws GRBException{
		StringBuilder solutionString = new StringBuilder( super.logSolution() ).append( "\n" );

		double[][][] droneFlightsVars = new double[dimension+1][dimension][dimension+1];
		for( int i = 0; i < dimension + 1; i++ ) {
			droneFlightsVars[i] = getSolution( grbDroneFlightsVars[i] );
		}

		ArrayList<Integer[]> droneFlights = findDroneFlights( droneFlightsVars );

		if( droneFlights.size() > 0 ){
			solutionString.append( "Drone_Flights_Size" ).append( ": " ).append( droneFlights.size() ).append( "\n" );
			for( int v = 0; v < droneFlights.size(); v++ ){
				if( droneFlights.get( v ).length > 0 ){
					solutionString.append( "Drone_Flight_" ).append( v ).append( ": " );
					for( int i = 0; i < droneFlights.get( v ).length; i++ ){
						solutionString.append( droneFlights.get( v )[i] ).append( ", " );
					}
					solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) ).append( "\n" );
				}
			}
		}
		return solutionString.toString();
	}

	@Override public TspModelResult getResult(){
		return result;
	}

	@Override protected void logIterationDebug() throws GRBException{
		/*
		super.logIterationDebug();
		log.debug( "Drone customer vars of solution:" );
		for( int v = 0; v < droneFleetSize; v++ ){
			StringBuilder rowString = new StringBuilder( "Drone_" ).append( v ).append( " : " );
			for( int i = 1; i < dimension; i++ ){
				if( grbDronesCustomersVars[v][i] == null ){
					rowString.append( "-, " );
				} else {
					rowString.append( (int)grbDronesCustomersVars[v][i].get( GRB.DoubleAttr.X ) ).append( ", " );
				}
			}
			log.debug( rowString.substring( 0, rowString.length() - 2 ) );
		}

		 */
	}

	@Override protected boolean addViolatedConstraints() throws GRBException{
/*
		ArrayList<ArrayList<Integer>> subtours = result.getLast().getTruckTours();
		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints" );
			for( ArrayList<Integer> subtour : subtours ){

				if( subtour.contains( 0 ) ){
				//TODO FSTSP
					log.info( "Skip subtour with depot, cause it is a possible solution for the PDSTSP." );
				} else {
					double subtourVertexCounter = subtour.size();

					ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
					StringBuilder subtourEliminationConstraintString = new StringBuilder();
					String subtourEliminationConstraintName = "sec_";
					GRBLinExpr grbExpr = new GRBLinExpr();
					for( int[] edge : edges ){
						subtourEliminationConstraintString.append( "x" ).append( edge[0] ).append( "_" ).append( edge[1] ).append( " + " );
						grbExpr.addTerm( 1.0, grbTruckEdgeVars[edge[0]][edge[1]] );
					}
					subtourEliminationConstraintName += additionalConstraintsCounter;
					log.debug( "Add subtour elimination constraint: " + subtourEliminationConstraintString.substring( 0, subtourEliminationConstraintString.length() - 2 )
									+ "<= " + (subtour.size() - 1) );
					grbModel.addConstr( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, subtourEliminationConstraintName );
					additionalConstraintsCounter++;
				}
			}
			return true;
		} else {
			return false;
		}

 */
	return false;
	}

	@Override protected boolean addViolatedLazyConstraints() throws GRBException{

		/*
		log.info( "Look for subtours and add lazy constraints." );

		ArrayList<ArrayList<Integer>> subtours = null;
		double[][] truckEdgeVars;

		truckEdgeVars = getSolution( grbTruckEdgeVars );
		subtours = findSubtours( truckEdgeVars );

		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints as lazy constraints" );
			for( ArrayList<Integer> subtour : subtours ){

				if( subtour.contains( 0 ) ){
					log.info( "Skip subtour with depot, cause it is a possible solution for the PDSTSP." );
				} else {
					double subtourVertexCounter = subtour.size();

					ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
					StringBuilder subtourEliminationConstraintString = new StringBuilder();
					GRBLinExpr grbExpr = new GRBLinExpr();
					for( int[] edge : edges ){
						subtourEliminationConstraintString.append( "x" ).append( edge[0] ).append( "_" ).append( edge[1] ).append( " + " );
						grbExpr.addTerm( 1.0, grbTruckEdgeVars[edge[0]][edge[1]] );
					}
					log.debug( "Add (lazy) subtour elimination constraint: " + subtourEliminationConstraintString
									.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + (subtour.size() - 1) );
					addLazy( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1 );
					additionalConstraintsCounter++;
				}
			}
			return true;
		} else {
			return false;
		}

		 */
		return false;
	}

	public double getDroneFlightTime(){
		return droneFlightTime;
	}

	public void setDroneFlightTime( int droneFlightTime ){
		this.droneFlightTime = droneFlightTime;
	}

	public int[][] getDroneTimes(){
		return droneTimes;
	}

	public void setDroneTimes( int[][] droneTimes ){
		this.droneTimes = droneTimes;
	}

	public int[] getDroneDeliveryPossible(){
		return droneDeliveryPossible;
	}

	public void setDroneDeliveryPossible( int[] droneDeliveryPossible ){
		this.droneDeliveryPossible = droneDeliveryPossible;
	}

	public double getTruckSpeed(){
		return truckSpeed;
	}

	public void setTruckSpeed( double truckSpeed ){
		this.truckSpeed = truckSpeed;
	}

	public int[][] getTruckTimes(){
		return truckTimes;
	}

	public void setTruckTimes( int[][] truckTimes ){
		this.truckTimes = truckTimes;
	}

	public double getDroneSpeed(){
		return droneSpeed;
	}

	public void setDroneSpeed( int droneSpeed ){
		this.droneSpeed = droneSpeed;
	}

	public void setDroneSpeed( double droneSpeed ){
		this.droneSpeed = droneSpeed;
	}

	public void setDroneFlightTime( double droneFlightTime ){
		this.droneFlightTime = droneFlightTime;
	}

	public boolean[][][] getPossibleDroneFlights(){
		return possibleDroneFlights;
	}

	public void setPossibleDroneFlights( boolean[][][] possibleDroneFlights ){
		this.possibleDroneFlights = possibleDroneFlights;
	}

	public double getDroneFlightRange(){
		return droneFlightTime * droneSpeed;
	}

	public double getMaximumCustomerDistance(){
		double maxDistance = -1;
		for( int i = 1; i < dimension; i++ ){
			if( distances[0][i] > maxDistance ){
				maxDistance = distances[0][i];
			}
		}
		return maxDistance;
	}

	public double getDroneFlightRangePercentage(){
		double droneFlightRangePercentage = (getDroneFlightRange() / getMaximumCustomerDistance()) * 100.0;
		return Math.round( droneFlightRangePercentage ) / 2.0;
	}

	public double[][][] getGrbDroneFlightsVarsStartValues(){
		return grbDroneFlightsVarsStartValues;
	}

	public void setGrbDronesCustomersVarsStartValues( double[][][] grbDroneFlightsVarsStartValues ){
		this.grbDroneFlightsVarsStartValues = grbDroneFlightsVarsStartValues;
	}

	public int getPossibleDroneFlightsSize(){
		return this.possibleDroneFlightsSize;
	}
}
