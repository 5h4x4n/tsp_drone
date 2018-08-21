package de.hbrs.inf;

class Configuration{

	private static String logFile = "tsp_drone.log";
	private static String jsonFileOrDir = null;
	private static String logLevel = "INFO";
	private static String outputDirectory = null;


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

	static void setSystemProperties() {
		System.setProperty( "log4j.logLevel", logLevel );
		System.setProperty( "log4j.logFile", logFile );
	}

	static void setOutputDirectory( String csvDirectory ){
		Configuration.outputDirectory = csvDirectory;
	}

	public static String getOutputDirectory(){
		return outputDirectory;
	}
}
