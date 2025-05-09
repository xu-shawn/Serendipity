/*
 * Copyright 2017 Ben-Hur Carlos Vieira Langoni Junior
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.shawn.games.Serendipity.Chess;

import static org.shawn.games.Serendipity.Chess.DiagonalA1H8.*;
import static org.shawn.games.Serendipity.Chess.DiagonalH1A8.*;
import static org.shawn.games.Serendipity.Chess.Square.*;

import java.util.LinkedList;
import java.util.List;

/**
 * A collection of bitboards and related constant values useful to perform
 * efficient board manipulations, fast squares comparisons, and to mask some
 * operations to limited portions of the board.
 * <p>
 * A bitboard is a specialized bit array data structure used in chess
 * programming, where each bit corresponds to some binary information stored in
 * a board (e.g. the presence of a piece, the property of a square, etc.). Given
 * chessboards have 64 squares, bitboards can be represented by 64-bits numbers
 * (long unsigned integer values). Data manipulation and comparison of different
 * bitboards can be performed using bitwise operations.
 */
public class Bitboard
{
	/**
	 * The bitboard representing the light squares on a chessboard.
	 */
	public static final long lightSquares = 0x55AA55AA55AA55AAL;
	/**
	 * The bitboard representing the dark squares on a chessboard.
	 */
	public static final long darkSquares = 0xAA55AA55AA55AA55L;

	/**
	 * The bitboards representing the ranks on a chessboard. Bitboard at index 0
	 * identifies the 1st rank on a board, bitboard at index 1 the 2nd rank, etc.
	 */
	final static long[] rankBB = { 0x00000000000000FFL, 0x000000000000FF00L, 0x0000000000FF0000L, 0x00000000FF000000L,
			0x000000FF00000000L, 0x0000FF0000000000L, 0x00FF000000000000L, 0xFF00000000000000L };
	/**
	 * The bitboards representing the files on a chessboard. Bitboard at index 0
	 * identifies the 1st file on a board, bitboard at index 1 the 2nd file, etc.
	 */
	final static long[] fileBB = { 0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L, 0x0808080808080808L,
			0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L };

	/**
	 * Table of bitboards that represent portions of board included between two
	 * squares, specified by the indexes used to access the table. A portion of
	 * board is the two-dimensional space defined by the ranks and files of the
	 * squares.
	 * <p>
	 * For instance, the bitboard looked up by coordinates {@code (0, 18)} defines
	 * the portion of the board included between squares {@code A1} and {@code C3}
	 * (indexes 0 and 18 respectively), i.e. the set of squares
	 * {@code [A1, B1, C1, A2, B2, C2, A3, B3, C3]}.
	 */
	final static long[][] bbTable = new long[64][64];

