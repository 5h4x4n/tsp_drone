package de.hbrs.inf;

import org.junit.Test;
import static org.junit.Assert.*;

public class PdstspTest{

	@Test
	public void testGetPDSTSPFromJson() {
		assertNull( Pdstsp.getObjectFromJson( "resources/pdstsp_test2.json" ) );
		assertNotNull( Pdstsp.getObjectFromJson( "resources/pdstsp_test.json" ) );
	}

}
