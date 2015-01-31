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
public class AtomFeedItemCreator extends ItemCreator
{
    private String url;
    private AtomChannelReader reader;

    public AtomFeedItemCreator(final Control c, final String name)
    {
        super(c, name);
    }

    public AtomFeedItemCreator(final Control c, final String url, final String name)
    {
        this(c, name);
        this.url = url;
        reader = new AtomChannelReader(url, name);
        Control.L.log(Level.FINE, "Created {0} url:{1} name:{2}", new Object[]{this.getClass().getName(), url, name});
    }

    @Override
    public String toString()
    {
        return super.toString()+" url:"+url;
    }

    @Override
    public void setType(final ItemCreator.ItemCreatorType type)
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
        if(reader.read())
        {
            this.items.addAll(reader.getItems());
            return true;
        }
        return false;
    }
}