	/**
	 * Table of <i>right-pointing</i> diagonals accessed by square index. For
	 * example, the diagonal looked up by index 1 is the diagonal {@code B1-H7}, the
	 * diagonal the square {@code B1} (index 1) belongs to.
	 */
	static final DiagonalA1H8[] squareToDiagonalA1H8 = { H8_A1, B1_H7, C1_H6, D1_H5, E1_H4, F1_H3, G1_H2, H1_H1, G8_A2,
			H8_A1, B1_H7, C1_H6, D1_H5, E1_H4, F1_H3, G1_H2, F8_A3, G8_A2, H8_A1, B1_H7, C1_H6, D1_H5, E1_H4, F1_H3,
			E8_A4, F8_A3, G8_A2, H8_A1, B1_H7, C1_H6, D1_H5, E1_H4, D8_A5, E8_A4, F8_A3, G8_A2, H8_A1, B1_H7, C1_H6,
			D1_H5, C8_A6, D8_A5, E8_A4, F8_A3, G8_A2, H8_A1, B1_H7, C1_H6, B8_A7, C8_A6, D8_A5, E8_A4, F8_A3, G8_A2,
			H8_A1, B1_H7, A8_A8, B8_A7, C8_A6, D8_A5, E8_A4, F8_A3, G8_A2, H8_A1 };
	/**
	 * Table of <i>left-pointing</i> diagonals accessed by square index. For
	 * example, the diagonal looked up by index 1 is the diagonal {@code B1-A2}, the
	 * diagonal the square {@code B1} (index 1) belongs to.
	 */
	static final DiagonalH1A8[] squareToDiagonalH1A8 = { A1_A1, B1_A2, C1_A3, D1_A4, E1_A5, F1_A6, G1_A7, H1_A8, B1_A2,
			C1_A3, D1_A4, E1_A5, F1_A6, G1_A7, H1_A8, B8_H2, C1_A3, D1_A4, E1_A5, F1_A6, G1_A7, H1_A8, B8_H2, C8_H3,
			D1_A4, E1_A5, F1_A6, G1_A7, H1_A8, B8_H2, C8_H3, D8_H4, E1_A5, F1_A6, G1_A7, H1_A8, B8_H2, C8_H3, D8_H4,
			E8_H5, F1_A6, G1_A7, H1_A8, B8_H2, C8_H3, D8_H4, E8_H5, F8_H6, G1_A7, H1_A8, B8_H2, C8_H3, D8_H4, E8_H5,
			F8_H6, G8_H7, H1_A8, B8_H2, C8_H3, D8_H4, E8_H5, F8_H6, G8_H7, H8_H8 };
	/**
	 * The bitboards representing the <i>right-pointing</i> diagonals on a
	 * chessboard. For example, the bitboard at index 0 identifies the
	 * <i>right-pointing</i> diagonal at position 0, as defined by
	 * {@link DiagonalA1H8}.
	 */
	final static long[] diagonalA1H8BB = { sq2Bb(A8), sq2Bb(B8) | sq2Bb(A7), sq2Bb(C8) | sq2Bb(B7) | sq2Bb(A6),
			sq2Bb(D8) | sq2Bb(C7) | sq2Bb(B6) | sq2Bb(A5), sq2Bb(E8) | sq2Bb(D7) | sq2Bb(C6) | sq2Bb(B5) | sq2Bb(A4),
			sq2Bb(F8) | sq2Bb(E7) | sq2Bb(D6) | sq2Bb(C5) | sq2Bb(B4) | sq2Bb(A3),
			sq2Bb(G8) | sq2Bb(F7) | sq2Bb(E6) | sq2Bb(D5) | sq2Bb(C4) | sq2Bb(B3) | sq2Bb(A2),
			sq2Bb(H8) | sq2Bb(G7) | sq2Bb(F6) | sq2Bb(E5) | sq2Bb(D4) | sq2Bb(C3) | sq2Bb(B2) | sq2Bb(A1),
			sq2Bb(B1) | sq2Bb(C2) | sq2Bb(D3) | sq2Bb(E4) | sq2Bb(F5) | sq2Bb(G6) | sq2Bb(H7),
			sq2Bb(C1) | sq2Bb(D2) | sq2Bb(E3) | sq2Bb(F4) | sq2Bb(G5) | sq2Bb(H6),
			sq2Bb(D1) | sq2Bb(E2) | sq2Bb(F3) | sq2Bb(G4) | sq2Bb(H5), sq2Bb(E1) | sq2Bb(F2) | sq2Bb(G3) | sq2Bb(H4),
			sq2Bb(F1) | sq2Bb(G2) | sq2Bb(H3), sq2Bb(G1) | sq2Bb(H2), sq2Bb(H1) };
	/**
	 * The bitboards representing the <i>left-pointing</i> diagonals on a
	 * chessboard. For example, the bitboard at index 0 identifies the
	 * <i>left-pointing</i> diagonal at position 0, as defined by
	 * {@link DiagonalH1A8}.
	 */
	final static long[] diagonalH1A8BB = { sq2Bb(A1), sq2Bb(B1) | sq2Bb(A2), sq2Bb(C1) | sq2Bb(B2) | sq2Bb(A3),
			sq2Bb(D1) | sq2Bb(C2) | sq2Bb(B3) | sq2Bb(A4), sq2Bb(E1) | sq2Bb(D2) | sq2Bb(C3) | sq2Bb(B4) | sq2Bb(A5),
			sq2Bb(F1) | sq2Bb(E2) | sq2Bb(D3) | sq2Bb(C4) | sq2Bb(B5) | sq2Bb(A6),
			sq2Bb(G1) | sq2Bb(F2) | sq2Bb(E3) | sq2Bb(D4) | sq2Bb(C5) | sq2Bb(B6) | sq2Bb(A7),
			sq2Bb(H1) | sq2Bb(G2) | sq2Bb(F3) | sq2Bb(E4) | sq2Bb(D5) | sq2Bb(C6) | sq2Bb(B7) | sq2Bb(A8),
			sq2Bb(B8) | sq2Bb(C7) | sq2Bb(D6) | sq2Bb(E5) | sq2Bb(F4) | sq2Bb(G3) | sq2Bb(H2),
			sq2Bb(C8) | sq2Bb(D7) | sq2Bb(E6) | sq2Bb(F5) | sq2Bb(G4) | sq2Bb(H3),
			sq2Bb(D8) | sq2Bb(E7) | sq2Bb(F6) | sq2Bb(G5) | sq2Bb(H4), sq2Bb(E8) | sq2Bb(F7) | sq2Bb(G6) | sq2Bb(H5),
			sq2Bb(F8) | sq2Bb(G7) | sq2Bb(H6), sq2Bb(G8) | sq2Bb(H7), sq2Bb(H8) };

