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
		/*
		TspLibJson tspLibJson = JsonTspMapper.getJsonObjectFromJson( "../resources/test/fstsp_test.json" );
		TspModel tspModel = JsonTspMapper.getTspModelFromJsonObject( tspLibJson );
		assert tspModel != null;
		TspModelResult result = tspModel.grbOptimize();
		assert result != null;
		Assert.assertEquals( tspModel.getResult().getLast().getObjective(), 193800.0, 0.0 );
		 */
	}

	//TODO Remove: Just for fast testing
	@Test public void test(){

		ArrayList<FstspDroneFlight> arrayList = new ArrayList<>();
		HashSet<Integer> set = new HashSet<>();
		set.add( 1 );
		set.add( 2 );
		arrayList.add( new FstspDroneFlight( set, 3 ) );
		set = new HashSet<>();
		set.add( 1 );
		set.add( 3 );
		arrayList.add( new FstspDroneFlight( set, 4 ) );
		set = new HashSet<>();
		set.add( 4 );
		set.add( 1 );
		arrayList.add( new FstspDroneFlight( set, 5 ) );

		set = new HashSet<>();
		set.add( 1 );
		set.add( 2 );
		System.out.println( arrayList.contains( new FstspDroneFlight( set, 3 ) ) );
		set = new HashSet<>();
		set.add( 3 );
		set.add( 1 );
		System.out.println( arrayList.contains( new FstspDroneFlight( set, 4 ) ) );
		set = new HashSet<>();
		set.add( 4 );
		set.add( 1 );
		System.out.println( arrayList.contains( new FstspDroneFlight( set, 5 ) ) );

		set = new HashSet<>();
		set.add( 1 );
		set.add( 4 );
		System.out.println( arrayList.contains( new FstspDroneFlight( set, 9 ) ) );
		set = new HashSet<>();
		set.add( 9 );
		set.add( 1 );
		System.out.println( arrayList.contains( new FstspDroneFlight( set, 5 ) ) );

	}

}
