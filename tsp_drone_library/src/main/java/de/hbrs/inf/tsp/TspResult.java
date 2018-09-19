package de.hbrs.inf.tsp;

import java.util.ArrayList;

public class TspResult extends TspModelResult {

	private ArrayList<TspIterationResult> iterationResults = new ArrayList<>();

	public TspResult( String name ){
		super( name );
	}

	@Override
	public ArrayList<? extends TspModelIterationResult> getIterationResults(){
		return iterationResults;
	}

	public ArrayList<TspIterationResult> getTspIterationResults() {
		return iterationResults;
	}

}
