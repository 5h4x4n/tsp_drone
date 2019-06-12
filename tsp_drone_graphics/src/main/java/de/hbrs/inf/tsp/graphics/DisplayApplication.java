package de.hbrs.inf.tsp.graphics;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.hbrs.inf.tsp.*;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisplayApplication extends Application{

	private static final int CANVAS_SIZE = 500;
	private static final double NODE_SIZE = 6.0;
	private static final double DEPOT_SIZE = 1.5 * NODE_SIZE;
	private static final int DRONE_FLIGHT_RANGE_LINE_WIDTH = 1;
	private static final int EDGE_LINE_WIDTH = 1;
	private static final int DASHES_DISTANCE = 5;
	private static final double NODE_MARGIN_DISTANCE_RATIO = 0.02;

	private static final Color[] DRONE_EDGE_COLORS = { Color.GREEN, Color.ORANGE, Color.PURPLE, Color.MAGENTA };

	private static Logger log;

	public static void main( String[] args ){
		launch( args );
	}

	@Override
	public void start( Stage stage ) throws Exception {

		//create options for command line arguments
		Options options = createOptions();

		//parse the options passed as command line arguments
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		Parameters params = getParameters();
		Object[] argumentsAsObjectArray = params.getRaw().toArray();
		String[] arguments = Arrays.copyOf( argumentsAsObjectArray, argumentsAsObjectArray.length, String[].class );
		try{
			cmd = parser.parse( options, arguments );
		} catch(ParseException e){
			System.out.println( "Error while parsing parameters! Error message: " + e.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "tsp_drone_graphics", options );
			System.exit( 0 );
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
		log = Logger.getLogger( DisplayApplication.class.getName() );

		List<File> jsonFiles = new ArrayList<>();
		if( cmd.hasOption( "j" ) ) {
			String[] filesAndDirectories = cmd.getOptionValues( "j" );
			log.info( "Given arguments for json result files and directories:" );
			for( String fileOrDirectory : filesAndDirectories ) {
				File fileOrDir = new File( fileOrDirectory );
				if( fileOrDir.exists() ) {
					if( fileOrDir.isFile() ) {
						if( fileOrDirectory.endsWith( ".results.json" ) ) {
							jsonFiles.add( fileOrDir );
							log.info( "Add file '" + fileOrDir.getAbsolutePath() + "' to json results list!" );
						} else {
							log.warn( "File '" + fileOrDir.getAbsolutePath() + "' is not from type '.results.json'. It will be skipped!" );
						}
					} else {
						log.info( "Directory '" + fileOrDir.getAbsolutePath() + "' will be browsed to search for json result files!" );
						File[] files = fileOrDir.listFiles();
						for( File file : files ) {
							if( file.isFile() && file.getName().endsWith( ".results.json" ) ) {
								jsonFiles.add( file );
								log.info( "Add file '" + fileOrDir.getAbsolutePath() + "' to json results list!" );
							} else {
								log.warn( "File '" + fileOrDir.getAbsolutePath() + "' is not from type '.results.json'. It will be skipped!" );
							}
						}
					}
				} else {
					log.warn( "Given argument '" + fileOrDirectory + "' is no file or directory. It will be skipped! ");
				}
			}
		} else {
			log.info( "No json result files or directories are given in program arguments. Start file chooser!" );

			FileChooser fileChooser = new FileChooser();
			FileChooser.ExtensionFilter extFilter =
					new FileChooser.ExtensionFilter("tsp json result files (*.results.json)", "*.results.json" );
			fileChooser.getExtensionFilters().add( extFilter );
			File jsonFile = fileChooser.showOpenDialog( stage );

			if( jsonFile == null ) {
				log.info( "No json result file selected. Exit!" );
				System.exit(0);
			} else {
				jsonFiles.add( jsonFile );
			}
		}

		log.info( "The following json result files will be used to display and save the iterations of the according TSP results:" );
		for ( File jsonFile : jsonFiles ) {
			log.info( jsonFile.getAbsolutePath() );
		}

		for( File jsonFile : jsonFiles ){

			log.info( "##################### Start: " + jsonFile.getName() + " #####################" );
			TspModel tspModel = null;
			String tspType = null;
			try{
				Gson gson = new Gson();
				JsonReader reader = new JsonReader( new FileReader( jsonFile ) );
				tspModel = gson.fromJson( reader, Tsp.class );
				log.info( "TspModel successfully read: " + tspModel.getName() );

				tspType = tspModel.getType().toUpperCase();
				log.info( "TSP Type: " + tspType );

				switch(tspType){
					case Defines.TSP:
						reader = new JsonReader( new FileReader( jsonFile ) );
						tspModel = gson.fromJson( reader, Tsp.class );
						break;
					case Defines.PDSTSP:
						reader = new JsonReader( new FileReader( jsonFile ) );
						tspModel = gson.fromJson( reader, Pdstsp.class );
						break;
					case Defines.FSTSP:
						reader = new JsonReader( new FileReader( jsonFile ) );
						tspModel = gson.fromJson( reader, Fstsp.class );
						break;
					default:
						log.info( "TSP Type '" + tspType + "' not supported yet." );
						tspModel = null;
				}
			} catch(FileNotFoundException e){
				log.error( "File not found '" + jsonFile + "'. Skip to next json result file if existing! Error message: " + e.getMessage() );
				continue;
			} catch(Exception e){
				log.error( "Something went wrong while reading json result File '" + jsonFile + "'! Skip to next json result file if existing! "
								+ "Error message: " + e.getMessage() );
				continue;
			}

			if( tspModel == null ){
				log.error( "tspModel for '" + jsonFile + "' is null! Skip to next json result file if existing!" );
				continue;
			}


			//TODO generalize log warnings
			if( tspModel.getNodes() == null ){
				log.warn( "The json result file '" + jsonFile + "' has no node coordinates so it can not be visualized! "
								+ "Skip to next json result file if existing!" );
				continue;
			}
			double[][] nodeCoordinates = tspModel.getNodes().clone();
			log.debug( "node coordinates: " + Arrays.deepToString( nodeCoordinates ) );

			//transform/normalize coordinates for CANVAS_SIZE
			double normalizeFactor = normalizeCoordinates( nodeCoordinates );
			log.debug( "normalize factor: " + normalizeFactor );
			log.debug( "normalized node coordinates: " + Arrays.deepToString( nodeCoordinates ) );
			//calculate new y-coordinates, cause the coordinate origin should be at the bottom instead of top
			for(double[] node : nodeCoordinates){
				node[1] = CANVAS_SIZE - node[1];
			}

			Node[] nodes = new Node[tspModel.getDimension()];

			for(int i = 0; i < nodes.length; i++){
				Node.NodeType type = null;
				switch(tspType){
					case Defines.TSP:
						type = Node.NodeType.DRONE_DELIVERY_NOT_POSSIBLE;
						break;
					case Defines.PDSTSP:
						Pdstsp pdstsp = (Pdstsp)tspModel;
						int[] droneDeliveryPossible = pdstsp.getDroneDeliveryPossible();
						ArrayList<Integer> droneDeliveryPossibleAndInFlightRange = pdstsp.getDroneDeliveryPossibleAndInFlightRange();
						if( i == 0 ){
							type = Node.NodeType.DEPOT;
						} else {
							type = Node.NodeType.DRONE_DELIVERY_NOT_POSSIBLE;

							for(int j : droneDeliveryPossible){
								if( i == j ){
									type = Node.NodeType.DRONE_DELIVERY_POSSIBLE_BUT_NOT_IN_FLIGHT_RANGE;
								}
							}

							if( droneDeliveryPossibleAndInFlightRange.contains( new Integer( i ) ) ){
								type = Node.NodeType.DRONE_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE;
							}
						}
						break;
					case Defines.FSTSP:
						Fstsp fstsp = (Fstsp)tspModel;
						ArrayList<Integer> droneDeliveryPossibleFstsp = new ArrayList<>();
						int[] droneDeliveryPossibleFstspArray = fstsp.getDroneDeliveryPossible();
						for( int node : droneDeliveryPossibleFstspArray ){
							droneDeliveryPossibleFstsp.add( node );
						}
						if( i == 0 ){
							type = Node.NodeType.DEPOT;
						} else {
							type = Node.NodeType.DRONE_DELIVERY_NOT_POSSIBLE;

							for( int j : droneDeliveryPossibleFstsp ){
								if( i == j ){
									type = Node.NodeType.DRONE_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE;
								}
							}
						}
						break;
				}
				nodes[i] = new Node( nodeCoordinates[i][0], nodeCoordinates[i][1], type );
			}

			TspModelResult result = tspModel.getResult();
			if( result == null ){
				log.warn( "The json result file '" + jsonFile + "' has no results so it can not be visualized! "
								+ "Skip to next json result file if existing!" );
				continue;
			}
			ArrayList<?> iterationResults = result.getIterationResults();
			if( iterationResults == null || iterationResults.size() < 1 ){
				log.warn( "The json result file '" + jsonFile + "' has no iteration results so it can not be visualized! "
								+ "Skip to next json result file if existing!" );
				continue;
			}

			//TODO add loop here for each iteration
			int iterationCount = 0;
			do{
				Group root = new Group();
				Canvas canvas = new Canvas( CANVAS_SIZE, CANVAS_SIZE );
				GraphicsContext gc = canvas.getGraphicsContext2D();
				root.getChildren().add( canvas );
				stage.setScene( new Scene( root ) );
				//stage.setTitle( "TSP Drone - Display Application - " + tspType + ": " + tspModel.getName() + " - Iteration " + iterationCount + "/"
				//				+ iterationResults.size() );

				if( tspType.equals( Defines.PDSTSP ) ){
					double normalizedDroneFlightRange = ((Pdstsp)tspModel).getDroneFlightRange() * normalizeFactor;
					log.info( "normalized drone flight range: " + normalizedDroneFlightRange );
					log.info( "check if normalized drone flight range can be correct. This can be the case if the node coordinates differs from the "
									+ "real coordinates or something like that." );
					boolean changeDroneFlightRange = false;
					double maxNodeFlightRange = 0;
					//change droneFlightRange when normalizedDroneFlightRange smaller than it should be
					for( int i : ((Pdstsp)tspModel).getDroneDeliveryPossibleAndInFlightRange() ) {
						double nodeFlightRange = calculateDistance( nodes[0], nodes[i] );
						log.debug( "node flight range for node " + i + ": " + nodeFlightRange );
						if( nodeFlightRange > maxNodeFlightRange ) {
							maxNodeFlightRange = nodeFlightRange;
						}
					}
					if( maxNodeFlightRange * 2.0 > normalizedDroneFlightRange ) {
						changeDroneFlightRange = true;
					} else {
						//change droneFlightRange when normalizedDroneFlightRange greater than it should be
						for( int i : ((Pdstsp)tspModel).getDroneDeliveryPossible() ){
							if( !((Pdstsp)tspModel).getDroneDeliveryPossibleAndInFlightRange().contains( i ) ){
								double distanceToDepot = calculateDistance( nodes[0], nodes[i] );
								if( distanceToDepot * 2.0 < normalizedDroneFlightRange ){
									changeDroneFlightRange = true;
									break;
								}
							}
						}
					}
					if( changeDroneFlightRange ) {
						normalizedDroneFlightRange = maxNodeFlightRange * 2.0;
						log.info( "Adjust normalized drone flight range to: " + normalizedDroneFlightRange );
					} else {
						log.info( "No adjustment for the normalized drone flight range necessary." );
					}

					drawDroneFlightRange( gc, nodes[0], normalizedDroneFlightRange );
				}

				//TODO
				if( tspType.equals( Defines.FSTSP ) ){
					double normalizedDroneFlightRange = ((Fstsp)tspModel).getDroneFlightRange() * normalizeFactor;
					normalizedDroneFlightRange *= (calculateDistance( nodes[0], nodes[1] ) / tspModel.getDistances()[0][1]);

					log.info( "normalized drone flight range: " + normalizedDroneFlightRange );

					//TODO maybe normalizedDroneFlightRange is not correct ...
					drawDroneFlightRange( gc, nodes[0], normalizedDroneFlightRange );
				}

				TspModelIterationResult iterationResult;
				if( iterationCount == 0 ) {
					iterationResult = (TspModelIterationResult)iterationResults.get( 0 );
				} else {
					iterationResult = (TspModelIterationResult)iterationResults.get( iterationCount - 1 );
				}
				log.debug( "Iteration " + iterationCount + "/" + iterationResults.size() + ": " + iterationResult.getSolutionString() );

				if( iterationCount > 0 ){
					if( tspType.equals( Defines.PDSTSP ) ){
						PdstspIterationResult pdstspIterationResult = (PdstspIterationResult)iterationResult;
						ArrayList<Integer>[] dronesCustomers = pdstspIterationResult.getDronesCustomers();
						for( int v = 0; v < dronesCustomers.length; v++ ){
							for(int droneCustomer : dronesCustomers[v]){
								drawEdge( gc, new Edge( nodes[0], nodes[droneCustomer], Edge.EdgeType.DRONE ), DRONE_EDGE_COLORS[v], DASHES_DISTANCE );
							}
						}
					}

					if( tspType.equals( Defines.FSTSP ) ){
						FstspIterationResult fstspIterationResult = (FstspIterationResult)iterationResult;
						ArrayList<Integer[]> dronesFlights = fstspIterationResult.getDroneFlights();
						for( Integer[] droneFlight : dronesFlights ){
							drawEdge( gc, new Edge( nodes[droneFlight[0]], nodes[droneFlight[2]], Edge.EdgeType.DRONE ) );
							drawEdge( gc, new Edge( nodes[droneFlight[1]], nodes[droneFlight[2]], Edge.EdgeType.DRONE ) );
						}
					}

					for(ArrayList<Integer> truckTour : iterationResult.getTruckTours()){
						for(int j = 0; j < truckTour.size(); j++){
							if( j < truckTour.size() - 1 ){
								drawEdge( gc, new Edge( nodes[truckTour.get( j )], nodes[truckTour.get( j + 1 )], Edge.EdgeType.TRUCK ) );
							} else {
								drawEdge( gc, new Edge( nodes[truckTour.get( j )], nodes[truckTour.get( 0 )], Edge.EdgeType.TRUCK ) );
							}
						}
					}
				}

				for( Node node : nodes ){
					drawNode( gc, node );
				}

				//TODO add option for saving images
				File resultDir = new File( jsonFile.getAbsolutePath().replace( ".results.json", "" ) + "/" );
				resultDir.mkdirs();
				String resultName = resultDir.getName();

				File imageFile = new File( resultDir.getAbsolutePath() + "/" + resultName + "_" + iterationCount + ".png" );
				if( imageFile.exists() ) {
					imageFile.delete();
				}
				if( imageFile.createNewFile() ){
					try{
						WritableImage writableImage = new WritableImage( CANVAS_SIZE, CANVAS_SIZE );
						canvas.snapshot( null, writableImage );
						RenderedImage renderedImage = SwingFXUtils.fromFXImage( writableImage, null );
						ImageIO.write( renderedImage, "png", imageFile );
						log.info( "Created image: " + imageFile.getAbsolutePath() );
					} catch(IOException ex){
						log.error( "Error while writing image: " + ex.getMessage() );
					}
				}
				//TODO add possibility to show each iteration and navigate through them
				//stage.show();

				iterationCount++;
			} while( iterationCount <= iterationResults.size() && iterationCount > 0 );
			//stage.close();
		}
		System.exit( 0 );
	}

	private Options createOptions(){
		Options options = new Options();

		Option debug = Option.builder( "d" ).longOpt( "debug" ).required( false ).desc( "set log level to debug" ).build();
		options.addOption( debug );

		Option logFile = Option.builder( "l" ).longOpt( "logFile" ).argName( "file" ).hasArg().required( false )
						.desc( "use given file for log" ).build();
		options.addOption( logFile );

		Option jsonResultFilesAndDirs = Option.builder( "j" ).longOpt( "json" ).argName( "files_and_directories" ).hasArg().required( false )
						.numberOfArgs( Option.UNLIMITED_VALUES )
						.desc( "read json result files from all given files and directories separated by spaces "
										+ "(i.e. ../tests/tsp1.result.json /Users/username/results_directory)" ).build();
		options.addOption( jsonResultFilesAndDirs );

		return options;
	}

	private void drawDroneFlightRange( GraphicsContext gc, Node depot, double droneFlightRange ) {
		gc.setStroke( Color.GREEN );
		gc.setFill( Color.LIGHTGRAY );
		gc.setLineWidth( DRONE_FLIGHT_RANGE_LINE_WIDTH );
		gc.fillOval( depot.getX() - 0.5 * droneFlightRange, depot.getY() - 0.5 * droneFlightRange, droneFlightRange, droneFlightRange );
		gc.strokeOval( depot.getX() - 0.5 * droneFlightRange, depot.getY() - 0.5 * droneFlightRange, droneFlightRange, droneFlightRange );
	}

	private void drawNode( GraphicsContext gc, Node node ) {
		Color fill = Color.BLACK;
		Color stroke = Color.BLACK;

		switch ( node.getType() ) {
			case DEPOT:
				fill = stroke = Color.BLUE;
				break;
			case DRONE_DELIVERY_NOT_POSSIBLE:
				fill = stroke = Color.BLACK;
				break;
			case DRONE_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE:
				fill = stroke = Color.GREEN;
				break;
			case DRONE_DELIVERY_POSSIBLE_BUT_NOT_IN_FLIGHT_RANGE:
				fill = stroke = Color.RED;
				break;
		}

		gc.setFill( fill );
		gc.setStroke( stroke );

		switch ( node.getType() ) {
			case DEPOT:
				gc.fillRect( node.getX() - 0.5 * DEPOT_SIZE, node.getY() - 0.5 * DEPOT_SIZE, DEPOT_SIZE, DEPOT_SIZE );
				break;
			case DRONE_DELIVERY_NOT_POSSIBLE:
			case DRONE_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE:
			case DRONE_DELIVERY_POSSIBLE_BUT_NOT_IN_FLIGHT_RANGE:
				gc.fillOval( node.getX() - 0.5 * NODE_SIZE, node.getY() - 0.5 * NODE_SIZE, NODE_SIZE, NODE_SIZE );
				break;
		}

	}

	private void drawEdge( GraphicsContext gc, Edge edge ) {
		gc.setLineWidth( EDGE_LINE_WIDTH );
		Color color = Color.BLACK;
		double dashesDistance = 0;

		switch ( edge.getType() ) {
			case TRUCK:
				color = Color.BLACK;
				break;
			case DRONE:
				dashesDistance = DASHES_DISTANCE;
				color = Color.GREEN;
				break;
		}

		drawEdge( gc, edge, color, dashesDistance );
	}

	private void drawEdge( GraphicsContext gc, Edge edge, Color color, double dashesDistance ){
		gc.setLineDashes( dashesDistance );
		gc.setStroke( color );
		gc.strokeLine( edge.getNode1().getX(), edge.getNode1().getY(), edge.getNode2().getX(), edge.getNode2().getY() );
		gc.setLineDashes( 0 );
	}

	private double normalizeCoordinates( double[][] nodeCoordinates ) {
		final double minCoordinate = CANVAS_SIZE * NODE_MARGIN_DISTANCE_RATIO;
		final double maxTmpCoordinate = CANVAS_SIZE * ( 1.0 - ( 2 * NODE_MARGIN_DISTANCE_RATIO ) );

		log.debug( "minCoordinate: " + minCoordinate );
		log.debug( "maxTmpCoordinate: " + maxTmpCoordinate );

		double minXNodeCoordinate = nodeCoordinates[0][0];
		double minYNodeCoordinate = nodeCoordinates[0][1];
		for( double[] node : nodeCoordinates ){
			if( node[0] < minXNodeCoordinate )
				minXNodeCoordinate = node[0];
			if( node[1] < minYNodeCoordinate )
				minYNodeCoordinate = node[1];
		}

		log.debug( "minXNodeCoordinate: " + minXNodeCoordinate );
		log.debug( "minYNodeCoordinate: " + minYNodeCoordinate );

		for( double[] node : nodeCoordinates ){
			node[0] = node[0] - minXNodeCoordinate;
			node[1] = node[1] - minYNodeCoordinate;
		}

		log.debug( "transformend node coordinates to zero: " + Arrays.deepToString( nodeCoordinates ) );

		double maxNodeCoordinate = 0;
		for( double[] node : nodeCoordinates ){
			if( node[0] > maxNodeCoordinate )
				maxNodeCoordinate = node[0];
			if( node[1] > maxNodeCoordinate )
				maxNodeCoordinate = node[1];
		}
		log.debug( "maxNodeCoordinate: " + maxNodeCoordinate );
		double normalizeFactor = maxTmpCoordinate / maxNodeCoordinate;

		//normalize and round to one decimal point
		for(double[] node : nodeCoordinates){
			node[0] = Math.round( (normalizeFactor * node[0]) * 10.0 ) / 10.0;
			node[1] = Math.round( (normalizeFactor * node[1]) * 10.0 ) / 10.0;
		}

		log.debug( "normalized node coordinates but not moved: " + Arrays.deepToString( nodeCoordinates ) );

		for( double[] node : nodeCoordinates ){
			node[0] += minCoordinate;
			node[1] += minCoordinate;
		}

		return normalizeFactor;
	}

	private double calculateDistance( Node n1, Node n2 ) {
		return Math.sqrt( Math.pow( ( n1.getX() - n2.getX() ), 2 ) + Math.pow( ( n1.getY() - n2.getY() ) , 2 ) );
	}
}
