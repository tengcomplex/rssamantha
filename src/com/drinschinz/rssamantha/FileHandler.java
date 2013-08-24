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

import java.util.Calendar;
import java.util.logging.Level;

/**
 *
 * @author teng
 */
public abstract class FileHandler extends Thread
{
    protected final Control control;
    protected final String filename;
    protected final long sleep;
    /** Implementing class can use this in order to check potentially changes in the list of items */
    protected int lastwrittenhashcode = -1;
    protected final int[] channelindices;

    public FileHandler(final Control c, final int[] channelindices, final String filename, final long sleep)
    {
        this.control = c;
        this.channelindices = channelindices;
        this.filename = filename;
        this.sleep = sleep;
    }

    @Override
    public String toString()
    {
        return "channel:["+control.getChannelDataToShortString(channelindices)+"] sleep:"+sleep+" lastwrittenfilename:"+lastwrittenhashcode+" filename:"+filename;
    }

    protected boolean isFutureDump(final Item i, final Calendar c)
    {
        if(control.isFutureDump() && i.getCreated() > c.getTimeInMillis())
        {
            if(Control.L.isLoggable(Level.FINE))
            {
                Control.L.log(Level.FINE, "Ignore in future published item:{0}", new String[]{i.toShortString()});
            }
            return true;
        }
        return false;
    }

    protected abstract void write();

    protected String getStatus()
    {
        return toString()+" (storage:"+control.getNumberOfStoreItems(channelindices)+"/"+control.getStoreLimit(channelindices)+" showlimit:"+control.getShowLimit(channelindices)+") "
                +control.getStatus();
    }

    protected boolean hasChanged(final int hash)
    {
        if(hash == this.lastwrittenhashcode)
        {
            if(Control.L.isLoggable(Level.FINE))
            {
                Control.L.log(Level.FINE, "Items hashcode unchanged {0}", new Object[]{getStatus()});
            }
            return false;
        }
        Control.L.log(Level.INFO, "Writing {0}", new Object[]{getStatus()});
        return true;
    }
}
