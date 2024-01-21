package org.shawn.games.Serendipity;

public class IntegerOption
{
	int value;
	final String name;
	final int defaultValue;
	final int lowerBound;
	final int upperBound;

	public IntegerOption(int value, int lowerBound, int upperBound, String name)
	{
		if (lowerBound > upperBound)
		{
			throw new IllegalArgumentException(
					"lowerBound must be less than or equal to upperBound");
		}

		this.value = this.defaultValue = value;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.name = name;

		UCI.addOption(name, this);
	}

	public void set(int value)
	{
		if (value < this.lowerBound)
		{
			this.value = this.lowerBound;
		}

		else if (value > this.upperBound)
		{
			this.value = this.upperBound;
		}

		else
		{
			this.value = value;
		}
	}

	public int get()
	{
		return value;
	}

	public String toString()
	{
		return "option name " + name + " type spin default " + Integer.toString(defaultValue)
				+ " min " + Integer.toString(lowerBound) + " max " + Integer.toString(upperBound);
	}
}