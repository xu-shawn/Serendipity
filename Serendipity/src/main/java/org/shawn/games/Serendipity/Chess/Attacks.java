package org.shawn.games.Serendipity.Chess;

import java.util.ArrayList;

import org.shawn.games.Serendipity.Chess.util.XorShiftRandom;

public class Attacks
{
	private final static long[] kingAttacks;
	private final static long[] knightAttacks;
	private final static long[] bishopMask;
	private final static long[] bishopMagic;
	private final static long[] rookMask;
	private final static long[] rookMagic;

	static
	{
		knightAttacks = new long[64];
		kingAttacks = new long[64];
		bishopMask = new long[64];
		bishopMagic = new long[64];
		rookMask = new long[64];
		rookMagic = new long[64];

		for (int sqIdx = 0; sqIdx < 64; sqIdx++)
		{
			final Square sq = Square.values()[sqIdx];

			knightAttacks[sqIdx] = generateKnightAttacks(sq);
			kingAttacks[sqIdx] = generateKingAttacks(sq);
			bishopMask[sqIdx] = generateBishopMask(sq);
			rookMask[sqIdx] = generateRookMask(sq);
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

	private static long generateKnightAttacks(Square square)
	{
		long attacks = 0L;
		long squareBB = square.getBitboard();

		attacks |= shift(shift(squareBB, Direction.NORTH), Direction.NORTHEAST);
		attacks |= shift(shift(squareBB, Direction.NORTH), Direction.NORTHWEST);
		attacks |= shift(shift(squareBB, Direction.SOUTH), Direction.SOUTHEAST);
		attacks |= shift(shift(squareBB, Direction.SOUTH), Direction.SOUTHWEST);
		attacks |= shift(shift(squareBB, Direction.EAST), Direction.NORTHEAST);
		attacks |= shift(shift(squareBB, Direction.EAST), Direction.SOUTHEAST);
		attacks |= shift(shift(squareBB, Direction.WEST), Direction.NORTHWEST);
		attacks |= shift(shift(squareBB, Direction.WEST), Direction.SOUTHWEST);

		return attacks;
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

	private static long generateRayMask(Square square, Direction direction)
	{
		long mask = 0L;
		long sqBB = square.getBitboard();
		long nextBB = shift(sqBB, direction);

		while (true)
		{
			sqBB = nextBB;
			nextBB = shift(sqBB, direction);

			if (0L == nextBB)
			{
				break;
			}

			mask |= nextBB;
		}

		return mask;
	}

	private static long generateBishopMask(Square square)
	{
		long mask = 0L;

		mask |= generateRayMask(square, Direction.NORTHEAST);
		mask |= generateRayMask(square, Direction.NORTHWEST);
		mask |= generateRayMask(square, Direction.SOUTHEAST);
		mask |= generateRayMask(square, Direction.SOUTHWEST);

		return mask;
	}

	private static long generateRookMask(Square square)
	{
		long mask = 0L;

		mask |= generateRayMask(square, Direction.NORTH);
		mask |= generateRayMask(square, Direction.SOUTH);
		mask |= generateRayMask(square, Direction.EAST);
		mask |= generateRayMask(square, Direction.WEST);

		return mask;
	}

	/**
	 * Returns the bitboard representing the squares attacked by pawns placed on a
	 * set of locations for a given side.
	 *
	 * @param side   the side to move
	 * @param pawnBB the bitboard of pawns
	 * @return the bitboard of the squares attacked by the pawn
	 */
	public static long getPawnAttacks(Side side, long pawnBB)
	{
		if (side.equals(Side.WHITE))
		{
			return shift(pawnBB, Direction.NORTHEAST) | shift(pawnBB, Direction.NORTHWEST);
		}

		else
		{
			return shift(pawnBB, Direction.SOUTHEAST) | shift(pawnBB, Direction.SOUTHWEST);
		}
	}

	/**
	 * Returns the bitboard representing the possible captures by a pawn placed on
	 * the input square for a given side. The method expects a bitboard of possible
	 * targets, the occupied squares, and the square for which en passant is
	 * playable ({@link Square#NONE} if en passant can not be played).
	 *
	 * @param side      the side to move
	 * @param square    the square the pawn is placed
	 * @param occupied  a bitboard of possible targets
	 * @param enPassant the square in which en passant capture is possible,
	 *                  {@link Square#NONE} otherwise
	 * @return the bitboard of the squares where the pawn can move to capturing a
	 *         piece
	 */
	public static long getPawnCaptures(Side side, long pawnBB, long occupied, Square enPassant)
	{
		long pawnAttacks = getPawnAttacks(side, pawnBB);

		if (!enPassant.equals(Square.NONE))
		{
			long ep = enPassant.getBitboard();
			occupied |= side.equals(Side.WHITE) ? ep << 8L : ep >> 8L;
		}

		return pawnAttacks & occupied;
	}

	/**
	 * Returns the bitboard representing the possible moves, excluding captures, by
	 * a pawn placed on the input square for a given side. The method expects a
	 * bitboard of occupied squares where the pawn can not move to.
	 *
	 * @param side     the side to move
	 * @param square   the square the pawn is placed
	 * @param occupied a bitboard of occupied squares
	 * @return the bitboard of the squares where the pawn can move to
	 */
	public static long getPawnMoves(Side side, long pawnBB, long occupied)
	{
		Direction pushDirection = side.equals(Side.WHITE) ? Direction.NORTH : Direction.SOUTH;
		final long doublePushMask = side.equals(Side.WHITE) ? Bitboard.rankBB[3] : Bitboard.rankBB[4];

		final long pawnPushes = shift(pawnBB, pushDirection) & ~occupied;
		final long pawnDoublePushes = shift(pawnPushes, pushDirection) & doublePushMask & ~occupied;

		return pawnPushes | pawnDoublePushes;
	}

	/**
	 * Returns the bitboard representing the knight movement attacks, computed
	 * applying the provided mask. It could either refer to the squares attacked by
	 * a knight placed on the input square, or conversely the knights that can
	 * attack the square.
	 *
	 * @param square the square for which to calculate the knight attacks
	 * @param mask   the mask to apply to the knight attacks
	 * @return the bitboard of knight movement attacks
	 */
	public static long getKnightAttacks(Square square, long mask)
	{
		return knightAttacks[square.ordinal()] & mask;
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
		return kingAttacks[square.ordinal()] & mask;
	}
}