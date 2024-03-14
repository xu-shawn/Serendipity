package org.shawn.games.Serendipity.NNUE;

import java.io.*;

import com.github.bhlangonijr.chesslib.*;

public class NNUE
{
	private static final int COLOR_STRIDE = 64 * 6;
	private static final int PIECE_STRIDE = 64;

	private static final int HIDDEN_SIZE = 768;
	private static final int FEATURE_SIZE = 768;
	private static final int OUTPUT_BUCKET_SIZE = 8;
	private static final int BUCKET_DIVISOR = (32 + OUTPUT_BUCKET_SIZE - 1) / OUTPUT_BUCKET_SIZE;

	private static final int SCALE = 400;
	private static final int QA = 255;
	private static final int QB = 64;

	private final short[][] L1Weights;
	private final short[] L1Biases;
	private final short[][] L2Weights;
	private final short[] outputBiases;

	public static class NNUEAccumulator
	{
		private final short[] values;

		public NNUEAccumulator(NNUE network)
		{
			values = network.L1Biases.clone();
		}

		public void addFeature(int featureIndex, NNUE network)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.L1Weights[featureIndex][i];
			}
		}

		public void subtractFeature(int featureIndex, NNUE network)
		{
			for (int i = 0; i < HIDDEN_SIZE; i++)
			{
				values[i] -= network.L1Weights[featureIndex][i];
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

		L1Weights = new short[FEATURE_SIZE][HIDDEN_SIZE];

		for (int i = 0; i < FEATURE_SIZE; i++)
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

		L2Weights = new short[HIDDEN_SIZE * 2][OUTPUT_BUCKET_SIZE];

		for (int i = 0; i < HIDDEN_SIZE * 2; i++)
		{
			for (int j = 0; j < OUTPUT_BUCKET_SIZE; j++)
			{
				L2Weights[i][j] = toLittleEndian(networkData.readShort());
			}
		}

		outputBiases = new short[OUTPUT_BUCKET_SIZE];

		for (int i = 0; i < OUTPUT_BUCKET_SIZE; i++)
		{
			outputBiases[i] = toLittleEndian(networkData.readShort());
		}

		networkData.close();
	}

	private static int crelu(short i)
	{
		return Math.max(0, Math.min(i, QA));
	}

	public static int evaluate(NNUE network, NNUEAccumulator us, NNUEAccumulator them, Board board)
	{
		int bucket = (Long.bitCount(board.getBitboard()) - 2) / BUCKET_DIVISOR;
		int eval = network.outputBiases[bucket];

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			eval += crelu(us.values[i]) * (int) network.L2Weights[i][bucket]
					+ crelu(them.values[i]) * (int) network.L2Weights[i + HIDDEN_SIZE][bucket];
		}

		eval *= SCALE;
		eval /= QA * QB;

		return eval;
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
