package de.hbrs.inf;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TSPTest{

	private static final Logger log = Logger.getLogger( TSPTest.class.getName() );

	@Test
	public void testGetTSPFromJson() {
		assertNull( TSP.getObjectFromJson( "resources/test/tsp_test2.json" ) );
		assertNotNull( TSP.getObjectFromJson( "resources/test/tsp_test.json" ) );
	}

	@Test
	public void testGrb() {
		TSP tsp = TSP.getObjectFromJson( "resources/tsplib/a280.json" );
		tsp.grbOptimize();
	}

	@Test
	public void testExamplesTspLib() {
		File directory = new File( "resources/tsplib" );
		for( File file : directory.listFiles() ) {
			log.info( "##################### " + file.getName() + "#####################" );
			TSP tsp = TSP.getObjectFromJson( file.getPath() );
			tsp.grbOptimize();
		}

	}

}
