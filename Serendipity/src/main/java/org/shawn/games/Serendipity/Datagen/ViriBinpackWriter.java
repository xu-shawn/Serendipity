package org.shawn.games.Serendipity.Datagen;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

public class ViriBinpackWriter implements FormatWriter
{
	private DataOutputStream output;
	public static final short[] PROMOTION_FLAG = new short[] { 0x0000, (short) 0xC000, (short) 0x8000, 0x4000 };

	public ViriBinpackWriter(String filePath) throws FileNotFoundException
	{
		this.output = new DataOutputStream(new FileOutputStream(filePath));
	}

	@Override
	public void writeBoard(Board board)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void writeMove(Board board, Move move)
	{
		short compressedMove = 0;

		compressedMove |= move.getFrom().ordinal();
		compressedMove |= move.getTo().ordinal() << 6;
		compressedMove |= PROMOTION_FLAG[move.getPromotion().getPieceType().ordinal() - 1];

		try
		{
			output.writeByte(Short.reverseBytes(compressedMove));
		}
		catch (IOException e)
		{
			System.err.print("WARNING: UNABLE TO WRITE TO FILE");
		}
	}

	@Override
	public void writeWDL(byte wdl)
	{
		try
		{
			output.writeByte(wdl);
		}
		catch (IOException e)
		{
			System.err.print("WARNING: UNABLE TO WRITE TO FILE");
		}
	}

}
