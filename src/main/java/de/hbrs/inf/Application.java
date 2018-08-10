package de.hbrs.inf;

import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Arrays;

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
						.longOpt( "jsonFileOrDir" )
						.argName( "file_or_directory" )
						.hasArg()
						.required( true )
						.desc( "read tsp problem/s in json format from file or directory (required parameter)" )
						.build();
		options.addOption( jsonFileOrDir );

		Option outputFile = Option.builder( "o" )
						.longOpt( "outputDir" )
						.argName( "directory" )
						.hasArg()
						.required( false )
						.desc( "use given directory to create file/s with solution/s for each solved problem" )
						.build();
		options.addOption( outputFile );

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

		if( cmd.hasOption("o") ) {
			Configuration.setOutputDirectory( cmd.getOptionValue( "o" ) );
			log.info( "Set output directory: " + Configuration.getOutputDirectory() );
		}

		if( cmd.hasOption( "d" ) ){
			Configuration.setLogLevel( "DEBUG" );
		}
		Configuration.setSystemProperties();

		log = Logger.getLogger( Application.class.getName() );
		log.info( "Start application" );

		if( cmd.hasOption( "j" ) ) {
			Configuration.setJsonFileOrDir( cmd.getOptionValue( "j" ) );
			log.info( "Set json file/directory: " + Configuration.getJsonFileOrDir() );
		}

		log.info( "Set log level: " + System.getProperty( "log4j.logLevel" ) );
		log.info( "Set log file: " + Configuration.getLogFile() );

		//check if file or directory is given for tsp problem/s
		File fileOrDir = new File( Configuration.getJsonFileOrDir() );
		File[] jsonFiles;
		if( fileOrDir.isFile() ) {
			jsonFiles = new File[1];
			jsonFiles[0] = fileOrDir;
		} else {
			jsonFiles = fileOrDir.listFiles();
		}

		//check if output directory is given
		if( Configuration.getOutputDirectory() != null ) {
			File outputDirectory = new File( Configuration.getOutputDirectory() );
			if( !outputDirectory.exists() ) {
				log.info( "Output directory '" + outputDirectory.toString() + "' does no exists. Create now." );
				if( outputDirectory.mkdir() ) {
					log.info( "Output directory created successful." );
				} else {
					log.info( "Creation of output directory failed." );
				}
			} else {
				if( outputDirectory.isFile() ){
					log.info( "Given output directory is a file. No solution files will be created." );
					Configuration.setOutputDirectory( null );
				}
			}
		}

		log.info( "Try to solve the following tsp problems: " + Arrays.toString( jsonFiles ) );

		//iterate over all given json files
		for( File file : jsonFiles ) {
			log.info( "##################### Start: " + file.getName() + " #####################" );
			TspModel tspModel = JsonTspMapper.getObjectFromJson( file.getPath() );
			if( tspModel != null ){
				tspModel.grbOptimize();
			} else {
				log.error( "Could not convert JSON File '" + file.getName() + "' to TSP Model!" );
			}
			log.info( "##################### End: " + file.getName() + " #####################" );
		}

		//TODO get result objects from grbOptimize, show them and write to output file

	}

}