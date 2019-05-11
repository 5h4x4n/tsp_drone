package de.hbrs.inf.tsp;

import gurobi.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;

public class Pdstsp extends TspModel{

	private double truckSpeed;
	private transient int[][] truckTimes;
	private double droneSpeed;
	private double droneFlightTime;
	private int droneFleetSize;
	private transient int[][] droneTimes;
	private int[] droneDeliveryPossible;
	private ArrayList<Integer> droneDeliveryPossibleAndInFlightRange;
	private transient GRBVar[][] grbDronesCustomersVars;
	private double[][] grbDronesCustomersVarsStartValues = null;
	private transient GRBVar grbObjectiveVar;
	private PdstspResult result;

	private static Logger log = Logger.getLogger( Pdstsp.class.getName() );

	public Pdstsp(){
	}

	public Pdstsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances, double truckSpeed,
					int[][] truckTimes, double droneSpeed, double droneFlightTime, int droneFleetSize, int[][] droneTimes,
					int[] droneDeliveryPossible ){
		super( name, comment, type, dimension, nodes, distances );
		this.truckSpeed = truckSpeed;
		this.truckTimes = truckTimes;
		this.droneSpeed = droneSpeed;
		this.droneFlightTime = droneFlightTime;
		this.droneTimes = droneTimes;
		this.droneDeliveryPossible = droneDeliveryPossible;
		this.droneFleetSize = droneFleetSize;
		this.droneDeliveryPossibleAndInFlightRange = new ArrayList<>();
		for( int i : droneDeliveryPossible ){
			if( droneTimes[0][i] <= this.droneFlightTime / 2 ){
				droneDeliveryPossibleAndInFlightRange.add( i );
			}
		}
		this.result = new PdstspResult( name );
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
		toString += "Drone Fleet Size: " + droneFleetSize + "\n";
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
		toString += "Drone Delivery Possible: \n [ ";
		for( int i = 0; i < droneDeliveryPossible.length; i++ ) {
			toString += droneDeliveryPossible[i] + ", ";
		}
		toString = toString.substring( 0, toString.length() - 2 ) + " ]\n";

		return toString;
		*/
	}

	@Override
	protected GRBModel calcGrbModel() throws GRBException{
		log.info( "Start calculation of gurobi model for the PDSTSP without subtour elimination constraints" );

		GRBModel grbModel = new GRBModel( grbEnv );

		//init arrays for decision variables
		grbDronesCustomersVars = new GRBVar[droneFleetSize][dimension];
		grbTruckEdgeVars = new GRBVar[dimension][dimension];

		//add traveltime as helping decision variable and the only var in objective function
		grbObjectiveVar = grbModel.addVar( 0.0, GRB.INFINITY, 1.0, GRB.INTEGER, "traveltime" );
		GRBLinExpr grbLinExpr = new GRBLinExpr();
		grbLinExpr.addTerm( 1.0, grbObjectiveVar );
		grbModel.setObjective( grbLinExpr, GRB.MINIMIZE );
		log.debug( "Add var 'traveltime' as objective function" );

		//Add Truck time constraint as lower bounds for traveltime
		grbLinExpr = new GRBLinExpr();
		StringBuilder logString = new StringBuilder();
		for( int i = 0; i < dimension; i++ ){
			for( int j = i; j < dimension; j++ ){
				if( i != j ){
					logString.append( "trucktimes_" ).append( i ).append( "_" ).append( j ).append( " * x" ).append( i ).append( "_" )
							 .append( j ).append( " + " );
					grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "x" + i + "_" + j );
					grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];
					log.debug( "Add decision var x" + i + "_" + j + " with factor " + truckTimes[i][j] );
					grbLinExpr.addTerm( truckTimes[i][j], grbTruckEdgeVars[i][j] );
				} else {
					//TODO test if nothing goes wrong now
					grbTruckEdgeVars[i][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "x" + i + "_" + i );
				}

			}
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add constraint trucktime: " + logString + " <= traveltime" );
		grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, grbObjectiveVar, "trucktime" );
		calculatedConstraintsCounter++;

		//Add Drone times constraints also as lower bounds for traveltime
		for( int v = 0; v < droneFleetSize; v++ ){
			logString = new StringBuilder();
			grbLinExpr = new GRBLinExpr();
			for( int i = 0; i < dimension; i++ ){
				logString.append( "(dronetimes_0_" ).append( i ).append( " + dronetimes_" ).append( i ).append( "_0) * y" ).append( v ).append( "_" ).append( i )
								.append( " + " );
				if( droneDeliveryPossibleAndInFlightRange.contains( i ) ){
					grbDronesCustomersVars[v][i] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "y" + v + "_" + i );
					log.debug( "Add decision var y" + v + "_" + i );
					grbLinExpr.addTerm( droneTimes[0][i] + droneTimes[i][0], grbDronesCustomersVars[v][i] );
				} else {
					//TODO test if nothing goes wrong now
					grbDronesCustomersVars[v][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "y" + v + "_" + i );
					log.debug( "Add decision var y" + v + "_" + i + " = 0 (" + i + " is not in Flight range)" );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
			log.debug( "Add constraint dronetime_" + v + ": " + logString + " <= traveltime" );
			grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, grbObjectiveVar, "dronetime_" + v );
			calculatedConstraintsCounter++;
		}

		//create constraints that each customer is served by truck or drone exactly once
		//the constraint implicit contains degree-2 constraints for customers
		for( int j = 1; j < dimension; j++ ){
			logString = new StringBuilder( "0.5 * (" );
			grbLinExpr = new GRBLinExpr();
			for( int i = 0; i < dimension; i++ ){
				if( i != j ){
					logString.append( "x" ).append( i ).append( "_" ).append( j ).append( " + " );
					grbLinExpr.addTerm( 0.5, grbTruckEdgeVars[i][j] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) ).append( ") + " );
			if( droneDeliveryPossibleAndInFlightRange.contains( j ) ){
				for( int v = 0; v < droneFleetSize; v++ ){
					logString.append( "y" ).append( v ).append( "_" ).append( j ).append( " + " );
					grbLinExpr.addTerm( 1.0, grbDronesCustomersVars[v][j] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
			log.debug( "Add constraint customer_served_once_" + j + ": " + logString + " = 1" );
			grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "customer_served_once_" + j );
			calculatedConstraintsCounter++;
		}

		//Add degree-2 constraint for depot
		grbLinExpr = new GRBLinExpr();
		logString = new StringBuilder();
		for( int i = 1; i < dimension; i++ ){
			logString.append( "x0_" ).append( i ).append( " + " );
			grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[0][i] );
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add degree-2 constraint for depot: " + logString + " = 2" );
		grbModel.addConstr( grbLinExpr, GRB.EQUAL, 2.0, "deg2_depot" );
		calculatedConstraintsCounter++;

		log.info( "End calculation of gurobi model for the PDSTSP without subtour elimination constraints" );

		return grbModel;
	}

	@Override public boolean presolveHeuristic( Defines.PresolveHeuristicType presolveHeuristicType ){

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
	}

	@Override protected void setStartValues() throws GRBException{
		super.setStartValues();
		if( grbDronesCustomersVarsStartValues != null ) {
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
	}

	@Override
	protected TspModelIterationResult calculateAndAddIterationResult() throws GRBException{
		PdstspIterationResult pdstspIterationResult = new PdstspIterationResult();
		double[][] truckEdgeVars = grbModel.get( GRB.DoubleAttr.X, grbTruckEdgeVars );
		pdstspIterationResult.setTruckTours( findSubtours( truckEdgeVars ) );
		double[][] dronesCustomersVars = grbModel.get( GRB.DoubleAttr.X, grbDronesCustomersVars );
		pdstspIterationResult.setDronesCustomers( findDronesCustomers( dronesCustomersVars ) );
		result.getPdstspIterationResults().add( pdstspIterationResult );

		return pdstspIterationResult;
	}

	private ArrayList<Integer>[] findDronesCustomers( double[][] dronesCustomerVars ) throws GRBException{
		ArrayList<Integer>[] dronesCustomers = new ArrayList[droneFleetSize];
		for( int v = 0; v < droneFleetSize; v++ ){
			dronesCustomers[v] = new ArrayList<>();
			for( int i = 0; i < dimension; i++ ){
				if( (int)(dronesCustomerVars[v][i] + 0.5d) != 0 ){
					dronesCustomers[v].add( i );
				}
			}
		}
		return dronesCustomers;
	}

	protected String logSolution() throws GRBException{
		StringBuilder solutionString = new StringBuilder( super.logSolution() ).append( "\n" );
		double[][] dronesCustomersVars = getSolution( grbDronesCustomersVars );
		ArrayList<Integer>[] dronesCustomers = findDronesCustomers( dronesCustomersVars );

		if( dronesCustomers.length > 0 ){
			for( int v = 0; v < dronesCustomers.length; v++ ){
				solutionString.append( "Drone_" ).append( v ).append( "_Customers_Size: " ).append( dronesCustomers[v].size() ).append( "\n" );
				if( dronesCustomers[v].size() > 0 ){
					solutionString.append( "Drone_" ).append( v ).append( "_Customers: " );
					for( int i = 0; i < dronesCustomers[v].size(); i++ ){
						solutionString.append( dronesCustomers[v].get( i ) ).append( ", " );
					}
					solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 1 ) ).append( "\n" );
				}
			}
			return solutionString.substring( 0, solutionString.length() - 2 );
		}

		return solutionString.toString();
	}

	@Override
	public TspModelResult getResult(){
		return result;
	}

	@Override
	protected void logIterationDebug() throws GRBException{
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
	}

	@Override
	protected boolean addViolatedConstraints() throws GRBException{

		ArrayList<ArrayList<Integer>> subtours = result.getLast().getTruckTours();
		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints" );
			for( ArrayList<Integer> subtour : subtours ){

				if( subtour.contains( 0 ) ){
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
					log.debug( "Add subtour elimination constraint: " + subtourEliminationConstraintString
									.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + ( subtour.size() - 1 ) );
					grbModel.addConstr( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, subtourEliminationConstraintName );
					additionalConstraintsCounter++;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override protected boolean addViolatedLazyConstraints() throws GRBException{

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
									.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + ( subtour.size() - 1 ) );
					addLazy( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1 );
					additionalConstraintsCounter++;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public double getDroneFlightTime(){
		return droneFlightTime;
	}

	public void setDroneFlightTime( int droneFlightTime ){
		this.droneFlightTime = droneFlightTime;
	}

	public int getDroneFleetSize(){
		return droneFleetSize;
	}

	public void setDroneFleetSize( int droneFleetSize ){
		this.droneFleetSize = droneFleetSize;
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

	public ArrayList<Integer> getDroneDeliveryPossibleAndInFlightRange(){
		return droneDeliveryPossibleAndInFlightRange;
	}

	public void setDroneDeliveryPossibleAndInFlightRange( ArrayList<Integer> droneDeliveryPossibleAndInFlightRange ){
		this.droneDeliveryPossibleAndInFlightRange = droneDeliveryPossibleAndInFlightRange;
	}

	public int getDroneDeliveryPossibleAndInFlightRangeCounter(){
		return droneDeliveryPossibleAndInFlightRange.size();
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
		double droneFlightRangePercentage = ( getDroneFlightRange() / getMaximumCustomerDistance() ) * 100.0;
		return Math.round( droneFlightRangePercentage ) / 2.0;
	}


	public double[][] getGrbDronesCustomersVarsStartValues(){
		return grbDronesCustomersVarsStartValues;
	}

	public void setGrbDronesCustomersVarsStartValues( double[][] grbDronesCustomersVarsStartValues ){
		this.grbDronesCustomersVarsStartValues = grbDronesCustomersVarsStartValues;
	}

}
