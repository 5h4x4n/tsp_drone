package de.hbrs.inf;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class DisplayApplication extends Application{

	public static void main( String[] args ){
		launch( args );
	}

	@Override
	public void start( Stage stage ) throws Exception{
		stage.setTitle( "TSP Drone - Display Application" );
		Group root = new Group();
		Canvas canvas = new Canvas( 800, 800 );
		GraphicsContext gc = canvas.getGraphicsContext2D();
		drawShapes( gc );
		root.getChildren().add( canvas );
		stage.setScene( new Scene( root ) );
		stage.show();
	}

	private void drawShapes( GraphicsContext gc ){
		gc.setFill( Color.BLUE );
		gc.setStroke( Color.BLACK );
		gc.setLineWidth( 2 );
		gc.strokeLine( 40, 10, 10, 40 );
		gc.fillOval( 10, 10, 5, 5 );
		gc.fillText( "0", 20, 15);
	}

}
