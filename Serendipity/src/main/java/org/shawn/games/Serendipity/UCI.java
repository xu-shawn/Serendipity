package org.shawn.games.Serendipity;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.shawn.games.Serendipity.NNUE.NNUE;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;

public class UCI
{
	private static Board internalBoard;
	private static Map<String, UCIOption> options;
	private static AlphaBeta engine;
	private static StringOption networkName;
	private static NNUE network;

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
				engine = new AlphaBeta(network);
			}

			catch (IOException e)
			{
				System.out.print("Error Loading NNUE");
				return;
			}

			this.value = value;
		}
	}

	public static void main(String args[])
	{
		options = new HashMap<>();
		networkName = new NNUEOption("simple.nnue", "nnuefile");

		try
		{
			network = new NNUE("/" + networkName.get());
		}

		catch (IOException e)
		{
			System.out.print("Error Loading NNUE");
		}

		internalBoard = new Board();
		engine = new AlphaBeta(network);

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

	public static void report(int depth, int selDepth, int nodes, int score, long ms, Move[] pv)
	{
		if (Math.abs(score) < AlphaBeta.MATE_EVAL - AlphaBeta.MAX_PLY)
		{
			System.out.printf("info depth %d seldepth %d nodes %d nps %d score cp %d time %d pv %s\n", depth, selDepth,
					nodes, nodes * 1000L / Math.max(1, ms), score / PeSTO.MAX_PHASE, ms, String.join(" ", Arrays
							.stream(pv).takeWhile(x -> x != null).map(Object::toString).collect(Collectors.toList())));
		}

		else
		{
			int mateInPly = AlphaBeta.MATE_EVAL - Math.abs(score);
			System.out.printf("info depth %d seldepth %d nodes %d nps %d score mate %d time %d pv %s\n", depth,
					selDepth, nodes, nodes * 1000L / Math.max(1, ms),
					mateInPly % 2 != 0 ? (mateInPly + 1) / 2 : -mateInPly / 2, ms, String.join(" ", Arrays.stream(pv)
							.takeWhile(x -> x != null).map(Object::toString).collect(Collectors.toList())));
		}
	}

	public static void reportBestMove(Move bestMove)
	{
		System.out.println("bestmove " + (bestMove == null ? "(none)" : bestMove));
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
					System.out.println("id name weakEngine");
					System.out.println("id author me");
					for (var option : options.values())
					{
						System.out.println(option);
					}
					System.out.println("uciok");
					break;
				case "ucinewgame":
					internalBoard = new Board();
					engine = new AlphaBeta(network);
					break;
				case "quit":
					input.close();
					return;
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
							limits.setMovesToGo(1);
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
