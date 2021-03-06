package de.hbrs.inf.tsp;

import de.hbrs.inf.tsp.gurobi.GurobiConstraint;
import gurobi.*;
import org.apache.log4j.Logger;

import java.util.*;

public class Fstsp extends TspModel{

	private double truckSpeed;
	private int[][] truckTimes;
	private double droneSpeed;
	private double droneFlightTime;
	private int[][] droneTimes;
	private int[] droneDeliveryPossible;
	private boolean[][][] possibleDroneFlights;
	private int possibleDroneFlightsSize = 0;
	private transient GRBVar[][] grbTruckEdgeWaitVars;
	private transient double[][] grbTruckEdgeWaitVarsStartValues = null;
	private transient GRBVar[][][] grbDroneFlightsVars;
	private transient double[][][] grbDroneFlightsVarsStartValues = null;
	private transient double valueBiggerThanObjective = 0;
	private FstspResult result;

	private static Logger log = Logger.getLogger( Fstsp.class.getName() );

	public Fstsp(){
	}

	public Fstsp( String name, String comment, String type, int dimension, double[][] nodes, int[][] distances, double truckSpeed, int[][] truckTimes, double droneSpeed,
					double droneFlightTime, int[][] droneTimes, int[] droneDeliveryPossible ){
		super( name, comment, type, dimension, nodes, distances );
		this.truckSpeed = truckSpeed;
		this.truckTimes = truckTimes;
		this.droneSpeed = droneSpeed;
		this.droneFlightTime = droneFlightTime;
		this.droneTimes = droneTimes;
		this.droneDeliveryPossible = droneDeliveryPossible;

		// [i][j][customer]
		this.possibleDroneFlights = new boolean[dimension][dimension][dimension];
		for( int i = 0; i < dimension; i++ ){
			for( int customer : droneDeliveryPossible ){
				for( int j = 0; j < dimension; j++ ){
					if( i != j && i != customer && j != customer ){
						if( droneTimes[i][customer] + droneTimes[customer][j] <= this.droneFlightTime ){
							possibleDroneFlights[i][j][customer] = true;
							possibleDroneFlights[j][i][customer] = true;
							possibleDroneFlightsSize++;
						}

					}
				}
			}
		}
		log.info( "possibleDroneFlightsSize: " + possibleDroneFlightsSize );

		this.result = new FstspResult( name );

		//calculate valueBiggerThanObjective
		int maxTruckTimes = 0;
		for( int i = 0; i < dimension; i++ ){
			for( int j = i; j < dimension; j++ ){
				if( truckTimes[i][j] > maxTruckTimes ){
					maxTruckTimes = truckTimes[i][j];
				}
			}
		}
		valueBiggerThanObjective = maxTruckTimes * dimension;
		log.info( "maxTruckTimes: " + maxTruckTimes );
		log.info( "valueBiggerThanObjective: " + valueBiggerThanObjective );

	}

	public String toString(){
		return super.toString();

		//TODO implement toString
		/*
		String toString = "Nodes: \n";
		for( int i = 0; i < getNodes().length; i++ ) {
			toString += "[ " + getNodes()[i][0] + ", " + getNodes()[i][1] + " ]\n";
		}
		toString += "Drone Flight Time: " + droneFlightTime + "\n";
		toString += "Truck Times: \n";
		for( int i = 0; i < getTruckTimes().length; i++ ){
			for( int j = 0; j < getTruckTimes()[0].length; j++ ){
				toString += getTruckTimes()[i][j] + ", ";
			}
			toString = toString.substring( 0, toString.length() - 2 ) + "\n";
		}
		toString += "Drone Times: \n";
		for( int i = 0; i < droneTimes.length; i++ ){
			for( int j = 0; j < droneTimes[0].length; j++ ){
				toString += droneTimes[i][j] + ", ";
			}
			toString = toString.substring( 0, toString.length() - 2 ) + "\n";
		}
		toString += "Possible Drone Flights: \n;
		...

		return toString;
		*/
	}

