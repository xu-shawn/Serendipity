package org.shawn.games.Serendipity.NNUE;

import java.io.*;

import com.github.bhlangonijr.chesslib.*;

public class NNUE
{
	private static final int COLOR_STRIDE = 64 * 6;
	private static final int PIECE_STRIDE = 64;

	private static final int HIDDEN_SIZE = 1536;
	private static final int FEATURE_SIZE = 768;
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

	private final short[][] L1Weights;
	private final short[] L1Biases;
	private final short[][] L2Weights;
	private final short outputBiases[];

	public static class NNUEAccumulator
	{
		private short[] values;
		private int bucketIndex;
		NNUE network;

		public NNUEAccumulator(NNUE network, int bucketIndex)
		{
			this.network = network;
			this.bucketIndex = bucketIndex;
			values = network.L1Biases.clone();
		}

		public void reset()
		{
			values = network.L1Biases.clone();
		}

		public void setBucketIndex(int bucketIndex)
		{
			this.bucketIndex = bucketIndex;
		}

		public void add(int featureIndex)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndex + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void sub(int featureIndex)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] -= network.L1Weights[featureIndex + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addsub(int featureIndexToAdd, int featureIndexToSubtract)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addaddsub(int featureIndexToAdd1, int featureIndexToAdd2, int featureIndexToSubtract)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd1 + bucketIndex * FEATURE_SIZE][i]
						+ network.L1Weights[featureIndexToAdd2 + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addsubsub(int featureIndexToAdd, int featureIndexToSubtract1, int featureIndexToSubtract2)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract1 + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract2 + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addaddsubsub(int featureIndexToAdd1, int featureIndexToAdd2, int featureIndexToSubtract1,
				int featureIndexToSubtract2)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndexToAdd1 + bucketIndex * FEATURE_SIZE][i]
						+ network.L1Weights[featureIndexToAdd2 + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract1 + bucketIndex * FEATURE_SIZE][i]
						- network.L1Weights[featureIndexToSubtract2 + bucketIndex * FEATURE_SIZE][i];
			}
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

		L2Weights = new short[HIDDEN_SIZE * 2][OUTPUT_BUCKETS];

		for (int i = 0; i < HIDDEN_SIZE * 2; i++)
		{
			for (int j = 0; j < OUTPUT_BUCKETS; j++)
			{
				L2Weights[i][j] = toLittleEndian(networkData.readShort());
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

	public static int evaluate(NNUE network, NNUEAccumulator us, NNUEAccumulator them, int chosenBucket)
	{
		int eval = 0;

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			eval += screlu(us.values[i]) * (int) network.L2Weights[i][chosenBucket]
					+ screlu(them.values[i]) * (int) network.L2Weights[i + HIDDEN_SIZE][chosenBucket];
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

	public static int chooseInputBucket(int squareIndex)
	{
		return INPUT_BUCKETS[squareIndex];
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
