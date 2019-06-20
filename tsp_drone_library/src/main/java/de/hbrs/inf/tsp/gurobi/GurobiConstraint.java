package de.hbrs.inf.tsp.gurobi;

import gurobi.GRBLinExpr;

public class GurobiConstraint{

	private GRBLinExpr linExpr;
	private char sense;
	private double rhs;
	private String name;

	public GurobiConstraint( GRBLinExpr linExpr, char sense, double rhs, String name ){
		this.linExpr = linExpr;
		this.sense = sense;
		this.rhs = rhs;
		this.name = name;
	}

	public GRBLinExpr getLinExpr(){
		return linExpr;
	}

	public void setLinExpr( GRBLinExpr linExpr ){
		this.linExpr = linExpr;
	}

	public char getSense(){
		return sense;
	}

	public void setSense( char sense ){
		this.sense = sense;
	}

	public double getRhs(){
		return rhs;
	}

	public void setRhs( double rhs ){
		this.rhs = rhs;
	}

	public String getName(){
		return name;
	}

	public void setName( String name ){
		this.name = name;
	}
}
