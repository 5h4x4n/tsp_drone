package de.hbrs.inf.tsp;

import gurobi.*;

import java.util.*;

public class Tsp extends TspModel {

	private ArrayList<Integer> truckTspTour;
	private TspResult result;

	public Tsp() {}

	public Tsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances ){
		super(name, comment, type, dimension, nodes, distances );
		this.result = new TspResult( name );
	}

	public String toString(){
		//TODO implement toString
		return super.toString();
	}

	@Override
	protected GRBModel calcGrbModel() throws GRBException{
		log.info( "Start calculation of gurobi model for the TSP without subtour elimination constraints" );

		GRBModel grbModel = new GRBModel( grbEnv );

		// create decision variables
		grbTruckEdgeVars = new GRBVar[dimension][dimension];
		for(int i = 0; i < dimension; i++){
			for(int j = i + 1; j < dimension; j++){
				log.debug( "Add decision var x" + i + "_" + j + " with factor " + distances[i][j] );
				grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, distances[i][j], GRB.BINARY, "x" + i  + "_" + j );
				grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];
			}
		}

		// create degree-2 constraints
		for(int i = 0; i < dimension; i++){
			GRBLinExpr grbLinExpr = new GRBLinExpr();
			StringBuilder logString = new StringBuilder();
			for(int j = 0; j < dimension; j++){
				if( i != j ){
					logString.append( "x" ).append( i ).append( "_" ).append( j ).append( " + " );
					grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[i][j] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 2 ) );
			log.debug( "Add degree-2 constraint deg2_" + i + ": " + logString + " = 2" );
			grbModel.addConstr( grbLinExpr, GRB.EQUAL, 2.0, "deg2_" + String.valueOf( i ) );
			calculatedConstraintsCounter++;
		}

		log.info( "End calculation of gurobi model for the TSP without subtour elimination constraints" );

		return grbModel;
	}

	@Override
	protected TspModelIterationResult calculateAndAddIterationResult( ) throws GRBException{
		TspIterationResult tspIterationResult = new TspIterationResult();
		tspIterationResult.setTruckTours( findSubtours() );
		result.getTspIterationResults().add( tspIterationResult );
		return tspIterationResult;
	}

	@Override
	protected boolean addViolatedConstraints() throws GRBException{

		ArrayList<ArrayList<Integer>> subtours = result.getLast().getTruckTours();
		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints" );
			for( ArrayList<Integer> subtour : subtours ){
				double subtourVertexCounter = subtour.size();

				//skip subtours with bigger size than half of the dimension, cause it is not needed
				if( subtourVertexCounter > dimension / 2 ){
					log.debug( "Skip subtour cause it's bigger than half the dimension: " + subtour );
					continue;
				}
				ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
				StringBuilder subtourEliminationConstraintString = new StringBuilder();
				String subtourEliminationConstraintName = "sec_";
				GRBLinExpr grbExpr = new GRBLinExpr();
				for(int[] edge : edges){
					subtourEliminationConstraintString.append( "x" ).append( edge[0] ).append( "_" ).append( edge[1] ).append( " + " );
					grbExpr.addTerm( 1.0, grbTruckEdgeVars[edge[0]][edge[1]] );
				}
				subtourEliminationConstraintName += additionalConstraintsCounter;
				log.debug( "Add subtour elimination constraint: " + subtourEliminationConstraintString
								.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + (subtour.size() - 1) );
				grbModel.addConstr( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, subtourEliminationConstraintName );
				additionalConstraintsCounter++;
			}
			return true;
		} else {
			return false;
		}
	}

	public double[][] getNodes(){
		return nodes;
	}

	public void setNodes( double[][] nodes ){
		this.nodes = nodes;
	}

	public int[][] getDistances(){
		return distances;
	}

	public void setDistances( int[][] distances ){
		this.distances = distances;
	}

	@Override
	public TspModelResult getResult(){
		return result;
	}

	public ArrayList<Integer> getTruckTspTour() {
		return truckTspTour;
	}

	public void setTruckTspTour(ArrayList<Integer> truckTspTour) {
		this.truckTspTour = truckTspTour;
	}
}
