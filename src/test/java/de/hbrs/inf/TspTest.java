package de.hbrs.inf;

import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TspTest{

	private static final Logger log = Logger.getLogger( TspTest.class.getName() );

	@Before
	public void setUp() {
		Configuration.setLogLevel( "DEBUG" );
	}

	@Test
	public void testGetTSPFromJson() {
		assertNull( JsonTspMapper.getObjectFromJson( "resources/test/tsp_test2.json" ) );
		assertNotNull( JsonTspMapper.getObjectFromJson( "resources/test/tsp_test.json" ) );
	}

	@Test
	public void testGrb() {
		TspModel tspModel = JsonTspMapper.getObjectFromJson( "resources/tsplib/a280.json" );
		tspModel.grbOptimize();
	}

}
