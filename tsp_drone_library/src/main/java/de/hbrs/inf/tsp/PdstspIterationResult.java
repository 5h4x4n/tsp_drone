package de.hbrs.inf.tsp;

import java.util.ArrayList;

public class PdstspIterationResult extends TspModelIterationResult {

	private ArrayList<Integer>[] dronesCustomers;

	public ArrayList<Integer>[] getDronesCustomers(){
		return dronesCustomers;
	}

	public void setDronesCustomers( ArrayList<Integer>[] dronesCustomers ){
		this.dronesCustomers = dronesCustomers;
	}

	@Override
	public String getSolutionString(){
		StringBuilder solutionString = new StringBuilder( super.getSolutionString() ).append( "\n" );
		for(int v = 0; v < dronesCustomers.length; v++){
			solutionString.append( "Drone_" ).append( v ).append( "_Customers_Size: " ).append( dronesCustomers[v].size() ).append( "\n" );
			if( dronesCustomers[v].size() > 0 ){
				solutionString.append( "Drone_" ).append( v ).append( "_Customers: " );
				for(int i = 0; i < dronesCustomers[v].size(); i++){
					solutionString.append( dronesCustomers[v].get( i ) ).append( ", " );
				}
				solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 1 ) ).append( "\n" );
			}
		}
		return solutionString.substring( 0, solutionString.length() - 2 );
	}
}
