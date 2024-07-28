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
		@SuppressWarnings("unused")
		boolean needsRefresh;
		int kingBucket;

		public Accumulator()
		{

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

		public void loadFrom(Accumulator prev)
		{
			this.values = prev.values.clone();
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

		public void addsub(int featureIndexToAdd, int featureIndexToSubtract)
		{
			featureIndexToAdd = featureIndexToAdd + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract = featureIndexToSubtract + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd][i] - network.L1Weights[featureIndexToSubtract][i];
			}
		}

		public void addsubsub(int featureIndexToAdd, int featureIndexToSubtract1, int featureIndexToSubtract2)
		{
			featureIndexToAdd = featureIndexToAdd + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract1 = featureIndexToSubtract1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract2 = featureIndexToSubtract2 + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd][i] - network.L1Weights[featureIndexToSubtract1][i]
						- network.L1Weights[featureIndexToSubtract2][i];
			}
		}

		public void addaddsubsub(int featureIndexToAdd1, int featureIndexToAdd2, int featureIndexToSubtract1,
				int featureIndexToSubtract2)
		{
			featureIndexToAdd1 = featureIndexToAdd1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToAdd2 = featureIndexToAdd2 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract1 = featureIndexToSubtract1 + kingBucket * NNUE.FEATURE_SIZE;
			featureIndexToSubtract2 = featureIndexToSubtract2 + kingBucket * NNUE.FEATURE_SIZE;
			for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd1][i] + network.L1Weights[featureIndexToAdd2][i]
						- network.L1Weights[featureIndexToSubtract1][i] - network.L1Weights[featureIndexToSubtract2][i];
			}
		}

		private void efficientlyUpdate(Board board, Move move)
		{
			if (board.getContext().isKingSideCastle(move)
					&& (board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_SIDE)
							|| board.getCastleRight(board.getSideToMove()).equals(CastleRight.KING_AND_QUEEN_SIDE)))
			{
				this.addaddsubsub(
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
				this.addaddsubsub(
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

			if (move.getPromotion().equals(Piece.NONE))
			{
				if (!board.getPiece(move.getTo()).equals(Piece.NONE))
				{
					this.addsubsub(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), this.color),
							NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), this.color),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), this.color));
					return;
				}

				if (move.getTo().equals(board.getEnPassant())
						&& board.getPiece(move.getFrom()).getPieceType().equals(PieceType.PAWN))
				{
					this.addsubsub(

							NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), this.color),
							NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), this.color),

							NNUE.getIndex(board.getEnPassantTarget(), board.getPiece(board.getEnPassantTarget()),
									this.color));

					return;
				}

				this.addsub(NNUE.getIndex(move.getTo(), board.getPiece(move.getFrom()), this.color),
						NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), this.color));

				return;
			}

			else
			{
				if (!board.getPiece(move.getTo()).equals(Piece.NONE))
				{
					this.addsubsub(NNUE.getIndex(move.getTo(), move.getPromotion(), this.color),
							NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), this.color),
							NNUE.getIndex(move.getTo(), board.getPiece(move.getTo()), this.color));

					return;
				}

				this.addsub(NNUE.getIndex(move.getTo(), move.getPromotion(), this.color),
						NNUE.getIndex(move.getFrom(), board.getPiece(move.getFrom()), this.color));

				return;
			}
		}

		private void fullAccumulatorUpdate(Board board)
		{
			for (Square sq : Square.values())
			{
				if (!board.getPiece(sq).equals(Piece.NONE))
				{
					this.add(NNUE.getIndex(sq, board.getPiece(sq), this.color));
				}
			}
		}

		private void makeMove(Board board, Move move)
		{
			if (board.getSideToMove().equals(this.color)
					&& board.getPiece(move.getFrom()).equals(Piece.make(this.color, PieceType.KING))
					&& this.kingBucket != NNUE.chooseInputBucket(move.getTo(), this.color))
			{
				this.changeKingBucket(NNUE.chooseInputBucket(move.getTo(), this.color));
				fullAccumulatorUpdate(board);
			}

			efficientlyUpdate(board, move);
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
			this.accumulators[0].loadFrom(prev.accumulators[0]);
			this.accumulators[1].loadFrom(prev.accumulators[1]);
		}

		public void makeMove(Board board, Move move)
		{
			this.accumulators[0].makeMove(board, move);
			this.accumulators[1].makeMove(board, move);
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
		this.stack[top].makeMove(board, move);
	}

	public void init(Board board)
	{
		this.top = 0;
		this.stack[0].init();
		this.stack[0].loadFromBoard(board);
	}

	public AccumulatorStack.Accumulator getAccumulator(Side side)
	{
		return this.stack[top].accumulators[side.ordinal()];
	}
}
