package org.shawn.games.Serendipity;

public class TimeManager
{
	private long timeLeft;
	private long increment;
	private int movesToGo;
	private long hardLimit;
	private long softLimit;
	private long moveOverHead;

	private long startTime;
	private long hardLimitTimeStamp;
	private long softLimitTimeStamp;

	public TimeManager(long timeLeft, long increment, int movesToGo, long moveOverHead, int moves)
	{
		this.timeLeft = timeLeft;
		this.increment = increment;
		this.moveOverHead = moveOverHead;

		if (movesToGo == 0)
		{
			movesToGo = Math.max(20, 50 - moves);
		}

		this.movesToGo = movesToGo;

		this.hardLimit = this.timeLeft / this.movesToGo + this.increment / 2 - this.moveOverHead;
		this.softLimit = this.movesToGo != 1 ? this.hardLimit / 2 : this.hardLimit;

		this.startTime = System.nanoTime();
		this.hardLimitTimeStamp = startTime + 1000000L * this.hardLimit;
		this.softLimitTimeStamp = startTime + 1000000L * this.softLimit;
	}

	public boolean stop()
	{
		return System.nanoTime() > this.hardLimitTimeStamp;
	}

	public boolean stopIterativeDeepening()
	{
		return System.nanoTime() > this.softLimitTimeStamp;
	}

	public long timePassed()
	{
		return (System.nanoTime() - startTime) / 1000000;
	}
}
