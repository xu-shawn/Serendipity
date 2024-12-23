package org.shawn.games.Serendipity.NNUE;

import jdk.incubator.vector.ShortVector;

public class InferenceChooser
{
	public static Inference chooseInference()
	{
		return new ScalarInference();
	}
}
