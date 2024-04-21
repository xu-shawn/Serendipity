package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.move.Move;

public class SearchData
{
	int nodes;

	private Move[][] pv;
	private Move[][] counterMoves;
	private int[][] history;
	private int rootDepth;
	private int selDepth;
	
	public SearchData()
	{
		
	}

	private void updatePV(Move move, int ply)
	{
		pv[ply][0] = move;
		System.arraycopy(pv[ply + 1], 0, pv[ply], 1, AlphaBeta.MAX_PLY - 1);
	}

	private void clearPV()
	{
		this.pv = new Move[AlphaBeta.MAX_PLY][AlphaBeta.MAX_PLY];
	}
}
