package de.hbrs.inf.tsp;

import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.json.TspLibJson;
import de.hbrs.inf.tsp.solver.Configuration;
import org.junit.Assert;
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
	public void testGrbOptimizeIterative() {
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/tsplib/wi29.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.setLazyActive( false );
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( result.getLast().getObjective(), 2760300.0, 0.0 );
	}

	@Test
	public void testGrbOptimizeLazy(){
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/tsplib/wi29.json" );
		//TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/tsp_test.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( result.getLast().getObjective(), 2760300.0, 0.0 );
	}

	@Test
	public void testGrbOptimizeLazyPresolveHeuristic(){
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/tsplib/wi29.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		tspModel.setPresolveHeuristicType( Defines.PresolveHeuristicType.TSP );
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( result.getLast().getObjective(), 2760300.0, 0.0 );
	}

}
