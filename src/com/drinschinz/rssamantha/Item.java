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
import java.util.Locale;

/**
 *
 * @author teng
 */
public class Item implements Comparable<Item>, Serializable
{
    private String title, itemCreatorName, descriptionS, link, category, source, createdS, pubDate, summary, contentUrl, contentLength, contentType, contentFolder;
    private int index;
    private long created;
    private boolean foundrsscreated = true;
    private byte[] description;
    private final ItemCreator.ItemCreatorType type;

    private final static SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US);

    public Item(final ItemCreator.ItemCreatorType type, final byte[] desc)
    {
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
        this.title = title;
        this.descriptionS = description;
        this.itemCreatorName = itemCreatorName;
    }
    
    public String getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(String contentLength)
    {
        this.contentLength = contentLength;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
    
    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }
    
    public String getContentUrl()
    {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl)
    {
        this.contentUrl = contentUrl;
    }

    public String getContentFolder()
    {
        return contentFolder;
    }

    public void setContentFolder(String contentFolder)
    {
        this.contentFolder = contentFolder;
    }
    
    public String getSummary()
    {
        return summary;
    }

    public void setSummary(final String summary)
    {
        this.summary = summary;
    }
    
    public String getTitle()
    {
        return title;
    }

    public void setTitle(final String title)
    {
        this.title = title;
    }
    
    public String getItemCreatorName()
    {
        return itemCreatorName;
    }

    public void setItemCreatorName(final String itemCreatorName)
    {
        this.itemCreatorName = itemCreatorName;
    }
    
    public String getDescriptionS()
    {
        return descriptionS;
    }

    public void setDescriptionS(final String descriptionS)
    {
        this.descriptionS = descriptionS;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(final String link)
    {
        this.link = link;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(final String category)
    {
        this.category = category;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(final String source)
    {
        this.source = source;
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

    public long getCreated()
    {
        return created;
    }

    public final void setCreated(final long created)
    {
        this.created = created;
        //elements.putElementValue("created", String.valueOf(created));
        createdS = String.valueOf(created);
        synchronized(formatter)
        {
            //elements.putElementValue("pubDate", formatter.format(created));
            pubDate = formatter.format(created);
        }
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Item clone()
    {
        final Item ret = new Item(this.created, this.type, this.description);
        ret.setDescriptionS(this.descriptionS);
        ret.setTitle(this.title);
        ret.setCategory(this.category);
        ret.setSource(this.source);
        ret.setLink(this.link);
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
        return this.created == cmp.created && this.title.equals(cmp.getTitle());
    }

    /**
     * 
     * @return
     */
    @Override
    public int hashCode()
    {
        return 679
                + (this.title != null ? this.title.hashCode() : 0)
                + (this.descriptionS != null ? this.descriptionS.hashCode() : (description != null ? Arrays.hashCode(description) : 0) )
                + (this.source != null ? this.source.hashCode() : 0);
    }

    @Override
    public String toString()
    {
        return toShortString()+" category:"+this.category+" link:"+this.link;
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
        ret.append("] type:").append(type).append(" foundrsscreated:").append(foundrsscreated).append(" source:").append(this.source).append(" title:").append(this.title);
        return ret.toString();
    }

    /** 
     * Youngest on top. If created is equals sort by title alphabetically.
     * @param another The #Item to compare against.
     */
    @Override
    public int compareTo(final Item another)
    {
        return (this.created < another.created ? 1 : (this.created == another.created ? this.title.compareTo(another.getTitle()) : -1));
    }
}
