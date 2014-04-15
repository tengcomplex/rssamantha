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

import com.drinschinz.rssamantha.ItemCreator.ItemCreatorType;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 *
 * @author teng
 */
public class Item implements Comparable<Item>, Serializable
{
    private final ElementData elements;
    private long created;
    private boolean foundrsscreated = true;
    private byte[] description;
    private final ItemCreator.ItemCreatorType type;

    private final static SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US);

    public Item(final ItemCreator.ItemCreatorType type, final byte[] desc)
    {
        elements = new ElementData();
        this.description = desc;
        this.type = type;
    }

    private Item(final long created, final ItemCreator.ItemCreatorType type, final byte[] desc)
    {
        this(type, desc);
        setCreated(created);
    }

    public Item(final long created, final String title, final String description, final String itemCreatorName, final ItemCreator.ItemCreatorType type)
    {
        this(created, type, null);
        elements.putElementValue("title", title);
        elements.putElementValue("description", description);
        elements.putElementValue("itemcreatorname", itemCreatorName);
    }

    public ItemCreatorType getType()
    {
        return type;
    }

    public boolean isFoundrsscreated()
    {
        return foundrsscreated;
    }

    public void setFoundrsscreated(final boolean foundrsscreated)
    {
        this.foundrsscreated = foundrsscreated;
    }

    public void setDescriptionAsBytes(final byte[] d)
    {
        this.description = d;
    }

    public byte[] getDescriptionAsBytes()
    {
        return this.description;
    }

    public void putElement(final String key, final String value)
    {
        elements.putElementValue(key, value);
    }

    public ElementData getElements()
    {
        return elements;
    }

    public long getCreated()
    {
        return created;
    }

    public final void setCreated(final long created)
    {
        this.created = created;
        elements.putElementValue("created", String.valueOf(created));
        synchronized(formatter)
        {
            elements.putElementValue("pubDate", formatter.format(created));
        }
    }

    @Override
    public Object clone()
    {
        final Item ret = new Item(this.created, this.type, this.description);
        for(Iterator<String> iter = elements.getElementKeys(); iter.hasNext();)
        {
            final String n = iter.next();
            ret.getElements().putElementValue(n, elements.getElementValue(n));
        }
        return ret;
    }

    /**
     * Checks created/title, which means we would not accept an item with same timestamp/title
     * if it has a different description.<br>
     * In the ItemData TreeSet should never be a null Object or an Object from class other than Item.
     * @param o
     * @return
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(final Object o)
    {
        final Item cmp = (Item)o;
        return this.created == cmp.created && this.elements.getElementValue("title").equals(cmp.elements.getElementValue("title"));
    }

    /**
     * 
     * @return
     */
    @Override
    public int hashCode()
    {
        return 679
                + (this.elements.getElementValue("title") != null ? this.elements.getElementValue("title").hashCode() : 0)
                + (this.elements.getElementValue("description") != null ? this.elements.getElementValue("description").hashCode() : (description != null ? Arrays.hashCode(description) : 0) )
                + (this.elements.getElementValue("source") != null ? this.elements.getElementValue("source").hashCode() : 0);
    }

    @Override
    public String toString()
    {
        return toShortString()+" "+elements.toString();
    }

    /** 
     * 
     * @return Basic data and just the title from elements.
     */
    public String toShortString()
    {
        final StringBuilder ret = new StringBuilder("created:"+created+"[");
        synchronized(formatter)
        {
            ret.append(formatter.format(new Date(created)));
        }
        ret.append("] type:").append(type).append(" foundrsscreated:").append(foundrsscreated).append(" source:").append(elements.getElementValue("source")).append(" title:").append(elements.getElementValue("title"));
        return ret.toString();
    }

    /** 
     * Youngest on top. If created is equals sort by title alphabetically.
     * @param another The #Item to compare against.
     */
    @Override
    public int compareTo(final Item another)
    {
        return (this.created < another.created ? 1 : (this.created == another.created ? this.elements.getElementValue("title").compareTo(another.elements.getElementValue("title")) : -1));
    }
}
