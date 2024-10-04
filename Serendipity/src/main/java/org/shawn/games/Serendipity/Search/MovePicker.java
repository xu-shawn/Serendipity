package org.shawn.games.Serendipity.Search;

import java.util.Collections;
import java.util.List;

import org.shawn.games.Serendipity.Search.History.History;

import com.github.bhlangonijr.chesslib.Bitboard;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.CastleRight;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;

public class MovePicker
{
	private Board board;
	private Move ttMove;

	private Move killer;
	private History history;
	private History captureHistory;
	private History[] continuationHistories;

	private int[] moveScore;
	private List<Move> moves;

	private int stage;
	private int moveIndex;

	private static final int STAGE_TT_MOVE = 0;
	private static final int STAGE_NORMAL = 1;
	private static final int[] promoValue = { -2000000001, 2000000000, -2000000001, -2000000001, 2000000001 };

	public MovePicker(Board board, Move ttMove)
	{
		this(board, ttMove, null, null, null, null);
	}

	public MovePicker(Board board, Move ttMove, Move killer, History history, History captureHistory,
			History[] continuationHistories)
	{
		this.board = board;
		this.ttMove = ttMove;
		this.moveIndex = 0;

		if (ttMove == null || !isPseudoLegal(board, ttMove))
		{
			this.ttMove = null;
			this.stage = STAGE_NORMAL;
		}
		else
		{
			this.stage = STAGE_TT_MOVE;
		}

		this.killer = killer;
		this.history = history;
		this.captureHistory = captureHistory;
		this.continuationHistories = continuationHistories;
	}

	public static boolean isPseudoLegal(Board board, Move move)
	{
		if (Piece.NONE.equals(board.getPiece(move.getFrom())) || !board.isMoveLegal(move, true))
		{
			return false;
		}

		if (board.getContext().isCastleMove(move))
		{
			return true;
		}

		Square from = move.getFrom();
		Square to = move.getTo();
		Piece movedPiece = board.getPiece(move.getFrom());
		Side side = movedPiece.getPieceSide();
		PieceType movedPieceType = movedPiece.getPieceType();

		long occupied = board.getBitboard();

		if ((board.getBbSide()[side.ordinal()] & to.getBitboard()) != 0)
		{
			return false;
		}

		switch (movedPieceType)
		{
			case PAWN:
				long pawnThreats = Bitboard.getPawnCaptures(side, from,
						to.getBitboard() & (board.getBbSide()[1 - side.ordinal()] | board.getEnPassant().getBitboard()),
						board.getEnPassant());
				pawnThreats |= Bitboard.getPawnMoves(side, from, board.getBitboard()) & to.getBitboard();
				if (pawnThreats == 0L)
				{
					return false;
				}
				break;
			case KNIGHT:
				if (Bitboard.getKnightAttacks(from, to.getBitboard()) == 0L)
				{
					return false;
				}
				break;
			case BISHOP:
				if ((Bitboard.getBishopAttacks(occupied, from) & to.getBitboard()) == 0L)
				{
					return false;
				}
				break;
			case ROOK:
				if ((Bitboard.getRookAttacks(occupied, from) & to.getBitboard()) == 0L)
				{
					return false;
				}
				break;
			case QUEEN:
				if ((Bitboard.getQueenAttacks(occupied, from) & to.getBitboard()) == 0L)
				{
					return false;
				}
				break;
			case KING:
				if (Bitboard.getKingAttacks(from, to.getBitboard()) == 0L)
				{
					return false;
				}
				break;
			default:
				return false;
		}

		return true;
	}

	private static int pieceValue(Piece p)
	{
		if (p.getPieceType() == null)
		{
			return 0;
		}

		return p.getPieceType().ordinal() + 1;
	}

	private int captureValue(Move move)
	{
		return pieceValue(board.getPiece(move.getTo())) * 100 + captureHistory.get(board, move) / 256;
	}

	private int scoreMove(Move move)
	{
		if (!move.getPromotion().equals(Piece.NONE))
		{
			return promoValue[move.getPromotion().getPieceType().ordinal()];
		}

		if (!AlphaBeta.isQuiet(move, board))
		{
			int score = SEE.staticExchangeEvaluation(board, move, -20) ? 900000000 : -1000000;
			score += captureValue(move);
			return score;
		}

		if (move.equals(killer))
		{
			return 800000000;
		}

		int moveValue = history.get(board, move);

		moveValue += continuationHistories[0].get(board, move);
		moveValue += continuationHistories[1].get(board, move);
		moveValue += continuationHistories[3].get(board, move);
		moveValue += continuationHistories[5].get(board, move) / 2;

		return moveValue;
	}

	public void initMoves()
	{
		this.moves = board.legalMoves();
		this.moveScore = new int[this.moves.size()];

		for (int i = 0; i < this.moves.size(); i++)
		{
			this.moveScore[i] = scoreMove(this.moves.get(i));
		}
	}

	public Move selectMove()
	{
		if (this.moveIndex >= this.moves.size())
		{
			return null;
		}

		int temp;
		for (int i = this.moves.size() - 1; i > this.moveIndex; i--)
		{
			if (this.moveScore[i] > this.moveScore[i - 1])
			{
				Collections.swap(moves, i, i - 1);

				temp = this.moveScore[i];
				this.moveScore[i] = this.moveScore[i - 1];
				this.moveScore[i - 1] = temp;
			}
		}

		this.moveIndex++;

		return this.moves.get(moveIndex - 1);
	}

	public Move next()
	{
		switch (stage)
		{
			case STAGE_TT_MOVE:
				stage++;
				return this.ttMove;

			case STAGE_NORMAL:
				if (this.moveScore == null)
				{
					initMoves();
				}

				Move ret = selectMove();

				if (ret != null && ret.equals(ttMove))
				{
					ret = selectMove();
				}

				return ret;
		}

		return null;
	}
}
