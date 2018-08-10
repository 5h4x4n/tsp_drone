package de.hbrs.inf;

import java.util.ArrayList;

public class PdstspIterationResult extends TspIterationResult {

	private ArrayList<Integer>[] dronesCustomers;

	public ArrayList<Integer>[] getDronesCustomers(){
		return dronesCustomers;
	}

	public void setDronesCustomers( ArrayList<Integer>[] dronesCustomers ){
		this.dronesCustomers = dronesCustomers;
	}

	public String getSolutionString(){
		StringBuilder solutionString = new StringBuilder( super.getSolutionString() ).append( "\n" );
		solutionString.append( "Drones Customer Flights\n" );
		for(int v = 0; v < dronesCustomers.length; v++){
			solutionString.append( "Drone_" ).append( v ).append( " Customers size: " ).append( dronesCustomers[v].size() ).append( "\n" );
			if( dronesCustomers[v].size() > 0 ){
				solutionString.append( "Drone_" ).append( v ).append( " Customers: " );
				for(int i = 0; i < dronesCustomers[v].size(); i++){
					solutionString.append( dronesCustomers[v].get( i ) ).append( ", " );
				}
				solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) ).append( "\n" );
			}
		}
		return solutionString.toString();
	}
}
