package org.shawn.games.Serendipity.Search.Listener;

public interface ISearchListener {
    public void notify(SearchReport report);

    public void notify(FinalReport report);
}
