package de.hbrs.inf.tsp;

import java.util.ArrayList;

public abstract class TspModelResult {

	private String name;
	private boolean isOptimal = false;
	private double runtime;
	private double runtimeGrbModelCalculation;
	private double runtimePresolveHeuristic;
	private double objective;
	private double usedHeuristicValue = -1.0;

	public TspModelResult( String name ) {
		this. name = name;
	}

	public String getName(){
		return name;
	}

	public void setName( String name ){
		this.name = name;
	}

	public boolean isOptimal(){
		return isOptimal;
	}

	public void setOptimal( boolean optimal ){
		isOptimal = optimal;
	}

	public double getRuntime(){
		return runtime;
	}

	public void setRuntime( double runtime ){
		this.runtime = runtime;
	}

	public abstract ArrayList<? extends TspModelIterationResult> getIterationResults();

	public TspModelIterationResult getLast() {
		if( getIterationResults() == null || getIterationResults().size() < 1 ) {
			return null;
		} else {
			return getIterationResults().get( getIterationResults().size() - 1 );
		}
	}

	public double getRuntimeGrbModelCalculation(){
		return runtimeGrbModelCalculation;
	}

	public void setRuntimeGrbModelCalculation( double runtimeGrbModelCalculation ){
		this.runtimeGrbModelCalculation = runtimeGrbModelCalculation;
	}

	public double getRuntimePresolveHeuristic(){
		return runtimePresolveHeuristic;
	}

	public void setRuntimePresolveHeuristic( double runtimePresolveHeuristic ){
		this.runtimePresolveHeuristic = runtimePresolveHeuristic;
	}

	public int getIterationCounter() {
		return getIterationResults().size();
	}

	public double getObjective(){
		return objective;
	}

	public void setObjective( double objective ){
		this.objective = objective;
	}

	public double getUsedHeuristicValue(){
		return usedHeuristicValue;
	}

	public void setUsedHeuristicValue( double heuristicValue ){
		this.usedHeuristicValue = heuristicValue;
	}
}
