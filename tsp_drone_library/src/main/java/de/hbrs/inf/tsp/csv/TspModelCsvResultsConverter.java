package de.hbrs.inf.tsp.csv;

import de.hbrs.inf.tsp.Defines;
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
		resultList.add( Boolean.toString( tspModel.isLazyActive() ) );
		resultList.add( tspModel.getHostname() );
		resultList.add( Integer.toString( tspModel.getDimension() ) );
		resultList.add( Double.toString( tspModel.getResult().getRuntime() ) );
		resultList.add( Boolean.toString( tspModel.getResult().isOptimal() ) );
		resultList.add( Double.toString( tspModel.getResult().getObjective() ) );
		resultList.add( Integer.toString( tspModel.getDecisionVariablesCounter() ) );
		resultList.add( Integer.toString( tspModel.getTotalConstraintsCounter() ) );
		resultList.add( Integer.toString( tspModel.getAdditionalConstraintsCounter() ) );
		resultList.add( Integer.toString( tspModel.getResult().getIterationCounter() ) );

		String type = tspModel.getType();
		//add additional results
		switch( type ) {
			case Defines.PDSTSP:
				Pdstsp pdstsp = (Pdstsp) tspModel;
				resultList.add( Double.toString( pdstsp.getTruckSpeed() ) );
				resultList.add( Double.toString( pdstsp.getDroneSpeed() ) );
				resultList.add( Integer.toString( pdstsp.getDroneFleetSize() ) );
				resultList.add( Integer.toString( pdstsp.getDroneDeliveryPossibleAndInFlightRangeCounter() ) );
				resultList.add( Double.toString( pdstsp.getDroneFlightRangePercentage() / 100.0 ) );
				break;
		}

		return convertListToCsvString( resultList );
	}

	public static String getCsvHeaderString( String type ) {
		ArrayList<String> parameterList = new ArrayList<>();

		//add all common parameters
		parameterList.add( "Name" );
		parameterList.add( "Type" );
		parameterList.add( "IsLazyActive" );
		parameterList.add( "Hostname" );
		parameterList.add( "Dimension" );
		parameterList.add( "Runtime [s]" );
		parameterList.add( "IsOptimal" );
		parameterList.add( "Objective" );
		parameterList.add( "DecisionVariables" );
		parameterList.add( "TotalConstraints" );
		parameterList.add( "AdditionalConstraints" );
		parameterList.add( "Iterations" );

		//add additional parameters
		switch( type ) {
			case Defines.PDSTSP:
				parameterList.add( "TruckSpeed" );
				parameterList.add( "DroneSpeed" );
				parameterList.add( "DroneFleetSize" );
				parameterList.add( "DronesDeliveryPossibleAndInFlightRange" );
				parameterList.add( "DroneFlightRangePercentage" );
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
