package de.hbrs.inf.tsp;

import java.util.ArrayList;

public abstract class TspModelResult {

	private String name;
	private boolean isOptimal = false;
	private double runtime;
	private double runtimeGrbModelCalculation;
	private int objective;

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
		return getIterationResults().get( getIterationResults().size() - 1 );
	}

	public double getRuntimeGrbModelCalculation(){
		return runtimeGrbModelCalculation;
	}

	public void setRuntimeGrbModelCalculation( double runtimeGrbModelCalculation ){
		this.runtimeGrbModelCalculation = runtimeGrbModelCalculation;
	}

	public int getIterationCounter() {
		return getIterationResults().size();
	}

	public int getObjective(){
		return objective;
	}

	public void setObjective( int objective ){
		this.objective = objective;
	}
}
