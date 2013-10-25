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
import java.util.List;

/**
 *
 * @author teng
 */
public class HtmlFileHandler extends TxtFileHandler
{
    public static final String DEFAULTDATETIMEHTMLPATTERN = "dd MMM yyyy HH:mm:ss";
    public final static DateFormat timeformat = new SimpleDateFormat("HH:mm:ss");

    public HtmlFileHandler(final Control c, final int [] channelindices, final String rssfilename, final long sleep, final String pattern)
    {
        super(c, channelindices, rssfilename, sleep, pattern);
        initialsleep = 6000;
    }
    
    /**
     * 
     * @param c
     * @param channelindices
     * @param rssfilename
     * @param sleep
     * @param datetimeformat 
     */
    public HtmlFileHandler(final Control c, final int [] channelindices, final String rssfilename, final long sleep, final DateFormat datetimeformat)
    {
        super(c, channelindices, rssfilename, sleep, null);
        this.datetimeformat = datetimeformat;
        initialsleep = 6000;
    }
        
    /**
     * 
     * @param items
     * @return The html code, eventually written to harddisk.
     */
    @Override
    protected String getAsString(final List<Item> items)
    {
        return getAsString(items, String.valueOf((sleep*1000)), null);
    }
    
    protected String getAsString(final List<Item> items, final String refresh, final String css)
    {
        final Calendar now = Calendar.getInstance();
        final StringBuilder table = new StringBuilder(32);
        table.append("<TABLE border=\"0\">");
        int numitems = 0;
        for(Item ii : items)
        {
            if(isFutureDump(ii, now))
            {
                continue;
            }
            numitems++;
            table.append("<TR>").append(ClientThread.EOL);
            table.append("<TD>");
            /* We are never changing the formatter reference */
            synchronized(datetimeformat)
            {
                table.append(datetimeformat.format(ii.getCreated()));
            }
            table.append("</TD>").append(ClientThread.EOL);
            table.append("<TD><A HREF=\"").append(ii.getElements().getElementValue("link")).append("\" target=\"_blank\">").append(ii.getElements().getElementValue("title")).append("</A>" +"</TD>"+ClientThread.EOL);
            table.append("</TR>").append(ClientThread.EOL);
        }
        table.append("</TABLE>").append(ClientThread.EOL);
        final StringBuilder str = new StringBuilder(512);
        str.append("<HTML>");
        str.append(ClientThread.EOL).append("<HEAD>").append(ClientThread.EOL);
        str.append("<META HTTP-EQUIV=\"refresh\" CONTENT=\"").append(refresh).append("\">"+ClientThread.EOL);
        String time;
        synchronized(timeformat)
        {
            time = timeformat.format(now.getTime());
        }
        str.append("<TITLE>").append(control.getChannelName(channelindices)).append(" [").append(numitems).append("] ").append(time).append("</TITLE>"+ClientThread.EOL);
        if(css != null)
        {
            str.append("<STYLE>").append(ClientThread.EOL).append(css).append("</STYLE>").append(ClientThread.EOL);
        }
        str.append("</HEAD>").append(ClientThread.EOL).append("<BODY>").append(ClientThread.EOL);
        str.append(table.toString());
        str.append("</BODY>").append(ClientThread.EOL).append("</HTML>").append(ClientThread.EOL);
        return str.toString();
    }
}
