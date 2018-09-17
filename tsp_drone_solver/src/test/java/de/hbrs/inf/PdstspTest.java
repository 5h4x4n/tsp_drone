package de.hbrs.inf;

import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.json.TspLibJson;
import de.hbrs.inf.tsp.solver.Configuration;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PdstspTest{

	@Before
	public void setUp() {
		Configuration.setLogLevel( "DEBUG" );
		Configuration.setSystemProperties();
	}

	@Test
	public void testGetPDSTSPFromJson() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/pdstsp_test.json" );
		assertNotNull( tspLibJson );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assertNotNull( tspModel );
	}

	@Test
	public void testGrbOptimize() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/pdstsp_test.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.grbOptimize();
	}

}
