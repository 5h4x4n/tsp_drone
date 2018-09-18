package de.hbrs.inf.tsp.graphics;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import de.hbrs.inf.tsp.Defines;
import de.hbrs.inf.tsp.Pdstsp;
import de.hbrs.inf.tsp.Tsp;
import de.hbrs.inf.tsp.TspModel;
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

	private static final int CANVAS_WIDTH = 1000;
	private static final int CANVAS_HEIGHT = 1000;
	private static final double NODE_SIZE = 8.0;
	private static final double DEPOT_SIZE = 1.5 * NODE_SIZE;
	private static final int DRONE_FLIGHT_RANGE_LINE_WIDTH = 2;
	private static final int EDGE_LINE_WIDTH = 2;

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
							log.info( "File '" + fileOrDir.getAbsolutePath() + "' is not from type '.results.json'. It will be skipped!" );
						}
					} else {
						log.info( "Directory '" + fileOrDir.getAbsolutePath() + "' will be browsed to search for json result files!" );
						File[] files = fileOrDir.listFiles();
						for( File file : files ) {
							if( file.isFile() && file.getName().endsWith( ".results.json" ) ) {
								jsonFiles.add( file );
								log.info( "Add file '" + fileOrDir.getAbsolutePath() + "' to json results list!" );
							} else {
								log.info( "File '" + fileOrDir.getAbsolutePath() + "' is not from type '.results.json'. It will be skipped!" );
							}
						}
					}
				} else {
					log.info( "Given argument '" + fileOrDirectory + "' is no file or directory. It will be skipped! ");
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

		for( File jsonFile : jsonFiles ) {

			TspModel tspModel = null;
			try {
				Gson gson = new Gson();
				JsonReader reader = new JsonReader( new FileReader( jsonFile ) );
				tspModel = gson.fromJson(reader, Tsp.class);
				log.info( "TspModel successfully read: " + tspModel.getName() );

				String tspType = tspModel.getType().toUpperCase();
				log.info( "TSP Type: " + tspType );

				switch( tspType ) {
					case Defines.TSP:
						reader = new JsonReader( new FileReader( jsonFile ) );
						tspModel = gson.fromJson(reader, Tsp.class);
						break;
					case Defines.PDSTSP:
						reader = new JsonReader( new FileReader( jsonFile ) );
						tspModel = gson.fromJson(reader, Pdstsp.class);
						break;
					default:
						log.info( "TSP Type '" + tspType + "' not supported yet." );
						tspModel = null;
				}
			} catch( FileNotFoundException e ) {
				log.error( "File not found '" + jsonFile + "'." );
			} catch( Exception e ) {
				log.error( "Something went wrong while reading json result File! Error message: " + e.getMessage() );
			}

			if( tspModel == null ) {
				log.error( "tspModel is null! Exit!" );
				System.exit(0);
			}





			double[][] nodeCoordinates = tspModel.getNodes();
			if( nodeCoordinates == null ) {
				log.warn( "Can not display result files without node coordinates! Exit!" );
				System.exit(0);
			}
			log.info( "nodes: \n" + Arrays.deepToString( nodeCoordinates ) );

			//TODO add coordinates transformation for CANVAS_WIDTH and CANVAS_HEIGHT and turn y-coordinate

			Node[] nodes = new Node[tspModel.getDimension()];

			for( int i = 0; i < nodes.length; i++ ) {
				Node.NodeType type = null;

				if( i == 0 ) {
					type = Node.NodeType.DEPOT;
				} else {
					//TODO check which type when not depot
					type = Node.NodeType.DRONE_DELIVERY_NOT_POSSIBLE;


				}

				nodes[i] = new Node( nodeCoordinates[i][0], nodeCoordinates[i][1], type );
			}

			stage.setTitle("TSP Drone - Display Application");
			Group root = new Group();
			Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
			GraphicsContext gc = canvas.getGraphicsContext2D();

			String tspType = tspModel.getType().toUpperCase();
			if ( tspType.equals( Defines.PDSTSP ) ) {
				drawDroneFlightRange(gc, nodes[0], ( (Pdstsp) tspModel ).getDroneFlightRange() );
			}

			//TODO draw edges
		/*
		drawEdge( gc, nodesPoints[0], nodesPoints[2], EdgeType.DRONE );
		drawEdge( gc, nodesPoints[0], nodesPoints[20], EdgeType.TRUCK );
		drawEdge( gc, nodesPoints[20], nodesPoints[3], EdgeType.TRUCK );
		drawEdge( gc, nodesPoints[3], nodesPoints[0], EdgeType.TRUCK );
 		*/

			for( Node node : nodes ) {
				drawNode( gc, node );
			}

			root.getChildren().add( canvas );
			stage.setScene( new Scene( root ) );
			stage.show();


			File resultDir = new File(jsonFile.getAbsolutePath().replace(".results.json", "" ) + "/" );
			resultDir.mkdirs();

			File imageFile = new File(resultDir.getAbsolutePath() + "/" + "test" + ".png" );
			imageFile.createNewFile();
			if( imageFile != null ) {
				try {
					WritableImage writableImage = new WritableImage( CANVAS_WIDTH, CANVAS_HEIGHT );
					canvas.snapshot(null, writableImage );
					RenderedImage renderedImage = SwingFXUtils.fromFXImage( writableImage, null );
					ImageIO.write( renderedImage, "png", imageFile );
				} catch( IOException ex ) {
					log.error( "Error while writing image: " + ex.getMessage() );
				}
			}
		}
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
		gc.setFill( Color.LIGHTGREEN );
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
		Color stroke = Color.BLACK;

		switch ( edge.getType() ) {
			case TRUCK:
				stroke = Color.BLACK;
				break;
			case DRONE:
				stroke = Color.GREEN;
				break;
		}

		gc.setStroke( stroke );

		gc.strokeLine( edge.getNode1().getX(), edge.getNode1().getY(), edge.getNode2().getX(), edge.getNode2().getY() );
	}

}
