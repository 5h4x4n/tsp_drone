package de.hbrs.inf.tsp.csv;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

class CsvToSetOfMaps{

	private static final Logger log = Logger.getLogger( CsvToSetOfMaps.class.getName() );

	public static Set<Map<String, String>> convertCsvFile( String pathToFile, char delimiter ) {

		log.info( "Try to read csv file: " + pathToFile );
		Set<Map<String, String>> result = new HashSet<>();
		int lineCounter = 0;

		List<String> headlineElements = null;

		File csvFile = new File( pathToFile );

		try{
			Scanner scanner = new Scanner( csvFile );
			while ( scanner.hasNextLine() ){
				List<String> lineElements = Arrays.asList( scanner.nextLine().split( Character.toString( delimiter ) ) );
				if( lineCounter == 0 ) {
					//headline
					headlineElements = lineElements;
				} else {
					//all other lines
					if( headlineElements.size() != lineElements.size() ) {
						result = null;
						log.error( "The structure of the csv file is not correct. The headline has " + headlineElements.size() + " elements, "
										+ "but row " + (lineCounter + 1) + " has " + lineElements.size() + " elements!");
						break;
					} else {
						Map<String, String> line = new HashMap<>();
						for( int i = 0; i < headlineElements.size(); i++ ){
							line.put( headlineElements.get( i ), lineElements.get( i ) );
						}
						result.add( line );
					}
				}
				lineCounter++;
			}
			log.info( "CSV file successfully read!" );
		} catch( FileNotFoundException e ){
			result = null;
			log.error( "The csv file does not exists! Path: " + csvFile.getAbsolutePath() );
			e.printStackTrace();
		}

		return result;
	}

}
