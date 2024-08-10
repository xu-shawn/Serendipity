package org.shawn.games.Serendipity.UCI;

import org.shawn.games.Serendipity.Search.AlphaBeta;
import org.shawn.games.Serendipity.Search.Listeners.FinalReport;
import org.shawn.games.Serendipity.Search.Listeners.ISearchListener;
import org.shawn.games.Serendipity.Search.Listeners.SearchReport;

import com.github.bhlangonijr.chesslib.move.Move;

public class UCIListener implements ISearchListener
{
	@Override
	public void notify(SearchReport report)
	{
		StringBuffer pvString = new StringBuffer();

		for (Move move : report.pv)
		{
			if (move == null)
			{
				break;
			}

			pvString.append(" " + move.toString());
		}

		if (Math.abs(report.score) < AlphaBeta.MATE_EVAL - AlphaBeta.MAX_PLY)
		{
			int cp = WDLModel.normalizeEval(report.score, report.board);
			int[] wdl = WDLModel.calculateWDL(report.score, report.board);

			System.out.printf(
					"info depth %d seldepth %d nodes %d nps %d hashfull %d score cp %d wdl %d %d %d time %d pv%s\n",
					report.depth, report.selDepth, report.nodes, report.nodes * 1000L / Math.max(1, report.ms),
					report.hashfull, cp, wdl[0], wdl[1], wdl[2], report.ms, pvString.toString());
		}

		else
		{
			int mateInPly = AlphaBeta.MATE_EVAL - Math.abs(report.score);
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
					report.depth, report.selDepth, report.nodes, report.nodes * 1000L / Math.max(1, report.ms),
					report.hashfull, mateValue, wdl[0], wdl[1], wdl[2], report.ms, pvString);
		}
	}

	@Override
	public void notify(FinalReport report)
	{
		System.out.println("bestmove " + (report.bestMove == null ? "(none)" : report.bestMove));
	}
}
