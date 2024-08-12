package org.shawn.games.Serendipity.UCI;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;

public class WDLModel
{
	private final static double[] as = { -117.29796837, 388.52982654, -512.96172771, 477.99147478 };
	private final static double[] bs = { -47.75093113, 118.81734426, -33.05800930, 94.01690687 };

	@SuppressWarnings("unused")
	private final static int NormalizeToPawnValue = 236;

	private final static int PAWN_VALUE = 1;
	private final static int KNIGHT_VALUE = 3;
	private final static int BISHOP_VALUE = 3;
	private final static int ROOK_VALUE = 5;
	private final static int QUEEN_VALUE = 9;

	private static int countMaterial(Board board)
	{
		return Long.bitCount(board.getBitboard(Piece.WHITE_PAWN)) * PAWN_VALUE
				+ Long.bitCount(board.getBitboard(Piece.WHITE_KNIGHT)) * KNIGHT_VALUE
				+ Long.bitCount(board.getBitboard(Piece.WHITE_BISHOP)) * BISHOP_VALUE
				+ Long.bitCount(board.getBitboard(Piece.WHITE_ROOK)) * ROOK_VALUE
				+ Long.bitCount(board.getBitboard(Piece.WHITE_QUEEN)) * QUEEN_VALUE
				+ Long.bitCount(board.getBitboard(Piece.BLACK_PAWN)) * PAWN_VALUE
				+ Long.bitCount(board.getBitboard(Piece.BLACK_KNIGHT)) * KNIGHT_VALUE
				+ Long.bitCount(board.getBitboard(Piece.BLACK_BISHOP)) * BISHOP_VALUE
				+ Long.bitCount(board.getBitboard(Piece.BLACK_ROOK)) * ROOK_VALUE
				+ Long.bitCount(board.getBitboard(Piece.BLACK_QUEEN)) * QUEEN_VALUE;
	}

	private static double[] calculateParameters(Board board)
	{
		int material = countMaterial(board);

		material = Math.max(material, 17);
		material = Math.min(material, 78);

		double m = material / 58.0;

		double a = (((as[0] * m + as[1]) * m + as[2]) * m) + as[3];
		double b = (((bs[0] * m + bs[1]) * m + bs[2]) * m) + bs[3];

		return new double[] { a, b };
	}

	private static int calculateWinRate(int v, Board board)
	{
		double[] parameters = calculateParameters(board);

		return (int) (0.5 + 1000 / (1 + Math.exp((parameters[0] - (double) v) / parameters[1])));
	}

	public static int[] calculateWDL(int v, Board board)
	{
		int winRate = calculateWinRate(v, board);
		int lossRate = calculateWinRate(-v, board);
		int drawRate = 1000 - winRate - lossRate;

		return new int[] { winRate, drawRate, lossRate };
	}

	public static int normalizeEval(int v, Board board)
	{
		double a = calculateParameters(board)[0];

		return (int) Math.round((100 * v) / a);
	}
}
