package de.hbrs.inf;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import gurobi.*;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class TSP{

	private Point2D.Double[] nodes;
	private double[][] distances;
	private double[][] truckTimes;
	private GRBModel grbModel;
	private GRBEnv grbEnv;
	private static Logger log = Logger.getLogger( TSP.class.getName() );

	public TSP(){
	}

	public TSP( Point2D.Double[] nodes, double[][] distances, double[][] truckTimes ){
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
			log.info( "TSPLibJson successfully read: \n" + tspLibJson );

		} catch(FileNotFoundException e){
			log.error( "File not found '" + fileName + "'." );
			return null;
		} catch(Exception e){
			log.error( "Something went wrong while reading TSPLibJson File! Error message: " + e.getMessage() );
			return null;
		}

		//convert TSPLibJson to TSP object
		log.info( "Convert node coordinates to Point2D.Double array." );
		Double[] nodes = convertNodeArrayToPoint2dArray( tspLibJson.getNode_coordinates() );

		log.info( "Calculate distances with distanceTye (edge_weight_type) '" + tspLibJson.getEdge_weight_type() + "'." );
		double[][] distances = calculateTravelDistances( nodes, tspLibJson.getEdge_weight_type() );

		log.info( "Calculate truckTimes with speed '" + tspLibJson.getTruck_speed() + "'." );
		double[][] truckTimes = calculateTravelTimes( tspLibJson.getTruck_speed(), distances );

		TSP tsp = new TSP( nodes, distances, truckTimes );
		log.info( tsp );

		return tsp;
	}

	public static double[][] calculateTravelDistances( Point2D[] nodes, String distanceType ){

		int dimension = nodes.length;
		double[][] distances = new double[dimension][dimension];

		//if distanceTime equals "GEO" we need additional arrays for the latitude and longitude
		double[] latitude = new double[dimension];
		double[] longitude = new double[dimension];

		if( distanceType.equals( "GEO" ) ){
			for(int i = 0; i < dimension; i++){
				int degX = (int)nodes[i].getX();
				int degY = (int)nodes[i].getY();
				double minX = nodes[i].getX() - degX;
				double minY = nodes[i].getY() - degY;
				latitude[i] = Math.PI * (degX + 5.0 * minX / 3.0) / 180.0;
				longitude[i] = Math.PI * (degY + 5.0 * minY / 3.0) / 180.0;
			}
		}

		//calculate tarvel distances dependent on the distance type
		for(int i = 0; i < dimension; i++){
			for(int j = 0; j < dimension; j++){

				if( distanceType.equals( "GEO" ) ){

					double q1 = Math.cos( longitude[i] - longitude[j] );
					double q2 = Math.cos( latitude[i] - latitude[j] );
					double q3 = Math.cos( latitude[i] + latitude[j] );
					distances[i][j] = Defines.EARTH_RADIUS * Math.acos( 0.5 * ((1.0 + q1) * q2 - (1.0 - q1) * q3) ) + 1.0;

				} else if( distanceType.equals( "EUC_2D" ) ){

					double deltaX = nodes[i].getX() - nodes[j].getX();
					double deltaY = nodes[i].getY() - nodes[j].getY();
					distances[i][j] = Math.sqrt( deltaX * deltaX + deltaY * deltaY );

				} else {
					log.error( "DistanceTye (edge_weight_type) '" + distanceType + "' not supported." );
					return null;
				}
			}
		}

		return distances;
	}

	public static double[][] calculateTravelTimes( double speed, double[][] distances ){

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
			toString += "[ " + nodes[i].getX() + ", " + nodes[i].getY() + " ], \n";
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

	public void calcGrbModel() {
		//calculate gurobi model for the TSP without subtour elimination constraints
		try {
			grbEnv = new GRBEnv();
			grbEnv.set( GRB.IntParam.LogToConsole, 0 );
			grbModel = new GRBModel( grbEnv );

			int n = nodes.length;

			// create decision variables
			GRBVar[][] grbVars = new GRBVar[n][n];
			for( int i = 0; i < n; i++ ) {
				for( int j = i + 1; j < n; j++ ) {
					log.info( "Add decision var x" + i + "_" + j + " with factor " + distances[i][j] );
					grbVars[i][j] = grbModel.addVar( 0.0, 1.0, distances[i][j], GRB.BINARY, "x" + String.valueOf( i ) + "_" + String.valueOf( j ) );
					grbVars[j][i] = grbVars[i][j];
				}
			}

			// create degree-2 constraints
			for( int i = 0; i < n; i++ ) {
				GRBLinExpr grbExpr = new GRBLinExpr();
				String logString = "";
				for( int j = 0; j < n; j++ ) {
					if( i != j) {
						logString += "x" + i + "_" + j + " + ";
						grbExpr.addTerm( 1.0, grbVars[i][j] );
					}
				}
				logString = logString.substring( 0, logString.length() - 2 );
				log.info( "Add degree-2 constraint deg2_" + i + ": " + logString + " = 2" );
				grbModel.addConstr( grbExpr, GRB.EQUAL, 2.0, "deg2_" + String.valueOf( i ) );
			}

		} catch( GRBException e ) {
			log.error( "Error code: " + e.getErrorCode() + ". " + e.getMessage() );
		}
	}

	public void grbOptimize() {
		try{
			grbModel.optimize();

			int optimstatus = grbModel.get( GRB.IntAttr.Status );

			if( optimstatus == GRB.Status.INF_OR_UNBD ) {
				grbModel.set( GRB.IntParam.Presolve, 0 );
				grbModel.optimize();
				optimstatus = grbModel.get( GRB.IntAttr.Status );
			}

			if( optimstatus == GRB.Status.OPTIMAL ) {
				double objval = grbModel.get( GRB.DoubleAttr.ObjVal );
				log.info( "Optimal objective: " + objval );

				GRBVar[] vars = grbModel.getVars();
				String [] varNames = grbModel.get ( GRB.StringAttr.VarName, vars );
				double [] x = grbModel.get( GRB.DoubleAttr.X, vars );
				String solutionString = "";
				for( int i = 0; i < vars.length; i++ ) {
					if( x[i] != 0.0 ) solutionString += varNames[i] + ", ";
				}
				solutionString = solutionString.substring( 0, solutionString.length() - 2 );
				log.info( "Edges in solution: " + solutionString);

			} else if( optimstatus == GRB.Status.INFEASIBLE ) {
				log.info( "Model is infeasible" );

				// Compute and write out IIS
				grbModel.computeIIS();
				grbModel.write( "model.ilp" );
			} else if( optimstatus == GRB.Status.UNBOUNDED ) {
				log.info( "Model is unbounded" );
			} else {
				log.info( "Optimization was stopped with status = " + optimstatus );
			}

			// Dispose of model and environment
			grbModel.dispose();
			grbEnv.dispose();

		} catch( GRBException e ) {
			log.error( "Error code: " + e.getErrorCode() + ". " + e.getMessage() );
		}
	}


	public Double[] getNodes(){
		return nodes;
	}

	public void setNodes( Double[] nodes ){
		this.nodes = nodes;
	}

	public double[][] getDistances(){
		return distances;
	}

	public void setDistances( double[][] distances ){
		this.distances = distances;
	}

	public double[][] getTruckTimes(){
		return truckTimes;
	}

	public void setTruckTimes( double[][] truckTimes ){
		this.truckTimes = truckTimes;
	}
}
