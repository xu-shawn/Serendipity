package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.move.Move;

public class ScoredMove
{
	private final Move move;
	private final int score;
	
	public ScoredMove(Move move, int score)
	{
		this.move = move;
		this.score = score;
	}
	
	public Move getMove()
	{
		return move;
	}
	
	public int getScore()
	{
		return score;
	}
}
