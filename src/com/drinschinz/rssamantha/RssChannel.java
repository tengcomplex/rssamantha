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

import java.util.List;

/**
 *
 * @author teng
 */
public class RssChannel
{
    private final ElementData elements;
    private List<Item> items;

    public RssChannel()
    {
        elements = new ElementData();
    }

    public boolean containsElement(final String key)
    {
        return elements.containsElement(key);
    }

    public ElementData getElements()
    {
        return elements;
    }

    public void putElement(final String key, final String value)
    {
        elements.putElementValue(key, value);
    }

    public List<Item> getItems()
    {
        return items;
    }

    public void setItems(final List<Item> items)
    {
        this.items = items;
    }
}
