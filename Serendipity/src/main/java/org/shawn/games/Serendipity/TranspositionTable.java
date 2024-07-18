package org.shawn.games.Serendipity;

import java.util.Arrays;

import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.*;

public class TranspositionTable
{
	public static enum NodeType
	{
		EXACT, LOWERBOUND, UPPERBOUND
	}

	public class Entry
	{
		// depth: (0-255) 8 bits
		// NodeType: 2 bits
		// evaluation: 16 bits
		// staticEval: 16 bits
		// Square: 6 bits
		// Signature: 64 bits

		// Total: 32 Bytes (Padding and Class Header)

		private long signature;
		private short depthAndType;
		private short evaluation;
		private short staticEval;
		private short move;

		public Entry(NodeType type, short depth, int evaluation, long signature, Move move, int staticEval)
		{
			this.signature = signature;
			this.depthAndType = (short) ((depth << 2) + typeToByte(type));
			this.move = (move == null) ? 0 : (short) ((move.getFrom().ordinal() << 6) + move.getTo().ordinal());
			this.evaluation = (short) evaluation;
			this.staticEval = (short) staticEval;
		}

		private byte typeToByte(NodeType type)
		{
			return switch (type)
			{
				case EXACT -> 0;
				case LOWERBOUND -> 1;
				case UPPERBOUND -> 2;
			};
		}

		public void write(long signature, NodeType type, short depth, int evaluation, Move move, int staticEval)
		{
			this.signature = signature;
			this.depthAndType = (short) ((depth << 2) + typeToByte(type));
			this.move = (move == null) ? 0 : (short) ((move.getFrom().ordinal() << 6) + move.getTo().ordinal());
			this.evaluation = (short) evaluation;
			this.staticEval = (short) staticEval;
		}

		public long getSignature()
		{
			return signature;
		}

		public boolean verifySignature(long signature)
		{
			return signature == this.signature;
		}

		public NodeType getType()
		{
			return switch (this.depthAndType & 0b11)
			{
				case 0 -> NodeType.EXACT;
				case 1 -> NodeType.LOWERBOUND;
				case 2 -> NodeType.UPPERBOUND;
				default -> throw new IllegalArgumentException("Unexpected value: " + (this.depthAndType & 0b11));
			};
		}

		public short getDepth()
		{
			return (short) (depthAndType >> 2);
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
			return move == 0 ? null : new Move(Square.squareAt(move >> 6), Square.squareAt(move & 0b111111));
		}
	}

	private int size;
	private int mask;
	private Entry[] entries;

	private static final int ENTRY_SIZE = 32;

	public TranspositionTable(int size)
	{
		size *= 1048576 / ENTRY_SIZE;

		this.size = Integer.highestOneBit(size);
		this.mask = this.size - 1;
		this.entries = new Entry[this.size];
	}

	public Entry probe(long hash)
	{
		return entries[(int) (hash & mask)];
	}

	public void write(long hash, NodeType type, int depth, int evaluation, Move move, int staticEval)
	{
		if (entries[(int) (hash & mask)] == null)
		{
			entries[(int) (hash & mask)] = new Entry(type, (short) depth, evaluation, hash, move, staticEval);
		}
		else
		{
			entries[(int) (hash & mask)].write(hash, type, (short) depth, evaluation, move, staticEval);
		}
	}

	public void clear()
	{
		Arrays.fill(entries, null);
	}

	public void resize(int size)
	{
		size *= 1048576 / ENTRY_SIZE;
		this.size = Integer.highestOneBit(size);
		this.mask = this.size - 1;
		this.entries = new Entry[this.size];
	}

	public int hashfull()
	{
		int hashfull = 0;

		for (int i = 0; i < 1000; i++)
		{
			if (this.entries[i] != null)
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
			if (this.entries[i] != null)
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