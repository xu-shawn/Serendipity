package org.shawn.games.Serendipity;

import java.util.Arrays;

import com.github.bhlangonijr.chesslib.move.*;

public class TranspositionTable
{
	public enum NodeType
	{
		EXACT, LOWERBOUND, UPPERBOUND
	}

	public class Entry
	{
		private long signature;
		private NodeType type;
		private short depth;
		private int evaluation;
		private int staticEval;
		private Move move;

		public Entry(NodeType type, short depth, int evaluation, long signature, Move move, int staticEval)
		{
			this.signature = signature;
			this.type = type;
			this.depth = depth;
			this.evaluation = evaluation;
			this.move = move;
			this.staticEval = staticEval;
		}

		public long getSignature()
		{
			return signature;
		}

		public NodeType getType()
		{
			return type;
		}

		public short getDepth()
		{
			return depth;
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

	public final int size;
	private final int mask;
	private Entry[] entries;

	public TranspositionTable(int size)
	{
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
		entries[(int) (hash & mask)] = new Entry(type, (short) depth, evaluation, hash, move, staticEval);
	}

	public void clear()
	{
		Arrays.fill(entries, null);
	}
}