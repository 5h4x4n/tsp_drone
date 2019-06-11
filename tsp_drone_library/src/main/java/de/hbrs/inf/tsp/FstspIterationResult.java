package de.hbrs.inf.tsp;

import java.util.ArrayList;

public class FstspIterationResult extends TspModelIterationResult{

	private ArrayList<Integer[]> droneFlights;

	@Override public String getSolutionString(){
		StringBuilder solutionString = new StringBuilder( super.getSolutionString() ).append( "\n" );
		solutionString.append( "Drone_Flights_Size" ).append( ": " ).append( droneFlights.size() ).append( "\n" );
		for( int v = 0; v < droneFlights.size(); v++ ){
			if( droneFlights.get( v ).length > 0 ){
				solutionString.append( "Drone_Flight_" ).append( v ).append( ": " );
				for( int i = 0; i < droneFlights.get( v ).length; i++ ){
					solutionString.append( droneFlights.get( v )[i] ).append( ", " );
				}
				solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) ).append( "\n" );
			}
		}
		return solutionString.toString();
	}

	public void setDroneFlights( ArrayList<Integer[]> droneFlights ) {
		this.droneFlights = droneFlights;
	}
}