package org.shawn.games.Serendipity.NNUE;

import jdk.incubator.vector.ShortVector;

public class InferenceChooser
{
	public static Inference chooseInference()
	{
		if (ShortVector.SPECIES_PREFERRED.vectorBitSize() >= 16)
		{
			return new SIMDInference();
		}

		return new ScalarInference();
	}
}
