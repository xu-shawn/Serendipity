package org.shawn.games.Serendipity.Search;

import java.util.Arrays;

import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.*;

public class TranspositionTable
{
	public static enum NodeType
	{
		EXACT, LOWERBOUND, UPPERBOUND
	}

	private static final NodeType[] byteToNodeType = new NodeType[] { NodeType.EXACT, NodeType.LOWERBOUND,
			NodeType.UPPERBOUND };

	public class Entry
	{
		// depth: (0-255) 8 bits
		// NodeType: 8 bits
		// evaluation: 16 bits
		// staticEval: 16 bits
		// Square: 6 bits
		// Signature: 16 bits

		// Total: 32 Bytes (Padding and Class Header)

		private short signature;
		private byte depth;
		private byte type;
		private short evaluation;
		private short staticEval;
		private short move;

		public Entry(NodeType type, short depth, int evaluation, long signature, Move move, int staticEval)
		{
			this.signature = (short) (signature >>> 48);
			this.depth = (byte) depth;
			this.type = (byte) type.ordinal();
			this.move = (move == null) ? 0 : (short) ((move.getFrom().ordinal() << 6) + move.getTo().ordinal());
			this.evaluation = (short) evaluation;
			this.staticEval = (short) staticEval;
		}

		public void write(long signature, NodeType type, short depth, int evaluation, Move move, int staticEval)
		{
			if (type.equals(NodeType.EXACT) || !this.verifySignature(signature) || depth > this.getDepth() - 4)
			{
				this.signature = (short) (signature >>> 48);
				this.depth = (byte) depth;
				this.type = (byte) type.ordinal();
				this.move = (move == null) ? 0 : (short) ((move.getFrom().ordinal() << 6) + move.getTo().ordinal());
				this.evaluation = (short) evaluation;
				this.staticEval = (short) staticEval;
			}
		}

		public long getSignature()
		{
			return signature;
		}

		public boolean verifySignature(long signature)
		{
			return (short) (signature >>> 48) == this.signature;
		}

		public NodeType getType()
		{
			return byteToNodeType[this.type];
		}

		public short getDepth()
		{
			return (short) (this.depth);
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