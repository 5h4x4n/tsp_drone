package de.hbrs.inf;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class JsonTspMapper{

	private static final Logger log = Logger.getLogger( JsonTspMapper.class.getName() );

	public static TspModel getObjectFromJson( String fileName ) {

		TspLibJson tspLibJson;
		log.info( "Try to read json file '" + fileName + "' and convert it to corresponding tsp object." );
		try{
			Gson gson = new Gson();
			JsonReader reader = new JsonReader( new FileReader( fileName ) );
			tspLibJson = gson.fromJson( reader, TspLibJson.class );
			log.debug( "TspLibJson successfully read: \n" + tspLibJson );

		} catch(FileNotFoundException e){
			log.error( "File not found '" + fileName + "'." );
			return null;
		} catch(Exception e){
			log.error( "Something went wrong while reading TspLibJson File! Error message: " + e.getMessage() );
			return null;
		}

		//convert TspLibJson to Tsp object
		double[][] nodes = Tsp.calculateNodes( tspLibJson );

		log.info( "Calculate distances with edge_weight_type '" + tspLibJson.getEdge_weight_type() + "'." );
		int[][] distances = TspModel.calculateTravelDistances( tspLibJson.getNode_coordinates(), tspLibJson.getEdge_weights(),
						tspLibJson.getDimension(), tspLibJson.getEdge_weight_type(), tspLibJson.getEdge_weight_format() );

		log.info( "Calculate truckTimes with speed '" + tspLibJson.getTruck_speed() + "'." );
		double[][] truckTimes = TspModel.calculateTravelTimes( tspLibJson.getTruck_speed(), distances );

		Tsp tsp = new Tsp( tspLibJson.getName(), tspLibJson.getComment(), tspLibJson.getType(), tspLibJson.getDimension(), nodes, distances, truckTimes );
		log.info( "Created Tsp model from JSON file." );
		//log.debug( tsp );

		return tsp;
	}

}
