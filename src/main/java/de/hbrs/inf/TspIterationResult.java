package de.hbrs.inf;

import org.apache.log4j.Logger;

import java.util.ArrayList;

public class TspIterationResult{

	private static Logger log = Logger.getLogger( de.hbrs.inf.TspIterationResult.class.getName() );

	private double iterationRuntime;
	private ArrayList<ArrayList<Integer>> truckTours;

	public double getIterationRuntime(){
		return iterationRuntime;
	}

	public void setIterationRuntime( double iterationRuntime ){
		this.iterationRuntime = iterationRuntime;
	}

	public ArrayList<ArrayList<Integer>> getTruckTours(){
		return truckTours;
	}

	public void setTruckTours( ArrayList<ArrayList<Integer>> truckTours ){
		this.truckTours = truckTours;
	}

	public String getSolutionString(){
		StringBuilder solutionString = new StringBuilder( "\nTruck Tours size: " ).append( truckTours.size() );
		if( truckTours.size() > 0 ){
			for( int i = 0; i < truckTours.size(); i++ ) {
				solutionString.append( "\nTruck Tour " ).append( i ).append( " Size: " ).append( truckTours.get( i ).size() );
				solutionString.append( "\nTruck Tour " ).append( i ).append( ": " );
				for( int j = 0; j < truckTours.get( i ).size(); j++ ) {
					solutionString.append( truckTours.get( i ).get( j ) ).append( ", " );
				}
			}
		}
		return new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) ).toString();
	}

}
