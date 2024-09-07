package org.shawn.games.Serendipity.Search;

import org.shawn.games.Serendipity.Search.History.History;
import org.shawn.games.Serendipity.Search.History.PieceToHistory;

import com.github.bhlangonijr.chesslib.move.Move;

public class SearchStack
{
	public static class SearchState
	{
		public boolean inCheck;
		public boolean ttHit;
		public int moveCount;
		public int staticEval = AlphaBeta.VALUE_NONE;
		public Move killer;
		public Move move;
		public Move excludedMove;
		public History continuationHistory;
	}

	private final SearchState[] stack;

	public SearchStack(int n)
	{
		stack = new SearchState[n + 9];

		for (int i = 0; i < stack.length; i++)
		{
			stack[i] = new SearchState();
		}

		for (int i = 0; i < 6; i++)
		{
			stack[i].continuationHistory = new PieceToHistory();
		}
	}

	public SearchState get(int index)
	{
		return stack[index + 6];
	}
}
