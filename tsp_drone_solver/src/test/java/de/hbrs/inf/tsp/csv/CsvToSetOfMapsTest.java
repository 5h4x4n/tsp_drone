package de.hbrs.inf.tsp.csv;

import de.hbrs.inf.tsp.solver.Configuration;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class CsvToSetOfMapsTest{

	private static Logger log;

	@Before
	public void setUp() {
		Configuration.setLogLevel( "DEBUG" );
		Configuration.setSystemProperties();
		log = Logger.getLogger( CsvToSetOfMapsTest.class.getName() );
	}

	@Test
	public void convertCsvFileTest(){
		Set<Map<String, String>> csv = CsvToSetOfMaps.convertCsvFile( "src/test/resources/test.csv", ';' );

		Assert.assertEquals( csv.size(), 4 );
		Assert.assertEquals( csv.iterator().next().size(), 7 );
	}
}
