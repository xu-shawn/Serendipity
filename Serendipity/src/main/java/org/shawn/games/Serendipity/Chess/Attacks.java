package org.shawn.games.Serendipity.Chess;

public class Attacks
{
	private final static long[] adjacentSquares;

	static
	{
		adjacentSquares = new long[64];

		for (int i = 0; i < 64; i++)
		{
			adjacentSquares[i] = generateKingAttacks(Square.values()[i]);
		}
	}

	public enum Direction
	{
		NORTH, SOUTH, EAST, WEST, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST
	}

	public static Square shift(Square square, Direction direction)
	{
		return Square.fromBitboard(shift(square.getBitboard(), direction));
	}

	public static long shift(long bitboard, Direction direction)
	{
		switch (direction)
		{
			case NORTH:
				return bitboard << 8;
			case SOUTH:
				return bitboard >>> 8;
			case EAST:
				return (bitboard & ~Bitboard.fileBB[7]) << 1;
			case WEST:
				return (bitboard & ~Bitboard.fileBB[0]) >>> 1;
			case NORTHEAST:
				return (bitboard & ~Bitboard.fileBB[7]) << 9;
			case NORTHWEST:
				return (bitboard & ~Bitboard.fileBB[0]) << 7;
			case SOUTHEAST:
				return (bitboard & ~Bitboard.fileBB[7]) >>> 7;
			case SOUTHWEST:
				return (bitboard & ~Bitboard.fileBB[0]) >>> 9;
		}

		return bitboard;
	}

	private static long generateKingAttacks(Square square)
	{
		long attacks = 0L;
		long squareBB = square.getBitboard();

		attacks |= shift(squareBB, Direction.NORTH);
		attacks |= shift(squareBB, Direction.SOUTH);
		attacks |= shift(squareBB, Direction.EAST);
		attacks |= shift(squareBB, Direction.WEST);
		attacks |= shift(squareBB, Direction.NORTHEAST);
		attacks |= shift(squareBB, Direction.NORTHWEST);
		attacks |= shift(squareBB, Direction.SOUTHEAST);
		attacks |= shift(squareBB, Direction.SOUTHWEST);

		return attacks;
	}

	/**
	 * Returns the bitboard representing the king movement attacks, computed
	 * applying the provided mask. It could either refer to the squares attacked by
	 * a king placed on the input square, or conversely the kings that can attack
	 * the square.
	 *
	 * @param square the square for which to calculate the king attacks
	 * @param mask   the mask to apply to the king attacks
	 * @return the bitboard of king movement attacks
	 */
	public static long getKingAttacks(Square square, long mask)
	{
		return adjacentSquares[square.ordinal()] & mask;
	}
}