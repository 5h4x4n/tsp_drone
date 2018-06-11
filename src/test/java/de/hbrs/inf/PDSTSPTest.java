package de.hbrs.inf;

import org.junit.Test;
import static org.junit.Assert.*;

public class PDSTSPTest{

	@Test
	public void testGetPDSTSPFromJson() {
		assertNull( PDSTSP.getObjectFromJson( "resources/pdstsp_test2.json" ) );
		assertNotNull( PDSTSP.getObjectFromJson( "resources/pdstsp_test.json" ) );
	}

}
