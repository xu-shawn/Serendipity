package org.shawn.games.Serendipity;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class SEE
{
	private static int see_value(PieceType piece)
	{
		return switch (piece)
		{
			case PAWN -> 103;
			case KNIGHT -> 422;
			case BISHOP -> 437;
			case ROOK -> 694;
			case QUEEN -> 1313;
			case KING -> 100000;
			default -> 0;
		};
	}
	
	int moveEstimatedValue(Board board, Move move) {

	    // Start with the value of the piece on the target square
	    int value = see_value(board.getPiece(move.getTo()).getPieceType());

	    // Factor in the new piece's value and remove our promoted pawn
	    if (move.getPromotion().equals(Piece.NONE))
	        value += see_value(move.getPromotion().getPieceType()) - see_value(PieceType.PAWN);

	    // Target square is encoded as empty for enpass moves
	    else if (move == ENPASS_MOVE)
	        value = SEEPieceValues[PAWN];

	    // We encode Castle moves as KxR, so the initial step is wrong
	    else if (MoveType(move) == CASTLE_MOVE)
	        value = 0;

	    return value;
	}


	public static int staticExchangeEvaluation(Board board, Move move, int threshold)
	{
		Square from, to;
		Side colour;
		int balance;
		Piece nextVictim;
		long bishops, rooks, occupied, attackers, myAttackers;

		// Unpack move information
		from = move.getFrom();
		to = move.getTo();

		// Next victim is moved piece or promotion type
		nextVictim = move.getPromotion() == null ? board.getPiece(move.getFrom()) : MovePromoPiece(move);

		// Balance is the value of the move minus threshold. Function
		// call takes care for Enpass, Promotion and Castling moves.
		balance = moveEstimatedValue(board, move) - threshold;

		// Best case still fails to beat the threshold
		if (balance < 0)
			return 0;

		// Worst case is losing the moved piece
		balance -= SEEPieceValues[nextVictim];

		// If the balance is positive even if losing the moved piece,
		// the exchange is guaranteed to beat the threshold.
		if (balance >= 0)
			return 1;

		// Grab sliders for updating revealed attackers
		bishops = board.pieces[BISHOP] | board.pieces[QUEEN];
		rooks = board.pieces[ROOK] | board.pieces[QUEEN];

		// Let occupied suppose that the move was actually made
		occupied = (board.colours[WHITE] | board.colours[BLACK]);
		occupied = (occupied ^ (1L << from)) | (1L << to);
		if (type == ENPASS_MOVE)
			occupied ^= (1L << board.epSquare);

		// Get all pieces which attack the target square. And with occupied
		// so that we do not let the same piece attack twice
		attackers = allAttackersToSquare(board, occupied, to) & occupied;

		// Now our opponents turn to recapture
		colour = board.turn ? 0 : 1;

		while (true)
		{

			// If we have no more attackers left we lose
			myAttackers = attackers & board.colours[colour];
			if (myAttackers == 0L)
				break;

			// Find our weakest piece to attack with
			for (nextVictim = PAWN; nextVictim <= QUEEN; nextVictim++)
				if ((myAttackers & board.pieces[nextVictim]) != 0)
					break;

			// Remove this attacker from the occupied
			occupied ^= (1L << getlsb(myAttackers & board.pieces[nextVictim]));

			// A diagonal move may reveal bishop or queen attackers
			if (nextVictim == PAWN || nextVictim == BISHOP || nextVictim == QUEEN)
				attackers |= bishopAttacks(to, occupied) & bishops;

			// A vertical or horizontal move may reveal rook or queen attackers
			if (nextVictim == ROOK || nextVictim == QUEEN)
				attackers |= rookAttacks(to, occupied) & rooks;

			// Make sure we did not add any already used attacks
			attackers &= occupied;

			// Swap the turn
			colour = colour == 0 ? 1 : 0;

			// Negamax the balance and add the value of the next victim
			balance = -balance - 1 - SEEPieceValues[nextVictim];

			// If the balance is non-negative after giving away our piece then we win
			if (balance >= 0)
			{

				// As a slide speed up for move legality checking, if our last attacking
				// piece is a king, and our opponent still has attackers, then we've
				// lost as the move we followed would be illegal
				if (nextVictim == KING && (attackers & board.colours[colour]) != 0)
					colour = colour == 0 ? 1 : 0;

				break;
			}
		}

		return 0;
	}

	public static int see(Board board, Move move)
	{
		int[] gain = new int[32];
		int depth = 0;

		gain[depth] = move.getPromotion() != null ? see_value(board.getPiece(move.getTo()))
				: see_value(move.getPromotion());

		Piece attacker = board.getPiece(move.getFrom());

		long attadef = board.squareAttackedBy(move.getTo(), board.getSideToMove())
				| board.squareAttackedBy(move.getTo(), board.getSideToMove().flip());

		long occupancies = board.getBitboard(board.getSideToMove());

		Side sideToMove = board.getSideToMove().flip();

		while (true)
		{
			depth++;
			gain[depth] = see_value(attacker) - gain[depth - 1];

			long ourBoard = board.getBitboard(sideToMove);
			long attackers = ourBoard & attadef;

			if (Math.max(gain[depth], -gain[depth - 1]) < 0)
			{
				break;
			}

		}

		while (depth > 0)
		{
			gain[depth - 1] = -Math.max(-gain[depth - 1], gain[depth]);
			depth--;
		}

		return gain[0];
	}
}
