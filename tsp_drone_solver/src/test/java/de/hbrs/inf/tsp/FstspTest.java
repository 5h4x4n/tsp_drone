package de.hbrs.inf.tsp;

import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.json.TspLibJson;
import de.hbrs.inf.tsp.solver.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;

public class FstspTest{

	@Before public void setUp(){
		Configuration.setLogLevel( "DEBUG" );
		Configuration.setSystemProperties();
	}

	@Test public void testGetFSTSPFromJson(){
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/fstsp_test.json" );
		assertNotNull( tspLibJson );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assertNotNull( tspModel );
	}

	@Test public void testGrbOptimizeLazy(){
		//TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/fstsp_test.json" );
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/fstsp_test2.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		//Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 328700.0, 0.0 );
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 2417500.0, 0.0 );
	}

}
