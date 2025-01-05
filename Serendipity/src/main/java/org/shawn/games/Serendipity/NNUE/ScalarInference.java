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

import com.github.bhlangonijr.chesslib.Side;

public class ScalarInference implements Inference
{
	private final static int[] screlu = new int[Short.MAX_VALUE - Short.MIN_VALUE + 1];

	static
	{
		for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++)
		{
			screlu[i - (int) Short.MIN_VALUE] = screlu((short) (i));
		}
	}

	private static int screlu(short i)
	{
		int v = Math.max(0, Math.min(i, NNUE.QA));
		return v * v;
	}

	@Override
	public int forward(AccumulatorStack accumulators, Side side, short[] weights, short bias)
	{
		int eval = 0;

		AccumulatorStack.Accumulator us = accumulators.getAccumulator(side);
		AccumulatorStack.Accumulator them = accumulators.getAccumulator(side.flip());

		for (int i = 0; i < NNUE.HIDDEN_SIZE; i++)
		{
			eval += screlu[us.values[i] - (int) Short.MIN_VALUE] * (int) weights[i]
					+ screlu[them.values[i] - (int) Short.MIN_VALUE] * (int) weights[i + NNUE.HIDDEN_SIZE];
		}

		eval /= NNUE.QA;
		eval += bias;

		eval *= NNUE.SCALE;
		eval /= NNUE.QA * NNUE.QB;

		return eval;
	}
}
