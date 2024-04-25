package org.shawn.games.Serendipity.NNUE;

import java.io.*;

import jdk.incubator.vector.*;

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

	private final short[][] featureTransformerWeights;
	private final short[] featureTransformerBiases;
	private final int[][] L1Weights;
	private final short outputBiases[];

	private final static int screlu[] = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];

	private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;

	static
	{
		for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++)
		{
			screlu[i - (int) Short.MIN_VALUE] = screlu((short) (i));
		}
	}

	public static class NNUEAccumulator
	{
		private short[] values;
		private int bucketIndex;
		NNUE network;

		private static final VectorSpecies<Short> SPECIES = ShortVector.SPECIES_PREFERRED;

		public NNUEAccumulator(NNUE network, int bucketIndex)
		{
			this.network = network;
			this.bucketIndex = bucketIndex;
			values = network.featureTransformerBiases.clone();
		}

		public void reset()
		{
			values = network.featureTransformerBiases.clone();
		}

		public void setBucketIndex(int bucketIndex)
		{
			this.bucketIndex = bucketIndex;
		}

		public void add(int featureIndex)
		{
			int upperBound = SPECIES.loopBound(HIDDEN_SIZE);

			var i = 0;
			for (; i < upperBound; i += SPECIES.length())
			{
				var va = ShortVector.fromArray(SPECIES, values, i);
				var vb = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndex + bucketIndex * FEATURE_SIZE], i);
				va = va.add(vb);
				va.intoArray(values, i);
			}

			// Compute elements not fitting in the vector alignment.
			for (; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.featureTransformerWeights[featureIndex + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void sub(int featureIndex)
		{
			int upperBound = SPECIES.loopBound(HIDDEN_SIZE);

			var i = 0;
			for (; i < upperBound; i += SPECIES.length())
			{
				var va = ShortVector.fromArray(SPECIES, values, i);
				var vb = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndex + bucketIndex * FEATURE_SIZE], i);
				va = va.sub(vb);
				va.intoArray(values, i);
			}

			// Compute elements not fitting in the vector alignment.
			for (; i < HIDDEN_SIZE; i++)
			{
				values[i] -= network.featureTransformerWeights[featureIndex + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addsub(int featureIndexToAdd, int featureIndexToSubtract)
		{
			int upperBound = SPECIES.loopBound(HIDDEN_SIZE);

			var i = 0;
			for (; i < upperBound; i += SPECIES.length())
			{
				var va = ShortVector.fromArray(SPECIES, values, i);
				var vb = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToAdd + bucketIndex * FEATURE_SIZE], i);
				var vc = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE], i);
				va = va.add(vb).sub(vc);
				va.intoArray(values, i);
			}

			// Compute elements not fitting in the vector alignment.
			for (; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.featureTransformerWeights[featureIndexToAdd + bucketIndex * FEATURE_SIZE][i]
						- network.featureTransformerWeights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addaddsub(int featureIndexToAdd1, int featureIndexToAdd2, int featureIndexToSubtract)
		{
			int upperBound = SPECIES.loopBound(HIDDEN_SIZE);

			var i = 0;
			for (; i < upperBound; i += SPECIES.length())
			{
				var va = ShortVector.fromArray(SPECIES, values, i);
				var vb = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToAdd1 + bucketIndex * FEATURE_SIZE], i);
				var vc = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToAdd2 + bucketIndex * FEATURE_SIZE], i);
				var vd = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE], i);
				va = va.add(vb).add(vc).sub(vd);
				va.intoArray(values, i);
			}

			// Compute elements not fitting in the vector alignment.
			for (; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.featureTransformerWeights[featureIndexToAdd1 + bucketIndex * FEATURE_SIZE][i]
						+ network.featureTransformerWeights[featureIndexToAdd2 + bucketIndex * FEATURE_SIZE][i]
						- network.featureTransformerWeights[featureIndexToSubtract + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addsubsub(int featureIndexToAdd, int featureIndexToSubtract1, int featureIndexToSubtract2)
		{
			int upperBound = SPECIES.loopBound(HIDDEN_SIZE);

			var i = 0;
			for (; i < upperBound; i += SPECIES.length())
			{
				var va = ShortVector.fromArray(SPECIES, values, i);
				var vb = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToAdd + bucketIndex * FEATURE_SIZE], i);
				var vc = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToSubtract1 + bucketIndex * FEATURE_SIZE], i);
				var vd = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToSubtract2 + bucketIndex * FEATURE_SIZE], i);
				va = va.add(vb).sub(vc).sub(vd);
				va.intoArray(values, i);
			}

			// Compute elements not fitting in the vector alignment.
			for (; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.featureTransformerWeights[featureIndexToAdd + bucketIndex * FEATURE_SIZE][i]
						- network.featureTransformerWeights[featureIndexToSubtract1 + bucketIndex * FEATURE_SIZE][i]
						- network.featureTransformerWeights[featureIndexToSubtract2 + bucketIndex * FEATURE_SIZE][i];
			}
		}

		public void addaddsubsub(int featureIndexToAdd1, int featureIndexToAdd2, int featureIndexToSubtract1,
				int featureIndexToSubtract2)
		{
			int upperBound = SPECIES.loopBound(HIDDEN_SIZE);

			var i = 0;
			for (; i < upperBound; i += SPECIES.length())
			{
				var va = ShortVector.fromArray(SPECIES, values, i);
				var vb = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToAdd1 + bucketIndex * FEATURE_SIZE], i);
				var vc = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToAdd2 + bucketIndex * FEATURE_SIZE], i);
				var vd = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToSubtract1 + bucketIndex * FEATURE_SIZE], i);
				var ve = ShortVector.fromArray(SPECIES,
						network.featureTransformerWeights[featureIndexToSubtract2 + bucketIndex * FEATURE_SIZE], i);
				va = va.add(vb).add(vc).sub(vd).sub(ve);
				va.intoArray(values, i);
			}

			// Compute elements not fitting in the vector alignment.
			for (; i < HIDDEN_SIZE; i++)
			{
				values[i] += network.featureTransformerWeights[featureIndexToAdd1 + bucketIndex * FEATURE_SIZE][i]
						+ network.featureTransformerWeights[featureIndexToAdd2 + bucketIndex * FEATURE_SIZE][i]
						- network.featureTransformerWeights[featureIndexToSubtract1 + bucketIndex * FEATURE_SIZE][i]
						- network.featureTransformerWeights[featureIndexToSubtract2 + bucketIndex * FEATURE_SIZE][i];
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

		featureTransformerWeights = new short[FEATURE_SIZE * INPUT_BUCKET_SIZE][HIDDEN_SIZE];

		for (int i = 0; i < FEATURE_SIZE * INPUT_BUCKET_SIZE; i++)
		{
			for (int j = 0; j < HIDDEN_SIZE; j++)
			{
				featureTransformerWeights[i][j] = toLittleEndian(networkData.readShort());
			}
		}

		featureTransformerBiases = new short[HIDDEN_SIZE];

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			featureTransformerBiases[i] = toLittleEndian(networkData.readShort());
		}

		L1Weights = new int[OUTPUT_BUCKETS][HIDDEN_SIZE * 2];

		for (int i = 0; i < HIDDEN_SIZE * 2; i++)
		{
			for (int j = 0; j < OUTPUT_BUCKETS; j++)
			{
				L1Weights[j][i] = toLittleEndian(networkData.readShort());
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
		int[] usValues = new int[HIDDEN_SIZE];
		int[] themValues = new int[HIDDEN_SIZE];

		for (int i = 0; i < HIDDEN_SIZE; i++)
		{
			usValues[i] = (int) us.values[i];
			themValues[i] = (int) them.values[i];
		}

		int i = 0;
		int upperBound = INT_SPECIES.loopBound(HIDDEN_SIZE);

		IntVector sum = IntVector.zero(INT_SPECIES);

		for (; i < upperBound; i += INT_SPECIES.length())
		{
			IntVector va = IntVector.fromArray(INT_SPECIES, usValues, i);
			IntVector vb = IntVector.fromArray(INT_SPECIES, themValues, i);
			IntVector vc = IntVector.fromArray(INT_SPECIES, network.L1Weights[chosenBucket], i);
			IntVector vd = IntVector.fromArray(INT_SPECIES, network.L1Weights[chosenBucket], i + HIDDEN_SIZE);

			va = va.max(0).min(QA);
			va = va.mul(va).mul(vc);

			vb = vb.max(0).min(QA);
			vb = vb.mul(vb).mul(vd);

			sum = sum.add(va).add(vb);
		}

		int eval = sum.reduceLanes(VectorOperators.ADD);

		for (; i < HIDDEN_SIZE; i++)
		{
			eval += screlu[usValues[i] - (int) Short.MIN_VALUE] * network.L1Weights[chosenBucket][i]
					+ screlu[themValues[i] - (int) Short.MIN_VALUE] * network.L1Weights[chosenBucket][i + HIDDEN_SIZE];
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