	@Override protected GRBModel calcGrbModel() throws GRBException{

		log.info( "Start calculation of gurobi model for the FSTSP without all special constraints" );

		GRBModel grbModel = new GRBModel( grbEnv );

		//init arrays for decision variables
		grbTruckEdgeVars = new GRBVar[dimension][dimension];
		grbTruckEdgeWaitVars = new GRBVar[dimension][dimension];
		grbDroneFlightsVars = new GRBVar[dimension][dimension][dimension];

		GRBLinExpr grbLinExpr;
		StringBuilder logString;

		//create decision variables for the truck edges and for the wait times
		for( int i = 0; i < dimension; i++ ){
			for( int j = i; j < dimension; j++ ){
				if( i == j ){
					grbTruckEdgeVars[i][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "x" + i + "_" + i );
					grbTruckEdgeWaitVars[i][i] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.INTEGER, "w" + i + "_" + i );
				} else {
					log.debug( "Add decision var x" + i + "_" + j + " with factor " + truckTimes[i][j] );
					grbTruckEdgeVars[i][j] = grbModel.addVar( 0.0, 1.0, truckTimes[i][j], GRB.BINARY, "x" + i + "_" + j );
					grbTruckEdgeVars[j][i] = grbTruckEdgeVars[i][j];

					boolean droneFlightPossibleForIToJ = false;
					for( int customer = 0; customer < dimension; customer++ ){
						if( possibleDroneFlights[i][j][customer] ){
							droneFlightPossibleForIToJ = true;
							break;
						}
					}
					if( !droneFlightPossibleForIToJ ){
						log.debug( "- Add decision var w" + i + "_" + j + " with 0.0" );
						grbTruckEdgeWaitVars[i][j] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.INTEGER, "w" + i + "_" + j );
					} else {
						log.debug( "Add decision var w" + i + "_" + j + " with factor 1.0 and upper bound " + droneFlightTime );
						grbTruckEdgeWaitVars[i][j] = grbModel.addVar( 0.0, droneFlightTime, 1.0, GRB.INTEGER, "w" + i + "_" + j );
					}
					grbTruckEdgeWaitVars[j][i] = grbTruckEdgeWaitVars[i][j];
				}
			}
		}

		//create decision variables for the drone flights
		for( int i = 0; i < dimension; i++ ){
			for( int j = i; j < dimension; j++ ){
				for( int customer = 0; customer < dimension; customer++ ){
					if( possibleDroneFlights[i][j][customer] ){
						log.debug( "Add decision var y" + i + "_" + j + "_" + customer + " with factor 0.0 " );
						grbDroneFlightsVars[i][j][customer] = grbModel.addVar( 0.0, 1.0, 0.0, GRB.BINARY, "y" + i + "_" + j + "_" + customer );
					} else {
						grbDroneFlightsVars[i][j][customer] = grbModel.addVar( 0.0, 0.0, 0.0, GRB.BINARY, "y" + i + "_" + j + "_" + customer );
					}
					grbDroneFlightsVars[j][i][customer] = grbDroneFlightsVars[i][j][customer];
				}
			}
		}

		//create constraints that each customer is served by truck or drone exactly once
		//the constraint implicit contains degree-2 constraints for customers
		for( int customer = 1; customer < dimension; customer++ ) {
			grbLinExpr = new GRBLinExpr();
			logString = new StringBuilder( "0.5 * (" );
			for( int i = 0; i < dimension; i++ ) {
				if( i != customer ){
					logString.append( "x" ).append( i ).append( "_" ).append( customer ).append( " + " );
					grbLinExpr.addTerm( 0.5, grbTruckEdgeVars[i][customer] );
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) ).append( ") + " );

			for( int i = 0; i < dimension; i++ ){
				for( int j = i; j < dimension; j++ ){
					if( possibleDroneFlights[i][j][customer] ){
						logString.append( "y" ).append( i ).append( "_" ).append( j ).append( "_" ).append( customer ).append( " + " );
						grbLinExpr.addTerm( 1.0, grbDroneFlightsVars[i][j][customer] );
					}
				}
			}
			logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );

			log.debug( "Add constraint customer_served_once_" + customer + ": " + logString + " = 1" );
			grbModel.addConstr( grbLinExpr, GRB.EQUAL, 1.0, "customer_served_once_" + customer );
			calculatedConstraintsCounter++;
		}

		//Add degree-2 constraint for depot
		grbLinExpr = new GRBLinExpr();
		logString = new StringBuilder();
		for( int customer = 1; customer < dimension; customer++ ){
			logString.append( "x0_" ).append( customer ).append( " + " );
			grbLinExpr.addTerm( 1.0, grbTruckEdgeVars[0][customer] );
		}
		logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
		log.debug( "Add degree-2 constraint for depot: " + logString + " = 2" );
		grbModel.addConstr( grbLinExpr, GRB.EQUAL, 2.0, "deg2_depot" );
		calculatedConstraintsCounter++;

		// each node max 2 drone edges
		for( int i = 0; i < dimension; i++ ){
			grbLinExpr = new GRBLinExpr();
			logString = new StringBuilder();
			boolean termsAdded = false;
			for( int customer = 1; customer < dimension; customer++ ){
				for( int j = 0; j < dimension; j++ ){
					if( possibleDroneFlights[i][j][customer] ){
						termsAdded = true;
						logString.append( "y" ).append( i ).append( "_" ).append( j ).append( "_" ).append( customer ).append( " + " );
						grbLinExpr.addTerm( 1.0, grbDroneFlightsVars[i][j][customer] );
					}
				}
			}

			if( termsAdded ){
				logString = new StringBuilder( logString.substring( 0, logString.length() - 3 ) );
				log.debug( "Add constraint max_drone_edge_constraint_" + i + ":  2 >= " + logString );
				grbModel.addConstr( grbLinExpr, GRB.LESS_EQUAL, 2, "max_drone_edge_constraint_" + i );
				calculatedConstraintsCounter++;
			}
		}

		//create constraints that the truck visits start and end nodes of a drone flight
		for( int i = 0; i < dimension; i++ ){
			boolean possibleDroneFlightsForNodeI = false;
			GRBLinExpr grbLinExprRhs = new GRBLinExpr();
			StringBuilder logStringRhs = new StringBuilder();
			for( int j = 0; j < dimension; j++ ){
				for( int customer = 1; customer < dimension; customer++ ){
					if( i != j && i != customer && j != customer ){
						if( possibleDroneFlights[i][j][customer] ){
							logStringRhs.append( "y" ).append( i ).append( "_" ).append( j ).append( "_" ).append( customer ).append( " + " );
							grbLinExprRhs.addTerm( 1.0, grbDroneFlightsVars[i][j][customer] );
							possibleDroneFlightsForNodeI = true;
						}
					}
				}
			}
			if( !possibleDroneFlightsForNodeI ){
				continue;
			}

			GRBLinExpr grbLinExprLhs = new GRBLinExpr();
			StringBuilder logStringLhs = new StringBuilder();
			for( int j = 0; j < dimension; j++ ){
				if( i != j ){
					logStringLhs.append( "x" ).append( i ).append( "_" ).append( j ).append( " + " );
					grbLinExprLhs.addTerm( 1.0, grbTruckEdgeVars[i][j] );
				}
			}

			logStringLhs = new StringBuilder( logStringLhs.substring( 0, logStringLhs.length() - 3 ) );
			logStringRhs = new StringBuilder( logStringRhs.substring( 0, logStringRhs.length() - 3 ) );
			log.debug( "Add constraint truck_visits_drone_nodes: " + logStringLhs + " >= " + logStringRhs );
			grbModel.addConstr( grbLinExprLhs, GRB.GREATER_EQUAL, grbLinExprRhs, "truck_visits_drone_node_" + i );
			calculatedConstraintsCounter++;
		}

		log.debug( "calculatedConstraintsCounter: " + calculatedConstraintsCounter );
		log.info( "End calculation of gurobi model for the FSTSP without all special constraints" );

		return grbModel;
	}

	@Override public boolean presolveHeuristic( Defines.PresolveHeuristicType presolveHeuristicType ){

		switch( presolveHeuristicType ){
			case TSP:
				//tsp as heuristic solution
				Tsp tsp = new Tsp( this.name, this.comment, Defines.TSP, this.dimension, this.nodes, this.distances );
				if( tsp.grbOptimize() != null ){
					if( tsp.getResult().isOptimal() ){
						grbTruckEdgeVarsStartValues = tsp.truckEdgeVars.clone();
						//calculate heuristic value with tsp solution and calculated trucktimes of fstsp
						double calculatedHeuristicValue = 0.0;
						for( int i = 0; i < dimension; i++ ){
							for( int j = i; j < dimension; j++ ){
								if( (int)(grbTruckEdgeVarsStartValues[i][j] + 0.5d) == 1 ){
									calculatedHeuristicValue += truckTimes[i][j];
								}
							}
						}
						log.info( "Calculated heuristicValue with TSP solution and trucktimes: " + calculatedHeuristicValue );
						setHeuristicValue( calculatedHeuristicValue );
						// set the grbDroneFlightsVarsStartValues and grbTruckEdgeWaitVarsStartValues to 0 (doubles are 0 by default)
						grbDroneFlightsVarsStartValues = new double[dimension][dimension][dimension];
						grbTruckEdgeWaitVarsStartValues = new double[dimension][dimension];
						return true;
					}
				}
				return false;
			case FSTSP:
				//calculate tsp solution to create a heurisitc FSTSP solution
				Tsp tspProblem = new Tsp( this.name, this.comment, Defines.TSP, this.dimension, this.nodes, this.distances );
				if( tspProblem.grbOptimize() != null ){
					if( tspProblem.getResult().isOptimal() ){

						ArrayList<Integer> tspTruckTour = tspProblem.getResult().getLast().getTruckTours().get( 0 );

						int[][] adjMatrix = new int[dimension][dimension];
						int[][] customerWithGreatestSaving = new int[dimension][dimension];
						int[][] truckEdgeWaitTimes = new int[dimension][dimension];
						for( int[] row : adjMatrix ){
							Arrays.fill( row, -1 );
						}
						for( int[] row : customerWithGreatestSaving ){
							Arrays.fill( row, -1 );
						}

						//TODO interpret tsp solution the other way round
						//add directed weighted edges for tsp solution
						for( int i = 0; i < tspTruckTour.size() - 1; i++ ){
							adjMatrix[tspTruckTour.get( i )][tspTruckTour.get( i + 1 )] = 0;
						}
						//TODO use this when droneflight to end depot allowed
						//adjMatrix[tspTruckTour.size() - 1][0] = 0;

						for( int i = 0; i < tspTruckTour.size(); i++ ){
							for( int j = i + 2; j < tspTruckTour.size(); j++ ){

								int startNode = tspTruckTour.get( i );
								int endNode = tspTruckTour.get( j );

								List<Integer> truckTourFromIToJ = tspTruckTour.subList( i, j + 1 );
								List<Integer> truckTourBetweenIAndJ = tspTruckTour.subList( i + 1, j );
								log.debug( "truckTourFromIToJ: " + truckTourFromIToJ );
								log.debug( "truckTourBetweenIAndJ: " + truckTourBetweenIAndJ );

								//calculate truck time without customer
								int truckTimeIToJOld = 0;
								for( int k = 0; k < truckTourFromIToJ.size() - 1; k++ ){
									truckTimeIToJOld += truckTimes[truckTourFromIToJ.get( k )][truckTourFromIToJ.get( k + 1 )];
								}

								for( int customer : truckTourBetweenIAndJ ){

									if( possibleDroneFlights[startNode][endNode][customer] ){

										log.debug( "Check droneFlight: ({ " + startNode + ", " + endNode + " }, " + customer + " )" );

										List<Integer> truckTourFromIToJWithoutCustomer = new ArrayList<>( truckTourFromIToJ );
										truckTourFromIToJWithoutCustomer.remove( new Integer( customer ) );

										//calculate droneFlightTime
										int droneFlightTime = droneTimes[startNode][customer] + droneTimes[customer][endNode];

										//calculate truck time without customer
										int truckTimeIToJNew = 0;
										for( int k = 0; k < truckTourFromIToJWithoutCustomer.size() - 1; k++ ){
											truckTimeIToJNew += truckTimes[truckTourFromIToJWithoutCustomer.get( k )][truckTourFromIToJWithoutCustomer.get( k + 1 )];
										}

										int truckEdgeWaitTime = 0;
										if( droneFlightTime > truckTimeIToJNew ){
											truckEdgeWaitTime = droneFlightTime - truckTimeIToJNew;
										} else if( truckTimeIToJNew > this.droneFlightTime ){
											log.debug( "droneFlight: ({ " + startNode + ", " + endNode + " }, " + customer + " ) not possible, cause flight time plus "
															+ "wait time exceeds droneFlightTime!" );
											break;
										}

										int saving = (-truckEdgeWaitTime - truckTimeIToJNew) + truckTimeIToJOld;

										log.debug( "truckTimeIToJOld for tour ( " + startNode + ", " + endNode + " ) is: " + truckTimeIToJOld );
										log.debug( "truckTimeIToJNew for tour ( " + startNode + ", " + endNode + " ) is: " + truckTimeIToJNew );
										log.debug( "droneFlightTime for droneflight ({ " + startNode + ", " + endNode + " }, " + customer + " ) is: " + droneFlightTime );
										log.debug( "truckEdgeWaitTime for tour ( " + startNode + ", " + endNode + " ) is: " + truckEdgeWaitTime );
										log.debug( "saving for tour ( " + startNode + ", " + endNode + " ) is: " + saving );

										if( saving > 0 && saving > adjMatrix[startNode][endNode] ){
											adjMatrix[startNode][endNode] = saving;
											customerWithGreatestSaving[startNode][endNode] = customer;
											truckEdgeWaitTimes[startNode][endNode] = truckEdgeWaitTime;
										}

									} else {
										log.debug( "droneFlight: ({ " + startNode + ", " + endNode + " }, " + customer + " ) not possible. Skip it!" );
									}
								}
							}
						}

						log2DimIntArray( adjMatrix, "Adjacency matrix of directed heuristic graph" );
						log2DimIntArray( customerWithGreatestSaving, "customerWithGreatestSaving" );
						log2DimIntArray( truckEdgeWaitTimes, "truckEdgeWaitTimes" );

						//calculate path with most savings (longest path)
						log.debug( "tspTruckTour: " + tspTruckTour );
						ArrayList<Integer>[] longestPathOfI = new ArrayList[dimension];
						ArrayList<Integer>[] truckTourToI = new ArrayList[dimension];
						int[] greatestSavingOfI = new int[dimension];
						ArrayList<Integer[]>[] droneFlightsInLongestPathOfI = new ArrayList[dimension];

						longestPathOfI[0] = new ArrayList<>();
						longestPathOfI[0].add( 0 );
						for( int i = 0; i < dimension; i++ ){
							droneFlightsInLongestPathOfI[i] = new ArrayList<>();
						}

						for( int i = 1; i < tspTruckTour.size(); i++ ){
							longestPathOfI[tspTruckTour.get( i )] = new ArrayList<>( longestPathOfI[tspTruckTour.get( i - 1 )] );
							longestPathOfI[tspTruckTour.get( i )].add( tspTruckTour.get( i ) );
						}
/*
						for( int i = 0; i < longestPathOfI.length; i++ ){
							log.debug( "longestPathOfI[" + tspTruckTour.get( i ) + "]: " + longestPathOfI[tspTruckTour.get( i )] );
							log.debug( "truckTourOfI[" + tspTruckTour.get( i ) + "]: " + truckTourOfI[tspTruckTour.get( i )] );
							log.debug( "greatestSavingsOfI[" + tspTruckTour.get( i ) + "]: " + greatestSavingOfI[tspTruckTour.get( i )] );
						}
 */

						for( int k = 1; k < dimension; k++ ){
							int i = tspTruckTour.get( k );
							for( int j = 0; j < dimension; j++ ){
								if( i != j && adjMatrix[j][i] >= 0 ){
									//log.debug( "edge: (" + j + ", " + i + ") has saving: " + (adjMatrix[j][i] + greatestSavingOfI[j]) );
									if( adjMatrix[j][i] + greatestSavingOfI[j] > greatestSavingOfI[i] ){

										//log.debug( "New best saving found!" );

										longestPathOfI[i] = new ArrayList<>( longestPathOfI[j] );
										droneFlightsInLongestPathOfI[i] = new ArrayList<>( droneFlightsInLongestPathOfI[j] );
										int customer = customerWithGreatestSaving[j][i];
										if( customer > 0 ){
											longestPathOfI[i].add( customer );
											droneFlightsInLongestPathOfI[i].add( new Integer[]{ j, i, customer } );
										}
										//droneFlightCustomerInSolution[i] = customer;

										longestPathOfI[i].add( i );
										greatestSavingOfI[i] = adjMatrix[j][i] + greatestSavingOfI[j];

										//log.debug( "longestPathOfI[" + i + "]: " + longestPathOfI[i] );
										//log.debug( "greatestSavingOfI[" + i + "]: " + greatestSavingOfI[i] );
									} else {
										//log.debug( "Current saving is below current best saving of: " + greatestSavingOfI[i] );
									}
								}
							}
							truckTourToI[i] = new ArrayList<>( tspTruckTour.subList( 0, k + 1 ) );
							for( Integer[] droneFlight : droneFlightsInLongestPathOfI[i] ){
								truckTourToI[i].remove( new Integer( droneFlight[2] ) );
							}

							log.debug( "greatestSavingOfI[" + i + "]: " + greatestSavingOfI[i] );
							log.debug( "longestPathOfI[" + i + "]: " + longestPathOfI[i] );
							log.debug( "truckTourToI[" + i + "]: " + truckTourToI[i] );
							log.debug( "droneFlightsInLongestPathOfI[" + i + "]: " );
							if( droneFlightsInLongestPathOfI[i].size() <= 0 ){
								log.debug( "-" );
							}
							for( Integer[] droneFlight : droneFlightsInLongestPathOfI[i] ){
								log.debug( "droneFlight: ({ " + droneFlight[0] + ", " + droneFlight[1] + " }, " + droneFlight[2] + " )" );
							}
						}

						log.debug( "------------------------------------------------------------------" );
						log.debug( "tspTruckTour: " + tspTruckTour );
						int lastNode = tspTruckTour.get( tspTruckTour.size() - 1 );
						log.debug( "longestPathOf last node (" + lastNode + "): " + longestPathOfI[lastNode] );
						log.debug( "truckTourTo last node (" + lastNode + "): " + truckTourToI[lastNode] );
						log.debug( "greatestSavingOf last node (" + lastNode + "): " + greatestSavingOfI[lastNode] );
						log.debug( "droneFlightsInLongestPathOf last node (" + lastNode + "): " );
						if( droneFlightsInLongestPathOfI[lastNode].size() <= 0 ){
							log.debug( "-" );
						}
						for( Integer[] droneFlight : droneFlightsInLongestPathOfI[lastNode] ){
							log.debug( "droneFlight: ({ " + droneFlight[0] + ", " + droneFlight[1] + " }, " + droneFlight[2] + " )" );
						}

						// intialize grbTruckEdgeVarsStartValues, grbDroneFlightsVarsStartValues and grbTruckEdgeWaitVarsStartValues
						grbTruckEdgeVarsStartValues = new double[dimension][dimension];
						grbDroneFlightsVarsStartValues = new double[dimension][dimension][dimension];
						grbTruckEdgeWaitVarsStartValues = new double[dimension][dimension];

						for( int i = 0; i < truckTourToI[lastNode].size(); i++ ){
							int startNode = truckTourToI[lastNode].get( i );
							int endNode;
							if( i == truckTourToI[lastNode].size() - 1 ){
								endNode = 0;
							} else {
								endNode = truckTourToI[lastNode].get( i + 1 );
							}
							grbTruckEdgeVarsStartValues[startNode][endNode] = 1;
							grbTruckEdgeVarsStartValues[endNode][startNode] = 1;
						}

						for( Integer[] droneFlight : droneFlightsInLongestPathOfI[lastNode] ){
							grbDroneFlightsVarsStartValues[droneFlight[0]][droneFlight[1]][droneFlight[2]] = 1;
							grbDroneFlightsVarsStartValues[droneFlight[1]][droneFlight[0]][droneFlight[2]] = 1;

							grbTruckEdgeWaitVarsStartValues[droneFlight[0]][droneFlight[1]] = truckEdgeWaitTimes[droneFlight[0]][droneFlight[1]];
							grbTruckEdgeWaitVarsStartValues[droneFlight[1]][droneFlight[0]] = truckEdgeWaitTimes[droneFlight[0]][droneFlight[1]];
						}

						double calculatedHeuristicValue = 0.0;
						log.debug( "Calculate heuristicValue: " );
						for( int i = 0; i < dimension; i++ ){
							for( int j = i; j < dimension; j++ ){
								if( (int)(grbTruckEdgeVarsStartValues[i][j] + 0.5d) == 1 ){
									log.debug( "Add truckEdge ( " + i + ", " + j + " ) with truckTime " + truckTimes[i][j] );
									calculatedHeuristicValue += truckTimes[i][j];
								}
								if( grbTruckEdgeWaitVarsStartValues[i][j] > 0 ){
									log.debug( "Add truckEdgeWaitTime " + grbTruckEdgeWaitVarsStartValues[i][j] );
									calculatedHeuristicValue += grbTruckEdgeWaitVarsStartValues[i][j];
								}
							}
						}

						log.info( "Calculated heuristicValue with FSTSP heuristic solution: " + calculatedHeuristicValue );
						setHeuristicValue( calculatedHeuristicValue );

						return true;
					}
				}
				return false;
			default:
				log.info( "PresolveHeuristicType + '" + presolveHeuristicType.toString() + "' not supported for TSP!" );
				return false;
		}
	}

	@Override protected void setStartValues() throws GRBException{

		super.setStartValues();
		if( grbDroneFlightsVarsStartValues != null ){
			log.info( "Set start values for grbDroneFlightsVars!" );
			for( int i = 0; i < dimension; i++ ){
				for( int j = i; j < dimension; j++ ){
					for( int customer = 0; customer < dimension; customer++ ){
						if( possibleDroneFlights[i][j][customer] && grbDroneFlightsVarsStartValues[i][j][customer] >= 0 ){
							log.debug( "Set start value for y" + i + "_" + j + "_" + customer + ": " + (int)(grbDroneFlightsVarsStartValues[i][j][customer] + 0.5d) );
							grbDroneFlightsVars[i][j][customer].set( GRB.DoubleAttr.Start, (int)(grbDroneFlightsVarsStartValues[i][j][customer] + 0.5d) );
						} else {
							grbDroneFlightsVars[i][j][customer].set( GRB.DoubleAttr.Start, GRB.UNDEFINED );
						}
					}
				}
			}
		} else {
			log.info( "Could not set start values for grbDroneFlightsVars, because grbDroneFlightsVarsStartValues is null!" );
		}

		if( grbTruckEdgeWaitVarsStartValues != null ){
			log.info( "Set start values for grbTruckEdgeWaitVars!" );
			for( int i = 0; i < dimension; i++ ){
				for( int j = i; j < dimension; j++ ){
					if( i != j && grbTruckEdgeWaitVarsStartValues[i][j] >= 0 ){
						log.debug( "Set start value for w" + i + "_" + j + ": " + (int)(grbTruckEdgeWaitVarsStartValues[i][j] + 0.5d) );
						grbTruckEdgeWaitVars[i][j].set( GRB.DoubleAttr.Start, (int)(grbTruckEdgeWaitVarsStartValues[i][j] + 0.5d) );
					} else {
						grbTruckEdgeWaitVars[i][j].set( GRB.DoubleAttr.Start, GRB.UNDEFINED );
					}
				}
			}
		} else {
			log.info( "Could not set start values for grbTruckEdgeWaitVars, because grbTruckEdgeWaitVarsStartValues is null!" );
		}
	}

	@Override protected TspModelIterationResult calculateAndAddIterationResult() throws GRBException{

		FstspIterationResult fstspIterationResult = new FstspIterationResult();
		double[][] truckEdgeVars = grbModel.get( GRB.DoubleAttr.X, grbTruckEdgeVars );
		fstspIterationResult.setTruckTours( findSubtours( truckEdgeVars ) );
		double[][][] droneFlightsVars = grbModel.get( GRB.DoubleAttr.X, grbDroneFlightsVars );
		fstspIterationResult.setDroneFlights( findDroneFlights( droneFlightsVars ) );
		fstspIterationResult.setTruckEdgeWaitVars( grbModel.get( GRB.DoubleAttr.X, grbTruckEdgeWaitVars ) );
		result.getFstspIterationResults().add( fstspIterationResult );

		return fstspIterationResult;
	}

	private ArrayList<Integer[]> findDroneFlights( double[][][] droneFlightsVars ){
		ArrayList<Integer[]> droneFlights = new ArrayList<>();
		for( int i = 0; i < droneFlightsVars.length; i++ ) {
			for( int j = i + 1; j < droneFlightsVars[i].length; j++ ){
				for( int c = 0; c < droneFlightsVars[i][j].length; c++ ){
					if( (int)(droneFlightsVars[i][j][c] + 0.5d) != 0 ){
						droneFlights.add( new Integer[]{ i, j, c } );
					}
				}
			}
		}
		return droneFlights;
	}

	protected String logSolution() throws GRBException{
		StringBuilder solutionString = new StringBuilder( super.logSolution() ).append( "\n" );

		double[][][] droneFlightsVars = new double[dimension][dimension][dimension];
		for( int i = 0; i < dimension; i++ ){
			droneFlightsVars[i] = getSolution( grbDroneFlightsVars[i] );
		}

		ArrayList<Integer[]> droneFlights = findDroneFlights( droneFlightsVars );

		if( droneFlights.size() > 0 ){
			solutionString.append( "Drone_Flights_Size" ).append( ": " ).append( droneFlights.size() ).append( "\n" );
			for( int v = 0; v < droneFlights.size(); v++ ){
				if( droneFlights.get( v ).length > 0 ){
					solutionString.append( "Drone_Flight_" ).append( droneFlights.get( v )[2] ).append( ": " );
					for( int i = 0; i < 2; i++ ){
						solutionString.append( droneFlights.get( v )[i] ).append( ", " );
					}
					solutionString = new StringBuilder( solutionString.substring( 0, solutionString.length() - 2 ) ).append( "\n" );
				}
			}
		}
		return solutionString.toString();
	}

	@Override public TspModelResult getResult(){
		return result;
	}

	@Override protected void logIterationDebug() throws GRBException{
		super.logIterationDebug();
		//TODO add debug information for droneFlights?!
	}

	@Override protected boolean addViolatedConstraints() throws GRBException{
		log.info( "Look for violated constraints and add them." );

		ArrayList<ArrayList<Integer>> subtours = result.getLast().getTruckTours();

		ArrayList<Integer[]> droneFlights = ((FstspIterationResult)result.getLast()).getDroneFlights();

		double[][] truckEdgeWaitVars = ((FstspIterationResult)result.getLast()).getTruckEdgeWaitVars();

		ArrayList<GurobiConstraint> violatedConstraints = getViolatedConstraints( subtours, droneFlights, truckEdgeWaitVars );
		if( violatedConstraints.size() > 0 ){
			log.info( "Add all violated constraints!" );
			for( GurobiConstraint violatedConstraint : violatedConstraints ){
				grbModel.addConstr( violatedConstraint.getLinExpr(), violatedConstraint.getSense(), violatedConstraint.getRhs(), violatedConstraint.getName() );
				additionalConstraintsCounter++;
			}
			return true;
		} else {
			log.info( "No violated constraints found!" );
			return false;
		}
	}

	@Override protected boolean addViolatedLazyConstraints() throws GRBException{
		log.info( "Look for violated constraints and add them as lazy constraints." );

		log.info( "Looking for truck subtours." );
		ArrayList<ArrayList<Integer>> subtours;
		double[][] truckEdgeVars = getSolution( grbTruckEdgeVars );
		subtours = findSubtours( truckEdgeVars );

		double[][][] droneFlightsVars = new double[dimension][dimension][dimension];
		for( int i = 0; i < dimension; i++ ){
			droneFlightsVars[i] = getSolution( grbDroneFlightsVars[i] );
		}
		ArrayList<Integer[]> droneFlights = findDroneFlights( droneFlightsVars );

		double[][] truckEdgeWaitVars = getSolution( grbTruckEdgeWaitVars );

		ArrayList<GurobiConstraint> violatedConstraints = getViolatedConstraints( subtours, droneFlights, truckEdgeWaitVars );
		if( violatedConstraints.size() > 0 ){
			log.info( "Add all violated constraints as lazy constraints!" );
			for( GurobiConstraint violatedConstraint : violatedConstraints ){
				addLazy( violatedConstraint.getLinExpr(), violatedConstraint.getSense(), violatedConstraint.getRhs() );
				additionalConstraintsCounter++;
			}
			return true;
		} else {
			log.info( "No violated constraints found!" );
			return false;
		}
	}

	private ArrayList<GurobiConstraint> getViolatedConstraints( ArrayList<ArrayList<Integer>> subtours, ArrayList<Integer[]> droneFlights, double[][] truckEdgeWaitVars ){
		ArrayList<GurobiConstraint> violatedConstraints = new ArrayList<>();

		if( subtours.size() > 1 ){
			log.info( "Found subtours: " + subtours.size() );
			log.info( "Subtours: " + subtours );

			log.info( "Get violated subtour elimination constraints" );
			for( ArrayList<Integer> subtour : subtours ){

				if( subtour.contains( 0 ) ){
					log.info( "Skip subtour with depot, cause it is a possible solution for the FSTSP." );
				} else {
					int subtourVertexCounter = subtour.size();

					ArrayList<int[]> edges = createEdgesForSubtourEliminationConstraint( subtour );
					StringBuilder subtourEliminationConstraintString = new StringBuilder();
					GRBLinExpr grbExpr = new GRBLinExpr();
					for( int[] edge : edges ){
						subtourEliminationConstraintString.append( "x" ).append( edge[0] ).append( "_" ).append( edge[1] ).append( " + " );
						grbExpr.addTerm( 1.0, grbTruckEdgeVars[edge[0]][edge[1]] );
					}
					log.debug( "Found violated subtour elimination constraint: " + subtourEliminationConstraintString
									.substring( 0, subtourEliminationConstraintString.length() - 2 ) + "<= " + (subtour.size() - 1) );
					violatedConstraints.add( new GurobiConstraint( grbExpr, GRB.LESS_EQUAL, subtourVertexCounter - 1, null ) );
				}
			}
		}

		log.info( "Looking for forbidden sub-drone-flights." );
		if( droneFlights.size() > 0 ){
			log.info( "droneFlights:" );
			for( int k = 0; k < droneFlights.size(); k++ ){
				Integer i = droneFlights.get( k )[0];
				Integer j = droneFlights.get( k )[1];
				Integer c = droneFlights.get( k )[2];
				log.info( "droneFlight_" + k + ": ({ " + i + ", " + j + " }, " + c + " )" );
			}
		} else {
			log.debug( "No droneFlights in solution!" );
		}

		for( Integer[] droneFlight : droneFlights ){
			Integer i = droneFlight[0];
			Integer j = droneFlight[1];
			Integer c = droneFlight[2];
			log.debug( "Check drone flight ({ " + i + ", " + j + " }, " + c + " ) for sub-drone-flights." );
			for( ArrayList<Integer> subtour : subtours ){
				if( subtour.contains( i ) && subtour.contains( j ) ){
					//droneFlight start/end in same subtour
					int indexI = subtour.indexOf( i );
					int indexJ = subtour.indexOf( j );
					if( indexI > indexJ ){
						int tmp = indexI;
						indexI = indexJ;
						indexJ = tmp;
					}
					List<Integer> truckTourItoJ = subtour.subList( indexI, indexJ + 1 );
					log.debug( "truckTourItoJ: " + truckTourItoJ );
					List<Integer> truckTourBetweenIToJ = subtour.subList( indexI + 1, indexJ );
					log.debug( "truckTourBetweenIToJ: " + truckTourBetweenIToJ );

					for( Integer[] subDroneFlight : droneFlights ){
						Integer i2 = subDroneFlight[0];
						Integer j2 = subDroneFlight[1];
						Integer c2 = subDroneFlight[2];
						//skip the current sub drone flight
						if( !(i2.equals( i ) && j2.equals( j ) && c2.equals( c )) ){
							// skip sub drone flights which have start and end node in a different truck subtour

							if( truckTourBetweenIToJ.contains( i2 ) || truckTourBetweenIToJ.contains( j2 ) || (truckTourItoJ.get( 0 ).equals( i2 ) && truckTourItoJ
											.get( truckTourItoJ.size() - 1 ).equals( j2 )) || (truckTourItoJ.get( 0 ).equals( j2 ) && truckTourItoJ
											.get( truckTourItoJ.size() - 1 ).equals( i2 )) ){

								StringBuilder subDroneFlightEliminationConstraintString = new StringBuilder();
								GRBLinExpr grbExpr = new GRBLinExpr();

								grbExpr.addTerm( 1.0, grbDroneFlightsVars[i][j][c] );
								subDroneFlightEliminationConstraintString.append( "y" ).append( i ).append( "_" ).append( j ).append( "_" ).append( c ).append( " + " );
								grbExpr.addTerm( 1.0, grbDroneFlightsVars[i2][j2][c2] );
								subDroneFlightEliminationConstraintString.append( "y" ).append( i2 ).append( "_" ).append( j2 ).append( "_" ).append( c2 )
												.append( " + " );
								for( int k = 0; k < truckTourItoJ.size() - 1; k++ ){
									int node1 = truckTourItoJ.get( k );
									int node2 = truckTourItoJ.get( k + 1 );
									grbExpr.addTerm( 1.0, grbTruckEdgeVars[node1][node2] );
									subDroneFlightEliminationConstraintString.append( "x" ).append( node1 ).append( "_" ).append( node2 ).append( " + " );
								}

								log.debug( "Found violated sub-drone-flight elimination constraint: " + subDroneFlightEliminationConstraintString
												.substring( 0, subDroneFlightEliminationConstraintString.length() - 2 ) + "<= " + truckTourItoJ.size() );
								violatedConstraints.add( new GurobiConstraint( grbExpr, GRB.LESS_EQUAL, truckTourItoJ.size(), null ) );
							} else {
								log.debug( "Skip sub drone flight ({ " + i2 + ", " + j2 + " }, " + c2 + " ), cause it is not forbidden." );
							}
						} else {
							log.debug( "Skip the current sub drone flight ({ " + i2 + ", " + j2 + " }, " + c2 + " ), cause it is the current drone flight." );
						}
					}
				}
			}
		}

		log.info( "Looking for forbidden drone flights and violated wait times constraints for allowed drone flights." );
		// TODO do it together with the sub drone flights check to increase performance?!
		for( Integer[] droneFlight : droneFlights ){
			Integer i = droneFlight[0];
			Integer j = droneFlight[1];
			Integer c = droneFlight[2];
			log.debug( "Check drone flight ({ " + i + ", " + j + " }, " + c + " ) for wait time." );

			for( ArrayList<Integer> subtour : subtours ){
				if( subtour.contains( i ) && subtour.contains( j ) ){
					//droneFlight start/end in same subtour
					int indexI = subtour.indexOf( i );
					int indexJ = subtour.indexOf( j );
					if( indexI > indexJ ){
						int tmp = indexI;
						indexI = indexJ;
						indexJ = tmp;
					}
					List<Integer> truckTourItoJ = subtour.subList( indexI, indexJ + 1 );
					log.debug( "truckTourItoJ: " + truckTourItoJ );

					StringBuilder waitTimeConstraintString = new StringBuilder();
					GRBLinExpr grbExpr = new GRBLinExpr();

					grbExpr.addConstant( truckTourItoJ.size() * valueBiggerThanObjective );
					waitTimeConstraintString.append( valueBiggerThanObjective ).append( " * ( " ).append( truckTourItoJ.size() ).append( " - " );
					grbExpr.addTerm( -valueBiggerThanObjective, grbDroneFlightsVars[i][j][c] );
					waitTimeConstraintString.append( "y" ).append( i ).append( "_" ).append( j ).append( "_" ).append( c ).append( " - " );
					double rhs = droneTimes[i][c] + droneTimes[c][j];
					for( int k = 0; k < truckTourItoJ.size() - 1; k++ ){
						int node1 = truckTourItoJ.get( k );
						int node2 = truckTourItoJ.get( k + 1 );
						grbExpr.addTerm( -valueBiggerThanObjective, grbTruckEdgeVars[node1][node2] );
						waitTimeConstraintString.append( "x" ).append( node1 ).append( "_" ).append( node2 ).append( " - " );
						rhs -= truckTimes[node1][node2];
					}

					if( rhs == 0 ){
						log.debug( "Skip current drone flight, cause wait time is exactly 0 for the current tour!" );
						break;
					} else if( rhs < 0 ){
						log.debug( "Drone has to wait for the truck. Check if wait time plus flight time exceeds maximum drone flight time." );
						double droneWaitTime = -rhs;
						if( droneTimes[i][c] + droneTimes[c][j] + droneWaitTime > droneFlightTime ){
							grbExpr = new GRBLinExpr();
							StringBuilder droneFlightTimeExceedsConstraintString = new StringBuilder( "y" ).append( i ).append( "_" ).append( j ).append( "_" )
											.append( c ).append( " + " );
							grbExpr.addTerm( 1.0, grbDroneFlightsVars[i][j][c] );
							for( int k = 0; k < truckTourItoJ.size() - 1; k++ ){
								int node1 = truckTourItoJ.get( k );
								int node2 = truckTourItoJ.get( k + 1 );
								grbExpr.addTerm( 1.0, grbTruckEdgeVars[node1][node2] );
								droneFlightTimeExceedsConstraintString.append( "x" ).append( node1 ).append( "_" ).append( node2 ).append( " + " );
							}
							log.debug( "Found violated drone flight time exceeds constraint: " + droneFlightTimeExceedsConstraintString
											.substring( 0, droneFlightTimeExceedsConstraintString.length() - 2 ) + "<= " + (truckTourItoJ.size() - 1) );
							violatedConstraints.add( new GurobiConstraint( grbExpr, GRB.LESS_EQUAL, truckTourItoJ.size() - 1, null ) );
						} else {
							break;
						}
					} else {
						grbExpr.addTerm( 1.0, grbTruckEdgeWaitVars[i][j] );
						int w_ij = (int)(truckEdgeWaitVars[i][j] + 0.5d);
						if( w_ij >= rhs ){
							log.debug( "Do not add wait time constraint, cause w" + i + "_" + j + " = " + w_ij + " is already greater-equal " + rhs );
							break;
						}
						log.debug( "w" + i + "_" + j + ": " + w_ij );
						log.debug( "Found violated wait time constraint: " + waitTimeConstraintString.substring( 0, waitTimeConstraintString.length() - 2 ) + ") + w" + i
										+ "_" + j + " >= " + rhs );
						violatedConstraints.add( new GurobiConstraint( grbExpr, GRB.GREATER_EQUAL, rhs, null ) );
					}
				}
			}
		}

		return violatedConstraints;

	}

	public double getDroneFlightTime(){
		return droneFlightTime;
	}

	public void setDroneFlightTime( int droneFlightTime ){
		this.droneFlightTime = droneFlightTime;
	}

	public int[][] getDroneTimes(){
		return droneTimes;
	}

	public void setDroneTimes( int[][] droneTimes ){
		this.droneTimes = droneTimes;
	}

	public int[] getDroneDeliveryPossible(){
		return droneDeliveryPossible;
	}

	public void setDroneDeliveryPossible( int[] droneDeliveryPossible ){
		this.droneDeliveryPossible = droneDeliveryPossible;
	}

	public double getTruckSpeed(){
		return truckSpeed;
	}

	public void setTruckSpeed( double truckSpeed ){
		this.truckSpeed = truckSpeed;
	}

	public int[][] getTruckTimes(){
		return truckTimes;
	}

	public void setTruckTimes( int[][] truckTimes ){
		this.truckTimes = truckTimes;
	}

	public double getDroneSpeed(){
		return droneSpeed;
	}

	public void setDroneSpeed( int droneSpeed ){
		this.droneSpeed = droneSpeed;
	}

	public void setDroneSpeed( double droneSpeed ){
		this.droneSpeed = droneSpeed;
	}

	public void setDroneFlightTime( double droneFlightTime ){
		this.droneFlightTime = droneFlightTime;
	}

	public boolean[][][] getPossibleDroneFlights(){
		return possibleDroneFlights;
	}

	public void setPossibleDroneFlights( boolean[][][] possibleDroneFlights ){
		this.possibleDroneFlights = possibleDroneFlights;
	}

	public double getDroneFlightRange(){
		return droneFlightTime * droneSpeed;
	}

	public double getMaximumCustomerDistance(){
		double maxDistance = -1;
		for( int i = 1; i < dimension; i++ ){
			if( distances[0][i] > maxDistance ){
				maxDistance = distances[0][i];
			}
		}
		return maxDistance;
	}

	public double getDroneFlightRangePercentage(){
		double droneFlightRangePercentage = (getDroneFlightRange() / getMaximumCustomerDistance()) * 100.0;
		return Math.round( droneFlightRangePercentage ) / 2.0;
	}

	public double[][][] getGrbDroneFlightsVarsStartValues(){
		return grbDroneFlightsVarsStartValues;
	}

	public void setGrbDronesCustomersVarsStartValues( double[][][] grbDroneFlightsVarsStartValues ){
		this.grbDroneFlightsVarsStartValues = grbDroneFlightsVarsStartValues;
	}

	public int getPossibleDroneFlightsSize(){
		return this.possibleDroneFlightsSize;
	}
}
