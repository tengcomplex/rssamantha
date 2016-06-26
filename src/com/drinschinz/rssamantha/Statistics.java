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

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Offers general runtime status.
 * @author teng
 */
public class Statistics
{
    private final long started = System.currentTimeMillis();
    static public final int SECOND  = 1000;
    static public final int MINUTE  = SECOND * 60;
    static public final int HOUR    = MINUTE * 60;
    static public final int DAY     = HOUR * 24;
    static public final NumberFormat formatter = new DecimalFormat("####.##");

    /**
     * 
     * @return
     */
    public synchronized String getStatus()
    {
        final StringBuilder str = new StringBuilder("uptime:");
        str.append(getUptime());
        str.append(" ").append(getRuntimeStatus());
        str.append(" numthreads:").append(getNumThreads());
        return str.toString();
    }

    protected String formatMemory(final long lo)
    {
        return formatter.format((((double)lo/1000000)))+"MB";
    }

    protected String formatPercentage(final int a, final int b)
    {
        return formatter.format(100*((double)a/b))+"%";
    }

    protected String getRuntimeStatus()
    {
        final Runtime rt = Runtime.getRuntime();
        return "used memory:"+formatMemory(rt.totalMemory() - rt.freeMemory())+" free memory:"+formatMemory(rt.freeMemory())+" total memory:"+formatMemory(rt.totalMemory())+" max memory:"+formatMemory(rt.maxMemory());
    }

    protected int getNumThreads()
    {
        return Thread.getAllStackTraces().size();
    }

    protected String getUptime()
    {
        long diff = System.currentTimeMillis() - started;
        long days, hours, minutes, seconds;
        days = (long)(diff / DAY);
        diff -= days * DAY;
        hours = (long)(diff / HOUR);
        diff -= hours * HOUR;
        minutes = (long)(diff / MINUTE);
        diff -= minutes * MINUTE;
        seconds = (long)(diff / SECOND);
        return ""+days+"d "+hours+"h "+minutes+"m "+seconds+"s";
    }
}
