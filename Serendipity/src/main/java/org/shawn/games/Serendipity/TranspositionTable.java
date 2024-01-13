package org.shawn.games.Serendipity;

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
		private int depth;
		private int evaluation;

		public Entry(NodeType type, int depth, int evaluation, long signature)
		{
			this.signature = signature;
			this.type = type;
			this.depth = depth;
			this.evaluation = evaluation;
		}

		public long getSignature()
		{
			return signature;
		}

		public NodeType getType()
		{
			return type;
		}

		public int getDepth()
		{
			return depth;
		}

		public int getEvaluation()
		{
			return evaluation;
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

	public void write(long hash, NodeType type, int depth, int evaluation)
	{
		entries[(int) (hash & mask)] = new Entry(type, depth, evaluation, hash);
	}
}