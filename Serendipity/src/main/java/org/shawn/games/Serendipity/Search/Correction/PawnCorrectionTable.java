package org.shawn.games.Serendipity.Search.Correction;

import java.util.Arrays;

import org.shawn.games.Serendipity.Chess.Board;

public class PawnCorrectionTable implements CorrectionTable
{
	private int[][] values;

	private static final int SIZE = 16384;
	private static final int MASK = SIZE - 1;

	public PawnCorrectionTable()
	{
		values = new int[2][SIZE];
	}

	@Override
	public int get(Board board)
	{
		return this.values[board.getSideToMove().ordinal()][(int) board.getIncrementalHashKey() & MASK];
	}

	@Override
	public void register(Board board, int bonus)
	{
		int clampedBonus = CorrectionTable.clamp(bonus);

		this.values[board.getSideToMove().ordinal()][(int) board.getIncrementalHashKey() & MASK] += clampedBonus
				- this.values[board.getSideToMove().ordinal()][(int) board.getIncrementalHashKey() & MASK]
						* Math.abs(clampedBonus) / CORRECTION_HISTORY_MAX;
	}

	@Override
	public void fill(int x)
	{
		Arrays.fill(values[0], 0);
		Arrays.fill(values[1], 0);
	}
}