	/**
	 * The bitboards representing the squares a piece that moves horizontally can
	 * attack from any given square on the board. For example, bitboard at index 0
	 * identifies the squares on the same rank attacked from square {@code A1}
	 * (index 0), that is, all squares of the 1st rank except the square {@code A1}
	 * itself (i.e. {@code B1}, {@code C1}, etc.).
	 * <p>
	 * Contextually, the bitboards can also represent the squares from which a piece
	 * on the same rank can attack the square.
	 */
	final static long[] rankAttacks = { sq2RA(A1), sq2RA(B1), sq2RA(C1), sq2RA(D1), sq2RA(E1), sq2RA(F1), sq2RA(G1),
			sq2RA(H1), sq2RA(A2), sq2RA(B2), sq2RA(C2), sq2RA(D2), sq2RA(E2), sq2RA(F2), sq2RA(G2), sq2RA(H2),
			sq2RA(A3), sq2RA(B3), sq2RA(C3), sq2RA(D3), sq2RA(E3), sq2RA(F3), sq2RA(G3), sq2RA(H3), sq2RA(A4),
			sq2RA(B4), sq2RA(C4), sq2RA(D4), sq2RA(E4), sq2RA(F4), sq2RA(G4), sq2RA(H4), sq2RA(A5), sq2RA(B5),
			sq2RA(C5), sq2RA(D5), sq2RA(E5), sq2RA(F5), sq2RA(G5), sq2RA(H5), sq2RA(A6), sq2RA(B6), sq2RA(C6),
			sq2RA(D6), sq2RA(E6), sq2RA(F6), sq2RA(G6), sq2RA(H6), sq2RA(A7), sq2RA(B7), sq2RA(C7), sq2RA(D7),
			sq2RA(E7), sq2RA(F7), sq2RA(G7), sq2RA(H7), sq2RA(A8), sq2RA(B8), sq2RA(C8), sq2RA(D8), sq2RA(E8),
			sq2RA(F8), sq2RA(G8), sq2RA(H8) };
	/**
	 * The bitboards representing the squares a piece that moves vertically can
	 * attack from any given square on the board. For example, bitboard at index 0
	 * identifies the squares on the same file attacked from square {@code A1}
	 * (index 0), that is, all squares of the 1st file except the square {@code A1}
	 * itself (i.e. {@code A2}, {@code A3}, etc.).
	 * <p>
	 * Contextually, the bitboards can also represent the squares from which a piece
	 * on the same file can attack the square.
	 */
	final static long[] fileAttacks = { sq2FA(A1), sq2FA(B1), sq2FA(C1), sq2FA(D1), sq2FA(E1), sq2FA(F1), sq2FA(G1),
			sq2FA(H1), sq2FA(A2), sq2FA(B2), sq2FA(C2), sq2FA(D2), sq2FA(E2), sq2FA(F2), sq2FA(G2), sq2FA(H2),
			sq2FA(A3), sq2FA(B3), sq2FA(C3), sq2FA(D3), sq2FA(E3), sq2FA(F3), sq2FA(G3), sq2FA(H3), sq2FA(A4),
			sq2FA(B4), sq2FA(C4), sq2FA(D4), sq2FA(E4), sq2FA(F4), sq2FA(G4), sq2FA(H4), sq2FA(A5), sq2FA(B5),
			sq2FA(C5), sq2FA(D5), sq2FA(E5), sq2FA(F5), sq2FA(G5), sq2FA(H5), sq2FA(A6), sq2FA(B6), sq2FA(C6),
			sq2FA(D6), sq2FA(E6), sq2FA(F6), sq2FA(G6), sq2FA(H6), sq2FA(A7), sq2FA(B7), sq2FA(C7), sq2FA(D7),
			sq2FA(E7), sq2FA(F7), sq2FA(G7), sq2FA(H7), sq2FA(A8), sq2FA(B8), sq2FA(C8), sq2FA(D8), sq2FA(E8),
			sq2FA(F8), sq2FA(G8), sq2FA(H8) };
	/**
	 * The bitboards representing the squares a piece that moves diagonally can
	 * attack on the same <i>right-pointing</i> diagonal from any given square on
	 * the board. For example, bitboard at index 1 identifies the squares on the
	 * same diagonal attacked from square {@code B1} (index 1), that is, all squares
	 * of the B1-H7 diagonal (as defined by {@link DiagonalA1H8}) except the square
	 * {@code B1} itself (i.e. {@code B2}, {@code C3}, etc.).
	 * <p>
	 * Contextually, the bitboards can also represent the squares from which a piece
	 * on the same <i>right-pointing</i> diagonal can attack the square.
	 */
	final static long[] diagA1H8Attacks = { sq2A1(A1), sq2A1(B1), sq2A1(C1), sq2A1(D1), sq2A1(E1), sq2A1(F1), sq2A1(G1),
			sq2A1(H1), sq2A1(A2), sq2A1(B2), sq2A1(C2), sq2A1(D2), sq2A1(E2), sq2A1(F2), sq2A1(G2), sq2A1(H2),
			sq2A1(A3), sq2A1(B3), sq2A1(C3), sq2A1(D3), sq2A1(E3), sq2A1(F3), sq2A1(G3), sq2A1(H3), sq2A1(A4),
			sq2A1(B4), sq2A1(C4), sq2A1(D4), sq2A1(E4), sq2A1(F4), sq2A1(G4), sq2A1(H4), sq2A1(A5), sq2A1(B5),
			sq2A1(C5), sq2A1(D5), sq2A1(E5), sq2A1(F5), sq2A1(G5), sq2A1(H5), sq2A1(A6), sq2A1(B6), sq2A1(C6),
			sq2A1(D6), sq2A1(E6), sq2A1(F6), sq2A1(G6), sq2A1(H6), sq2A1(A7), sq2A1(B7), sq2A1(C7), sq2A1(D7),
			sq2A1(E7), sq2A1(F7), sq2A1(G7), sq2A1(H7), sq2A1(A8), sq2A1(B8), sq2A1(C8), sq2A1(D8), sq2A1(E8),
			sq2A1(F8), sq2A1(G8), sq2A1(H8) };
	/**
	 * The bitboards representing the squares a piece that moves diagonally can
	 * attack on the same <i>left-pointing</i> diagonal from any given square on the
	 * board. For example, bitboard at index 1 identifies the squares on the same
	 * diagonal attacked from square {@code B1} (index 1), that is, all squares of
	 * the B1-A2 diagonal (as defined by {@link DiagonalH1A8}) except the square
	 * {@code B1} itself (i.e. only square {@code A2}).
	 * <p>
	 * Contextually, the bitboards can also represent the squares from which a piece
	 * on the same <i>left-pointing</i> diagonal can attack the square.
	 */
	final static long[] diagH1A8Attacks = { sq2H1(A1), sq2H1(B1), sq2H1(C1), sq2H1(D1), sq2H1(E1), sq2H1(F1), sq2H1(G1),
			sq2H1(H1), sq2H1(A2), sq2H1(B2), sq2H1(C2), sq2H1(D2), sq2H1(E2), sq2H1(F2), sq2H1(G2), sq2H1(H2),
			sq2H1(A3), sq2H1(B3), sq2H1(C3), sq2H1(D3), sq2H1(E3), sq2H1(F3), sq2H1(G3), sq2H1(H3), sq2H1(A4),
			sq2H1(B4), sq2H1(C4), sq2H1(D4), sq2H1(E4), sq2H1(F4), sq2H1(G4), sq2H1(H4), sq2H1(A5), sq2H1(B5),
			sq2H1(C5), sq2H1(D5), sq2H1(E5), sq2H1(F5), sq2H1(G5), sq2H1(H5), sq2H1(A6), sq2H1(B6), sq2H1(C6),
			sq2H1(D6), sq2H1(E6), sq2H1(F6), sq2H1(G6), sq2H1(H6), sq2H1(A7), sq2H1(B7), sq2H1(C7), sq2H1(D7),
			sq2H1(E7), sq2H1(F7), sq2H1(G7), sq2H1(H7), sq2H1(A8), sq2H1(B8), sq2H1(C8), sq2H1(D8), sq2H1(E8),
			sq2H1(F8), sq2H1(G8), sq2H1(H8) };

