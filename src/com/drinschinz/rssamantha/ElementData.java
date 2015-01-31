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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author teng
 */
public class ElementData implements Serializable
{
    private final HashMap<String, String> elements;

    public ElementData()
    {
        elements = new HashMap<>();
    }

    public void putElementValue(final String key, final String value)
    {
        elements.put(key, value);
    }

    public String removeElementValue(final String key)
    {
        return elements.remove(key);
    }

    public Iterator<String> getElementKeys()
    {
        return elements.keySet().iterator();
    }

    public boolean containsElement(final String key)
    {
        return elements.containsKey(key);
    }

    public String getElementValue(final String key)
    {
        final String ret = elements.get(key);
        return ret != null ? ret : "n/a";
    }

    @Override
    public String toString()
    {
        return Arrays.toString(elements.keySet().toArray(new String [0]));
    }
}
