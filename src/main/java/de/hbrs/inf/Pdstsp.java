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

		//TODO
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
		String logString = "";
		for( int i = 0; i < dimension; i++ ) {
			for( int j = i + 1; j < dimension; j++ ) {
				if( i != j ) {
					logString += "trucktimes_" + i + "_" + j + " * x" + i + "_" + j + " + ";
					grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "x" + i + "_" + j );
					grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];
					log.debug( "Add decision var x" + i + "_" + j + " with factor " + truckTimes[i][j] );
					grbLinExpr.addTerm( truckTimes[i][j], grbTruckEdgeVars[i][j] );
				}

			}
		}
		logString = logString.substring( 0, logString.length() - 3 );
		log.debug( "Add constraint trucktime: " + logString + " <= traveltime" );
		grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, grbObjectiveVar, "trucktime" );

		//Add Drone times constraints also as lower bounds for traveltime
		for( int v = 0; v < droneFleetSize; v++ ) {
			logString = "";
			grbLinExpr = new GRBLinExpr();
			for( int i : droneDeliveryPossibleAndInFlightRange ){
				logString += "(dronetimes_0_" + i + " + dronetimes_" + i + "_0) * y" + v + "_" + i + " + ";
				grbDronesCustomersVars[v][i] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "y" + v + "_" + i );
				log.debug( "Add decision var y" + v + "_" + i );
				grbLinExpr.addTerm( droneTimes[0][i] + droneTimes[i][0], grbDronesCustomersVars[v][i] );
			}
			logString = logString.substring( 0, logString.length() - 3 );
			log.debug( "Add constraint dronetime_" + v + ": " + logString + " <= traveltime" );
			grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, grbObjectiveVar, "dronetime_" + v );
		}

		//create constraints that each customer is served by truck or drone exactly once
		//the constraint implicit contains degree-2 constraints for customers
		for( int j = 1; j < dimension; j++ ) {
			logString = "0.5 * (";
			grbLinExpr = new GRBLinExpr();
			for( int i = 0; i < dimension; i++ ) {
				if( i != j ) {
					logString += "x" + i + "_" + j + " + ";
					grbLinExpr.addTerm( 0.5, grbTruckEdgeVars[i][j] );
				}
			}
			logString = logString.substring( 0, logString.length() - 3 ) +  ") + ";
			if( droneDeliveryPossibleAndInFlightRange.contains( j ) ){
				for(int v = 0; v < droneFleetSize; v++){
					logString += "y" + v + "_" + j + " + ";
					grbLinExpr.addTerm( 1.0, grbDronesCustomersVars[v][j] );
				}
			}
			logString = logString.substring( 0, logString.length() - 3 );
			log.debug( "Add constraint customer_served_once_" + j + ": " + logString + " = 1" );
			grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "customer_served_once_" + j );
		}

		//Add degree-2 constraint for depot
		grbLinExpr = new GRBLinExpr();
		logString = "";
		for( int i = 1; i < dimension; i++ ) {
			logString += "x0_" + i + " + ";
			grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[0][i] );
		}
		logString = logString.substring( 0, logString.length() - 3 );
		log.debug( "Add degree-2 constraint for depot: " + logString + " = 2" );
		grbModel.addConstr( grbLinExpr, GRB.EQUAL, 2.0, "deg2_depot" );

		log.info( "End calculation of gurobi model for the PDSTSP without subtour elimination constraints" );

		return grbModel;
	}

	protected void logIterationDebug() throws GRBException{
		super.logIterationDebug();
		log.debug( "Drone customer vars of solution:" );
		for(int v = 0; v < droneFleetSize; v++){
			String rowString = "Drone_" + v + " : ";
			for(int i = 1; i < dimension; i++){
				if( grbDronesCustomersVars[v][i] == null ){
					rowString += "-, ";
				} else {
					rowString += ((int)grbDronesCustomersVars[v][i].get( GRB.DoubleAttr.X ) ) + ", ";
				}
			}
			log.debug( rowString.substring( 0, rowString.length() - 2 ) );
		}
	}

	/*
	public ArrayList<HashSet<Integer>> findSubtours() throws GRBException{
		log.debug( "Starting find subtours" );

		ArrayList<HashSet<Integer>> subtours = new ArrayList<HashSet<Integer>>();
		Stack<Integer> unvisitedVertices = new Stack<Integer>();
		for( int i = dimension - 1; i >= 0; i-- ){
			unvisitedVertices.add( i );
		}

		//determine which customer will be served from drones and remove it from unvisitedVertices
		for( int v = 0; v < droneFleetSize; v++ ){
			for( int i : droneDeliveryPossibleAndInFlightRange ){
				if( (int)(grbDronesCustomersVars[v][i].get( GRB.DoubleAttr.X ) + 0.5d ) != 0 ){
					boolean test = unvisitedVertices.remove( new Integer( i ) );
				}
			}
		}

		//TODO subtour elimination constraints for directed graph or model with undirected graph?!

		while( !unvisitedVertices.isEmpty() ){
			int currentVertex = unvisitedVertices.pop();
			log.debug( "currentVertex: " + currentVertex );
			log.debug( "unvisitedVertices: " + unvisitedVertices );
			HashSet<Integer> subtour = new HashSet<Integer>();
			subtours.add( subtour );
			Stack<Integer> unvisitedVerticesForSubtour = new Stack<Integer>();
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
						if( ( (int)( grbTruckEdgeVars[currentSubtourVertex][i].get( GRB.DoubleAttr.X ) + 0.5d ) ) == 1 && !subtour.contains( i ) ){
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