	static
	{
		for (int x = 0; x < 64; x++)
		{
			for (int y = 0; y < 64; y++)
			{
				bbTable[x][y] = ((1L << y) | ((1L << y) - (1L << x)));
			}
		}
	}

	/**
	 * Returns the bitboard representing the single square provided in input.
	 *
	 * @param sq the square for which the bitboard must be returned
	 * @return the bitboard representation of the square
	 */
	static long sq2Bb(Square sq)
	{
		return sq.getBitboard();
	}

	/**
	 * Returns the bitboard representing the squares on the same rank attacked from
	 * the square provided in input. For example, the bitboard of square {@code A1}
	 * includes all squares of the 1st rank except the square {@code A1} itself
	 * (i.e. {@code B1}, {@code C1}, etc.).
	 *
	 * @param x the square for which the bitboard must be returned
	 * @return the bitboard representation of the attacked squares on the same rank
	 */
	static long sq2RA(Square x)
	{
		return (rankBB[x.getRank().ordinal()] ^ sq2Bb(x));
	}

	/**
	 * Returns the bitboard representing the squares on the same file attacked from
	 * the square provided in input. For example, the bitboard of square {@code A1}
	 * includes all squares of the 1st file except the square {@code A1} itself
	 * (i.e. {@code A2}, {@code A3}, etc.).
	 *
	 * @param x the square for which the bitboard must be returned
	 * @return the bitboard representation of the attacked squares on the same file
	 */
	static long sq2FA(Square x)
	{
		return (fileBB[x.getFile().ordinal()] ^ x.getBitboard());
	}

