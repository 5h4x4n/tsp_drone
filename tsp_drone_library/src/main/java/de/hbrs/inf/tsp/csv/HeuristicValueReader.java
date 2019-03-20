package de.hbrs.inf.tsp.csv;

import de.hbrs.inf.tsp.Defines;
import de.hbrs.inf.tsp.Pdstsp;
import de.hbrs.inf.tsp.TspModel;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class HeuristicValueReader{

	private static final Logger log = Logger.getLogger( HeuristicValueReader.class.getName() );

	public static double getHeuristicValue( TspModel tspModel, String pathToFile ){

		double heuristicValue = -1.0;

		Set<Map<String, String>> csv = CsvToSetOfMaps.convertCsvFile( pathToFile, ';' );

		if( csv != null ){

			String type = tspModel.getType();
			String name = tspModel.getName();

			Optional<Map<String, String>> filteredRow;
			String heuristicValueString = null;

			Map<String, String> firstRow = csv.iterator().next();

			if( !firstRow.containsKey( Defines.Strings.TYPE ) || !firstRow.containsKey( Defines.Strings.NAME )
							|| !firstRow.containsKey( Defines.Strings.OBJECTIVE ) ){
				log.error( "Missing necessary column in CSV file!" );
				return -1.0;
			}

			switch( type ){
				case Defines.TSP:

					filteredRow = csv.stream().filter( line -> line.get( Defines.Strings.TYPE ).equalsIgnoreCase( type )
									&& line.get( Defines.Strings.NAME ).equalsIgnoreCase( name ) )
											  .findFirst();
					if( filteredRow.isPresent() ) {
						heuristicValueString = filteredRow.get().get( Defines.Strings.OBJECTIVE );
					}

					if( heuristicValueString == null ) {
						log.error( "Could not find a value for the TSP model of '" + name + "'!" );
					} else {
						//TODO parsing not safe - if there is a comma or s.th. else in the cell -> NumberFormatException
						heuristicValue = Double.parseDouble( heuristicValueString );
						log.info( "Found heuristic value for TSP model of '" + name + "': '" + heuristicValue + "' (String value: '" +
										heuristicValueString + "')" );
					}
					break;

				case Defines.PDSTSP:
					if( !firstRow.containsKey( Defines.Strings.TRUCK_SPEED ) || !firstRow.containsKey( Defines.Strings.DRONE_SPEED )
									|| !firstRow.containsKey( Defines.Strings.DRONE_FLEET_SIZE )
									|| !firstRow.containsKey( Defines.Strings.DRONE_FLIGHT_RANGE_PERCENTAGE ) ) {
						log.error( "Missing necessary column in CSV file!" );
						return -1.0;
					}

					Pdstsp pdstsp = (Pdstsp)tspModel;
					double ts = pdstsp.getTruckSpeed();
					double ds = pdstsp.getDroneSpeed();
					double dfs = pdstsp.getDroneFleetSize();
					double dfr = pdstsp.getDroneFlightRangePercentage();

					log.info( "DEBUG: ts=" + ts + " , ds=" + ds + " , dfs=" + dfs + " , dfr=" + dfr );

					filteredRow = csv.stream().filter( line -> line.get( Defines.Strings.TYPE ).equalsIgnoreCase( type )
									&& line.get( Defines.Strings.NAME ).equalsIgnoreCase( name )
									&& Double.parseDouble( line.get( Defines.Strings.TRUCK_SPEED ) ) == ts
									&& Double.parseDouble( line.get( Defines.Strings.DRONE_SPEED ) ) == ds
									&& Double.parseDouble( line.get( Defines.Strings.DRONE_FLEET_SIZE ) ) == dfs
									&& Double.parseDouble( line.get( Defines.Strings.DRONE_FLIGHT_RANGE_PERCENTAGE ) ) == dfr )
											  .findFirst();

					if( filteredRow.isPresent() ) {
						heuristicValueString = filteredRow.get().get( Defines.Strings.OBJECTIVE );
					}

					if( heuristicValueString == null ) {
						log.error( "Could not find a value for the PDSTSP model of '" + name + "' and values "
										+ "ts=" + ts + " , ds=" + ds + " , dfs=" + dfs + " , dfr=" + dfr );
					} else {
						//TODO parsing not safe - if there is a comma or s.th. else in the cell -> NumberFormatException
						heuristicValue = Double.parseDouble( heuristicValueString );
						log.info( "Found heuristic value for PDSTSP model of '" + name + "' and values: "
										+ "ts=" + ts + " , ds=" + ds + " , dfs=" + dfs + " , dfr=" + dfr + "': '" + heuristicValue + "' "
										+ "(String value: '" + heuristicValueString + "')" );
					}

					break;

				default:
					log.info( "TSP Type '" + type + "' not supported yet." );
			}
		}

		return heuristicValue;
	}
}
