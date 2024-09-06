package org.shawn.games.Serendipity.Search.Debug;

public class FrequencyEntry implements DebugEntry
{
	long count;
	long hit;

	public FrequencyEntry()
	{
		count = 0;
		hit = 0;
	}

	public void add(boolean hit)
	{
		count++;

		if (hit)
		{
			this.hit++;
		}
	}

	public String toString()
	{
		return "Hits: " + count + " Frequency: " + String.format("%.02f", 100d * hit / count) + "%";
	}
}
