package org.shawn.games.Serendipity.Search.Listeners;

public interface ISearchListener
{
	public void notify(SearchReport report);
	public void notify(FinalReport report);
}
