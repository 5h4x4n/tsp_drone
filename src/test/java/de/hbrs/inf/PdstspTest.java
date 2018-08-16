package de.hbrs.inf;

import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.json.JsonTspMapper;
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
		assertNotNull( JsonTspMapper.getObjectFromJson( "resources/test/pdstsp_test.json" ) );
	}

	@Test
	public void testGrbOptimize() {
		TspModel tspModel = JsonTspMapper.getObjectFromJson( "resources/test/pdstsp_test.json" );
		assert tspModel != null;
		tspModel.grbOptimize();
	}

}
