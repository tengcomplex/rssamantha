/*
 *  RSSamantha is a rss/atom feedaggregator.
 *  Copyright (C) 2011-2014  David Schröer <tengcomplexATgmail.com>
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

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author teng
 */
public class TxtFileHandler extends FileHandler
{
    public final static String DEFAULT_DATETIME_TXT_PATTERN = "HH:mm:ss";
    public final static String DEFAULT_FILE_ENCODING = "UTF-8";
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
        try
        {
            Control.writeFile(filename, getContentAsString(items), false, DEFAULT_FILE_ENCODING);
        }
        catch(IOException | NullPointerException ex)
        {
            Control.L.log(Level.SEVERE, "Error writing to filename:"+filename, ex);
        }
        lastwrittenhashcode = hash;
    }

    protected String getContentAsString(final List<Item> items)
    {
        final StringBuilder str = new StringBuilder(Math.max(32, Math.min(4096, items.size()*256)));
        for(Item ii : items)
        {
            str.append(datetimeformat.format(new Date(ii.getCreated())));
            str.append(" ").append(ii.getElements().getElementValue("title")).append(Control.LINESEP);
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
