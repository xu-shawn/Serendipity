package org.shawn.games.Serendipity.Search;

import java.util.Arrays;

import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.*;

public class TranspositionTable
{
	public static final int NODETYPE_NONE = 0b00;
	public static final int NODETYPE_LOWERBOUND = 0b01;
	public static final int NODETYPE_UPPERBOUND = 0b10;
	public static final int NODETYPE_EXACT = 0b11;

	public static final int DEPTH_NONE = -1;

	public class Entry
	{
		private final int signature;
		private final int depth;
		private final int type;
		private final int evaluation;
		private final int staticEval;
		private final Move move;

		public Entry(short fragment1, long fragment2)
		{
			this(fragment1 & 0b11, fragment1 >>> 2, (int) (fragment2 & 0xFFFF), (int) (fragment2 & 0xFFF0000),
					(int) (fragment2 & 0xFFFF0000000L), (int) (fragment2 >>> 44));
		}

		public Entry(int nodeType, int depth, int signature, int move, int staticEval, int evaluation)
		{
			this.signature = signature;
			this.depth = depth;
			this.type = nodeType;
			this.move = move == 0 ? null : new Move(Square.squareAt(move >> 6), Square.squareAt(move & 0b111111));
			this.evaluation = evaluation;
			this.staticEval = staticEval;
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

		public Move getMove()
		{
			return move;
		}
	}

	private int size;
	private int mask;

	// depth: (0-255) 8 bits
	// NodeType: 2 bits

	private short[] data1;

	// evaluation: 16 bits
	// staticEval: 16 bits
	// Move: 12 bits
	// Square: 6 bits
	// Square: 6 bits
	// Signature: 16 bits

	private long[] data2;

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
		if (entry == null || nodeType == NODETYPE_EXACT || !entry.verifySignature(hash) || depth > entry.getDepth() - 4)
		{
			final short fragment1 = (short) (nodeType | (depth << 2));
			final long fragment2 = ((hash >>> 48)
					| (((move == null) ? 0 : (short) ((move.getFrom().ordinal() << 6) + move.getTo().ordinal())) << 16)
					| ((long)staticEval << 28) | ((long)evaluation << 44));

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