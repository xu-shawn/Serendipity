package org.shawn.games.Serendipity;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.shawn.games.Serendipity.NNUE.NNUE;
import org.shawn.games.Serendipity.NNUE.NNUE.NNUEAccumulator;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class UCI
{
	private static Board internalBoard;
	private static Map<String, UCIOption> options;
	private static AlphaBeta engine;
	private static TranspositionTable transpositionTable;
	private static NNUE network;

	private static StringOption networkName;
	private static IntegerOption threads;
	private static HashOption hash;

	public static class NNUEOption extends StringOption
	{
		public NNUEOption(String value, String name)
		{
			super(value, name);
		}

		@Override
		public void set(String value)
		{
			try
			{
				network = new NNUE("/" + value);
				engine = new AlphaBeta(transpositionTable, network);
			}

			catch (IOException e)
			{
				System.out.print("Error Loading NNUE");
				return;
			}

			this.value = value;
		}
	}

	public static class HashOption extends IntegerOption
	{
		private TranspositionTable tt;

		public HashOption(int value, int lowerBound, int upperBound, TranspositionTable tt, String name)
		{
			super(value, lowerBound, upperBound, name);
			this.tt = tt;
		}

		@Override
		public void set(String value)
		{
			super.set(value);
			tt.resize(super.value);
		}
	}

	public static void main(String args[])
	{
		transpositionTable = new TranspositionTable(32);
		options = new HashMap<>();
		networkName = new NNUEOption("simple.nnue", "nnuefile");
		threads = new IntegerOption(1, 1, 1, "Threads");
		hash = new HashOption(32, 8, 4096, transpositionTable, "Hash");

		try
		{
			network = new NNUE("/" + networkName.get());
		}

		catch (IOException e)
		{
			System.out.print("Error Loading NNUE");
		}

		internalBoard = new Board();
		engine = new AlphaBeta(transpositionTable, network);

		if (args.length == 1 && args[0].equals("bench"))
		{
			Bench.bench(engine, 10, false, true);
			return;
		}

		UCIMainLoop();
	}

	public static void addOption(String name, UCIOption option)
	{
		options.put(name, option);
	}

	public static IntegerOption getOption(String name)
	{
		return (IntegerOption) options.get(name);
	}

	public static void setOption(String name, String value)
	{
		UCIOption myOption = options.get(name);
		myOption.set(value);
	}

	public static void report(int depth, int selDepth, int nodes, int hashfull, int score, long ms, Board board,
			Move[] pv)
	{
		String pvString = String.join(" ",
				Arrays.stream(pv).takeWhile(x -> x != null).map(Object::toString).collect(Collectors.toList()));

		if (Math.abs(score) < AlphaBeta.MATE_EVAL - AlphaBeta.MAX_PLY)
		{
			int cp = WDLModel.normalizeEval(score, board);
			int[] wdl = WDLModel.calculateWDL(score, board);

			System.out.printf(
					"info depth %d seldepth %d nodes %d nps %d hashfull %d score cp %d wdl %d %d %d time %d pv %s\n",
					depth, selDepth, nodes, nodes * 1000L / Math.max(1, ms), hashfull, cp, wdl[0], wdl[1], wdl[2], ms,
					pvString);
		}

		else
		{
			int mateInPly = AlphaBeta.MATE_EVAL - Math.abs(score);
			int mateValue = mateInPly % 2 != 0 ? (mateInPly + 1) / 2 : -mateInPly / 2;
			int[] wdl;

			if (mateValue < 0)
			{
				wdl = new int[] { 0, 0, 1000 };
			}
			else
			{
				wdl = new int[] { 1000, 0, 0 };
			}

			System.out.printf(
					"info depth %d seldepth %d nodes %d nps %d hashfull %d score mate %d wdl %d %d %d time %d pv %s\n",
					depth, selDepth, nodes, nodes * 1000L / Math.max(1, ms), hashfull, mateValue, wdl[0], wdl[1],
					wdl[2], ms, pvString);
		}
	}

	public static void reportBestMove(Move bestMove)
	{
		System.out.println("bestmove " + (bestMove == null ? "(none)" : bestMove));
	}

	private static long perft(Board board, int depth, int ply, boolean root)
	{
		if (depth == 0)
		{
			return 1;
		}

		long nodes = 0;
		List<Move> moves = board.legalMoves();

		for (Move move : moves)
		{
			board.doMove(move);

			if (root)
			{
				long thisMoveNodes;
				thisMoveNodes = perft(board, depth - 1, ply + 1, false);
				System.out.println(move + ":\t" + thisMoveNodes);
				nodes += thisMoveNodes;
			}

			else
			{
				nodes += perft(board, depth - 1, ply + 1, false);
			}

			board.undoMove();
		}

		return nodes;
	}

	public static void UCIMainLoop()
	{
		Scanner input = new Scanner(System.in);
		int depth;
		while (input.hasNextLine())
		{
			String command = input.nextLine();
			String[] fullCommand = command.split(" ");

			switch (command.split(" ")[0])
			{
				case "isready":
					System.out.println("readyok");
					break;
				case "uci":
					System.out.println("id name Serendipity");
					System.out.println("id author Shawn Xu");
					for (var option : options.values())
					{
						System.out.println(option);
					}
					System.out.println("uciok");
					break;
				case "ucinewgame":
					System.gc();
					internalBoard = new Board();
					transpositionTable.clear();
					engine.reset();
					break;
				case "quit":
					input.close();
					return;
				case "perft":
					if (fullCommand.length >= 2)
					{
						depth = Integer.parseInt(fullCommand[1]);
					}
					else
					{
						depth = 4;
					}
					long timeBegin = System.nanoTime();
					long totalNodes = perft(internalBoard, depth, 0, true);
					long timeEnd = System.nanoTime();
					System.out.println("\nNodes searched:\t\t" + totalNodes);
					System.out.println("Time spent (ms):\t" + (timeEnd - timeBegin) / 1000000);
					System.out.println(
							"Nodes per second:\t" + totalNodes * 1000 / Math.max((timeEnd - timeBegin) / 1000000, 1));
					break;
				case "eval":
					NNUEAccumulator whiteAccumulator = new NNUEAccumulator(network,
							NNUE.chooseInputBucket(internalBoard, Side.WHITE));
					NNUEAccumulator blackAccumulator = new NNUEAccumulator(network,
							NNUE.chooseInputBucket(internalBoard, Side.BLACK));

					// Initialize Accumulators
					for (Square sq : Square.values())
					{
						if (!internalBoard.getPiece(sq).equals(Piece.NONE))
						{
							whiteAccumulator.add(NNUE.getIndex(sq, internalBoard.getPiece(sq), Side.WHITE));
							blackAccumulator.add(NNUE.getIndex(sq, internalBoard.getPiece(sq), Side.BLACK));
						}
					}

					System.out.println(internalBoard);
					System.out.println(Side.WHITE.equals(internalBoard.getSideToMove())
							? NNUE.evaluate(network, whiteAccumulator, blackAccumulator,
									NNUE.chooseOutputBucket(internalBoard))
							: NNUE.evaluate(network, blackAccumulator, whiteAccumulator,
									NNUE.chooseOutputBucket(internalBoard)));
					break;
				case "go":
					Limits limits = new Limits();

					for (int i = 1; i < fullCommand.length; i++)
					{
						if (fullCommand[i].equals("infinite"))
						{
							limits.setTime(157680000000L);
							break;
						}

						if ((fullCommand[i].equals("wtime") && Side.WHITE.equals(internalBoard.getSideToMove()))
								|| (fullCommand[i].equals("btime") && Side.BLACK.equals(internalBoard.getSideToMove())))
						{
							limits.setTime(Long.parseLong(fullCommand[i + 1]));
						}

						if ((fullCommand[i].equals("winc") && Side.WHITE.equals(internalBoard.getSideToMove()))
								|| (fullCommand[i].equals("binc") && Side.BLACK.equals(internalBoard.getSideToMove())))
						{
							limits.setIncrement(Long.parseLong(fullCommand[i + 1]));
						}

						if (fullCommand[i].equals("depth"))
						{
							limits.setDepth(Integer.parseInt(fullCommand[i + 1]));
							limits.setTime(157680000000L);
							break;
						}

						if (fullCommand[i].equals("nodes"))
						{
							limits.setNodes(Integer.parseInt(fullCommand[i + 1]));
							break;
						}

						if (fullCommand[i].equals("movetime"))
						{
							limits.setTime(Long.parseLong(fullCommand[i + 1]));
							limits.setIncrement(0);
							limits.setMovesToGo(-1);
						}

						if (fullCommand[i].equals("movestogo"))
						{
							limits.setMovesToGo(Integer.parseInt(fullCommand[i + 1]));
						}
					}

					engine.nextMove(internalBoard.clone(), limits);
					break;
				case "position":
					for (int i = 1; i < fullCommand.length; i++)
					{
						if (fullCommand[i].equals("startpos"))
						{
							internalBoard = new Board();
						}
						if (fullCommand[i].equals("fen"))
						{
							internalBoard = new Board();
							internalBoard.loadFromFen(
									fullCommand[i + 1] + " " + fullCommand[i + 2] + " " + fullCommand[i + 3] + " "
											+ fullCommand[i + 4] + " " + fullCommand[i + 5] + " " + fullCommand[i + 6]);
						}
						if (fullCommand[i].equals("moves"))
						{
							for (int j = i + 1; j < fullCommand.length; j++)
							{
								internalBoard.doMove(fullCommand[j]);
							}
							break;
						}
					}
					break;
				case "option":
					for (var option : options.values())
					{
						System.out.println(option);
					}
					break;
				case "setoption":
					String key = "";
					for (int i = 1; i < fullCommand.length; i++)
					{
						if (fullCommand[i].equals("name"))
						{
							key = fullCommand[i + 1];
						}
						if (fullCommand[i].equals("value"))
						{
							options.get(key).set(fullCommand[i + 1]);
						}
					}
					break;
				case "showvalues":
					for (var option : options.values())
					{
						System.out.println(option + " value " + option.getString());
					}
					break;
				case "bench":
					depth = 10;
					if (fullCommand.length > 1)
					{
						depth = Integer.parseInt(fullCommand[1]);
					}
					Bench.bench(engine, depth);
					transpositionTable.clear();
					break;
				case "benches":
					int iterations = Integer.parseInt(fullCommand[1]);
					depth = 10;
					if (fullCommand.length > 2)
					{
						depth = Integer.parseInt(fullCommand[2]);
					}
					Bench.benchMultiple(engine, depth, iterations);
					break;
			}
		}

		input.close();
	}
}
