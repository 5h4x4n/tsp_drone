package de.hbrs.inf.tsp;

import de.hbrs.inf.Configuration;
import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBModel;
import org.apache.log4j.Logger;

public class TspGrbCallback extends GRBCallback{

	private GRBModel grbModel;
	private long startOptimizationTime;

	private static Logger log = Logger.getLogger( TspGrbCallback.class );

	public TspGrbCallback( GRBModel grbModel, long startOptimizationTime ) {
		this.grbModel = grbModel;
		this.startOptimizationTime = startOptimizationTime;
	}

	@Override
	protected void callback(){
		if ( where == GRB.CB_MIP ) {
			int maxOptimizationSeconds = Configuration.getMaxOptimizationSeconds();
			double currentRuntimeSeconds = ( System.nanoTime() - startOptimizationTime ) / 1e9 ;
			if( maxOptimizationSeconds > 0 && currentRuntimeSeconds > maxOptimizationSeconds ) {
				grbModel.terminate();
			}
		}
	}
}
