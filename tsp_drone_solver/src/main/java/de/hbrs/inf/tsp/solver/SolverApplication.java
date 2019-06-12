package de.hbrs.inf.tsp.solver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.hbrs.inf.tsp.Defines;
import de.hbrs.inf.tsp.Pdstsp;
import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.TspModelResult;
import de.hbrs.inf.tsp.csv.HeuristicValueReader;
import de.hbrs.inf.tsp.csv.TspModelCsvResultsConverter;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.json.TspLibJson;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
			printHelp( options );
			return;
		}

		//read options and prepare configuration for the application
		if( cmd.hasOption( "l" ) ){
			Configuration.setLogFile( cmd.getOptionValue( "l" ) );
		}

		if( cmd.hasOption( "d" ) ){
			Configuration.setLogLevel( "DEBUG" );
		}
		Configuration.setSystemProperties();

		log = Logger.getLogger( SolverApplication.class.getName() );
		log.info( "Start application" );

		if( cmd.hasOption( "j" ) ){
			Configuration.setJsonFileOrDir( cmd.getOptionValue( "j" ) );
			log.info( "Set json file/directory: " + Configuration.getJsonFileOrDir() );
		}

		if( cmd.hasOption( "hvf" ) ){
			Configuration.setHeuristicValuesFile( cmd.getOptionValue( "hvf" ) );
			log.info( "Set heuristic values file: " + Configuration.getHeuristicValuesFile() );
		}

		if( cmd.hasOption( "ph" ) ){
			try{
				Configuration.setPresolveHeuristicType( Defines.PresolveHeuristicType.valueOf( cmd.getOptionValue( "ph" ).toUpperCase() ) );
			} catch( IllegalArgumentException e ){
				log.info( "PresolveHeuristicType + '" + cmd.getOptionValue( "ph" ) + "' not supported!" );
				printHelp( options );
				return;
			}
		}

		if( cmd.hasOption( "nl" ) ){
			Configuration.setLazyActive( false );
		}
		log.info( "IsLazyActive set to: " + Configuration.isLazyActive() );

		if( cmd.hasOption( "tc" ) ){
			Configuration.setThreadCount( Integer.parseInt( cmd.getOptionValue( "tc" ) ) );
		}
		log.info( "ThreadCount set to: " + Configuration.getThreadCount() );

		if( cmd.hasOption( "td" ) ){
			Configuration.setTestDescription( cmd.getOptionValue( "td" ) );
		}
		log.info( "TestDescription set to: " + Configuration.getTestDescription() );

		log.info( "Set log level: " + System.getProperty( "log4j.logLevel" ) );
		log.info( "Set log file: " + Configuration.getLogFile() );

		if( cmd.hasOption( "a" ) ){
			Configuration.setAllCustomersByDrones( true );
		}
		log.info( "AllCustomersByDrones: " + Configuration.isAllCustomersByDrones() );

		if( cmd.hasOption( "ts" ) ){
			double[] truckSpeeds = parseStringArrayToDoubleArray( cmd.getOptionValues( "ts" ) );
			Configuration.setTruckSpeeds( truckSpeeds );
			log.info( "TruckSpeeds set to: " + Arrays.toString( truckSpeeds ) );
		}

		if( cmd.hasOption( "ds" ) ){
			double[] droneSpeeds = parseStringArrayToDoubleArray( cmd.getOptionValues( "ds" ) );
			Configuration.setDroneSpeeds( droneSpeeds );
			log.info( "DroneSpeeds set to: " + Arrays.toString( droneSpeeds ) );
		}

		if( cmd.hasOption( "dfs" ) ){
			int[] droneFleetSizes = parseStringArrayToIntArray( cmd.getOptionValues( "dfs" ) );
			Configuration.setDroneFleetSizes( droneFleetSizes );
			log.info( "DroneFleetSizes set to: " + Arrays.toString( droneFleetSizes ) );
		}

		if( cmd.hasOption( "dfr" ) ){
			int[] droneFlightRanges = parseStringArrayToIntArray( cmd.getOptionValues( "dfr" ) );
			Configuration.setDroneFlightRanges( droneFlightRanges );
			log.info( "DroneFlightRanges set to: " + Arrays.toString( droneFlightRanges ) );
		}

		if( cmd.hasOption( "ms" ) ){
			Configuration.setMaxOptimizationSeconds( Integer.parseInt( cmd.getOptionValue( "ms" ) ) );
			log.info( "MaxOptimizationSeconds set to: " + Configuration.getMaxOptimizationSeconds() );
		}

		//check if file or directory is given for tsp problem/s
		File fileOrDir = new File( Configuration.getJsonFileOrDir() );
		File[] jsonFiles;
		if( !fileOrDir.exists() ){
			log.warn( "Given json parameter '" + Configuration.getJsonFileOrDir() + "' is no existing file or directory!" );
			return;
		}
		if( fileOrDir.isFile() ){
			jsonFiles = new File[1];
			jsonFiles[0] = fileOrDir;
		} else {
			jsonFiles = fileOrDir.listFiles();
		}

		StringBuilder outputPath = new StringBuilder( "tests/" );
		if( cmd.hasOption( "o" ) ){
			outputPath = new StringBuilder( cmd.getOptionValue( "o" ) );
			if( !( outputPath.charAt( outputPath.length() - 1 ) == '/' ) ){
				outputPath.append( "/" );
			}
		}
		String datetime = new SimpleDateFormat( "dd-MM-yyyy_HH-mm-ss" ).format( new Date() );
		String fileOrDirName = fileOrDir.getName();
		//Remove file extension for output directory
		if( fileOrDir.isFile() ){
			fileOrDirName = fileOrDirName.substring( 0, fileOrDirName.lastIndexOf( '.' ) );
		}
		outputPath.append( fileOrDirName ).append( "_" ).append( datetime );

		//Add hostname (if available) to output dir
		String hostname;
		try{
			hostname = InetAddress.getLocalHost().getHostName();
		} catch( UnknownHostException e ){
			log.warn( "Could not get hostname of local machine!" );
			hostname = "Unknown";
		}
		log.info( "Set hostname to '" + hostname + "'." );
		Configuration.setHostname( hostname );
		outputPath.append( "_" ).append( Configuration.getHostname() );

		//Add threadCount to output dir
		outputPath.append( "_tc-" ).append( Configuration.getThreadCount() );

		//Add if lazy constraints or iterative mode is active to output dir
		outputPath.append( "_" );
		if( Configuration.isLazyActive() ){
			outputPath.append( "lazy" );
			if( cmd.hasOption( "hvf" ) ) {
				outputPath.append( "-heuristicValues" );
			}
		} else {
			outputPath.append( "iterative" );
		}

		outputPath.append( "_ph-" ).append( Configuration.getPresolveHeuristicType().getType() );

		Configuration.setOutputDirectory( outputPath.toString() );
		log.info( "Set output directory: " + Configuration.getOutputDirectory() );

		//check if any output option is set and create output directory if necessary
		if( cmd.hasOption( "c" ) || cmd.hasOption( "r" ) ){
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
		for( File file : jsonFiles ){
			log.info( "##################### Start: " + file.getName() + " #####################" );

			TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( file.getPath() );

			if( tspLibJson == null ){
				log.error( "Could not convert JSON Object '" + file.getName() + "' to TSP Model!" );
				continue;
			}

			double[] truckSpeeds = { -1.0 };
			double[] droneSpeeds = { -1.0 };
			int[] droneFleetSizes = { -1 };
			int[] droneFlightRanges = { -1 };

			String type = tspLibJson.getType();
			if( type.equals( Defines.PDSTSP ) || type.equals( Defines.FSTSP ) ){
				if( Configuration.getTruckSpeeds() != null ){
					truckSpeeds = Configuration.getTruckSpeeds();
				}
				if( Configuration.getDroneSpeeds() != null ){
					droneSpeeds = Configuration.getDroneSpeeds();
				}
				if( Configuration.getDroneFlightRanges() != null ){
					droneFlightRanges = Configuration.getDroneFlightRanges();
				}

				if( type.equals( Defines.PDSTSP ) ){
					if( Configuration.getDroneFleetSizes() != null ){
						droneFleetSizes = Configuration.getDroneFleetSizes();
					}
				}
			}

			List<double[]> alreadyCalculatedParameters = new ArrayList<>();
			TspModel tspModel = null;

			for( int ts = 0; ts < truckSpeeds.length; ts++ ){
				for( int ds = 0; ds < droneSpeeds.length; ds++ ){
					for( int dfs = 0; dfs < droneFleetSizes.length; dfs++ ){
						for( int dfr = 0; dfr < droneFlightRanges.length; dfr++ ){

							tspModel = JsonTspMapper
											.getTspModelFromJsonObject( tspLibJson, truckSpeeds[ts], droneSpeeds[ds], droneFleetSizes[dfs],
															droneFlightRanges[dfr], Configuration.isAllCustomersByDrones() );

							if( tspModel == null ){
								log.error( "Could not convert JSON file '" + file.getName() + "' to JSON Object!" );
								break;
							}

							if( hasSpeedRatioAlreadyBeenCalculated( alreadyCalculatedParameters, droneFleetSizes[dfs], droneFlightRanges[dfr],
											truckSpeeds[ts]/droneSpeeds[ds] ) ) {
								log.info( "Skip current tspModel, cause it has already been optimized with the same speed ratio!" );
								continue;
							}

							if( Configuration.getHeuristicValuesFile() != null ) {
								double heuristicValue = HeuristicValueReader.getHeuristicValue( tspModel, Configuration.getHeuristicValuesFile() );
								if( heuristicValue <= 0.0 ) {
									log.info( "No heuristic value for current model in file + '" + Configuration.getHeuristicValuesFile() +
													"' found! Skip model for test!" );
									continue;
								} else {
									tspModel.setHeuristicValue( heuristicValue );
								}
							}

							if( Configuration.getMaxOptimizationSeconds() > 0 ){
								tspModel.setMaxOptimizationSeconds( Configuration.getMaxOptimizationSeconds() );
							}

							tspModel.setLazyActive( Configuration.isLazyActive() );
							tspModel.setPresolveHeuristicType( Configuration.getPresolveHeuristicType() );
							tspModel.setHostname( Configuration.getHostname() );
							tspModel.setThreadCount( Configuration.getThreadCount() );
							tspModel.setTestDescription( Configuration.getTestDescription() );

							log.info( "Start Optimization for: " + tspModel.getName() );
							TspModelResult tspResults = tspModel.grbOptimize();

							double speedRatio = truckSpeeds[ts]/droneSpeeds[ds];
							log.info( "Speed ratio: " + speedRatio );
							alreadyCalculatedParameters.add( new double[] { droneFleetSizes[dfs], droneFlightRanges[dfr], speedRatio } );

							if( cmd.hasOption( "c" ) ){
								File csvFile = new File( Configuration.getOutputDirectory() + "/" + type + ".csv" );
								if( !csvFile.exists() ){
									try{
										csvFile.createNewFile();
										log.info( "Creation of csv result file '" + csvFile.getAbsolutePath() + "' was successful!" );
									} catch( IOException e ){
										log.info( "Can not create csv result file '" + csvFile.getAbsolutePath() + "'!" );
										log.info( "Error: " + e.getMessage() );
										continue;
									}

									try{
										StringBuilder headerString = new StringBuilder(
														TspModelCsvResultsConverter.getCsvHeaderString( type ) )
														.append( System.lineSeparator() );
										Files.write( Paths.get( csvFile.toURI() ), headerString.toString().getBytes() );
										log.info( "Header written to csv result file '" + csvFile.getAbsolutePath() + "'!" );
									} catch( IOException e ){
										log.info( "Can not write csv results header to file '" + csvFile.getAbsolutePath() + "'!" );
										log.info( "Error: " + e.getMessage() );
										continue;
									}

								}

								try{
									StringBuilder csvString = new StringBuilder(
													TspModelCsvResultsConverter.getCsvResultString( tspModel ) )
													.append( System.lineSeparator() );
									Files.write( Paths.get( csvFile.toURI() ), csvString.toString().getBytes(), StandardOpenOption.APPEND );
									log.info( "Results written to results file '" + csvFile.getAbsolutePath() + "'!" );
								} catch( IOException e ){
									log.info( "Can not write csv results to file '" + csvFile.getAbsolutePath() + "'!" );
									log.info( "Error: " + e.getMessage() );
									continue;
								}

							}

							if( cmd.hasOption( "r" ) ){
								StringBuilder jsonResultsFileName = new StringBuilder(
												file.getName().substring( 0, file.getName().lastIndexOf( '.' ) ) );
								jsonResultsFileName.append( "_" ).append( tspModel.getType() );
								if( type.equals( Defines.PDSTSP ) || type.equals( Defines.FSTSP ) ){
									Pdstsp pdstsp = (Pdstsp)tspModel;
									jsonResultsFileName.append( "_ts-" ).append( pdstsp.getTruckSpeed() ).append( "_ds-" ).append( pdstsp.getDroneSpeed() );

									if( type.equals( Defines.PDSTSP ) ){
										jsonResultsFileName.append( "_dfs-" ).append( pdstsp.getDroneFleetSize() );
									}

									jsonResultsFileName.append( "_dfr-" ).append( pdstsp.getDroneFlightRangePercentage() ).append( "_ms-" )
													.append( Configuration.getMaxOptimizationSeconds() );
								}
								jsonResultsFileName.append( ".results.json" );
								File jsonResultsFile = new File( Configuration.getOutputDirectory() + "/" + jsonResultsFileName );
								if( !jsonResultsFile.exists() ){
									try{
										jsonResultsFile.createNewFile();
										log.info( "Creation of json results file '" + jsonResultsFile.getAbsolutePath()
														+ "' was successful!" );
									} catch( IOException e ){
										log.info( "Can not create json results file '" + jsonResultsFile.getAbsolutePath() + "'!" );
										log.info( "Error: " + e.getMessage() );
									}
								}
								Gson gson = new GsonBuilder().setPrettyPrinting().create();
								try{
									Files.write( Paths.get( jsonResultsFile.toURI() ), gson.toJson( tspModel ).getBytes(),
													StandardOpenOption.CREATE );
									log.info( "JSON results written to results file '" + jsonResultsFile.getAbsolutePath() + "'!" );
								} catch( IOException e ){
									log.info( "Can not write json results to file '" + jsonResultsFile.getAbsolutePath() + "'!" );
									log.info( "Error: " + e.getMessage() );
								}
							}
						}
					}
				}
			}

			log.info( "##################### End: " + file.getName() + " #####################" );
		}
	}

	private static void printHelp( Options options ){
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp( "tsp_drone_solver", options );
	}

	private static boolean hasSpeedRatioAlreadyBeenCalculated( List<double[]> alreadyCalculatedParameters, int droneFleetSize,
					int droneFlightRange, double speedRatio ){
		for( double[] parameters : alreadyCalculatedParameters ){
			if( parameters[0] == droneFleetSize && parameters[1] == droneFlightRange && parameters[2] == speedRatio ) {
				return true;
			}
		}
		return false;
	}

	private static Options createOptions(){
		Options options = new Options();

		Option debug = Option.builder( "d" ).longOpt( "debug" ).required( false ).desc( "set log level to debug" ).build();
		options.addOption( debug );

		Option noLazy = Option.builder( "nl" ).longOpt( "noLazy" ).required( false )
							  .desc( "deactivate lazy constraints and" + "activate iterative solving" ).build();
		options.addOption( noLazy );

		Option logFile = Option.builder( "l" ).longOpt( "logFile" ).argName( "file" ).hasArg().required( false )
							   .desc( "use given file for log" ).build();
		options.addOption( logFile );

		Option jsonFileOrDir = Option.builder( "j" ).longOpt( "json" ).argName( "file_or_directory" ).hasArg().required( true )
									 .desc( "read tsp problem/s in json format from file or directory (required parameter)" ).build();
		options.addOption( jsonFileOrDir );

		Option outputDir = Option.builder( "o" ).longOpt( "outputDir" ).argName( "directory" ).hasArg().required( false )
								 .desc( "set directory for outputs like option -c and -r (default: tests)" ).build();
		options.addOption( outputDir );

		Option csvDir = Option.builder( "c" ).longOpt( "csv" ).required( false )
							  .desc( "use output directory (option: -o) to create file/s with CSV result/s for each solved problem" )
							  .build();
		options.addOption( csvDir );

		Option heuristicValuesFile = Option.builder( "hvf" ).longOpt( "heuristicValuesFile" ).argName( "csv_file" ).hasArg().required( false )
									 .desc( "read heuristic value/s from csv file" ).build();
		options.addOption( heuristicValuesFile );

		Option jsonResults = Option.builder( "r" ).longOpt( "results" ).required( false )
								   .desc( "use output directory (option: -o) to create file/s with json results for each solved problem" )
								   .build();
		options.addOption( jsonResults );

		Option droneDeliveryPossibleForAllCustomers = Option.builder( "a" ).longOpt( "allCustomersByDrones" ).required( false )
															.desc( "for tsp variants with drones all customers are set to be possible served by drones" )
															.build();
		options.addOption( droneDeliveryPossibleForAllCustomers );

		Option truckSpeeds = Option.builder( "ts" ).longOpt( "truckSpeed" ).required( false ).argName( "speeds" )
								   .numberOfArgs( Option.UNLIMITED_VALUES )
								   .desc( "for tsp variants with parameter 'truckSpeed' it will iterative set to the given parameters separated by"
												   + " spaces (i.e. 1.0 2.0 5.0 10.0)" ).build();
		options.addOption( truckSpeeds );

		Option droneSpeeds = Option.builder( "ds" ).longOpt( "droneSpeed" ).required( false ).argName( "speeds" )
								   .numberOfArgs( Option.UNLIMITED_VALUES )
								   .desc( "for tsp variants with drones the droneSpeed will iterative set to the given parameters separated by"
												   + " spaces (i.e. 1.0 2.0 5.0 10.0)" ).build();
		options.addOption( droneSpeeds );

		Option droneFleetSize = Option.builder( "dfs" ).longOpt( "droneFleetSize" ).required( false ).argName( "sizes" )
									  .numberOfArgs( Option.UNLIMITED_VALUES )
									  .desc( "for tsp variants with drones the droneFleetSize will be iterative set to the given parameters separated by"
													  + " spaces (i.e. 1 5 10)" ).build();
		options.addOption( droneFleetSize );

		Option droneFlightRanges = Option.builder( "dfr" ).longOpt( "droneFlightRange" ).required( false ).argName( "ranges" )
										 .numberOfArgs( Option.UNLIMITED_VALUES )
										 .desc( "for tsp variants with drones the droneFlightRange will iterative set to the given parameters separated by"
														 + " spaces (i.e. 5 10 20). The parameters are given in percentage and they are in relation to the"
														 + " furthest customer from the depot." ).build();
		options.addOption( droneFlightRanges );

		Option maxOptimizationSeconds = Option.builder( "ms" ).longOpt( "maxSeconds" ).required( false ).argName( "seconds" ).hasArg()
											  .desc( "an optimization process will be canceled when the total runtime exceeds the given parameter in seconds." )
											  .build();
		options.addOption( maxOptimizationSeconds );

		Option testDescription = Option.builder( "td" ).longOpt( "testDescription" ).required( false ).argName( "comment" ).hasArg()
						.desc( "a description/comment of the test settings, which will be written to the results." )
						.build();
		options.addOption( testDescription );

		Option threadCount = Option.builder( "tc" ).longOpt( "threadCount" ).required( false ).argName( "number of threads to use" )
								   .hasArg().desc( "the optimization process will use this number of threads for parallelism." ).build();
		options.addOption( threadCount );

		StringBuilder supportedPresolveHeuristicTypes = new StringBuilder();
		for( Defines.PresolveHeuristicType presolveHeuristicType : Defines.PresolveHeuristicType.values() ){
			supportedPresolveHeuristicTypes.append( presolveHeuristicType.getType() ).append( " " );
		}
		Option presolveHeuristicType = Option.builder( "ph" ).longOpt( "presolveHeuristic" ).required( false ).argName( "presolve heuristic type" ).hasArg()
						.desc( "if the option is set a presolve heuristic will be used. its solution will be used for the start values in the optimization process. supported presolveHeuristicTypes are: "
										+ supportedPresolveHeuristicTypes ).build();

		options.addOption( presolveHeuristicType );

		return options;
	}

	private static double[] parseStringArrayToDoubleArray( String[] stringArray ){
		log.debug( "Try to parse String array to double array; " + Arrays.toString( stringArray ) );
		double[] values = new double[stringArray.length];
		for( int i = 0; i < values.length; i++ ){
			values[i] = Double.parseDouble( stringArray[i] );
		}
		return values;
	}

	private static int[] parseStringArrayToIntArray( String[] stringArray ){
		log.debug( "Try to parse String array to int array; " + Arrays.toString( stringArray ) );
		int[] values = new int[stringArray.length];
		for( int i = 0; i < values.length; i++ ){
			values[i] = Integer.parseInt( stringArray[i] );
		}
		return values;
	}
}