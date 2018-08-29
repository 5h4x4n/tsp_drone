package de.hbrs.inf;

public class Configuration{

	private static String logFile = "tsp_drone.log";
	private static String jsonFileOrDir = null;
	private static String logLevel = "INFO";
	private static String outputDirectory = null;
	private static boolean allCustomersByDrones = false;
	private static double[] truckSpeeds = null;
	private static double[] droneSpeeds = null;
	private static int[] droneFleetSizes = null;
	private static int[] droneFlightRanges = null;
	private static int maxOptimizationSeconds = 0;

	static void setSystemProperties() {
		System.setProperty( "log4j.logLevel", logLevel );
		System.setProperty( "log4j.logFile", logFile );
	}

	static void setJsonFileOrDir( String jsonFileOrDir ){
		Configuration.jsonFileOrDir = jsonFileOrDir;
	}

	static String getJsonFileOrDir(){
		return jsonFileOrDir;
	}

	static String getLogFile(){
		return logFile;
	}

	static void setLogFile( String logFile ){
		Configuration.logFile = logFile;
	}

	static String getLogLevel(){
		return logLevel;
	}

	static void setLogLevel( String logLevel ){
		Configuration.logLevel = logLevel;
	}

	static void setOutputDirectory( String csvDirectory ){
		Configuration.outputDirectory = csvDirectory;
	}

	static String getOutputDirectory(){
		return outputDirectory;
	}

	static boolean isAllCustomersByDrones(){
		return allCustomersByDrones;
	}

	static void setAllCustomersByDrones( boolean allCustomersByDrones ){
		Configuration.allCustomersByDrones = allCustomersByDrones;
	}

	static double[] getTruckSpeeds(){
		return truckSpeeds;
	}

	static void setTruckSpeeds( double[] truckSpeeds ){
		Configuration.truckSpeeds = truckSpeeds;
	}

	static double[] getDroneSpeeds(){
		return droneSpeeds;
	}

	static void setDroneSpeeds( double[] droneSpeeds ){
		Configuration.droneSpeeds = droneSpeeds;
	}

	public static int[] getDroneFleetSizes(){
		return droneFleetSizes;
	}

	public static void setDroneFleetSizes( int[] droneFleetSizes ){
		Configuration.droneFleetSizes = droneFleetSizes;
	}

	static int[] getDroneFlightRanges(){
		return droneFlightRanges;
	}

	static void setDroneFlightRanges( int[] droneFlightRanges ){
		Configuration.droneFlightRanges = droneFlightRanges;
	}

	public static int getMaxOptimizationSeconds(){
		return maxOptimizationSeconds;
	}

	public static void setMaxOptimizationSeconds( int maxOptimizationSeconds ){
		Configuration.maxOptimizationSeconds = maxOptimizationSeconds;
	}
}
