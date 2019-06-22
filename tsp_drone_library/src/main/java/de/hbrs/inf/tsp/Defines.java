package de.hbrs.inf.tsp;

public class Defines{

	public static final String TSP = "TSP";
	public static final String PDSTSP = "PDSTSP";
	public static final String FSTSP = "FSTSP";

	public static class Strings {
		public static final String TYPE = "Type";
		public static final String NAME = "Name";
		public static final String IS_LAZY_ACTIVE = "IsLazyActive";
		public static final String HOSTNAME = "Hostname";
		public static final String DIMENSION = "Dimension";
		public static final String RUNTIME_TOTAL = "RuntimeTotal [s]";
		public static final String RUNTIME_OPTIMIZATION = "RuntimeOptimization [s]";
		public static final String RUNTIME_MODEL_CALCULATION = "RuntimeModelCalculation [s]";
		public static final String RUNTIME_PRESOLVE_HEURISTIC = "RuntimePresolveHeuristic [s]";
		public static final String IS_OPTIMAL = "IsOptimal";
		public static final String OBJECTIVE = "Objective";
		public static final String DECISION_VARIABLES = "DecisionVariables";
		public static final String TOTAL_CONSTRAINTS = "TotalConstraints";
		public static final String ADDITIONAL_CONSTRAINTS = "AdditionalConstraints";
		public static final String ITERATIONS = "Iterations";
		public static final String TRUCK_SPEED = "TruckSpeed";
		public static final String DRONE_SPEED = "DroneSpeed";
		public static final String DRONE_FLEET_SIZE = "DroneFleetSize";
		public static final String DRONES_DELIVERY_POSSIBLE_AND_IN_FLIGHT_RANGE = "DronesDeliveryPossibleAndInFlightRange";
		public static final String DRONE_FLIGHT_RANGE_PERCENTAGE = "DroneFlightRangePercentage";
		public static final String USED_HEURISTIC_VALUE = "UsedHeuristicValue";
		public static final String TRUCK_SPEED_DRONE_SPEED_RATIO = "SpeedRatio" ;
		public static final String ERROR_CODE = "ErrorCode";
		public static final String THREAD_COUNT = "ThreadCount";
		public static final String MAX_RUNTIME_SECONDS = "MaxSeconds";
		public static final String TEST_DESCRIPTION = "testDescription";
		public static final String PRESOLVE_HEURISTIC_TYPE = "PresolveHeuristicType";
		public static final String OBJECTIVE_BOUND = "ObjectiveBound";
		public static final String OBJECTIVE_GAP = "ObjectiveGap";
		public static final String POSSIBLE_DRONE_FLIGHTS = "PossibleDroneFlights";
	}

	public enum PresolveHeuristicType{

		NONE( "None" ), TSP( "TSP" ), FSTSP( "FSTSP" );

		private String type;

		PresolveHeuristicType( String type ){
			this.type = type.toUpperCase();
		}

		public String getType(){
			return type;
		}
	}

}
