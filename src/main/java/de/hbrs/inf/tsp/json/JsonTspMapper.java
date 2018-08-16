package de.hbrs.inf.tsp.json;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.hbrs.inf.tsp.Pdstsp;
import de.hbrs.inf.tsp.Tsp;
import de.hbrs.inf.tsp.TspModel;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class JsonTspMapper{

	private static final Logger log = Logger.getLogger( JsonTspMapper.class.getName() );

	public static TspModel getObjectFromJson( String fileName ) {

		TspModel tsp = null;
		log.info( "Try to read json file '" + fileName + "' and convert it to corresponding tsp object." );
		try{
			TspLibJson tspLibJson;
			Gson gson = new Gson();
			JsonReader reader = new JsonReader( new FileReader( fileName ) );
			tspLibJson = gson.fromJson( reader, TspLibJson.class );
			log.debug( "TspLibJson successfully read: \n" + tspLibJson );


			double[][] nodes = TspModel.calculateNodes( tspLibJson );
			if( nodes == null && !tspLibJson.getEdge_weight_type().equals( "EXPLICIT" ) ) {
				log.info( "Could not calculate nodes." );
				return null;
			}
			log.info( "Calculate distances with edge_weight_type '" + tspLibJson.getEdge_weight_type() + "'." );
			int[][] distances = TspModel.calculateTravelDistances( tspLibJson.getNode_coordinates(), tspLibJson.getEdge_weights(),
							tspLibJson.getDimension(), tspLibJson.getEdge_weight_type(), tspLibJson.getEdge_weight_format() );
			if( distances == null ) {
				log.info( "Could not calculate distances." );
				return null;
			}

			String tspType = tspLibJson.getType().toUpperCase();
			log.info( "TSP Type: " + tspType );

			switch( tspType ) {
				case "TSP":
					//convert TspLibJson to Tsp object
					tsp = new Tsp( tspLibJson.getName(), tspLibJson.getComment(), tspType, tspLibJson.getDimension(), nodes, distances );
					break;

				case "PDSTSP":
					reader = new JsonReader( new FileReader( fileName ) );
					PdstspLibJson pdstspLibJson = gson.fromJson( reader, PdstspLibJson.class );

					double droneFlightTime = pdstspLibJson.getDrone_flight_range() / pdstspLibJson.getDrone_speed();
					log.debug( "Drone Flight Time: " + droneFlightTime );
					log.info( "Calculate droneTimes with speed '" + pdstspLibJson.getDrone_speed() + "'." );
					double [][] droneTimes = TspModel.calculateTravelTimes( pdstspLibJson.getDrone_speed(), distances );

					log.info( "Calculate truckTimes with speed '" + pdstspLibJson.getTruck_speed() + "'." );
					double [][] truckTimes = TspModel.calculateTravelTimes( pdstspLibJson.getTruck_speed(), distances );

					//convert PdstspLibJson to Pdstsp object
					tsp = new Pdstsp( pdstspLibJson.getName(), pdstspLibJson.getComment(), tspType, pdstspLibJson.getDimension(),
									nodes, distances, pdstspLibJson.getTruck_speed(), truckTimes, pdstspLibJson.getDrone_speed(),
									droneFlightTime, pdstspLibJson.getDrone_fleet_size(), droneTimes, pdstspLibJson.getDrone_delivery_possible() );
					break;

				default:
					log.info( "TSP Type '" + tspType + "' not supported yet." );
					break;
			}

		} catch(FileNotFoundException e){
			log.error( "File not found '" + fileName + "'." );
			return null;
		} catch(Exception e){
			log.error( "Something went wrong while reading TspLibJson File! Error message: " + e.getMessage() );
			return null;
		}

		log.info( "Created Tsp model from JSON file." );
		log.debug( tsp );
		return tsp;
	}

}
