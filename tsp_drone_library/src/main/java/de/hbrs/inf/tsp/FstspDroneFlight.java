package de.hbrs.inf.tsp;

import java.util.HashSet;

public class FstspDroneFlight{

	protected HashSet<Integer> startEnd;
	protected int customer;

	public FstspDroneFlight( HashSet<Integer> startEnd, int customer ){
		this.startEnd = startEnd;
		this.customer = customer;
	}

	public FstspDroneFlight( int start, int end, int customer ){
		HashSet<Integer> startEnd = new HashSet<>();
		startEnd.add( start );
		startEnd.add( end );
		this.startEnd = startEnd;
		this.customer = customer;
	}

	@Override public boolean equals( Object object ){
		boolean sameSame = false;

		if( object != null && object instanceof FstspDroneFlight ){
			if( this.startEnd.equals( ((FstspDroneFlight)object).startEnd ) ){
				if( this.customer == ((FstspDroneFlight)object).customer ){
					sameSame = true;
				}
			}
		}

		return sameSame;
	}

	@Override public String toString(){
		return "FstspDroneFlight{" + "startEnd=" + startEnd + ", customer=" + customer + '}';
	}
}
