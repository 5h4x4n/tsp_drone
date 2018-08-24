package de.hbrs.inf.tsp.csv;

import de.hbrs.inf.Defines;
import de.hbrs.inf.tsp.Pdstsp;
import de.hbrs.inf.tsp.TspModel;

import java.util.ArrayList;
import java.util.List;

public class TspModelCsvResultsConverter{
	public static String getCsvResultString( TspModel tspModel ) {
		ArrayList<String> resultList = new ArrayList<>();

		//add all common results
		resultList.add( tspModel.getName() );
		resultList.add( tspModel.getType() );
		resultList.add( Integer.toString( tspModel.getDimension() ) );
		resultList.add( Integer.toString( tspModel.getDecisionVariablesCounter() ) );
		resultList.add( Integer.toString( tspModel.getTotalConstraintsCounter() ) );
		resultList.add( Integer.toString( tspModel.getAdditionalConstraintsCounter() ) );
		resultList.add( Integer.toString( tspModel.getTspResults().getIterationCounter() ) );
		resultList.add( Double.toString( tspModel.getTspResults().getRuntime() ) );
		resultList.add( Double.toString( tspModel.getTspResults().getObjective() ) );

		String type = tspModel.getType();
		//add additional results
		switch( type ) {
			case Defines.PDSTSP:
				Pdstsp pdstsp = (Pdstsp) tspModel;
				resultList.add( Double.toString( pdstsp.getTruckSpeed() ) );
				resultList.add( Double.toString( pdstsp.getDroneSpeed() ) );
				double droneFlightRange = pdstsp.getDroneFlightRange();
				resultList.add( Double.toString( droneFlightRange ) );
				resultList.add( Integer.toString( pdstsp.getDroneFleetSize() ) );
				resultList.add( Integer.toString( pdstsp.getDroneDeliveryPossibleAndInFlightRangeCounter() ) );
				double maxDistance = pdstsp.getMaximumCustomerDistance();
				resultList.add( Double.toString( maxDistance ) );
				resultList.add( Double.toString( droneFlightRange / maxDistance ) );
				break;
		}

		return convertListToCsvString( resultList );
	}

	public static String getCsvHeaderString( String type ) {
		ArrayList<String> parameterList = new ArrayList<>();

		//add all common parameters
		parameterList.add( "Name" );
		parameterList.add( "Type" );
		parameterList.add( "Dimension" );
		parameterList.add( "Decision Variables" );
		parameterList.add( "Total Constraints" );
		parameterList.add( "Additional Constraints" );
		parameterList.add( "Iterations" );
		parameterList.add( "Runtime [s]" );
		parameterList.add( "Objective" );

		//add additional parameters
		switch( type ) {
			case Defines.PDSTSP:
				parameterList.add( "Truck Speed" );
				parameterList.add( "Drone Speed" );
				parameterList.add( "Drone Flight Range (DFR)" );
				parameterList.add( "Drone Fleet Size" );
				parameterList.add( "Drones Delivery Possible and in Flight Range" );
				parameterList.add( "Maximum Customer Distance (MCD)" );
				parameterList.add( "DFR/MCD" );
				break;
		}

		return convertListToCsvString( parameterList );
	}

	private static String convertListToCsvString( List<String> list ) {
		StringBuilder stringBuilder = new StringBuilder();
		for( String string : list ) {
			stringBuilder.append( string ).append( ";" );
		}
		return stringBuilder.toString().substring( 0, stringBuilder.length() - 1 );
	}

}
