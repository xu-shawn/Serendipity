package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.move.Move;

public class ScoredMove extends Move
{
	private final int score;

	public ScoredMove(Move move, int score)
	{
		super(move.getFrom(), move.getTo(), move.getPromotion());
		this.score = score;
	}

	public int getScore()
	{
		return score;
	}
}
