package org.shawn.games.Serendipity.Search;

import org.shawn.games.Serendipity.UCI.IntegerOption;

public class TimeManager
{
	private long timeLeft;
	private long increment;
	private long hardLimit;
	private long softLimit;

	private long startTime;
	private long hardLimitTimeStamp;
	private long softLimitTimeStamp;

	private static IntegerOption moveOverHead = new IntegerOption(100, 0, 30000, "Move_Overhead");

	public void set(Limits limits)
	{
		set(limits.getTime(), limits.getIncrement(), limits.getMovesToGo());
	}

	public void set(long timeLeft, long increment, int movesToGo)
	{
		if (timeLeft < 0)
		{
			timeLeft = 1000;
		}

		this.timeLeft = timeLeft - Math.min(moveOverHead.get(), timeLeft) / 2;
		this.increment = increment;

		if (movesToGo != 0 && movesToGo != -1)
		{
			this.hardLimit = this.timeLeft / movesToGo + this.increment * 3 / 4;
			this.softLimit = this.hardLimit / 2;

			this.startTime = System.nanoTime();
			this.hardLimitTimeStamp = startTime + 1000000L * this.hardLimit;
			this.softLimitTimeStamp = startTime + 1000000L * this.softLimit;

			return;
		}

		else if (movesToGo == -1)
		{
			this.hardLimit = this.softLimit = this.timeLeft;

			this.startTime = System.nanoTime();
			this.hardLimitTimeStamp = this.softLimitTimeStamp = startTime + 1000000L * this.timeLeft;

			return;
		}

		int baseTime = (int) (this.timeLeft * 0.054 + this.increment * 0.85);
		int maxTime = (int) (this.timeLeft * 0.76);

		this.hardLimit = Math.min(maxTime, (int) (baseTime * 3.04));
		this.softLimit = Math.min(maxTime, (int) (baseTime * 0.76));

		this.startTime = System.nanoTime();
		this.hardLimitTimeStamp = startTime + 1000000L * this.hardLimit;
		this.softLimitTimeStamp = startTime + 1000000L * this.softLimit;
	}

	public boolean shouldStop()
	{
		return System.nanoTime() > this.hardLimitTimeStamp;
	}

	public boolean shouldStopIterativeDeepening()
	{
		return System.nanoTime() > this.softLimitTimeStamp;
	}

	public long timePassed()
	{
		return (System.nanoTime() - this.startTime) / 1000000;
	}
}
