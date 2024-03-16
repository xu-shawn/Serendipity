package org.shawn.games.Serendipity;

public class IntegerOption implements UCIOption
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

	@Override
	public void set(String value)
	{
		int intValue = Integer.parseInt(value);
		if (intValue < this.lowerBound)
		{
			this.value = this.lowerBound;
		}

		else if (intValue > this.upperBound)
		{
			this.value = this.upperBound;
		}

		else
		{
			this.value = intValue;
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

	@Override
	public String getString()
	{
		return Integer.toString(this.value);
	}
}