	/**
	 * Returns the bitboard representing the squares on the same
	 * <i>right-pointing</i> diagonal attacked from the square provided in input.
	 * For example, the bitboard of square {@code B1} includes all squares of the
	 * B1-H7 diagonal (as defined by {@link DiagonalA1H8}) except the square
	 * {@code B1} itself (i.e. {@code B2}, {@code C3}, etc.).
	 *
	 * @param x the square for which the bitboard must be returned
	 * @return the bitboard representation of the attacked squares on the same
	 *         <i>right-pointing</i> diagonal
	 */
	static long sq2A1(Square x)
	{
		return (diagonalA1H8BB[squareToDiagonalA1H8[x.ordinal()].ordinal()] ^ sq2Bb(x));
	}

	/**
	 * Returns the bitboard representing the squares on the same
	 * <i>left-pointing</i> diagonal attacked from the square provided in input. For
	 * example, the bitboard of square {@code B1} includes all squares of the B1-A2
	 * diagonal (as defined by {@link DiagonalH1A8}) except the square {@code B1}
	 * itself (i.e. only the square {@code A2}).
	 *
	 * @param x the square for which the bitboard must be returned
	 * @return the bitboard representation of the attacked squares on the same
	 *         <i>left-pointing</i> diagonal
	 */
	static long sq2H1(Square x)
	{
		return (diagonalH1A8BB[squareToDiagonalH1A8[x.ordinal()].ordinal()] ^ sq2Bb(x));
	}

