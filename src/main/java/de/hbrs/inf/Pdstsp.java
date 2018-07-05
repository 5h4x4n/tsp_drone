package de.hbrs.inf;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.log4j.Logger;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class Pdstsp extends Tsp{

	private double droneFlightTime;
	private int droneFleetSize;
	private double[][] droneTimes;
	private int[] droneDeliveryPossible;

	private static Logger log = Logger.getLogger( Pdstsp.class.getName() );

	public Pdstsp( String name, String comment, String type, double[][] nodes, int[][] distances, double[][] truckTimes, double droneFlightTime, double[][] droneTimes,
					int[] droneDeliveryPossible, int droneFleetSize ){
		super( name, comment, type, nodes, distances, truckTimes );
		this.droneFlightTime = droneFlightTime;
		this.droneTimes = droneTimes;
		this.droneDeliveryPossible = droneDeliveryPossible;
		this.droneFleetSize = droneFleetSize;
	}

	public static Pdstsp getObjectFromJson( String fileName ) {
		PdstspLibJson pdstspLibJson;
		log.info( "Try to read PDSTPSJson file '" + fileName + "' and convert it to corresponding java object." );
		try {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader( new FileReader( fileName ));
			pdstspLibJson = gson.fromJson( reader, PdstspLibJson.class );
			log.info( "PdstspLibJson successfully read: \n" + pdstspLibJson );

		} catch ( FileNotFoundException e ) {
			log.error( "File not found '" + fileName + "'." );
			return null;
		} catch ( Exception e ) {
			log.error( "Something went wrong while reading PdstspLibJson File! Error message: " + e.getMessage() );
			return null;
		}

		//convert PdstspLibJson to Pdstsp object
		double droneFlightTime = pdstspLibJson.getDrone_flight_range() / pdstspLibJson.getDrone_speed();
		log.info( "Convert node coordinates to Point2D.Double array." );
		//Point2D.Double[] nodes = convertNodeArrayToPoint2dArray( pdstspLibJson.getNode_coordinates() );

		log.info( "Calculate distances with distanceTye (edge_weight_type) '" + pdstspLibJson.getEdge_weight_type() + "'." );
		//double[][] distances = calculateTravelDistances( nodes, pdstspLibJson.getEdge_weight_type() );

		log.info( "Calculate truckTimes with speed '" + pdstspLibJson.getTruck_speed() + "'." );
		//double[][] truckTimes = calculateTravelTimes( pdstspLibJson.getTruck_speed(), distances );

		log.info( "Calculate droneTimes with speed '" + pdstspLibJson.getTruck_speed() + "'." );
		//double[][] droneTimes = calculateTravelTimes( pdstspLibJson.getDrone_speed(), distances );

		//Pdstsp pdstsp = new Pdstsp( nodes, distances, truckTimes, droneFlightTime, droneTimes, pdstspLibJson.getDrone_delivery_possible(), pdstspLibJson.getDrone_fleet_size() );
		//log.info( pdstsp );

		//return pdstsp;

		return null;
	}

	public String toString() {
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
			toString += droneDeliveryPossible + ", ";
		}
		toString = toString.substring( 0, toString.length() - 2 ) + " ]\n";

		return toString;
	}

	public double getDroneFlightTime(){
		return droneFlightTime;
	}

	public void setDroneFlightTime( double droneFlightTime ){
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
}
