package org.shawn.games.Serendipity.Datagen;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public interface FormatWriter
{
	public void writeBoard(Board board);
	public void writeMove(Board board, Move move);
	public void writeWDL(byte wdl);
}
