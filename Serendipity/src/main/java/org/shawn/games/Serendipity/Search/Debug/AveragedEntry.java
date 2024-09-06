package org.shawn.games.Serendipity.Search.Debug;

public class AveragedEntry implements DebugEntry
{
	long total;
	long count;

	public AveragedEntry()
	{
		total = 0;
		count = 0;
	}

	public void add(long number)
	{
		count++;
		total += number;
	}

	public String toString()
	{
		return "Mean: " + total / count + " Hits: " + count;
	}
}
