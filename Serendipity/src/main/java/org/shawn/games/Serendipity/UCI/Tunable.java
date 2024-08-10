package org.shawn.games.Serendipity.UCI;

public class Tunable extends IntegerOption
{
	public Tunable(int value, int lowerBound, int upperBound, double step, double lr, String name)
	{
		super(value, lowerBound, upperBound, name);
		System.out.println(name + ", int, " + value + ", " + lowerBound + ", " + upperBound + ", " + step + ", " + lr);
	}
}
