/*
  This file is part of Serendipity, an UCI chess engine written in Java.

  Copyright (C) 2024-2025  Shawn Xu <shawn@shawnxu.org>

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.shawn.games.Serendipity.NNUE;

import static jdk.incubator.vector.VectorOperators.S2I;

import com.github.bhlangonijr.chesslib.Side;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class SIMDInference implements Inference
{
	private static final VectorSpecies<Short> SHORT_SPECIES = ShortVector.SPECIES_PREFERRED;
	private static final int UPPERBOUND = SHORT_SPECIES.loopBound(NNUE.HIDDEN_SIZE);

	@Override
	public int forward(AccumulatorStack accumulators, Side side, short[] weights, short bias)
	{
		AccumulatorStack.Accumulator us = accumulators.getAccumulator(side);
		AccumulatorStack.Accumulator them = accumulators.getAccumulator(side.flip());

		IntVector sum = IntVector.zero(SHORT_SPECIES.vectorShape().withLanes(int.class));

		for (int i = 0; i < UPPERBOUND; i += SHORT_SPECIES.length())
		{
			ShortVector usInputs = ShortVector.fromArray(SHORT_SPECIES, us.values, i);
			ShortVector themInputs = ShortVector.fromArray(SHORT_SPECIES, them.values, i);
			ShortVector usWeights = ShortVector.fromArray(SHORT_SPECIES, weights, i);
			ShortVector themWeights = ShortVector.fromArray(SHORT_SPECIES, weights, i + NNUE.HIDDEN_SIZE);

			usInputs = usInputs.max(ShortVector.zero(SHORT_SPECIES)).min(ShortVector.broadcast(SHORT_SPECIES, NNUE.QA));
			themInputs = themInputs.max(ShortVector.zero(SHORT_SPECIES))
					.min(ShortVector.broadcast(SHORT_SPECIES, NNUE.QA));

			ShortVector usWeightedTerms = usInputs.mul(usWeights);
			ShortVector themWeightedTerms = themInputs.mul(themWeights);

			Vector<Integer> usInputsLo = usInputs.convert(S2I, 0);
			Vector<Integer> usInputsHi = usInputs.convert(S2I, 1);
			Vector<Integer> themInputsLo = themInputs.convert(S2I, 0);
			Vector<Integer> themInputsHi = themInputs.convert(S2I, 1);

			Vector<Integer> usWeightedTermsLo = usWeightedTerms.convert(S2I, 0);
			Vector<Integer> usWeightedTermsHi = usWeightedTerms.convert(S2I, 1);
			Vector<Integer> themWeightedTermsLo = themWeightedTerms.convert(S2I, 0);
			Vector<Integer> themWeightedTermsHi = themWeightedTerms.convert(S2I, 1);

			sum = sum.add(usInputsLo.mul(usWeightedTermsLo)).add(usInputsHi.mul(usWeightedTermsHi))
					.add(themInputsLo.mul(themWeightedTermsLo)).add(themInputsHi.mul(themWeightedTermsHi));
		}

		int eval = sum.reduceLanes(VectorOperators.ADD);

		eval /= NNUE.QA;
		eval += bias;

		eval *= NNUE.SCALE;
		eval /= NNUE.QA * NNUE.QB;

		return eval;
	}

}
