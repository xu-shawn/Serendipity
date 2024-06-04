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
		if (timeLeft < 0)
		{
			timeLeft = 1000;
		}

		this.timeLeft = timeLeft - Math.min(moveOverHead, timeLeft) / 2;
		this.increment = increment;
		this.moveOverHead = moveOverHead;

		if (movesToGo != 0)
		{
			this.movesToGo = movesToGo;

			this.hardLimit = this.timeLeft / this.movesToGo + this.increment * 3 / 4;
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
