package de.hbrs.inf.tsp.graphics;

public class Configuration{

	private static String logFile = "tsp_drone_graphics.log";
	private static String logLevel = "INFO";

	public static void setSystemProperties() {
		System.setProperty( "log4j.logLevel", logLevel );
		System.setProperty( "log4j.logFile", logFile );
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
}
