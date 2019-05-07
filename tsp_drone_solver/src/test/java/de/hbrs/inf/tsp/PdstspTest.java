package de.hbrs.inf.tsp;

import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.json.TspLibJson;
import de.hbrs.inf.tsp.solver.Configuration;
import org.junit.Assert;
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
	public void testGrbOptimizeIterative() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/pdstsp_test.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.setLazyActive( false );
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 193800.0, 0.0 );
	}

	@Test
	public void testGrbOptimizeLazy(){
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/pdstsp_test.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 193800.0, 0.0 );
	}

	@Test
	public void testGrbOptimizePresolveHeuristic() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/pdstsp_test.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.setPresolveHeuristicActive( true );
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 193800.0, 0.0 );
	}

	//TODO Remove: Just for fast testing
	@Test
	public void testGrbOptimizeLazy2(){
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/pdstsplib_small/gr48.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 199650.0, 0.0 );
	}

	//TODO Remove: Just for fast testing
	@Test
	public void testGrbOptimizePresolveHeuristic2() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/pdstsplib_small/gr48.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.setPresolveHeuristicActive( true );
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 199650.0, 0.0 );
	}

}
