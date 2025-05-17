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

package org.shawn.games.Serendipity.NNUE;

import org.shawn.games.Serendipity.Search.AlphaBeta;
import org.shawn.games.Serendipity.Chess.AccumulatorDiff;
import org.shawn.games.Serendipity.Chess.Board;
import org.shawn.games.Serendipity.Chess.Piece;
import org.shawn.games.Serendipity.Chess.PieceType;
import org.shawn.games.Serendipity.Chess.Side;
import org.shawn.games.Serendipity.Chess.Square;
import org.shawn.games.Serendipity.Chess.move.Move;

public class AccumulatorStack
{
	NNUE network;

	private final AccumulatorPair[] stack;
	private int top;

	private static final Inference INFERENCE = InferenceChooser.chooseInference();

	public class Accumulator
	{
		short[] values;
		Side color;
		boolean needsRefresh;
		int kingBucket;

		public Accumulator()
		{
			this.values = new short[NNUE.HIDDEN_SIZE];
		}

		public Accumulator(NNUE network, Side color)
		{
			this.values = network.L1Biases.clone();
			this.color = color;
			this.needsRefresh = false;
		}

		public void changeKingBucket(int i)
		{
			this.kingBucket = i;
			needsRefresh = true;
		}

		public void loadAttributesFrom(Accumulator prev)
		{
			this.color = prev.color;
			this.needsRefresh = true;
			this.kingBucket = prev.kingBucket;
		}

		public void add(int featureIndex)
		{
			featureIndex = featureIndex + kingBucket * NNUE.FEATURE_SIZE;

			INFERENCE.add(values, values, network.L1Weights[featureIndex]);
		}

		public void add(Accumulator prev, int featureIndex)
		{
			featureIndex = featureIndex + kingBucket * NNUE.FEATURE_SIZE;

			INFERENCE.add(values, prev.values, network.L1Weights[featureIndex]);
		}

		public void addSub(Accumulator prev, int featureIndexToAdd, int featureIndexToSubtract)
		{
			featureIndexToAdd = featureIndexToAdd + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract = featureIndexToSubtract + kingBucket * NNUE.FEATURE_SIZE;

			INFERENCE.addSub(values, prev.values, network.L1Weights[featureIndexToAdd],
					network.L1Weights[featureIndexToSubtract]);
		}

		public void addSubSub(Accumulator prev, int featureIndexToAdd, int featureIndexToSubtract1,
				int featureIndexToSubtract2)
		{
			featureIndexToAdd = featureIndexToAdd + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract1 = featureIndexToSubtract1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract2 = featureIndexToSubtract2 + kingBucket * NNUE.FEATURE_SIZE;

			INFERENCE.addSubSub(values, prev.values, network.L1Weights[featureIndexToAdd],
					network.L1Weights[featureIndexToSubtract1], network.L1Weights[featureIndexToSubtract2]);
		}

		public void addAddSubSub(Accumulator prev, int featureIndexToAdd1, int featureIndexToAdd2,
				int featureIndexToSubtract1, int featureIndexToSubtract2)
		{
			featureIndexToAdd1 = featureIndexToAdd1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToAdd2 = featureIndexToAdd2 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract1 = featureIndexToSubtract1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract2 = featureIndexToSubtract2 + kingBucket * NNUE.FEATURE_SIZE;

			INFERENCE.addAddSubSub(values, prev.values, network.L1Weights[featureIndexToAdd1],
					network.L1Weights[featureIndexToAdd2], network.L1Weights[featureIndexToSubtract1],
					network.L1Weights[featureIndexToSubtract2]);
		}

