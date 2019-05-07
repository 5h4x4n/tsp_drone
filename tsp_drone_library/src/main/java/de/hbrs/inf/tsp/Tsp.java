package de.hbrs.inf.tsp;

import gurobi.*;

import java.util.*;

public class Tsp extends TspModel{

	private TspResult result;

	public Tsp(){
	}

	public Tsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances ){
		super( name, comment, type, dimension, nodes, distances );
		this.result = new TspResult( name );
	}

	public String toString(){
		return super.toString();
	}

	@Override
	protected GRBModel calcGrbModel() throws GRBException{
		log.info( "Start calculation of gurobi model for the TSP without subtour elimination constraints" );

		GRBModel grbModel = new GRBModel( grbEnv );

		// create decision variables
		grbTruckEdgeVars = new GRBVar[dimension][dimension];
		for( int i = 0; i < dimension; i++ ){
			for( int j = i; j < dimension; j++ ){
				if( i == j ){
					grbTruckEdgeVars[i][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "x" + i + "_" + i );
				} else {
					log.debug( "Add decision var x" + i + "_" + j + " with factor " + distances[i][j] );
					grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, distances[i][j], GRB.BINARY, "x" + i + "_" + j );
					grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];
				}
			}
		}

		// create degree-2 constraints
		for( int i = 0; i < dimension; i++ ){
			GRBLinExpr grbLinExpr = new GRBLinExpr();
			StringBuilder logString = new StringBuilder();
			for( int j = 0; j < dimension; j++ ){
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

	@Override public boolean presolveHeuristic(){
		//heuristic for testing is also optimal solution of the tsp
		Tsp tsp = new Tsp( this.name, this.comment, "TSP", this.dimension, this.nodes, this.distances );
		if( tsp.grbOptimize() != null ){
			if( tsp.getResult().isOptimal() ){
				grbTruckEdgeVarsStartValues = tsp.truckEdgeVars.clone();
				log.info( "Set heuristicValue: " + tsp.getResult().getObjective() );
				setHeuristicValue( tsp.getResult().getObjective() );
				return true;
			}
		}
		return false;
	}

	@Override
	protected TspModelIterationResult calculateAndAddIterationResult() throws GRBException{
		TspIterationResult tspIterationResult = new TspIterationResult();
		truckEdgeVars = grbModel.get( GRB.DoubleAttr.X, grbTruckEdgeVars );
		tspIterationResult.setTruckTours( findSubtours( truckEdgeVars ) );
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
				for( int[] edge : edges ){
					subtourEliminationConstraintString.append( "x" ).append( edge[0] ).append( "_" ).append( edge[1] ).append( " + " );
					grbExpr.addTerm( 1.0, grbTruckEdgeVars[edge[0]][edge[1]] );
				}
				subtourEliminationConstraintName += additionalConstraintsCounter;
				log.debug( "Add subtour elimination constraint: " + subtourEliminationConstraintString
								.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + ( subtour.size() - 1 ) );
				grbModel.addConstr( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, subtourEliminationConstraintName );
				additionalConstraintsCounter++;
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void addViolatedLazyConstraints() throws GRBException{
		log.info( "Look for subtours and add lazy constraints." );

		ArrayList<ArrayList<Integer>> subtours = null;
		double[][] truckEdgeVars;

		truckEdgeVars = getSolution( grbTruckEdgeVars );
		subtours = findSubtours( truckEdgeVars );

		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.debug( "Subtours: " + subtours );

			log.info( "Add violated subtour elimination constraints as lazy constraints" );
			for( ArrayList<Integer> subtour : subtours ){
				double subtourVertexCounter = subtour.size();

				//skip subtours with bigger size than half of the dimension, cause it is not needed
				if( subtourVertexCounter > dimension / 2 ){
					log.debug( "Skip subtour cause it's bigger than half the dimension: " + subtour );
					continue;
				}
				ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
				StringBuilder subtourEliminationConstraintString = new StringBuilder();
				GRBLinExpr grbExpr = new GRBLinExpr();
				for( int[] edge : edges ){
					subtourEliminationConstraintString.append( "x" ).append( edge[0] ).append( "_" ).append( edge[1] ).append( " + " );
					grbExpr.addTerm( 1.0, grbTruckEdgeVars[edge[0]][edge[1]] );
				}
				log.debug( "Add (lazy) subtour elimination constraint: " + subtourEliminationConstraintString
								.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + ( subtour.size() - 1 ) );
				addLazy( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1 );
				additionalConstraintsCounter++;
			}
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
}
