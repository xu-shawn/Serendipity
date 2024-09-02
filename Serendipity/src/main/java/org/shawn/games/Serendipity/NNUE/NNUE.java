package org.shawn.games.Serendipity.NNUE;

import java.io.*;

import com.github.bhlangonijr.chesslib.*;

public class NNUE
{
	private static final int COLOR_STRIDE = 64 * 6;
	private static final int PIECE_STRIDE = 64;

	static final int HIDDEN_SIZE = 1536;
	static final int FEATURE_SIZE = 768;
	private static final int OUTPUT_BUCKETS = 8;
	private static final int DIVISOR = (32 + OUTPUT_BUCKETS - 1) / OUTPUT_BUCKETS;
	private static final int INPUT_BUCKET_SIZE = 7;
	// @formatter:off
	private static final int[] INPUT_BUCKETS = new int[]
	{
			0, 0, 1, 1, 2, 2, 3, 3,
			4, 4, 4, 4, 5, 5, 5, 5,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6,
	};
	// @formatter:on

	private static final int SCALE = 400;
	private static final int QA = 255;
	private static final int QB = 64;

	final short[][] L1Weights;
	final short[] L1Biases;
	private final short[][] L2Weights;
	private final short outputBiases[];

	private final static int screlu[] = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];

	static
	{
		for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++)
		{
			screlu[i - (int) Short.MIN_VALUE] = screlu((short) (i));
		}
	}

	private short toLittleEndian(short input)
	{
		return (short) (((input & 0xFF) << 8) | ((input & 0xFF00) >> 8));
	}

	public NNUE(String filePath) throws IOException
	{
		DataInputStream networkData = new DataInputStream(getClass().getResourceAsStream(filePath));

		L1Weights = new short[FEATURE_SIZE * INPUT_BUCKET_SIZE][HIDDEN_SIZE];

		for (int i = 0; i < FEATURE_SIZE * INPUT_BUCKET_SIZE; i++)
		{
			for (int j = 0; j < HIDDEN_SIZE; j++)
			{
				L1Weights[i][j] = toLittleEndian(networkData.readShort());
			}
		}

		L1Biases = new short[HIDDEN_SIZE];

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			L1Biases[i] = toLittleEndian(networkData.readShort());
		}

		L2Weights = new short[OUTPUT_BUCKETS][HIDDEN_SIZE * 2];

		for (int i = 0; i < HIDDEN_SIZE * 2; i++)
		{
			for (int j = 0; j < OUTPUT_BUCKETS; j++)
			{
				L2Weights[j][i] = toLittleEndian(networkData.readShort());
			}
		}

		outputBiases = new short[OUTPUT_BUCKETS];

		for (int i = 0; i < OUTPUT_BUCKETS; i++)
		{
			outputBiases[i] = toLittleEndian(networkData.readShort());
		}

		networkData.close();
	}

	private static int screlu(short i)
	{
		int v = Math.max(0, Math.min(i, QA));
		return v * v;
	}

	public static int evaluate(NNUE network, AccumulatorStack accumulators, Side side, int chosenBucket)
	{
		int eval = 0;

		AccumulatorStack.Accumulator us = accumulators.getAccumulator(side);
		AccumulatorStack.Accumulator them = accumulators.getAccumulator(side.flip());

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			eval += screlu[us.values[i] - (int) Short.MIN_VALUE] * (int) network.L2Weights[chosenBucket][i]
					+ screlu[them.values[i] - (int) Short.MIN_VALUE]
							* (int) network.L2Weights[chosenBucket][i + HIDDEN_SIZE];
		}

		eval /= QA;
		eval += network.outputBiases[chosenBucket];

		eval *= SCALE;
		eval /= QA * QB;

		return eval;
	}

	public static int chooseOutputBucket(Board board)
	{
		return (Long.bitCount(board.getBitboard()) - 2) / DIVISOR;
	}

	public static int chooseInputBucket(Board board, Side side)
	{
		return side.equals(Side.WHITE) ? INPUT_BUCKETS[board.getKingSquare(side).ordinal()]
				: INPUT_BUCKETS[board.getKingSquare(side).ordinal() ^ 0b111000];
	}

	public static int chooseInputBucket(Square square, Side side)
	{
		return side.equals(Side.WHITE) ? INPUT_BUCKETS[square.ordinal()] : INPUT_BUCKETS[square.ordinal() ^ 0b111000];
	}

	public static int getIndex(Square square, Piece piece, Side perspective)
	{
		return Side.WHITE.equals(perspective)
				? piece.getPieceSide().ordinal() * COLOR_STRIDE + piece.getPieceType().ordinal() * PIECE_STRIDE
						+ square.ordinal()
				: (piece.getPieceSide().ordinal() ^ 1) * COLOR_STRIDE + piece.getPieceType().ordinal() * PIECE_STRIDE
						+ (square.ordinal() ^ 0b111000);
	}
}
