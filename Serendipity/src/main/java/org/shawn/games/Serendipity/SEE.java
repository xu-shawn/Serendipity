package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class SEE
{
	private final static int SEEPieceValues(PieceType piece)
	{
		return switch (piece)
		{
			case PAWN -> 103;
			case KNIGHT -> 422;
			case BISHOP -> 437;
			case ROOK -> 694;
			case QUEEN -> 1313;
			default -> 0;
		};
	}

	private final static int SEEPieceValues(Piece piece)
	{
		return SEEPieceValues(piece.getPieceType());
	}

	public static int moveEstimatedValue(Board board, Move move)
	{
		// Start with the value of the piece on the target square
		int value = SEEPieceValues(board.getPiece(move.getTo()).getPieceType());

		// Factor in the new piece's value and remove our promoted pawn
		if (!Piece.NONE.equals(move.getPromotion()))
			value += SEEPieceValues(move.getPromotion()) - SEEPieceValues(PieceType.PAWN);

		// Target square is encoded as empty for enpass moves
		else if (PieceType.PAWN.equals(board.getPiece(move.getFrom()).getPieceType())
				&& board.getEnPassantTarget().equals(move.getTo()))
			value = SEEPieceValues(PieceType.PAWN);

		return value;
	}

	public static boolean staticExchangeEvaluation(Board board, Move move, int threshold)
	{
		Square from, to;
		PieceType nextVictim;
		Side colour;
		int balance;
		long bishops, rooks, occupied, attackers, myAttackers;
		boolean isPromotion, isEnPassant;

		// Unpack move information
		from = move.getFrom();
		to = move.getTo();

		isPromotion = !Piece.NONE.equals(move.getPromotion());
		isEnPassant = PieceType.PAWN.equals(board.getPiece(from).getPieceType())
				&& board.getEnPassantTarget().equals(to);

		// Next victim is moved piece or promotion type
		nextVictim = !isPromotion ? board.getPiece(from).getPieceType() : move.getPromotion().getPieceType();

		// Balance is the value of the move minus threshold. Function
		// call takes care for Enpass, Promotion and Castling moves.
		balance = moveEstimatedValue(board, move) - threshold;

		// Best case still fails to beat the threshold
		if (balance < 0)
			return false;

		// Worst case is losing the moved piece
		balance -= SEEPieceValues(nextVictim);

		// If the balance is positive even if losing the moved piece,
		// the exchange is guaranteed to beat the threshold.
		if (balance >= 0)
			return true;

		// Grab sliders for updating revealed attackers
		bishops = board.getBitboard(Piece.BLACK_BISHOP) | board.getBitboard(Piece.WHITE_BISHOP)
				| board.getBitboard(Piece.BLACK_QUEEN) | board.getBitboard(Piece.WHITE_QUEEN);
		rooks = board.getBitboard(Piece.BLACK_ROOK) | board.getBitboard(Piece.WHITE_ROOK)
				| board.getBitboard(Piece.BLACK_QUEEN) | board.getBitboard(Piece.WHITE_QUEEN);

		// Let occupied suppose that the move was actually made
		occupied = board.getBitboard();
		occupied = (occupied ^ (1L << from.ordinal())) | (1L << to.ordinal());
		if (isEnPassant)
			occupied ^= (1L << board.getEnPassantTarget().ordinal());

		// Get all pieces which attack the target square. And with occupied
		// so that we do not let the same piece attack twice
		attackers = board.squareAttackedBy(to, board.getSideToMove(), occupied)
				| board.squareAttackedBy(to, board.getSideToMove().flip(), occupied) & occupied;

		// Now our opponents turn to recapture
		colour = board.getSideToMove().flip();

		while (true)
		{
			// If we have no more attackers left we lose
			myAttackers = attackers & board.getBitboard(colour);

			if (myAttackers == 0)
				break;

			if ((myAttackers & (board.getBitboard(Piece.BLACK_PAWN) | board.getBitboard(Piece.WHITE_PAWN))) != 0L)
			{
				nextVictim = PieceType.PAWN;
			}

			else if ((myAttackers
					& (board.getBitboard(Piece.BLACK_KNIGHT) | board.getBitboard(Piece.WHITE_KNIGHT))) != 0L)
			{
				nextVictim = PieceType.KNIGHT;
			}

			else if ((myAttackers
					& (board.getBitboard(Piece.BLACK_BISHOP) | board.getBitboard(Piece.WHITE_BISHOP))) != 0L)
			{
				nextVictim = PieceType.BISHOP;
			}

			else if ((myAttackers & (board.getBitboard(Piece.BLACK_ROOK) | board.getBitboard(Piece.WHITE_ROOK))) != 0L)
			{
				nextVictim = PieceType.ROOK;
			}

			else if ((myAttackers
					& (board.getBitboard(Piece.BLACK_QUEEN) | board.getBitboard(Piece.WHITE_QUEEN))) != 0L)
			{
				nextVictim = PieceType.QUEEN;
			}
			
			else
			{
				assert(false);
			}

			// Remove this attacker from the occupied
			occupied ^= (1L << Bitboard.extractLsb(myAttackers & board.getBitboard(Piece.make(colour, nextVictim))));

			// A diagonal move may reveal bishop or queen attackers
			if (nextVictim.equals(PieceType.PAWN) || nextVictim.equals(PieceType.BISHOP) || nextVictim.equals(PieceType.QUEEN))
				attackers |= Bitboard.getBishopAttacks(occupied, to) & bishops;

			// A vertical or horizontal move may reveal rook or queen attackers
			if (nextVictim.equals(PieceType.ROOK) || nextVictim.equals(PieceType.QUEEN))
				attackers |= Bitboard.getRookAttacks(occupied, to) & rooks;

			// Make sure we did not add any already used attacks
			attackers &= occupied;

			// Swap the turn
			colour = colour.flip();

			// Negamax the balance and add the value of the next victim
			balance = -balance - 1 - SEEPieceValues(nextVictim);

			// If the balance is non-negative after giving away our piece then we win
			if (balance >= 0)
			{
				// As a slide speed up for move legality checking, if our last attacking
				// piece is a king, and our opponent still has attackers, then we've
				// lost as the move we followed would be illegal
				if (nextVictim.equals(PieceType.KING) && (attackers & board.getBitboard(colour)) != 0)
					colour = colour.flip();

				break;
			}
		}

		// Side to move after the loop loses
		return !board.getSideToMove().equals(colour);
	}
}
