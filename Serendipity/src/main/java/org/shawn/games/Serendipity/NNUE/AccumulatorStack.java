package org.shawn.games.Serendipity.NNUE;

public class AccumulatorStack
{
	NNUE network;
	
	private static enum Color
	{
		WHITE,
		BLACK
	}
	
	private static class Accumulator
	{
		int[] values;
		Color color;
		boolean needsRefresh
	}
	
	private static class AccumulatorPair
	{
		
	}
}
