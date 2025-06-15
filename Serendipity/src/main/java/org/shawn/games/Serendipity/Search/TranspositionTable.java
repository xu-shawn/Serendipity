/*
  This file is part of Serendipity, an UCI chess engine written in Java.

  Copyright (C) 2024-2025  Shawn Xu <shawn@shawnxu.org>

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.shawn.games.Serendipity.Search;

import java.util.Arrays;

import org.shawn.games.Serendipity.Chess.move.Move;

public class TranspositionTable
{
	public static final int NODETYPE_NONE = 0b00;
	public static final int NODETYPE_LOWERBOUND = 0b01;
	public static final int NODETYPE_UPPERBOUND = 0b10;
	public static final int NODETYPE_EXACT = 0b11;

	private static final int DEPTH_OFFSET = -4;
	public static final int DEPTH_NONE = -3;
	public static final int DEPTH_QS = -1;

	public static final byte AGE_INCREMENT = 1 << 3;
	public static final int GENERATION_CYCLE = 1 << 8;
	public static final int GENERATION_MASK = (GENERATION_CYCLE - 1) & ~(AGE_INCREMENT - 1);

	public class Entry
	{
		private final int signature;
		private final int depth;
		private final int type;
		private final int evaluation;
		private final int staticEval;
		private final int relativeAge;
		private final byte age;
		private final Move move;
		private final boolean hit;

		public Entry(short fragment1, long fragment2)
		{
			this(fragment1 & 0b11, (fragment1 >>> 8) + DEPTH_OFFSET, (int) (fragment2 & 0xFFFF),
					(int) (fragment2 & 0xFFFF0000) >> 16, (short) ((fragment2 & 0xFFFF00000000L) >>> 32),
					(byte) (fragment1 & GENERATION_MASK), (int) (fragment2 >> 48),
					(fragment1 != 0) && (fragment2 != 0));
		}

		public Entry(int nodeType, int depth, int signature, int move, int staticEval, byte ttAge, int evaluation,
				boolean hit)
		{
			this.signature = signature;
			this.depth = depth;
			this.type = nodeType;
			this.move = move == 0 ? null : Move.fromBytes(move);
			this.evaluation = evaluation;
			this.staticEval = staticEval;
			this.age = ttAge;
			this.relativeAge = (age - ttAge + GENERATION_CYCLE) & GENERATION_MASK;
			this.hit = hit;
		}

		public long getSignature()
		{
			return signature;
		}

		public boolean verifySignature(long signature)
		{
			return (int) (signature >>> 48) == this.signature;
		}

		public int getDepth()
		{
			return this.depth;
		}

		public int getNodeType()
		{
			return this.type;
		}

		public int getEvaluation()
		{
			return evaluation;
		}

		public int getStaticEval()
		{
			return staticEval;
		}

		public int getRelativeAge()
		{
			return relativeAge;
		}

		public Move getMove()
		{
			return move;
		}

		public boolean hit()
		{
			return hit;
		}
	}

	private int size;
	private int mask;

	// depth (0-255): 8 bits
	// age: 5 bits
	// unused: 1 bit
	// nodeType: 2 bits

	private short[] data1;

	// evaluation: 16 bits
	// staticEval: 16 bits
	// move: 16 bits
	// signature: 16 bits

	private long[] data2;

	private byte age = 0;

	private static final int ENTRY_SIZE = 10;

	public TranspositionTable(int size)
	{
		size *= 1048576 / ENTRY_SIZE;

		this.size = Integer.highestOneBit(size);
		this.mask = this.size - 1;
		this.data1 = new short[this.size];
		this.data2 = new long[this.size];
	}

	public Entry probe(long hash)
	{
		final short fragment1 = data1[(int) (hash & mask)];
		final long fragment2 = data2[(int) (hash & mask)];

		return new Entry(fragment1, fragment2);
	}

	public void write(Entry entry, long hash, int nodeType, int depth, int evaluation, Move move, int staticEval)
	{
		if (entry == null || !entry.hit() || nodeType == NODETYPE_EXACT || !entry.verifySignature(hash)
				|| depth > entry.getDepth() - 4)
		{
			final int writtenDepth = depth - DEPTH_OFFSET;
			final short fragment1 = (short) ((writtenDepth << 8) | nodeType | age);
			final long fragment2 = (hash >>> 48) | ((move == null) ? 0 : move.asBytes() << 16)
					| ((staticEval & 0xFFFFL) << 32) | ((long) evaluation << 48);

			data1[(int) hash & mask] = fragment1;
			data2[(int) hash & mask] = fragment2;
		}
	}

	public void clear()
	{
		Arrays.fill(data1, (short) 0);
		Arrays.fill(data2, 0);
	}

	public void resize(int size)
	{
		size *= 1048576 / ENTRY_SIZE;
		this.size = Integer.highestOneBit(size);
		this.mask = this.size - 1;
		this.data1 = new short[this.size];
		this.data2 = new long[this.size];
	}

	public void incrementAge()
	{
		this.age += AGE_INCREMENT;
	}

	public int hashfull()
	{
		int hashfull = 0;

		for (int i = 0; i < 1000; i++)
		{
			if (this.data2[i] != 0)
			{
				hashfull++;
			}
		}

		return hashfull;
	}

	public int hashfull_accurate()
	{
		int hashfull = 0;
		int minimum_hash = 1048576 / ENTRY_SIZE;

		for (int i = 0; i < minimum_hash; i++)
		{
			if (this.data2[i] != 0)
			{
				hashfull++;
			}
		}

		return hashfull * 1000 / minimum_hash;
	}

	public int getSize()
	{
		return this.size * ENTRY_SIZE / 1048576;
	}
}