		private void efficientlyUpdate(Accumulator prev, Board board, final AccumulatorDiff diff)
		{
			final int addedCount = diff.getAddedCount();
			final int removedCount = diff.getRemovedCount();

			if (addedCount == 1 && removedCount == 1)
			{
				final int addedIndex = NNUE.getIndex(diff.getAdded(0), this.color);
				final int removedIndex = NNUE.getIndex(diff.getRemoved(0), this.color);

				this.addSub(prev, addedIndex, removedIndex);
			}

			else if (addedCount == 1 && removedCount == 2)
			{
				final int addedIndex = NNUE.getIndex(diff.getAdded(0), this.color);
				final int removedIndex0 = NNUE.getIndex(diff.getRemoved(0), this.color);
				final int removedIndex1 = NNUE.getIndex(diff.getRemoved(1), this.color);

				this.addSubSub(prev, addedIndex, removedIndex0, removedIndex1);
			}

			else
			{
				assert addedCount == 2 && removedCount == 2;

				final int addedIndex0 = NNUE.getIndex(diff.getAdded(0), this.color);
				final int addedIndex1 = NNUE.getIndex(diff.getAdded(1), this.color);
				final int removedIndex0 = NNUE.getIndex(diff.getRemoved(0), this.color);
				final int removedIndex1 = NNUE.getIndex(diff.getRemoved(1), this.color);

				this.addAddSubSub(prev, addedIndex0, addedIndex1, removedIndex0, removedIndex1);
			}
		}

		private void fullAccumulatorUpdate(Board board)
		{
			System.arraycopy(network.L1Biases, 0, this.values, 0, NNUE.HIDDEN_SIZE);
			for (Square sq : Square.values())
			{
				if (!board.getPiece(sq).equals(Piece.NONE))
				{
					this.add(NNUE.getIndex(sq, board.getPiece(sq), this.color));
				}
			}
		}

		private void makeMove(Accumulator prev, Board board, Move move, final AccumulatorDiff diff)
		{
			if (board.getPiece(move.getTo()).equals(Piece.make(this.color, PieceType.KING))
					&& this.kingBucket != NNUE.chooseInputBucket(move.getTo(), this.color))
			{
				this.changeKingBucket(NNUE.chooseInputBucket(move.getTo(), this.color));
				fullAccumulatorUpdate(board);
			}

			else
			{
				efficientlyUpdate(prev, board, diff);
			}
		}

		private void loadFromBoard(Board board)
		{
			this.changeKingBucket(NNUE.chooseInputBucket(board, this.color));
			fullAccumulatorUpdate(board);
		}
	}

	private class AccumulatorPair
	{
		Accumulator[] accumulators;

		public AccumulatorPair()
		{
			accumulators = new Accumulator[] { new Accumulator(), new Accumulator() };
		}

		public void init()
		{
			this.accumulators = new Accumulator[] { new Accumulator(network, Side.WHITE),
					new Accumulator(network, Side.BLACK) };
		}

		public void loadFromBoard(Board board)
		{
			this.accumulators[0].loadFromBoard(board);
			this.accumulators[1].loadFromBoard(board);
		}

		public void loadFrom(AccumulatorPair prev)
		{
			this.accumulators[0].loadAttributesFrom(prev.accumulators[0]);
			this.accumulators[1].loadAttributesFrom(prev.accumulators[1]);
		}

		public void makeMove(AccumulatorPair prev, Board board, Move move, final AccumulatorDiff diff)
		{
			this.accumulators[0].makeMove(prev.accumulators[0], board, move, diff);
			this.accumulators[1].makeMove(prev.accumulators[1], board, move, diff);
		}
	}

	public AccumulatorStack(NNUE network)
	{
		this.network = network;
		this.stack = new AccumulatorPair[AlphaBeta.MAX_PLY + 1];

		for (int i = 0; i < this.stack.length; i++)
		{
			this.stack[i] = new AccumulatorPair();
		}
	}

	public void pop()
	{
		top--;
	}

	public void push(Board board, Move move, final AccumulatorDiff diff)
	{
		top++;
		this.stack[top].loadFrom(this.stack[top - 1]);
		this.stack[top].makeMove(this.stack[top - 1], board, move, diff);
	}

	public void init(Board board)
	{
		this.top = 0;
		this.stack[0].init();
		this.stack[0].loadFromBoard(board);
	}

	public void printAccumulator(Side side)
	{
		for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
		{
			System.out.print(this.stack[top].accumulators[side.ordinal()].values[i] + ", ");
		}
	}

	public AccumulatorStack.Accumulator getAccumulator(Side side)
	{
		return this.stack[top].accumulators[side.ordinal()];
	}
}
