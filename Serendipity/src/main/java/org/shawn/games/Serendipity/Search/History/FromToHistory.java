package org.shawn.games.Serendipity.Search.History;

import java.util.Arrays;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

public class FromToHistory implements History
{
	private int[] history;

	static final int MAX_BONUS = 16384;

	final int DIMENSION = Square.values().length;

	public FromToHistory()
	{
		history = new int[DIMENSION * DIMENSION];
	}

	public int get(Square from, Square to)
	{
		return history[from.ordinal() * DIMENSION + to.ordinal()];
	}

	public int get(Move move)
	{
		return get(move.getFrom(), move.getTo());
	}

	private static int clamp(int v, int max, int min)
	{
		return v >= max ? max : (v <= min ? min : v);
	}

	public void register(Square from, Square to, int value)
	{
		int clampedValue = clamp(value, MAX_BONUS, -MAX_BONUS);

		history[from.ordinal() * DIMENSION + to.ordinal()] += clampedValue
				- history[from.ordinal() * DIMENSION + to.ordinal()] * Math.abs(clampedValue) / MAX_BONUS;
	}

	@Override
	public int get(Board board, Move move)
	{
		return get(move.getFrom(), move.getTo());
	}

	@Override
	public void register(Board board, Move move, int value)
	{
		register(move.getFrom(), move.getTo(), value);
	}

	@Override
	public void fill(int x)
	{
		Arrays.fill(history, x);
	}
}
