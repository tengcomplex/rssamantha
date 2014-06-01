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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;

/**
 *
 * @author teng
 */
public abstract class ItemCreator extends Thread
{
    protected final List<Item> items = new ArrayList<>();
    protected Control control;
    protected String creatorname;
    protected long sleep;
    protected ItemCreatorType type;
    /** -1 means any. */
    protected int [] days, hours;
    /** Appends description to title if true. Default false. */
    protected boolean appenddescription = false;
    /** If true "[channelname] $title", otherwise just "$title" */
    protected boolean titleprefix = Control.DEFAULTTITLEPREFIX;

    protected int channelindex;

    protected Map<String, Matcher> pattern;

    public ItemCreator()
    {
        
    }

    public ItemCreator(final Control c, final String creatorname)
    {
        this.control = c;
        this.creatorname = creatorname;
    }

    public void setTitlePrefix(final boolean b)
    {
        this.titleprefix = b;
    }

    public void setChannelindex(final int ix)
    {
        this.channelindex = ix;
    }

    public String getCreatorname()
    {
        return this.creatorname;
    }
    
    public void setPattern(final Map<String, Matcher> p)
    {
        this.pattern = p;
    }

    public void setHours(final int [] s)
    {
        this.hours = s;
    }

    public void setDays(final int[] d)
    {
        this.days = d;
    }

    public void setSleep(final long i)
    {
        this.sleep = i;
    }

    public long getSleep()
    {
        return this.sleep;
    }

    public void setAppendDescription(final boolean b)
    {
        this.appenddescription = b;
    }

    public ItemCreatorType getType()
    {
        return type;
    }

    public void setType(final ItemCreatorType type)
    {
        this.type = type;
    }

    public enum ItemCreatorType
    {
        SIMPLEFILE("simplefile"),
        RSSFEED("rss"),
        SIMPLERSSFEED("simplerss"),
        RSSIDENTICAFEED("rssidentica"),
        ATOMFEED("atom"),
        HTTPFEED("http"),
        PODCASTFEED("podcast"),
        UNKNOWN("unknown");
        
        final String name;
        
        ItemCreatorType(final String s)
        {
            this.name = s;
        }
    }
    
    /**
     * 
     * @param times
     * @param timename
     * @param calfieldtocheck
     * @return true if now is matching a value time, otherwise false.
     * @throws Exception 
     */
    private boolean isReadTime(final int[] times, final int calfieldtocheck) throws Exception
    {
        if(times != null)
        {
            Calendar cal = null;
            for(int time : times)
            {
                if(time == -1)
                {
                    return true;
                }
                else
                {
                    if(cal == null)
                    {
                        cal = Calendar.getInstance();
                    }
                    if(time == cal.get(calfieldtocheck))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Iterates through available pattern and tries to match. If one of them is not matching
     * we throw away the item. Note, if we don't have an attribute in an item ElementData
     * returns a "n/a".
     * @param item
     * @return
     */
    private boolean isThrowAway(final Item item)
    {
        boolean throwaway = false;
        String throwmsg = null;
        if(pattern != null)
        {   
            for(Iterator<String> iter = pattern.keySet().iterator(); iter.hasNext();)
            {
                final String key = iter.next();
                final Matcher matcher = pattern.get(key);
                final int ix = key.indexOf("_");
                final String attributename = key.substring(0, ix != -1 ? ix : key.length());
                final String candidate = item.getElements().getElementValue(attributename);
                matcher.reset(candidate);
                if(!matcher.matches())
                {
                    throwmsg = "["+key+":"+candidate+" not matching "+matcher.pattern().toString()+"]";
                    throwaway = true;
                    break;
                }
            }
        }
        if(Control.L.isLoggable(Level.FINEST))
        {
            Control.L.log(Level.FINEST, "channel:[{0}] creatorname:{1} item:{2} -> {3}throwing away", new Object[]{control.getChannelDataToShortString(channelindex), creatorname, item.toShortString(), throwaway ? throwmsg + " " : "not "});
        }
        return throwaway;
    }
    
    public abstract boolean read();

    protected String getNewTitle(final Item i)
    {
        String newTitle = "";
        final String oldTitle = i.getElements().getElementValue("title");
        if(i.getType() == ItemCreatorType.RSSFEED || i.getType() == ItemCreatorType.SIMPLERSSFEED || i.getType() == ItemCreatorType.ATOMFEED)
        {
            if(oldTitle.equals(i.getElements().getElementValue("description")))
            {
                i.putElement("description", "");
            }
            newTitle = (titleprefix ? "["+i.getElements().getElementValue("source")+"] " : "")+oldTitle;
        }
        else if(i.getType() == ItemCreatorType.RSSIDENTICAFEED)
        {
            newTitle = (titleprefix ? "[identica] " : "")+oldTitle;
        }
        else
        {
            newTitle = (titleprefix ? "["+i.getElements().getElementValue("source")+"] " : "")+oldTitle;
        }
        if(appenddescription)
        {
            newTitle += " | "+ChannelReader.smartTrim(i.getElements().getElementValue("description"));
        }
        /* Some clients not handling html in the title well, so we strip it out */
        return Control.decodeHtml(newTitle).replaceAll("\n", " ").replaceAll("\r", " ");
    }

    /**
     * Throws away items with non matching patterns.
     */
    private void putItems() throws Exception
    {
        if(Control.L.isLoggable(Level.FINEST))
        {
            Control.L.log(Level.FINEST, "channel:[{0}] creatorname:{1} trying to put {2} items", new Object[]{control.getChannelDataToShortString(channelindex), creatorname, items.size()});
        }
        for(Item i : items)
        {
            final boolean throwaway = isThrowAway(i);
//Control.L.log(Level.FINEST, "channel:[{0}] creatorname:{1} item:{2} {3} throwing away", new Object[]{control.getChannelDataToShortString(channelindex), creatorname, i.toShortString(), throwaway ? "" : "not"});
            if(throwaway)
            {
                continue;
            }
            i.putElement("title", getNewTitle(i));
            this.control.addItem(i, channelindex);
        }
    }

    protected void clearItems()
    {
        this.items.clear();
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run()
    {
        try
        {
            for(;;)
            {
                if(isReadTime(days, Calendar.DAY_OF_WEEK) && isReadTime(hours, Calendar.HOUR_OF_DAY))
                {
                    if(read())
                    {
                        Control.L.log(Level.FINE, "{0}. Read {1} items", new Object[]{toString(), items.size()});
                        putItems();
                    }
                    else
                    {
                        Control.L.log(Level.WARNING, "Couldn''t read {0}", this.toString());
                    }
                    clearItems();
                }
                else
                {
                    Control.L.log(Level.FINE, "{0}. No reading time, skipped", this.toString());
                }
                Thread.sleep(sleep);
            }
        }
        catch(Exception e)
        {
            Control.L.log(Level.SEVERE, "Error processing "+toString(), e);
            e.printStackTrace(System.err);
        }
    }

    @Override
    public String toString()
    {
        return "channel:["+control.getChannelDataToShortString(channelindex)+"]"
                +"creatorname:"+creatorname
                +" sleep:"+sleep
                +" titleprefix:"+titleprefix
                +" type:"+type
                +" pattern:"+(pattern == null ? "null" : pattern.toString())
                +" appenddescription:"+appenddescription
                +" number of items:"+items.size()
                +(days != null ? " days:"+Arrays.toString(days) : "")
                +(hours != null ? " hours:"+Arrays.toString(hours) : "");
    }
}
