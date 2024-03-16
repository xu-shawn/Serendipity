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
	private static NNUE network;

	public static void main(String args[])
	{
		try
		{
			network = new NNUE("/simple.nnue");
		}
		
		catch (IOException e)
		{
			System.out.print("Error Loading NNUE");
		}
		
		internalBoard = new Board();
		options = new HashMap<>();
		engine = new AlphaBeta(network);
		
		UCIMainLoop();
	}

	public static void addOption(String name, IntegerOption option)
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
		System.out.printf("info depth %d seldepth %d nodes %d score cp %d time %d pv %s\n", depth, selDepth, nodes, score,
				ms, String.join(" ", Arrays.stream(pv).takeWhile(x -> x != null)
						.map(Object::toString).collect(Collectors.toList())));
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
					long wtime = 100;
					long btime = 100;
					long winc = 0;
					long binc = 0;
					int nodesLimit = -1;
					depth = 256;

					for (int i = 1; i < fullCommand.length; i++)
					{
						if (fullCommand[i].equals("infinite"))
						{
							wtime = btime = 157680000000L;
							break;
						}
						if (fullCommand[i].equals("wtime"))
						{
							wtime = Long.parseLong(fullCommand[i + 1]);
						}
						if (fullCommand[i].equals("btime"))
						{
							btime = Long.parseLong(fullCommand[i + 1]);
						}
						if (fullCommand[i].equals("winc"))
						{
							winc = Long.parseLong(fullCommand[i + 1]);
						}
						if (fullCommand[i].equals("winc"))
						{
							binc = Long.parseLong(fullCommand[i + 1]);
						}
						if (fullCommand[i].equals("depth"))
						{
							depth = Integer.parseInt(fullCommand[i + 1]);
							wtime = btime = 157680000000L;
							break;
						}
						if (fullCommand[i].equals("nodes"))
						{
							nodesLimit = Integer.parseInt(fullCommand[i + 1]);
							wtime = btime = 157680000000L;
							break;
						}
						if(fullCommand[i].equals("movetime"))
						{
							wtime = btime = Long.parseLong(fullCommand[i + 1]) * 20;
							winc = binc = 0;
						}
					}

					long timeGiven = internalBoard.getSideToMove() == Side.WHITE
							? wtime / 20 + winc / 2
							: btime / 20 + binc / 2;

					engine.nextMove(internalBoard.clone(), depth, timeGiven - 100, nodesLimit);
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
							internalBoard.loadFromFen(fullCommand[i + 1] + " " + fullCommand[i + 2]
									+ " " + fullCommand[i + 3] + " " + fullCommand[i + 4] + " "
									+ fullCommand[i + 5] + " " + fullCommand[i + 6]);
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
					if(fullCommand.length > 1)
					{
						depth = Integer.parseInt(fullCommand[1]);
					}
					Bench.bench(engine, depth);
					break;
				case "benches":
					int iterations = Integer.parseInt(fullCommand[1]);
					depth = 10;
					if(fullCommand.length > 2)
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
