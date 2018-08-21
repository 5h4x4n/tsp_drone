package de.hbrs.inf.tsp;

import gurobi.*;

import java.util.*;

public class Tsp extends TspModel {

	private ArrayList<Integer> truckTspTour;

	public Tsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances ){
		super(name, comment, type, dimension, nodes, distances );
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
	protected TspIterationResult calculateTspIterationResult() throws GRBException{
		TspIterationResult tspIterationResult = new TspIterationResult();
		tspIterationResult.setTruckTours( findSubtours() );
		return tspIterationResult;
	}

	@Override
	protected boolean addViolatedConstraints() throws GRBException{

		ArrayList<ArrayList<Integer>> subtours = tspResults.getLast().getTruckTours();
		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints" );
			for( ArrayList<Integer> subtour : subtours ){
				double subtourVertexCounter = subtour.size();

				//skip subtours with bigger size than half of the dimension, cause it is not needed
				if( subtourVertexCounter > distances.length / 2 ){
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
				subtourEliminationConstraintName += additionalConstraintsCounter++;
				log.debug( "Add subtour elimination constraint: " + subtourEliminationConstraintString
								.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + (subtour.size() - 1) );
				grbModel.addConstr( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, subtourEliminationConstraintName );
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void logIterationDebug() throws GRBException{
		log.debug( "Adjacency matrix of solution:" );
		for(int i = 0; i < dimension; i++){
			StringBuilder rowString = new StringBuilder();
			for(int j = 0; j < dimension; j++){
				if( i == j ){
					rowString.append( "-, " );
				} else {
					rowString.append( (int)grbTruckEdgeVars[i][j].get( GRB.DoubleAttr.X ) ).append( ", " );
				}
			}
			log.debug( rowString.substring( 0, rowString.length() - 2 ) );
		}
	}

	private ArrayList<int[]> createEdgesForSubtourEliminationConstraint(  ArrayList<Integer> subtour ) {
		ArrayList<int[]> edges = new ArrayList<>();
		for( int i = 0; i < subtour.size() - 1; i++ ) {
			for( int j = i + 1; j < subtour.size(); j++ ) {
				int[] edge = new int[2];
				edge[0] = subtour.get( i );
				edge[1] = subtour.get( j );
				edges.add( edge );

			}
		}
		return edges;
	}

	ArrayList<ArrayList<Integer>> findSubtours() throws GRBException{
		log.debug( "Starting find subtours" );

		ArrayList<ArrayList<Integer>> subtours = new ArrayList<>();
		Stack<Integer> unvisitedVertices = new Stack<>();

		//this prevents to search subtours for vertices which are not currently in the solution
		//needed for e.g. pdstsp
		for( int i = dimension - 1; i >= 0; i-- ){
			for( int j = dimension - 1; j >= 0; j-- ){
				if( i != j ){
					if( ( (int)grbTruckEdgeVars[i][j].get( GRB.DoubleAttr.X ) ) == 1 ){
						unvisitedVertices.add( i );
						break;
					}
				}
			}
		}

		while( !unvisitedVertices.isEmpty() ){
			int currentVertex = unvisitedVertices.pop();
			log.debug( "currentVertex: " + currentVertex );
			log.debug( "unvisitedVertices: " + unvisitedVertices );
			ArrayList<Integer> subtour = new ArrayList<>();
			subtours.add( subtour );
			Stack<Integer> unvisitedVerticesForSubtour = new Stack<>();
			unvisitedVerticesForSubtour.add( currentVertex );
			log.debug( "unvisitedVerticesForSubtour: " + unvisitedVerticesForSubtour );

			while( !unvisitedVerticesForSubtour.isEmpty() ){
				Integer currentSubtourVertex = unvisitedVerticesForSubtour.pop();
				log.debug( "currentSubtourVertex: " + currentSubtourVertex );
				log.debug( "unvisitedVerticesForSubtour: " + unvisitedVerticesForSubtour );
				subtour.add( currentSubtourVertex );
				log.debug( "subtour: " + subtour );
				unvisitedVertices.remove( currentSubtourVertex );
				for(int i = 0; i < dimension; i++){
					if( i != currentSubtourVertex ){
						//log.debug( "Check x" + currentSubtourVertex + "_" + i + " = " + (int) grbTruckEdgeVars[currentSubtourVertex][i].get( GRB.DoubleAttr.X ) );
						if( ( (int)( grbTruckEdgeVars[currentSubtourVertex][i].get( GRB.DoubleAttr.X ) + 0.5d ) ) == 1
										&& !subtour.contains( i )
										&& !unvisitedVerticesForSubtour.contains( i ) ){
							unvisitedVerticesForSubtour.add( i );
							log.debug( "unvisitedVerticesForSubtour: " + unvisitedVerticesForSubtour );
						}
					}
				}
			}
			log.debug( "subtour: " + subtour );
		}
		log.debug( "Ending find subtours" );
		return subtours;

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
}
