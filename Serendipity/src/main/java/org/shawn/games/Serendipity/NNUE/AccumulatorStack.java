package org.shawn.games.Serendipity.NNUE;

import org.shawn.games.Serendipity.AlphaBeta;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.CastleRight;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class AccumulatorStack
{
	NNUE network;

	private AccumulatorPair[] stack;
	private short top;

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
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndex][i];
			}
		}

		public void add(Accumulator prev, int featureIndex)
		{
			featureIndex = featureIndex + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] = (short) (prev.values[i] + network.L1Weights[featureIndex][i]);
			}
		}

		public void addsub(Accumulator prev, int featureIndexToAdd, int featureIndexToSubtract)
		{
			featureIndexToAdd = featureIndexToAdd + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract = featureIndexToSubtract + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] = (short) (prev.values[i] + network.L1Weights[featureIndexToAdd][i]
						- network.L1Weights[featureIndexToSubtract][i]);
			}
		}

		public void addsubsub(Accumulator prev, int featureIndexToAdd, int featureIndexToSubtract1,
				int featureIndexToSubtract2)
		{
			featureIndexToAdd = featureIndexToAdd + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract1 = featureIndexToSubtract1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract2 = featureIndexToSubtract2 + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] = (short) (prev.values[i] + network.L1Weights[featureIndexToAdd][i]
						- network.L1Weights[featureIndexToSubtract1][i]
						- network.L1Weights[featureIndexToSubtract2][i]);
			}
		}

		public void addaddsubsub(Accumulator prev, int featureIndexToAdd1, int featureIndexToAdd2,
				int featureIndexToSubtract1, int featureIndexToSubtract2)
		{
			featureIndexToAdd1 = featureIndexToAdd1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToAdd2 = featureIndexToAdd2 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract1 = featureIndexToSubtract1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract2 = featureIndexToSubtract2 + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] = (short) (prev.values[i] + network.L1Weights[featureIndexToAdd1][i]
						+ network.L1Weights[featureIndexToAdd2][i] - network.L1Weights[featureIndexToSubtract1][i]
						- network.L1Weights[featureIndexToSubtract2][i]);
			}
		}

		private void efficientlyUpdate(Accumulator prev, Board board, Move move)
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				this.addaddsubsub(prev,
						NNUE.getIndex(board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getTo(), Piece.make(board.getSideToMove(), PieceType.ROOK), this.color),

						NNUE.getIndex(board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getTo(), Piece.make(board.getSideToMove(), PieceType.KING), this.color),

						NNUE.getIndex(board.getContext().getRookCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getFrom(), Piece.make(board.getSideToMove(), PieceType.ROOK), this.color),

						NNUE.getIndex(board.getContext().getKingCastleMove(board.getSideToMove(), CastleRight.KING_SIDE)
								.getFrom(), Piece.make(board.getSideToMove(), PieceType.KING), this.color));

				return;
			}

			if (board.getContext().isQueenSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.QUEEN_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				this.addaddsubsub(prev,
						NNUE.getIndex(board.getContext()
								.getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
								Piece.make(board.getSideToMove(), PieceType.ROOK), this.color),

						NNUE.getIndex(board.getContext()
								.getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getTo(),
								Piece.make(board.getSideToMove(), PieceType.KING), this.color),

						NNUE.getIndex(board.getContext()
								.getRookCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
								Piece.make(board.getSideToMove(), PieceType.ROOK), this.color),

						NNUE.getIndex(board.getContext()
								.getKingCastleMove(board.getSideToMove(), CastleRight.QUEEN_SIDE).getFrom(),
								Piece.make(board.getSideToMove(), PieceType.KING), this.color));

				return;
			}

			Square from = move.getFrom();
			Square to = move.getTo();

			Piece promoted = move.getPromotion();
			Piece moved = board.getPiece(from);
			Piece captured = board.getPiece(to);

			if (promoted.equals(Piece.NONE))
			{
				if (!captured.equals(Piece.NONE))
				{
					this.addsubsub(prev, NNUE.getIndex(to, moved, this.color), NNUE.getIndex(from, moved, this.color),
							NNUE.getIndex(to, captured, this.color));
					return;
				}

				if (move.getTo().equals(board.getEnPassant()) && moved.getPieceType().equals(PieceType.PAWN))
				{
					// @formatter:off
					this.addsubsub(prev,
							NNUE.getIndex(to, moved, this.color),
							NNUE.getIndex(from, moved, this.color),
							NNUE.getIndex(board.getEnPassantTarget(), board.getPiece(board.getEnPassantTarget()),
									this.color));
					// @formatter:on

					return;
				}

				this.addsub(prev, NNUE.getIndex(to, moved, this.color), NNUE.getIndex(from, moved, this.color));

				return;
			}

			else
			{
				if (!captured.equals(Piece.NONE))
				{
					this.addsubsub(prev, NNUE.getIndex(to, promoted, this.color),
							NNUE.getIndex(from, moved, this.color), NNUE.getIndex(to, captured, this.color));

					return;
				}

				this.addsub(prev, NNUE.getIndex(to, promoted, this.color), NNUE.getIndex(from, moved, this.color));

				return;
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

		private void makeMove(Accumulator prev, Board board, Move move)
		{
			if (board.getSideToMove().equals(this.color)
					&& board.getPiece(move.getFrom()).equals(Piece.make(this.color, PieceType.KING))
					&& this.kingBucket != NNUE.chooseInputBucket(move.getTo(), this.color))
			{
				this.changeKingBucket(NNUE.chooseInputBucket(move.getTo(), this.color));
				fullAccumulatorUpdate(board);
				efficientlyUpdate(this, board, move);
			}

			else
			{
				efficientlyUpdate(prev, board, move);
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
			this.accumulators = new Accumulator[] { new Accumulator(network, Side.WHITE) };
		}

		public void loadFromBoard(Board board)
		{
			this.accumulators[0].loadFromBoard(board);
		}

		public void loadFrom(AccumulatorPair prev)
		{
			this.accumulators[0].loadAttributesFrom(prev.accumulators[0]);
		}

		public void makeMove(AccumulatorPair prev, Board board, Move move)
		{
			this.accumulators[0].makeMove(prev.accumulators[0], board, move);
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

	public void push(Board board, Move move)
	{
		top++;
		this.stack[top].loadFrom(this.stack[top - 1]);
		this.stack[top].makeMove(this.stack[top - 1], board, move);
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
