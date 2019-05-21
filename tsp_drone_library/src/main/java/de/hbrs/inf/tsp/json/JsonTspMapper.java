package de.hbrs.inf.tsp.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.hbrs.inf.tsp.*;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class JsonTspMapper{

	private static final Logger log = Logger.getLogger( JsonTspMapper.class.getName() );

	public static TspLibJson getJsonObjectFromJson( String fileName ){
		TspLibJson tspLibJson;
		try{
			Gson gson = new Gson();
			JsonReader reader = new JsonReader( new FileReader( fileName ) );
			tspLibJson = gson.fromJson( reader, TspLibJson.class );
			log.debug( "TspLibJson successfully read: \n" + tspLibJson );

			String tspType = tspLibJson.getType().toUpperCase();
			log.info( "TSP Type: " + tspType );

			switch(tspType){
				case Defines.TSP:
					return tspLibJson;
				case Defines.PDSTSP:
					reader = new JsonReader( new FileReader( fileName ) );
					return gson.fromJson( reader, PdstspLibJson.class );
				case Defines.FSTSP:
					reader = new JsonReader( new FileReader( fileName ) );
					return gson.fromJson( reader, FstspLibJson.class );
				default:
					log.info( "TSP Type '" + tspType + "' not supported yet." );
					return null;
			}
		} catch(FileNotFoundException e){
			log.error( "File not found '" + fileName + "'." );
			return null;
		} catch(Exception e){
			log.error( "Something went wrong while reading TspLibJson File! Error message: " + e.getMessage() );
			return null;
		}
	}
	public static TspModel getTspModelFromJsonObject( TspLibJson tspLibJson ) {
		return getTspModelFromJsonObject( tspLibJson, -1, -1, -1, -1, false );
	}

	public static TspModel getTspModelFromJsonObject( TspLibJson tspLibJson, double truckSpeed, double droneSpeed,
					int droneFleetSize, int droneFlightRangePercentage, boolean droneDeliveryPossibleForAllCustomers ){

		double[][] nodes = TspModel.calculateNodes( tspLibJson );
		if( nodes == null && !tspLibJson.getEdge_weight_type().equals( "EXPLICIT" ) ){
			log.info( "Could not calculate nodes." );
			return null;
		}
		log.info( "Calculate distances with edge_weight_type '" + tspLibJson.getEdge_weight_type() + "'." );
		int[][] distances = TspModel.calculateTravelDistances( tspLibJson.getNode_coordinates(), tspLibJson.getEdge_weights(),
						tspLibJson.getDimension(), tspLibJson.getEdge_weight_type(), tspLibJson.getEdge_weight_format() );
		if( distances == null ){
			log.info( "Could not calculate distances." );
			return null;
		}

		//TODO TESTING increasing distances by factor 100
		for( int i = 0; i < tspLibJson.getDimension(); i++ ){
			for( int j = 0; j < tspLibJson.getDimension(); j++ ){
				distances[i][j] *= 100;
			}
		}
		//TODO TESTING increasing distances by factor 100

		String tspType = tspLibJson.getType().toUpperCase();
		log.info( "TSP Type: " + tspType );

		TspModel tspModel = null;

		switch(tspType){
			case Defines.TSP:
				//convert TspLibJson to Tsp object
				tspModel = new Tsp( tspLibJson.getName(), tspLibJson.getComment(), tspType, tspLibJson.getDimension(), nodes, distances );
				break;
			case Defines.PDSTSP:
				PdstspLibJson pdstspLibJson = (PdstspLibJson)tspLibJson;

				if( droneFleetSize > 0 )
					pdstspLibJson.setDrone_fleet_size( droneFleetSize );
				if( truckSpeed > 0 )
					pdstspLibJson.setTruck_speed( truckSpeed );
				if( droneSpeed > 0 )
					pdstspLibJson.setDrone_speed( droneSpeed );
				if( droneFlightRangePercentage > 0 ) {
					double maxDistance = -1;
					for( int i = 1; i < tspLibJson.getDimension(); i++ ) {
						if( distances[0][i] > maxDistance ) {
							maxDistance = distances[0][i];
						}
					}
					log.info( "MaxDistance: " + maxDistance );
					int droneFlightRange = (int)Math.ceil( ( (double)droneFlightRangePercentage / 100 * maxDistance ) * 2 );
					pdstspLibJson.setDrone_flight_range( droneFlightRange );
				}
				if( droneDeliveryPossibleForAllCustomers ) {
					int dimension = pdstspLibJson.getDimension();
					int[] droneDeliveryPossible = new int[dimension - 1];
					for( int i = 0; i < dimension - 1; i++ ) {
						droneDeliveryPossible[i] = i + 1;
					}
					pdstspLibJson.setDrone_delivery_possible( droneDeliveryPossible );
				}
				log.info( "TruckSpeed: " + truckSpeed );
				log.info( "DroneSpeed: " + droneSpeed );
				log.info( "DroneFlightRange: " + pdstspLibJson.getDrone_flight_range() );
				if( droneFlightRangePercentage > 0 ) {
					log.info( "DroneFlightRangePercentage: " + droneFlightRangePercentage + "%" );
				}
				log.info( "DroneFleetSize: " + pdstspLibJson.getDrone_fleet_size() );
				log.info( "DroneDeliveryPossibleForAllCustomers: " + droneDeliveryPossibleForAllCustomers );

				double droneFlightTime = pdstspLibJson.getDrone_flight_range() / pdstspLibJson.getDrone_speed();
				log.debug( "Drone Flight Time: " + droneFlightTime );
				log.info( "Calculate droneTimes with speed '" + pdstspLibJson.getDrone_speed() + "'." );
				int[][] droneTimes = TspModel.calculateTravelTimes( pdstspLibJson.getDrone_speed(), distances );

				log.info( "Calculate truckTimes with speed '" + pdstspLibJson.getTruck_speed() + "'." );
				int[][] truckTimes = TspModel.calculateTravelTimes( pdstspLibJson.getTruck_speed(), distances );

				//convert PdstspLibJson to Pdstsp object
				tspModel = new Pdstsp( pdstspLibJson.getName(), pdstspLibJson.getComment(), tspType, pdstspLibJson.getDimension(), nodes,
								distances, pdstspLibJson.getTruck_speed(), truckTimes, pdstspLibJson.getDrone_speed(), droneFlightTime,
								pdstspLibJson.getDrone_fleet_size(), droneTimes, pdstspLibJson.getDrone_delivery_possible() );
				break;
			case Defines.FSTSP:
				FstspLibJson fstspLibJson = (FstspLibJson)tspLibJson;

				if( truckSpeed > 0 )
					fstspLibJson.setTruck_speed( truckSpeed );
				if( droneSpeed > 0 )
					fstspLibJson.setDrone_speed( droneSpeed );
				if( droneFlightRangePercentage > 0 ){
					double maxDistance = -1;
					for( int i = 1; i < tspLibJson.getDimension(); i++ ){
						if( distances[0][i] > maxDistance ){
							maxDistance = distances[0][i];
						}
					}
					log.info( "MaxDistance: " + maxDistance );
					int droneFlightRange = (int)Math.ceil( ((double)droneFlightRangePercentage / 100 * maxDistance) * 2 );
					fstspLibJson.setDrone_flight_range( droneFlightRange );
				}
				if( droneDeliveryPossibleForAllCustomers ){
					int dimension = fstspLibJson.getDimension();
					int[] droneDeliveryPossible = new int[dimension - 1];
					for( int i = 0; i < dimension - 1; i++ ){
						droneDeliveryPossible[i] = i + 1;
					}
					fstspLibJson.setDrone_delivery_possible( droneDeliveryPossible );
				}
				log.info( "TruckSpeed: " + truckSpeed );
				log.info( "DroneSpeed: " + droneSpeed );
				log.info( "DroneFlightRange: " + fstspLibJson.getDrone_flight_range() );
				if( droneFlightRangePercentage > 0 ){
					log.info( "DroneFlightRangePercentage: " + droneFlightRangePercentage + "%" );
				}
				log.info( "DroneDeliveryPossibleForAllCustomers: " + droneDeliveryPossibleForAllCustomers );

				double droneFlightTime2 = fstspLibJson.getDrone_flight_range() / fstspLibJson.getDrone_speed();
				log.debug( "Drone Flight Time: " + droneFlightTime2 );
				log.info( "Calculate droneTimes with speed '" + fstspLibJson.getDrone_speed() + "'." );
				int[][] droneTimes2 = TspModel.calculateTravelTimes( fstspLibJson.getDrone_speed(), distances );

				log.info( "Calculate truckTimes with speed '" + fstspLibJson.getTruck_speed() + "'." );
				int[][] truckTimes2 = TspModel.calculateTravelTimes( fstspLibJson.getTruck_speed(), distances );

				//convert fstspLibJson to Fdstsp object
				tspModel = new Fstsp( fstspLibJson.getName(), fstspLibJson.getComment(), tspType, fstspLibJson.getDimension(), nodes, distances,
								fstspLibJson.getTruck_speed(), truckTimes2, fstspLibJson.getDrone_speed(), droneFlightTime2, droneTimes2,
								fstspLibJson.getDrone_delivery_possible() );

				break;
			default:
				log.info( "TSP Type '" + tspType + "' not supported yet." );
				return null;
		}

		log.info( "Created Tsp model from JSON file." );
		log.debug( "Tsp Model to String:" );
		log.debug( tspModel );
		return tspModel;
	}

}
