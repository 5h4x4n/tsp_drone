package de.hbrs.inf;

import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

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
		assert tspModel != null;
		tspModel.grbOptimize();
	}

}
