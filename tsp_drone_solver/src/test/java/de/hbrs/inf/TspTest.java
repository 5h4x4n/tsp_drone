package de.hbrs.inf;

import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.json.TspLibJson;
import de.hbrs.inf.tsp.solver.Configuration;
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
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/tsp_test.json" );
		assertNotNull( tspLibJson );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assertNotNull( tspModel );
	}

	@Test
	public void testGrbOptimize() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/tsplib/wi29.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.grbOptimize();
	}

}