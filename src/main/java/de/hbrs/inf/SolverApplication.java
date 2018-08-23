package de.hbrs.inf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

public class SolverApplication{

	private static Logger log;

	public static void main( String[] args ){

		//create options for command line arguments
		Options options = createOptions();

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

		log = Logger.getLogger( SolverApplication.class.getName() );
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

		StringBuilder outputPath = new StringBuilder( "tests/" );
		if( cmd.hasOption( "o" ) ){
			outputPath = new StringBuilder( cmd.getOptionValue( "o" ) );
			if( !(outputPath.charAt( outputPath.length() - 1 ) == '/') ){
				outputPath.append( "/" );
			}
		}
		String datetime = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss" ).format( new Date() );
		outputPath.append( fileOrDir.getName() ).append( "_" ).append( datetime );
		Configuration.setOutputDirectory( outputPath.toString() );
		log.info( "Set csv directory: " + Configuration.getOutputDirectory() );

		//check if any output option is set and create output directory if necessary
		if( cmd.hasOption( "c" ) || cmd.hasOption( "r" ) ) {
			File outputDirectory = new File( Configuration.getOutputDirectory() );
			if( !outputDirectory.exists() ){
				log.info( "Path to output directory '" + outputDirectory.getAbsolutePath() + "' does no exists. Create now." );
				if( outputDirectory.mkdirs() ){
					log.info( "Output directory '" + outputDirectory.getAbsolutePath() + "' for result file/s successfuly created." );
				} else {
					log.info( "Creation of output directory '" + outputDirectory.getAbsolutePath() + "' for result file/s failed." );
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

				if( cmd.hasOption( "c" ) ){
					String type = tspModel.getType();
					File csvFile = new File( Configuration.getOutputDirectory() + "/" + type + ".csv" );
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

				if( cmd.hasOption( "r" ) ){
					File jsonResultsFile = new File( Configuration.getOutputDirectory() + "/" + file.getName() + ".results" );
					if( !jsonResultsFile.exists() ) {
						try{
							jsonResultsFile.createNewFile();
							log.info( "Creation of json results file '" + jsonResultsFile.getAbsolutePath() + "' was successful!" );
						} catch(IOException e){
							log.info( "Can not create json results file '" + jsonResultsFile.getAbsolutePath() + "'!" );
							log.info( "Error: " + e.getMessage() );
						}
					}
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					try{
						Files.write( Paths.get( jsonResultsFile.toURI() ), gson.toJson( tspModel ).getBytes(), StandardOpenOption.CREATE );
						log.info( "JSON results written to results file '" + jsonResultsFile.getAbsolutePath() + "'!" );
					} catch(IOException e){
						log.info( "Can not write json results to file '" + jsonResultsFile.getAbsolutePath() + "'!" );
						log.info( "Error: " + e.getMessage() );
					}
				}

			} else {
				log.error( "Could not convert JSON File '" + file.getName() + "' to TSP Model!" );
			}
			log.info( "##################### End: " + file.getName() + " #####################" );
		}
	}

	private static Options createOptions(){
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

		Option outputDir = Option.builder( "o" )
						.longOpt( "outputDir" )
						.argName( "directory" )
						.hasArg()
						.required( false )
						.desc( "set directory for outputs like option -c and -r (default: tests)" )
						.build();
		options.addOption( outputDir );

		Option csvDir = Option.builder( "c" )
						.longOpt( "csv" )
						.required( false )
						.desc( "use output directory (option: -o) to create file/s with CSV result/s for each solved problem" )
						.build();
		options.addOption( csvDir );

		Option jsonResults = Option.builder( "r" )
						.longOpt( "results" )
						.required( false )
						.desc( "use output directory (option: -o) to create file/s with json results for each solved problem" )
						.build();
		options.addOption( jsonResults );

		//TODO add option for different subtour elimination constraint versions (MTZ, etc.)

		return options;
	}

}