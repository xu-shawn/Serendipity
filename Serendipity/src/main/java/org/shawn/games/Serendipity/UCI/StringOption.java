package org.shawn.games.Serendipity.UCI;

public class StringOption implements UCIOption
{
	String value;
	final String name;
	final String defaultValue;

	public StringOption(String value, String name)
	{
		this.value = this.defaultValue = value;
		this.name = name;

		UCI.addOption(name, this);
	}

	@Override
	public void set(String value)
	{
		this.value = value;
	}

	public String get()
	{
		return value;
	}

	public String toString()
	{
		return "option name " + name + " type string default " + defaultValue;
	}

	@Override
	public String getString()
	{
		return this.value;
	}
}