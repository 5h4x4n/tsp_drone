package de.hbrs.inf.tsp;

import java.util.ArrayList;

public class PdstspResult extends TspModelResult{

	private ArrayList<PdstspIterationResult> iterationResults = new ArrayList<>();

	public PdstspResult( String name ){
		super( name );
	}

	@Override
	public ArrayList<? extends TspModelIterationResult> getIterationResults(){
		return iterationResults;
	}

	public ArrayList<PdstspIterationResult> getPdstspIterationResults() {
		return iterationResults;
	}
}
