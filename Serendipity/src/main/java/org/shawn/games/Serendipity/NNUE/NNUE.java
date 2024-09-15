package org.shawn.games.Serendipity.NNUE;

import java.io.*;
import java.util.Objects;

import com.github.bhlangonijr.chesslib.*;

public class NNUE
{
	private static final int COLOR_STRIDE = 64 * 6;
	private static final int PIECE_STRIDE = 64;

	static final int HIDDEN_SIZE = 1536;
	static final int FEATURE_SIZE = 768;
	private static final int OUTPUT_BUCKETS = 8;
	private static final int DIVISOR = (32 + OUTPUT_BUCKETS - 1) / OUTPUT_BUCKETS;
	private static final int INPUT_BUCKET_SIZE = 4;
	// @formatter:off
	private static final int[] INPUT_BUCKETS = new int[]
	{
			0, 0, 1, 1, 5, 5, 4, 4,
			2, 2, 2, 2, 6, 6, 6, 6,
			3, 3, 3, 3, 7, 7, 7, 7,
			3, 3, 3, 3, 7, 7, 7, 7,
			3, 3, 3, 3, 7, 7, 7, 7,
			3, 3, 3, 3, 7, 7, 7, 7,
			3, 3, 3, 3, 7, 7, 7, 7,
			3, 3, 3, 3, 7, 7, 7, 7,
	};
	// @formatter:on

	public static final int SCALE = 400;
	public static final int QA = 255;
	public static final int QB = 64;

	final short[][] L1Weights;
	final short[] L1Biases;
	private final short[][] L2Weights;
	private final short[] outputBiases;

	private static final Inference INFERENCE = InferenceChooser.chooseInference();

	private short toLittleEndian(short input)
	{
		return (short) (((input & 0xFF) << 8) | ((input & 0xFF00) >> 8));
	}

	public NNUE(String filePath) throws IOException
	{
		DataInputStream networkData = new DataInputStream(
				Objects.requireNonNull(getClass().getResourceAsStream(filePath)));

		L1Weights = new short[FEATURE_SIZE * INPUT_BUCKET_SIZE * 2][HIDDEN_SIZE];

		for (int bucket = 0; bucket < INPUT_BUCKET_SIZE; bucket++)
		{
			for (int i = 0; i < FEATURE_SIZE; i++)
			{
				for (int j = 0; j < HIDDEN_SIZE; j++)
				{
					L1Weights[i + FEATURE_SIZE * bucket][j] = toLittleEndian(networkData.readShort());
					L1Weights[NNUE.flipIndex(i) + FEATURE_SIZE * (bucket + INPUT_BUCKET_SIZE)][j] = L1Weights[i
							+ FEATURE_SIZE * bucket][j];
				}
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

	public static int chooseOutputBucket(Board board)
	{
		return (Long.bitCount(board.getBitboard()) - 2) / DIVISOR;
	}

	public static int evaluate(NNUE network, AccumulatorStack accumulators, Side side, int chosenBucket)
	{
		return INFERENCE.forward(accumulators, side, network.L2Weights[chosenBucket],
				network.outputBiases[chosenBucket]);
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

	private static int flipIndex(final int index)
	{
		return (index / 64) * 64 + (index % 64) ^ 0b111;
	}
}
