package de.hbrs.inf;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TSPTest{

	@Test
	public void testGetTSPFromJson() {
		assertNull( TSP.getObjectFromJson( "resources/tsp_test2.json" ) );
		TSP tsp = TSP.getObjectFromJson( "resources/tsp_test.json" );
		assertNotNull( tsp );

		tsp.calcGrbModel();
		tsp.grbOptimize();
	}

}