	/**
	 * Returns the index of the first (<i>rightmost</i>) bit set to 1 in the
	 * bitboard provided in input. The bit is the Least Significant 1-bit (LS1B).
	 *
	 * @param bb the bitboard for which the LS1B is to be returned
	 * @return the index of the first bit set to 1
	 */
	public static int bitScanForward(long bb)
	{
		return Long.numberOfTrailingZeros(bb);
	}

	/**
	 * Returns the index of the last (<i>leftmost</i>) bit set to 1 in the bitboard
	 * provided in input. The bit is the Most Significant 1-bit (MS1B).
	 *
	 * @param bb the bitboard for which the MS1B is to be returned
	 * @return the index of the last bit set to 1
	 */
	public static int bitScanReverse(long bb)
	{
		return 63 - Long.numberOfLeadingZeros(bb);
	}

	/**
	 * Returns the <i>sub-bitboard</i> included between the two squares provided in
	 * input, that is, the portion of the bitboard included between two squares. The
	 * <i>sub-bitboard</i> is a two-dimensional space defined by the ranks and files
	 * of the two squares. For example, if squares {@code A1} and {@code C3} are
	 * provided in input, the method returns only the following squares of the
	 * original bitboard: {@code [A1, B1, C1, A2, B2, C2, A3, B3, C3]}. All other
	 * bits are set to 0.
	 *
	 * @param bb  the bitboard the portion between the two squares must be returned
	 * @param sq1 the first square that defines the two-dimensional space
	 * @param sq2 the second square that defines the two-dimensional space
	 * @return the portion of the original bitboard included between the two squares
	 */
	public static long bitsBetween(long bb, int sq1, int sq2)
	{
		return bbTable[sq1][sq2] & bb;
	}

	/**
	 * Unsets the first bit set to 1. In other words, it sets to 0 the Least
	 * Significant 1-bit (LS1B).
	 *
	 * @param bb the bitboard to compute
	 * @return the resulting bitboard, from which the first bit set to 1 has been
	 *         unset
	 */
	public static long extractLsb(Long bb)
	{
		return bb & (bb - 1);
	}

	/**
	 * Check whether the given bitboard has only one bit set to 1.
	 *
	 * @param bb the bitboard to check
	 * @return {@code true} if the bitboard has only one bit set to 1
	 */
	public static boolean hasOnly1Bit(Long bb)
	{
		return bb != 0L && extractLsb(bb) == 0L;
	}

	/**
	 * Returns the bitboard representing the square provided in input.
	 *
	 * @param sq the square for which the bitboard must be returned
	 * @return the bitboard representing the single square
	 */
	public static long getBbtable(Square sq)
	{
		return 1L << sq.ordinal();
	}

	/**
	 * Returns the bitboard representing the bishop movement attacks, computed
	 * applying the provided mask. It could either refer to the squares attacked by
	 * a bishop placed on the input square, or conversely the bishops that can
	 * attack the square.
	 *
	 * @param square the square for which to calculate the bishop attacks
	 * @param mask   the mask to apply to the bishop attacks
	 * @return the bitboard of bishop movement attacks
	 */
	public static long getBishopAttacks(long mask, Square square)
	{
		return getSliderAttacks(diagA1H8Attacks[square.ordinal()], mask, square.ordinal())
				| getSliderAttacks(diagH1A8Attacks[square.ordinal()], mask, square.ordinal());
	}

	/**
	 * Returns the bitboard representing the rook movement attacks, computed
	 * applying the provided mask. It could either refer to the squares attacked by
	 * a rook placed on the input square, or conversely the rooks that can attack
	 * the square.
	 *
	 * @param square the square for which to calculate the rook attacks
	 * @param mask   the mask to apply to the rook attacks
	 * @return the bitboard of rook movement attacks
	 */
	public static long getRookAttacks(long mask, Square square)
	{
		return getSliderAttacks(fileAttacks[square.ordinal()], mask, square.ordinal())
				| getSliderAttacks(rankAttacks[square.ordinal()], mask, square.ordinal());
	}

	private static long getSliderAttacks(long attacks, long mask, int index)
	{
		long occ = mask & attacks;
		if (occ == 0L)
		{
			return attacks;
		}
		long m = (1L << index) - 1L;
		long lowerMask = occ & m;
		long upperMask = occ & ~m;
		int minor = lowerMask == 0L ? 0 : bitScanReverse(lowerMask);
		int major = upperMask == 0L ? 63 : bitScanForward(upperMask);
		return bitsBetween(attacks, minor, major);
	}

