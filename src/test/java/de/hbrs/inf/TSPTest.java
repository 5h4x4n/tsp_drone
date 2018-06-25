package de.hbrs.inf;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TSPTest{

	@Test
	public void testGetTSPFromJson() {
		assertNull( TSP.getObjectFromJson( "resources/tsp_test2.json" ) );
		assertNotNull( TSP.getObjectFromJson( "resources/tsp_test.json" ) );
	}

	@Test
	public void testGrb() {
		TSP tsp = TSP.getObjectFromJson( "resources/gr137.json" );
		tsp.grbOptimize();
	}

}
