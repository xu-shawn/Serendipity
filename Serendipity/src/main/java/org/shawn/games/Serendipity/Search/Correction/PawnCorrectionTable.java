package org.shawn.games.Serendipity.Search.Correction;

import java.util.Arrays;

import org.shawn.games.Serendipity.Chess.Board;

public class PawnCorrectionTable implements CorrectionTable
{
	private int[] values;

	private static final int SIZE = 16384;
	private static final int MASK = SIZE - 1;

	public PawnCorrectionTable()
	{
		values = new int[SIZE];
	}

	@Override
	public int get(Board board)
	{
		return this.values[(int) board.getIncrementalHashKey() & MASK];
	}

	@Override
	public void register(Board board, int bonus)
	{
		int clampedBonus = CorrectionTable.clamp(bonus);

		this.values[(int) board.getIncrementalHashKey() & MASK] += clampedBonus
				- this.values[(int) board.getIncrementalHashKey() & MASK] * Math.abs(clampedBonus)
						/ CORRECTION_HISTORY_MAX;
	}

	@Override
	public void fill(int x)
	{
		Arrays.fill(values, x);
	}
}