	/**
	 * Returns the bitboard representing the queen movement attacks, computed
	 * applying the provided mask. It could either refer to the squares attacked by
	 * a queen placed on the input square, or conversely the queens that can attack
	 * the square.
	 *
	 * @param square the square for which to calculate the queen attacks
	 * @param mask   the mask to apply to the queen attacks
	 * @return the bitboard of queen movement attacks
	 */
	public static long getQueenAttacks(long mask, Square square)
	{
		return getRookAttacks(mask, square) | getBishopAttacks(mask, square);
	}

	/**
	 * Returns the list of squares that are set in the bitboard provided in input,
	 * that is, the squares corresponding to the indexes set to 1 in the bitboard.
	 *
	 * @param bb the bitboard from which the list of squares must be returned
	 * @return the list of squares corresponding to the bits set to 1 in the
	 *         bitboard
	 */
	public static List<Square> bbToSquareList(long bb)
	{
		List<Square> squares = new LinkedList<Square>();
		while (bb != 0L)
		{
			int sq = Bitboard.bitScanForward(bb);
			bb = Bitboard.extractLsb(bb);
			squares.add(Square.squareAt(sq));
		}
		return squares;
	}

	/**
	 * Returns the array of squares that are set in the bitboard provided in input,
	 * that is, the squares corresponding to the indexes set to 1 in the bitboard.
	 *
	 * @param bb the bitboard from which the array of squares must be returned
	 * @return the array of squares corresponding to the bits set to 1 in the
	 *         bitboard
	 */
	public static Square[] bbToSquareArray(long bb)
	{
		Square[] squares = new Square[Long.bitCount(bb)];
		int index = 0;
		while (bb != 0L)
		{
			int sq = bitScanForward(bb);
			bb = extractLsb(bb);
			squares[index++] = Square.squareAt(sq);
		}
		return squares;
	}

	/**
	 * Returns the bitboards representing the ranks on a chessboard.
	 *
	 * @return the bitboards representing the ranks
	 */
	public static long[] getRankbb()
	{
		return rankBB;
	}

	/**
	 * Returns the bitboards representing the files on a chessboard.
	 *
	 * @return the bitboards representing the files
	 */
	public static long[] getFilebb()
	{
		return fileBB;
	}

	/**
	 * Returns the bitboard representing the entire rank of the square given in
	 * input.
	 *
	 * @param sq the square for which the bitboard rank must be returned
	 * @return the bitboards representing the rank of the square
	 */
	public static long getRankbb(Square sq)
	{
		return rankBB[sq.getRank().ordinal()];
	}

	/**
	 * Returns the bitboard representing the entire file of the square given in
	 * input.
	 *
	 * @param sq the square for which the bitboard file must be returned
	 * @return the bitboards representing the file of the square
	 */
	public static long getFilebb(Square sq)
	{
		return fileBB[sq.getFile().ordinal()];
	}

	/**
	 * Returns the bitboard representing the rank given in input.
	 *
	 * @param rank the rank for which the corresponding bitboard must be returned
	 * @return the bitboards representing the rank
	 */
	public static long getRankbb(Rank rank)
	{
		return rankBB[rank.ordinal()];
	}

	/**
	 * Returns the bitboard representing the file given in input.
	 *
	 * @param file the file for which the corresponding bitboard must be returned
	 * @return the bitboards representing the file
	 */
	public static long getFilebb(File file)
	{
		return fileBB[file.ordinal()];
	}

	/**
	 * Returns the string representation of a bitboard in a readable format.
	 *
	 * @param bb the bitboard to print
	 * @return the string representation of the bitboard
	 */
	public static String bitboardToString(long bb)
	{
		StringBuilder b = new StringBuilder();
		for (int x = 0; x < 64; x++)
		{
			if (((1L << x) & bb) != 0L)
			{
				b.append("1");
			}
			else
			{
				b.append("0");
			}
			if ((x + 1) % 8 == 0)
			{
				b.append("\n");
			}
		}
		return b.toString();
	}
}
