package de.hbrs.inf.tsp;

import java.util.ArrayList;

public class FstspResult extends TspModelResult{
	private ArrayList<FstspIterationResult> iterationResults = new ArrayList<>();

	public FstspResult( String name ){
		super( name );
	}

	@Override public ArrayList<? extends TspModelIterationResult> getIterationResults(){
		return iterationResults;
	}

	public ArrayList<FstspIterationResult> getFstspIterationResults(){
		return iterationResults;
	}
}
