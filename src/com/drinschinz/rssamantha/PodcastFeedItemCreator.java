/*
 *  RSSamantha is a rss/atom feedaggregator.
 *  Copyright (C) 2011-2015  David Schröer <tengcomplexATgmail.com>
 *
 *
 *  This file is part of RSSamantha.
 *
 *  RSSamantha is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RSSamantha is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RSSamantha.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.drinschinz.rssamantha;

import java.util.logging.Level;

/**
 *
 * @author teng
 */
public class PodcastFeedItemCreator extends ItemCreator
{
    private String url;
    private PodcastChannelReader reader;
    /** Mandatory limit, default 0 which ends up in leeching just the last item */
    private int addLimit;

    public PodcastFeedItemCreator(final Control c, final String name)
    {
        super(c, name);
    }

    public PodcastFeedItemCreator(final Control c, final String url, final String name)
    {
        this(c, name);
        this.url = url;
        reader = new PodcastChannelReader(url, name);
        Control.L.log(Level.FINE, "Created {0} url:{1} name:{2}", new Object[]{this.getClass().getName(), url, name});
    }

    public void setAddLimit(final int i)
    {
        this.addLimit = i;
    }

    @Override
    public String toString()
    {
        return super.toString()+" url:"+url;
    }

    @Override
    public void setType(final ItemCreatorType type)
    {
        super.setType(type);
        reader.setType(type);
    }

    @Override
    protected void clearItems()
    {
        super.clearItems();
        reader.getItems().clear();
    }

    @Override
    public boolean read()
    {
        int count = 0;
        if(reader.read())
        {
            reader.sortItems();
            for(Item i : reader.getItems())
            {
//System.out.println("Podcastitem: "+i);
                i.setIndex(channelindex);
                i.setTitle(getNewTitle(i));
                final Control.CountEvent b = control.addDownloadItem(i, channelindex);
                if(Control.L.isLoggable(Level.FINE))
                {
                    Control.L.log(Level.FINE, "Returnvalue:{0} Channel:{1} Item:{2}", new Object[]{b.toString(), control.getChannelDataToShortString(channelindex), i.toShortString()});
                }
                count++;
                if(count >= addLimit)
                {
                    break;
                }
            }
            return true;
        }
        return false;
    }
}
