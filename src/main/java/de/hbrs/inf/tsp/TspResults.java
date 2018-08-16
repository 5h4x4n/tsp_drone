package de.hbrs.inf.tsp;

import java.util.ArrayList;

public class TspResults{

	private String name;
	private boolean isOptimal = false;
	private double runtime;
	private double runtimeGrbModelCalculation;
	private ArrayList<TspIterationResult> iterationResults = new ArrayList<>();

	public TspResults( String name ) {
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

	public ArrayList<TspIterationResult> getIterationResults(){
		return iterationResults;
	}

	public void setIterationResults( ArrayList<TspIterationResult> iterationResults ){
		this.iterationResults = iterationResults;
	}

	public TspIterationResult getLast() {
		return iterationResults.get( iterationResults.size() - 1 );
	}

	public double getRuntimeGrbModelCalculation(){
		return runtimeGrbModelCalculation;
	}

	public void setRuntimeGrbModelCalculation( double runtimeGrbModelCalculation ){
		this.runtimeGrbModelCalculation = runtimeGrbModelCalculation;
	}

	public int getIterationCounter() {
		return iterationResults.size();
	}
}
