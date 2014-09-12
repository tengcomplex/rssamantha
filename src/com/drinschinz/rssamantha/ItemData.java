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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teng
 */
public class ItemData
{
    private final SortedSet<Item> items = Collections.synchronizedSortedSet(new TreeSet<Item>());
    /**
     *  Storing title hashcodes, we just need them when we want to know whether
     *  we know an Item with foundrsscreated = false. Therefore we just collect the hashvalue
     *  as integer of those.
     *  @see addItem
     */
    private final HashSet<Integer> titles = new HashSet<>();
    private final int showlimit, storelimit;

    public ItemData(final int showlimit, final int storelimit)
    {
        this.showlimit = showlimit;
        this.storelimit = storelimit;
    }

    @Override
    public String toString()
    {
        return "storelimit:"+storelimit+" showlimit:"+showlimit+" number of items:"+items.size()+" number of titles:"+titles.size();
    }

    public int storesize()
    {
        return items.size();
    }

    public int getStoreLimit()
    {
        return storelimit;
    }

    public int getShowLimit()
    {
        return showlimit;
    }

    public boolean removeItem(final String s, final long c)
    {
        titles.remove(s.hashCode());
        for(Item i : items)
        {
            if(i.getCreated() == c && i.getTitle().equals(s))
            {
                return items.remove(i);
            }
        }
        return false;
    }

    public void addItem(final Item i)
    {
        items.add(i);
        if(!i.isFoundrsscreated())
        {
            titles.add(i.getTitle().hashCode());
        }
        if(Control.L.isLoggable(Level.FINE))
        {
            Control.L.log(Level.FINE, "{0} Adding item {1}", new Object[]{toString(), i.toShortString()});
        }
        if(items.size() > storelimit)
        {
            final Item rem = items.last();
            if(items.remove(rem))
            {
                if(!rem.isFoundrsscreated())
                {
                    titles.remove(rem.getTitle().hashCode());
                }
                if(Control.L.isLoggable(Level.FINE))
                {
                    Control.L.log(Level.FINE, "{0} Removed item {1}", new Object[]{toString(), rem.toShortString()});
                }
            }
        }
    }

    public SortedSet<Item> getAllItems()
    {
        return items;
    }

    public int getNumberOfItems()
    {
        return items.size();
    }

    /**
     * @return List<Item> of size <= limit in order of cloned Item instances.
     * @param numitems The maximum number of items.
     * @param cutoff The time cutoff, in milliseconds since epoch.
     * @param pt_title Optional #Pattern for #Item title.
     * @param t The current time in millis, used for checking ignore future.
     * @param uniqueTitle If true we return every distinct title just once, the youngest. Otherwise all Items.
     */
    public List<Item> getSortedItems(final int numitems, final long cutoff, final Pattern pt_title, final long t, final boolean uniqueTitle)
    {
        final int limit = items.size() < numitems ? items.size() : numitems;
        final Matcher matcher = pt_title != null ? pt_title.matcher("") : null;
        final ArrayList<Item> copy = new ArrayList<>(limit);
        final HashSet<String> itemTitles = uniqueTitle ? new HashSet<String>(limit) : null;
        final Iterator<Item> iter = items.iterator();
        int count = 0;
        while(count < limit && iter.hasNext())
        {
            final Item i = iter.next();
            if((cutoff != -1 && i.getCreated() < cutoff) || Control.isIgnoreFuture(i, t))
            {
                continue;
            }
            if(matcher != null)
            {
                matcher.reset(i.getTitle());
                if(!matcher.matches())
                {
                    continue;
                }
            }
            final String title = !uniqueTitle ? null : i.getTitle();
            /* We made sure at reading time element value title is never null, 
               therefore title is always not null if we care for.
               We also make sure itemTitle is not null if we want unique titles. */
            if(!uniqueTitle || !itemTitles.contains(title))
            {
                copy.add(i.clone());
                if(uniqueTitle)
                {
                    itemTitles.add(title);
                }
                count++;
            }
        }
        return copy;
    }

    public boolean isKnown(final Item i)
    {
        boolean known = false;
        if(i.getType() == ItemCreator.ItemCreatorType.SIMPLEFILE && items.contains(i))
        {
            known = true;
        }
        else
        {
            if(i.isFoundrsscreated() && items.contains(i))
            {
                known = true;
            }
            else if(!i.isFoundrsscreated() && titles.contains(i.getTitle().hashCode()))
            {
                known = true;
            }
        }
        return known;
    }

    public boolean isTooOld(final Item i)
    {
        return items.size() == storelimit && i.getCreated() <= items.last().getCreated();
    }
}
