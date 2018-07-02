package de.hbrs.inf;

public class Configuration{

	private static String logFile = "tsp_drone.log";
	private static String jsonFileOrDir = null;
	private static String logLevel = "INFO";


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
}
