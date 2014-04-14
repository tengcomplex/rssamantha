/*
 *  RSSamantha is a rss/atom feedaggregator.
 *  Copyright (C) 2011-2013  David Schröer <tengcomplexATgmail.com>
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author teng
 */
public class TxtFileHandler extends FileHandler
{
    public final static String DEFAULTDATETIMETXTPATTERN = "HH:mm:ss";
    protected final DateFormat datetimeformat;
    protected int initialsleep = 3000;

    public TxtFileHandler(final Control c, final int[] channelindices, final String rssfilename, final long sleep, final String pattern)
    {
        super(c, channelindices, rssfilename, sleep);
        this.datetimeformat = pattern == null ? null : new SimpleDateFormat(pattern);
    }
    
    public TxtFileHandler(final Control c, final int[] channelindices, final String rssfilename, final long sleep, final DateFormat datetimeformat)
    {
        super(c, channelindices, rssfilename, sleep);
        this.datetimeformat = datetimeformat;
    }

    @Override
    protected void write()
    {
        final List<Item> items = control.getSortedItems(channelindices);
        final int hash = items.hashCode();
        if(!hasChanged(hash))
        {
            return;
        }
        Control.writeFile(filename, getAsString(items), false, "UTF-8");
        lastwrittenhashcode = hash;
    }

    protected String getAsString(final List<Item> items)
    {
        final Calendar now = Calendar.getInstance();
        final StringBuilder str = new StringBuilder("");
        for(Item ii : items)
        {
            if(isIgnoreFuture(ii, now))
            {
                continue;
            }
            final String ti = ii.getElements().getElementValue("title");
            str.append(datetimeformat.format(new Date(ii.getCreated())));
            str.append(" ").append(ti).append(Control.LINESEP);
        }
        return str.toString();
    }

    @Override
    public void run()
    {
        try
        {
            /* Wait a bit in case we started other writers */
            sleep(initialsleep);
            for(;;)
            {
                write();
                sleep(sleep);
            }
        }
        catch(InterruptedException ex)
        {
            Control.L.log(Level.SEVERE, "", ex);
        }
    }
}
