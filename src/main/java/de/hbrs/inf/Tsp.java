package de.hbrs.inf;

import gurobi.*;
import org.apache.log4j.Logger;

import java.awt.geom.Point2D.Double;
import java.util.*;

public class Tsp extends TspModel {

	private int additionalConstraintsCounter = 0;
	private static Logger log = Logger.getLogger( Tsp.class.getName() );

	public Tsp( String name, String comment, String type, double[][] nodes, int[][] distances, double[][] truckTimes ){
		super(name, comment, type, nodes, distances, truckTimes );
	}

	public String toString(){
		//TODO
		return super.toString();
	}

	protected  GRBModel calcGrbModel() throws GRBException{
		log.info( "Start calculation of gurobi model for the Tsp without subtour elimination constraints" );

		GRBModel grbModel = new GRBModel( grbEnv );

		int n = nodes.length;

		// create decision variables
		grbVars = new GRBVar[n][n];
		for(int i = 0; i < n; i++){
			for(int j = i + 1; j < n; j++){
				log.debug( "Add decision var x" + i + "_" + j + " with factor " + distances[i][j] );
				grbVars[i][j] = grbModel.addVar( 0.0, 1.0, distances[i][j], GRB.BINARY, "x" + String.valueOf( i ) + "_" + String.valueOf( j ) );
				grbVars[j][i] = grbVars[i][j];
			}
		}

		// create degree-2 constraints
		for(int i = 0; i < n; i++){
			GRBLinExpr grbExpr = new GRBLinExpr();
			String logString = "";
			for(int j = 0; j < n; j++){
				if( i != j ){
					logString += "x" + i + "_" + j + " + ";
					grbExpr.addTerm( 1.0, grbVars[i][j] );
				}
			}
			logString = logString.substring( 0, logString.length() - 2 );
			log.debug( "Add degree-2 constraint deg2_" + i + ": " + logString + " = 2" );
			grbModel.addConstr( grbExpr, GRB.EQUAL, 2.0, "deg2_" + String.valueOf( i ) );
		}

		log.info( "End calculation of gurobi model for the Tsp without subtour elimination constraints" );

		return grbModel;
	}


	protected boolean addViolatedConstraints() throws GRBException{

		GRBVar[] vars = grbModel.getVars();
		String[] varNames = grbModel.get( GRB.StringAttr.VarName, vars );
		double[] x = grbModel.get( GRB.DoubleAttr.X, vars );

		ArrayList<String> solution = new ArrayList<String>();
		for(int i = 0; i < vars.length; i++){
			if( x[i] != 0.0 ){
				solution.add( varNames[i].substring( 1 ) );
			}
		}
		ArrayList<HashSet<Integer>> subtours = findSubtours( solution );
		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints" );
			for(HashSet<Integer> subtour : subtours){
				double subtourVertexCounter = subtour.size();

				//skip subtours with bigger size than half of the dimension, cause it is not needed
				if( subtourVertexCounter > distances.length / 2 ){
					log.debug( "Skip subtour cause it's bigger than half the dimension: " + subtour );
					continue;
				}
				ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
				String subtourEliminationConstraintString = "";
				String subtourEliminationConstraintName = "sec_";
				GRBLinExpr grbExpr = new GRBLinExpr();
				for(int[] edge : edges){
					String currentEdgeString = "x" + edge[0] + "_" + edge[1];
					//TODO change?!
					//subtourEliminationConstraintName += currentEdgeString + "-";
					subtourEliminationConstraintString += currentEdgeString + " + ";
					grbExpr.addTerm( 1.0, grbVars[edge[0]][edge[1]] );
				}
				//TODO change?!
				//subtourEliminationConstraintName = subtourEliminationConstraintString
				//				.substring( 0, subtourEliminationConstraintName.length() - 2 );
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


	private ArrayList<int[]> createEdgesForSubtourEliminationConstraint( HashSet<Integer> subtour ) {
		ArrayList<Integer> subtourList = new ArrayList<Integer>( subtour );
		ArrayList<int[]> edges = new ArrayList<int[]>();
		for( int i = 0; i < subtourList.size() - 1; i++ ) {
			for( int j = i + 1; j < subtourList.size(); j++ ) {
				int[] edge = new int[2];
				edge[0] = subtourList.get( i );
				edge[1] = subtourList.get( j );
				edges.add( edge );

			}
		}
		return edges;
	}

	public ArrayList<HashSet<Integer>> findSubtours( ArrayList<String> solutionEdges ) {
		log.debug( "Starting find subtours" );
		log.debug( "Solution Edges: " + solutionEdges );

		ArrayList<HashSet<Integer>> subtours = new ArrayList<HashSet<Integer>>();
		Stack<Integer> unhandledVerticesForSubtour = new Stack<Integer>();

		while( !solutionEdges.isEmpty() ) {

			HashSet<Integer> subtour = new HashSet<Integer>();
			subtours.add( subtour );
			String currentEdge = solutionEdges.remove( 0 );
			int[] currentEdgeVertices = getVerticesFromEdge( currentEdge );
			subtour.add( currentEdgeVertices[0] );
			subtour.add( currentEdgeVertices[1] );
			unhandledVerticesForSubtour.push( currentEdgeVertices[0] );
			unhandledVerticesForSubtour.push( currentEdgeVertices[1] );

			while( !unhandledVerticesForSubtour.empty() ) {

				int currentVertex = unhandledVerticesForSubtour.pop();
				ArrayList<Integer> edgesToRemove = new ArrayList<Integer>();
				for( int i = 0; i < solutionEdges.size(); i++ ) {
					currentEdgeVertices = getVerticesFromEdge( solutionEdges.get( i ) );
					if( currentEdgeVertices[0] == currentVertex ){
						subtour.add( currentEdgeVertices[1] );
						unhandledVerticesForSubtour.add( currentEdgeVertices[1] );
						edgesToRemove.add( i );
					} else if( currentEdgeVertices[1] == currentVertex ) {
						subtour.add( currentEdgeVertices[0] );
						unhandledVerticesForSubtour.add( currentEdgeVertices[0] );
						edgesToRemove.add( i );
					}
				}
				//Remove edgesToRemove from solutionEdges
				log.debug( "Edges to remove from solution edges: " + edgesToRemove );
				log.debug( "Solution edges size: " + solutionEdges.size() );
				log.debug( "Solution edges: " + solutionEdges );
				for( int i : edgesToRemove ) {
					log.debug( "Try to remove index: " + i + ", solution egdes size: " + solutionEdges.size() );
					solutionEdges.remove( i );
				}
			}
		}
		log.debug( "Ending find subtours" );
		return subtours;
	}

	private int[] getVerticesFromEdge( String edge ) {
		int[] vertices = new int[2];
		String[] edgeVertices = edge.split( "_" );
		vertices[0] = Integer.parseInt( edgeVertices[0] );
		vertices[1] = Integer.parseInt( edgeVertices[1] );
		return vertices;
	}

	private ArrayList<Integer> getTourFromSolutionEdges( ArrayList<String> solutionEdges ) {
		ArrayList<Integer> tour = new ArrayList<Integer>();
		//TODO implementation
		tour.add( 0 );
		for( int i=0; i < nodes.length; i++ ) {
			int currentNode = tour.get( i );


		}

		return tour;
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

	public double[][] getTruckTimes(){
		return truckTimes;
	}

	public void setTruckTimes( double[][] truckTimes ){
		this.truckTimes = truckTimes;
	}
}
