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
		resultList.add( tspModel.getTestDescription() );
		resultList.add( Boolean.toString( tspModel.isLazyActive() ) );
		resultList.add( tspModel.getPresolveHeuristicType().getType() );
		resultList.add( tspModel.getHostname() );
		resultList.add( Integer.toString( tspModel.getThreadCount() ) );
		resultList.add( Integer.toString( tspModel.getDimension() ) );
		resultList.add( Boolean.toString( tspModel.getResult().isOptimal() ) );
		resultList.add( Double.toString( tspModel.getResult().getRuntimeTotal() ) );
		resultList.add( Double.toString( tspModel.getResult().getRuntimeGrbModelCalculation() ) );
		resultList.add( Double.toString( tspModel.getResult().getRuntimePresolveHeuristic() ) );
		resultList.add( Double.toString( tspModel.getResult().getRuntimeOptimization() ) );
		resultList.add( Integer.toString( tspModel.getMaxOptimizationSeconds() ) );
		resultList.add( Integer.toString( tspModel.getErrorCode() ) );
		resultList.add( Double.toString( tspModel.getResult().getObjective() ) );
		resultList.add( Double.toString( tspModel.getResult().getUsedHeuristicValue() ) );
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
				resultList.add( Double.toString( pdstsp.getTruckSpeed() / pdstsp.getDroneSpeed() ) );
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
		parameterList.add( Defines.Strings.NAME );
		parameterList.add( Defines.Strings.TYPE );
		parameterList.add( Defines.Strings.TEST_DESCRIPTION );
		parameterList.add( Defines.Strings.IS_LAZY_ACTIVE );
		parameterList.add( Defines.Strings.PRESOLVE_HEURISTIC_TYPE );
		parameterList.add( Defines.Strings.HOSTNAME );
		parameterList.add( Defines.Strings.THREAD_COUNT );
		parameterList.add( Defines.Strings.DIMENSION );
		parameterList.add( Defines.Strings.IS_OPTIMAL );
		parameterList.add( Defines.Strings.RUNTIME_TOTAL );
		parameterList.add( Defines.Strings.RUNTIME_MODEL_CALCULATION);
		parameterList.add( Defines.Strings.RUNTIME_PRESOLVE_HEURISTIC );
		parameterList.add( Defines.Strings.RUNTIME_OPTIMIZATION );
		parameterList.add( Defines.Strings.MAX_RUNTIME_SECONDS );
		parameterList.add( Defines.Strings.ERROR_CODE );
		parameterList.add( Defines.Strings.OBJECTIVE );
		parameterList.add( Defines.Strings.USED_HEURISTIC_VALUE );
		parameterList.add( Defines.Strings.DECISION_VARIABLES );
		parameterList.add( Defines.Strings.TOTAL_CONSTRAINTS );
		parameterList.add( Defines.Strings.ADDITIONAL_CONSTRAINTS );
		parameterList.add( Defines.Strings.ITERATIONS );

		//add additional parameters
		switch( type ) {
			case Defines.PDSTSP:
				parameterList.add( Defines.Strings.TRUCK_SPEED );
				parameterList.add( Defines.Strings.DRONE_SPEED );
				parameterList.add( Defines.Strings.TRUCK_SPEED_DRONE_SPEED_RATIO );
				parameterList.add( Defines.Strings.DRONE_FLEET_SIZE );
				parameterList.add( Defines.Strings.DRONES_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE );
				parameterList.add( Defines.Strings.DRONE_FLIGHT_RANGE_PERCENTAGE );
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
