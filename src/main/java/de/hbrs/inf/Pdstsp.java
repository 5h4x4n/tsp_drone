package de.hbrs.inf;

import gurobi.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class Pdstsp extends Tsp {

	private double truckSpeed;
	private double[][] truckTimes;
	private double droneSpeed;
	private double droneFlightTime;
	private int droneFleetSize;
	private double[][] droneTimes;
	private int[] droneDeliveryPossible;
	private ArrayList<Integer> droneDeliveryPossibleAndInFlightRange;
	private GRBVar[][] grbDronesCustomersVars;
	private GRBVar grbObjectiveVar;

	private static Logger log = Logger.getLogger( Pdstsp.class.getName() );

	public Pdstsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances, double truckSpeed,
					double[][] truckTimes, double droneSpeed, double droneFlightTime, int droneFleetSize, double[][] droneTimes,
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
		for( int i : droneDeliveryPossible ) {
			if( droneTimes[0][i] <= this.droneFlightTime / 2 ) {
				droneDeliveryPossibleAndInFlightRange.add( i );
			}
		}
	}

	public String toString() {
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
		grbLinExpr.addTerm(1.0, grbObjectiveVar );
		grbModel.setObjective( grbLinExpr, GRB.MINIMIZE );
		log.debug( "Add var 'traveltime' as objective function" );

		//Add Truck time constraint as lower bounds for traveltime
		grbLinExpr = new GRBLinExpr();
		StringBuilder logString = new StringBuilder(  );
		for( int i = 0; i < dimension; i++ ) {
			for( int j = i + 1; j < dimension; j++ ) {
				if( i != j ) {
					logString.append( "trucktimes_" ).append( i ).append( "_" ).append( j ).append( " * x" ).append( i ).append( "_" )
									.append( j ).append( " + " );
					grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "x" + i + "_" + j );
					grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];
					log.debug( "Add decision var x" + i + "_" + j + " with factor " + truckTimes[i][j] );
					grbLinExpr.addTerm( truckTimes[i][j], grbTruckEdgeVars[i][j] );
				}

			}
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add constraint trucktime: " + logString + " <= traveltime" );
		grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, grbObjectiveVar, "trucktime" );

		//Add Drone times constraints also as lower bounds for traveltime
		if( droneDeliveryPossibleAndInFlightRange.size() > 0 ){
			for(int v = 0; v < droneFleetSize; v++){
				logString = new StringBuilder();
				grbLinExpr = new GRBLinExpr();
				for(int i : droneDeliveryPossibleAndInFlightRange){
					logString.append( "(dronetimes_0_" ).append( i ).append( " + dronetimes_" ).append( i ).append( "_0) * y" ).append( v ).append( "_" ).append( i ).append( " + " );
					grbDronesCustomersVars[v][i] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "y" + v + "_" + i );
					log.debug( "Add decision var y" + v + "_" + i );
					grbLinExpr.addTerm( droneTimes[0][i] + droneTimes[i][0], grbDronesCustomersVars[v][i] );
				}
				logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
				log.debug( "Add constraint dronetime_" + v + ": " + logString + " <= traveltime" );
				grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, grbObjectiveVar, "dronetime_" + v );
			}
		} else {
			log.debug( "No dronetime constraints added, because no customer in flight range can be served by a drone." );
		}

		//create constraints that each customer is served by truck or drone exactly once
		//the constraint implicit contains degree-2 constraints for customers
		for( int j = 1; j < dimension; j++ ) {
			logString = new StringBuilder( "0.5 * (" );
			grbLinExpr = new GRBLinExpr();
			for( int i = 0; i < dimension; i++ ) {
				if( i != j ) {
					logString.append( "x" ).append( i ).append( "_" ).append( j ).append( " + " );
					grbLinExpr.addTerm( 0.5, grbTruckEdgeVars[i][j] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) ).append( ") + " );
			if( droneDeliveryPossibleAndInFlightRange.contains( j ) ){
				for(int v = 0; v < droneFleetSize; v++){
					logString.append( "y" ).append( v ).append( "_" ).append( j ).append( " + " );
					grbLinExpr.addTerm( 1.0, grbDronesCustomersVars[v][j] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
			log.debug( "Add constraint customer_served_once_" + j + ": " + logString + " = 1" );
			grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "customer_served_once_" + j );
		}

		//Add degree-2 constraint for depot
		grbLinExpr = new GRBLinExpr();
		logString = new StringBuilder(  );
		for( int i = 1; i < dimension; i++ ) {
			logString.append( "x0_" ).append( i ).append( " + " );
			grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[0][i] );
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add degree-2 constraint for depot: " + logString + " = 2" );
		grbModel.addConstr( grbLinExpr, GRB.EQUAL, 2.0, "deg2_depot" );

		log.info( "End calculation of gurobi model for the PDSTSP without subtour elimination constraints" );

		return grbModel;
	}

	@Override
	protected TspIterationResult calculateTspIterationResult() throws GRBException{
		PdstspIterationResult pdstspIterationResult = new PdstspIterationResult();
		pdstspIterationResult.setTruckTours( findSubtours() );

		ArrayList<Integer>[] dronesCustomers = new ArrayList[droneFleetSize];
		for( int v = 0; v < droneFleetSize; v++ ) {
			dronesCustomers[v] = new ArrayList<>();
			for( int i = 0; i < dimension; i++ ) {
				if( grbDronesCustomersVars[v][i] != null ) {
					if( (int)grbDronesCustomersVars[v][i].get( GRB.DoubleAttr.X ) == 1 ){
						dronesCustomers[v].add( i );
					}
				}
			}
		}
		pdstspIterationResult.setDronesCustomers( dronesCustomers );

		return pdstspIterationResult;
	}

	@Override
	protected void logIterationDebug() throws GRBException{
		super.logIterationDebug();
		log.debug( "Drone customer vars of solution:" );
		for(int v = 0; v < droneFleetSize; v++){
			StringBuilder rowString = new StringBuilder( "Drone_" ).append( v ).append( " : " );
			for(int i = 1; i < dimension; i++){
				if( grbDronesCustomersVars[v][i] == null ){
					rowString.append( "-, " );
				} else {
					rowString.append( (int)grbDronesCustomersVars[v][i].get( GRB.DoubleAttr.X ) ).append( ", " );
				}
			}
			log.debug( rowString.substring( 0, rowString.length() - 2 ) );
		}
	}

	//TODO Remove?!
	/*
	@Override
	protected void getAndSetSolution() throws GRBException{
		super.getAndSetSolution();
		dronesCustomers = new ArrayList[droneFleetSize];
		for( int v = 0; v < droneFleetSize; v++ ) {
			dronesCustomers[v] = new ArrayList<>();
			for( int i = 0; i < dimension; i++ ) {
				if( grbDronesCustomersVars[v][i] != null ) {
					if( (int)grbDronesCustomersVars[v][i].get( GRB.DoubleAttr.X ) == 1 ){
						dronesCustomers[v].add( i );
					}
				}
			}
		}
	}
	*/

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

	public double[][] getDroneTimes(){
		return droneTimes;
	}

	public void setDroneTimes( double[][] droneTimes ){
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

	public double[][] getTruckTimes(){
		return truckTimes;
	}

	public void setTruckTimes( double[][] truckTimes ){
		this.truckTimes = truckTimes;
	}

	public double getDroneSpeed(){
		return droneSpeed;
	}

	public void setDroneSpeed( int droneSpeed ){
		this.droneSpeed = droneSpeed;
	}
}
