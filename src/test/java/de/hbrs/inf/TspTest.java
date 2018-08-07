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
		Configuration.setSystemProperties();
	}

	@Test
	public void testGetTSPFromJson() {
		assertNotNull( JsonTspMapper.getObjectFromJson( "resources/test/tsp_test.json" ) );
	}

	@Test
	public void testGrbOptimize() {
		TspModel tspModel = JsonTspMapper.getObjectFromJson( "resources/tsplib/wi29.json" );
		tspModel.grbOptimize();
	}

}
