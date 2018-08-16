package de.hbrs.inf;

import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.TspResults;
import de.hbrs.inf.tsp.csv.TspModelCsvResultsConverter;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Application{

	private static Logger log;

	public static void main( String[] args ){

		//create options for command lien arguments
		Options options = new Options();

		Option debug = Option.builder( "d" )
						.longOpt( "debug" )
						.required( false )
						.desc( "set log level to debug" )
						.build();
		options.addOption( debug );

		Option logFile = Option.builder( "l" )
						.longOpt( "logFile" )
						.argName( "file" )
						.hasArg()
						.required( false )
						.desc( "use given file for log" )
						.build();
		options.addOption( logFile );

		Option jsonFileOrDir = Option.builder( "j" )
						.longOpt( "json" )
						.argName( "file_or_directory" )
						.hasArg()
						.required( true )
						.desc( "read tsp problem/s in json format from file or directory (required parameter)" )
						.build();
		options.addOption( jsonFileOrDir );

		Option csvDir = Option.builder( "c" )
						.longOpt( "csv" )
						.argName( "directory" )
						.hasArg()
						.required( false )
						.desc( "use given directory to create file/s with CSV result/s for each solved problem" )
						.build();
		options.addOption( csvDir );

		//TODO add option for different subtour elimination constraint versions (MTZ, etc.)

		//parse the options passed as command line arguments
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try{
			cmd = parser.parse( options, args );
		} catch( ParseException e ){
			System.out.println( "Error while parsing parameters! Error message: " + e.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "tsp_drone", options );
			return;
		}

		//read options and prepare configuration for the application
		if( cmd.hasOption("l") ) {
			Configuration.setLogFile( cmd.getOptionValue( "l" ) );
		}

		if( cmd.hasOption( "d" ) ){
			Configuration.setLogLevel( "DEBUG" );
		}
		Configuration.setSystemProperties();

		log = Logger.getLogger( Application.class.getName() );
		log.info( "Start application" );

		if( cmd.hasOption("c") ) {
			StringBuilder csvPath = new StringBuilder( cmd.getOptionValue( "c" ) );
			if( !( csvPath.charAt( csvPath.length() - 1 ) == '/' ) ) {
				csvPath .append( "/" );
			}
			String datetime = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss" ).format( new Date() );
			csvPath.append( datetime );
			Configuration.setCsvDirectory( csvPath.toString() );
			log.info( "Set csv directory: " + Configuration.getCsvDirectory() );
		}

		if( cmd.hasOption( "j" ) ) {
			Configuration.setJsonFileOrDir( cmd.getOptionValue( "j" ) );
			log.info( "Set json file/directory: " + Configuration.getJsonFileOrDir() );
		}

		log.info( "Set log level: " + System.getProperty( "log4j.logLevel" ) );
		log.info( "Set log file: " + Configuration.getLogFile() );

		//check if file or directory is given for tsp problem/s
		File fileOrDir = new File( Configuration.getJsonFileOrDir() );
		File[] jsonFiles;
		if( !fileOrDir.exists() ) {
			log.info( "Given json parameter '" + Configuration.getJsonFileOrDir() + "' is no existing file or directory!" );
			return;
		}
		if( fileOrDir.isFile() ) {
			jsonFiles = new File[1];
			jsonFiles[0] = fileOrDir;
		} else {
			jsonFiles = fileOrDir.listFiles();
		}

		//check if output directory is given
		if( Configuration.getCsvDirectory() != null ) {
			File csvDirectory = new File( Configuration.getCsvDirectory() );
			if( !csvDirectory.exists() ) {
				log.info( "Path to csv directory '" + csvDirectory.getAbsolutePath() + "' does no exists. Create now." );
				if( csvDirectory.mkdirs() ) {
					log.info( "CSV directory '" + csvDirectory.getAbsolutePath() + "' for result file/s successfuly created." );
				} else {
					log.info( "Creation of csv directory '" + csvDirectory.getAbsolutePath() + "' for result file/s failed." );
					return;
				}
			} else {
				if( csvDirectory.isFile() ){
					log.info( "Given csv directory is a file." );
					return;
				}
			}
		}

		log.info( "Try to solve the following tsp problems: " + Arrays.toString( jsonFiles ) );

		//iterate over all given json files
		for( File file : jsonFiles ) {
			log.info( "##################### Start: " + file.getName() + " #####################" );
			TspModel tspModel = JsonTspMapper.getObjectFromJson( file.getPath() );
			if( tspModel != null ){
				TspResults tspResults = tspModel.grbOptimize();

				if( Configuration.getCsvDirectory() != null ){
					String type = tspModel.getType();
					File csvFile = new File( Configuration.getCsvDirectory() + "/" + type + ".csv" );
					if( !csvFile.exists() ){
						try{
							csvFile.createNewFile();
							log.info( "Creation of csv result file '" + csvFile.getAbsolutePath() + "' was successful!" );
						} catch(IOException e){
							log.info( "Can not create csv result file '" + csvFile.getAbsolutePath() + "'!" );
							log.info( "Error: " + e.getMessage() );
							continue;
						}

						try{
							StringBuilder headerString = new StringBuilder( TspModelCsvResultsConverter.getCsvHeaderString( type ) ).append( System.lineSeparator() );
							Files.write( Paths.get( csvFile.toURI() ), headerString.toString().getBytes() );
							log.info( "Header written to csv result file '" + csvFile.getAbsolutePath() + "'!" );
						} catch(IOException e){
							log.info( "Can not write csv results header to file '" + csvFile.getAbsolutePath() + "'!" );
							log.info( "Error: " + e.getMessage() );
							continue;
						}

					}

					try{
						StringBuilder csvString = new StringBuilder( TspModelCsvResultsConverter.getCsvResultString( tspModel ) ).append( System.lineSeparator() );
						Files.write( Paths.get( csvFile.toURI() ), csvString.toString().getBytes(), StandardOpenOption.APPEND );
						log.info( "Results written to results file '" + csvFile.getAbsolutePath() + "'!" );
					} catch(IOException e){
						log.info( "Can not write csv results to file '" + csvFile.getAbsolutePath() + "'!" );
						log.info( "Error: " + e.getMessage() );
						continue;
					}

				}
			} else {
				log.error( "Could not convert JSON File '" + file.getName() + "' to TSP Model!" );
			}
			log.info( "##################### End: " + file.getName() + " #####################" );
		}

		//TODO get result objects from grbOptimize, show them and write to output file

	}

}