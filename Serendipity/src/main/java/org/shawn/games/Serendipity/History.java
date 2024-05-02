package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public interface History
{
	public int get(Board board, Move move);
	public void register(Board board, Move move, int value);
}
