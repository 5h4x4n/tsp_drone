package de.hbrs.inf.tsp.solver;

import de.hbrs.inf.tsp.Defines;

public class Configuration{

	private static String logFile = "tsp_drone_solver.log";
	private static String jsonFileOrDir = null;
	private static String logLevel = "INFO";
	private static String outputDirectory = null;
	private static String testDescription = "";
	private static boolean allCustomersByDrones = false;
	private static boolean lazyActive = true;
	private static boolean gurobiHeuristicsActive = true;
	private static Defines.PresolveHeuristicType presolveHeuristicType = Defines.PresolveHeuristicType.NONE;
	private static double[] truckSpeeds = null;
	private static double[] droneSpeeds = null;
	private static int[] droneFleetSizes = null;
	private static int[] droneFlightRanges = null;
	private static int maxOptimizationSeconds = -1;
	private static String hostname = null;
	private static int threadCount = 0;
	private static String heuristicValuesFile = null;

	public static void setSystemProperties() {
		System.setProperty( "log4j.logLevel", logLevel );
		System.setProperty( "log4j.logFile", logFile );
	}

	public static void setJsonFileOrDir( String jsonFileOrDir ){
		Configuration.jsonFileOrDir = jsonFileOrDir;
	}

	public static String getJsonFileOrDir(){
		return jsonFileOrDir;
	}

	public static String getLogFile(){
		return logFile;
	}

	public static void setLogFile( String logFile ){
		Configuration.logFile = logFile;
	}

	public static String getLogLevel(){
		return logLevel;
	}

	public static void setLogLevel( String logLevel ){
		Configuration.logLevel = logLevel;
	}

	public static void setOutputDirectory( String csvDirectory ){
		Configuration.outputDirectory = csvDirectory;
	}

	public static String getOutputDirectory(){
		return outputDirectory;
	}

	public static boolean isAllCustomersByDrones(){
		return allCustomersByDrones;
	}

	public static void setAllCustomersByDrones( boolean allCustomersByDrones ){
		Configuration.allCustomersByDrones = allCustomersByDrones;
	}

	public static double[] getTruckSpeeds(){
		return truckSpeeds;
	}

	public static void setTruckSpeeds( double[] truckSpeeds ){
		Configuration.truckSpeeds = truckSpeeds;
	}

	public static double[] getDroneSpeeds(){
		return droneSpeeds;
	}

	public static void setDroneSpeeds( double[] droneSpeeds ){
		Configuration.droneSpeeds = droneSpeeds;
	}

	public static int[] getDroneFleetSizes(){
		return droneFleetSizes;
	}

	public static void setDroneFleetSizes( int[] droneFleetSizes ){
		Configuration.droneFleetSizes = droneFleetSizes;
	}

	public static int[] getDroneFlightRanges(){
		return droneFlightRanges;
	}

	public static void setDroneFlightRanges( int[] droneFlightRanges ){
		Configuration.droneFlightRanges = droneFlightRanges;
	}

	public static int getMaxOptimizationSeconds(){
		return maxOptimizationSeconds;
	}

	public static void setMaxOptimizationSeconds( int maxOptimizationSeconds ){
		Configuration.maxOptimizationSeconds = maxOptimizationSeconds;
	}

	public static boolean isLazyActive(){
		return lazyActive;
	}

	public static void setLazyActive( boolean lazyActive ){
		Configuration.lazyActive = lazyActive;
	}

	public static String getHostname(){
		return hostname;
	}

	public static void setHostname( String hostname ){
		Configuration.hostname = hostname;
	}

	public static int getThreadCount(){
		return threadCount;
	}

	public static void setThreadCount( int threadCount ){
		Configuration.threadCount = threadCount;
	}

	public static String getHeuristicValuesFile(){
		return heuristicValuesFile;
	}

	public static void setHeuristicValuesFile( String heuristicValuesFile ){
		Configuration.heuristicValuesFile = heuristicValuesFile;
	}

	public static void setTestDescription( String testDescription ){
		Configuration.testDescription = testDescription;
	}

	public static String getTestDescription(){
		return testDescription;
	}

	public static void setPresolveHeuristicType( Defines.PresolveHeuristicType presolveHeuristicType ){
		Configuration.presolveHeuristicType = presolveHeuristicType;
	}

	public static Defines.PresolveHeuristicType getPresolveHeuristicType(){
		return presolveHeuristicType;
	}

	public static void setGurobiHeuristicsActive( boolean gurobiHeuristicsActive ){
		Configuration.gurobiHeuristicsActive = gurobiHeuristicsActive;
	}

	public static boolean isGurobiHeuristicsActive(){
		return gurobiHeuristicsActive;
	}
}
