package de.hbrs.inf.tsp.csv;

import de.hbrs.inf.tsp.Tsp;
import de.hbrs.inf.tsp.TspModel;
import de.hbrs.inf.tsp.json.JsonTspMapper;
import de.hbrs.inf.tsp.solver.Configuration;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

public class HeuristicValueReaderTest{

	private static Logger log;

	@Before
	public void setUp() {
		Configuration.setLogLevel( "DEBUG" );
		Configuration.setSystemProperties();
		log = Logger.getLogger( CsvToSetOfMapsTest.class.getName() );
	}

	@Test
	public void getHeuristicValueTest(){

		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( JsonTspMapper.getJsonObjectFromJson( "../resources/tsplib_small/gr120.json" ) );
		double heuristicValue = HeuristicValueReader.getHeuristicValue( tspModel, "src/test/resources/test.csv" );
		Assert.assertEquals( heuristicValue, 100.4, 0 );

		tspModel = JsonTspMapper.getTspModelFromJsonObject( JsonTspMapper.getJsonObjectFromJson( "../resources/pdstsplib_small/gr120.json" ),
						10, 10, 4, 40, true );
		heuristicValue = HeuristicValueReader.getHeuristicValue( tspModel, "src/test/resources/test.csv" );
		Assert.assertEquals( heuristicValue, -1.0, 0 );

		tspModel = JsonTspMapper.getTspModelFromJsonObject( JsonTspMapper.getJsonObjectFromJson( "../resources/pdstsplib_small/gr120.json" ),
						10, 10, 10, 10, true );
		heuristicValue = HeuristicValueReader.getHeuristicValue( tspModel, "src/test/resources/test.csv" );
		Assert.assertEquals( heuristicValue, 10, 0 );
	}
}