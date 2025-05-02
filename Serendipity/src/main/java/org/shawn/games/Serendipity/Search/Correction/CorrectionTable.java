package org.shawn.games.Serendipity.Search.Correction;

import org.shawn.games.Serendipity.Chess.Board;

public interface CorrectionTable
{
	public static final int CORRECTION_HISTORY_MAX = 1024;

	static int clamp(int v)
	{
		return v >= CORRECTION_HISTORY_MAX ? CORRECTION_HISTORY_MAX : (Math.max(v, -CORRECTION_HISTORY_MAX));
	}

	int get(Board board);

	void register(Board board, int bonus);

	void fill(int x);
}
