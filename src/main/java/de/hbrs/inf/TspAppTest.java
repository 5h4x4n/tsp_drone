package de.hbrs.inf;

import gurobi.*;

import java.awt.geom.Point2D;

public class TspAppTest{

	public static double MAX_X = 100;
	public static double MAX_Y = 100;

	public static void main( String[] args ) {

		if (args.length < 1) {
			System.out.println("Usage: java TspAppTest n");
			System.exit(1);
		}

		int n = Integer.parseInt( args[0] );
		Point2D.Double[] position = new Point2D.Double[n];
		double[][] c = new double[n][n];

		for( int i = 0; i < n; i++ ) {
			position[i] = new Point2D.Double( Math.random() * MAX_X, Math.random() * MAX_Y );
		}

		for( int i = 0; i < n; i++ ){
			for(int j = 0; j < n; j++ ){
				c[i][j] = distance( position[i], position[j] );
			}
		}

		System.out.println( "Distance Matrix:" );
		printCoefficientMatrix( c );

		/*
		int[] array = new int[n];
		for( int i = 0; i < n; i++ ) {
			array[i] = i;
		}
		printAllSubsets( array );
		*/


		try {
			GRBEnv env = new GRBEnv("tsp.log" );
			//env.set( GRB.IntParam.LogToConsole, 0 );
			GRBModel model = new GRBModel( env );


			// create decision variables
			GRBVar[][] vars = new GRBVar[n][n];

			for (int i = 0; i < n; i++){
				for(int j = 0; j <= i; j++){
					vars[i][j] = model.addVar( 0.0, 1.0, c[i][j], GRB.BINARY, "x" + String.valueOf( i ) + "_" + String.valueOf( j ) );
					vars[j][i] = vars[i][j];
				}
			}

			// create degree-2 constraints
			for (int i = 0; i < n; i++) {
				GRBLinExpr expr = new GRBLinExpr();
				for (int j = 0; j < n; j++)
					expr.addTerm(1.0, vars[i][j]);
				model.addConstr(expr, GRB.EQUAL, 2.0, "deg2_"+String.valueOf(i));
			}

			// forbid edge from node back to itself
			for (int i = 0; i < n; i++){
				vars[i][i].set( GRB.DoubleAttr.UB, 0.0 );
			}

			// add subtour elimination constraints
			/*GRBLinExpr expr;

			for( int i = 0; i < n; i++) {

				expr = new GRBLinExpr();
				expr.addTerm(1.0, x); expr.addTerm(2.0, y); expr.addTerm(3.0, z);
				model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");
			}*/


			model.optimize();

			int optimstatus = model.get(GRB.IntAttr.Status);

			if (optimstatus == GRB.Status.INF_OR_UNBD) {
				model.set(GRB.IntParam.Presolve, 0);
				model.optimize();
				optimstatus = model.get(GRB.IntAttr.Status);
			}

			if (optimstatus == GRB.Status.OPTIMAL) {
				double objval = model.get(GRB.DoubleAttr.ObjVal);
				int tmp;
				System.out.println("Optimal objective: " + objval);
			} else if (optimstatus == GRB.Status.INFEASIBLE) {
				System.out.println("Model is infeasible");

				// Compute and write out IIS
				model.computeIIS();
				model.write("model.ilp");
			} else if (optimstatus == GRB.Status.UNBOUNDED) {
				int tmp2;
				System.out.println("Model is unbounded");
			} else {
				System.out.println("Optimization was stopped with status = "
								+ optimstatus);
			}

			// Dispose of model and environment
			model.dispose();
			env.dispose();

		} catch (GRBException e) {
			System.out.println("Error code: " + e.getErrorCode() + ". " +
							e.getMessage());
		}
	}

	public static double distance( Point2D.Double p1, Point2D.Double p2 ) {
		return Math.sqrt( Math.pow( p1.x - p2.x, 2 ) + Math.pow( p1.y - p2.y, 2) );
	}

	public static void printCoefficientMatrix( double[][] c ) {
		for( int i=0; i<c.length; i++ ) {
			for( int j=0; j<c[i].length; j++ ) {
				System.out.format( "%6.2f ", c[i][j] );
			}
			System.out.println();
		}
	}

	public static void printAllSubsets(int[] arr) {
		byte[] counter = new byte[arr.length];

		while (true) {
			// Print combination
			for (int i = 0; i < counter.length; i++) {
				if (counter[i] != 0)
					System.out.print(arr[i] + " ");
			}
			System.out.println();

			// Increment counter
			int i = 0;
			while (i < counter.length && counter[i] == 1)
				counter[i++] = 0;
			if (i == counter.length)
				break;
			counter[i] = 1;
		}
	}

}
