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

import java.util.logging.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author teng
 */
public class AtomChannelReader extends ChannelReader
{
    public AtomChannelReader(final String url, final String itemcreatorname)
    {
        super(url, itemcreatorname);
    }

    @Override
    public boolean read()
    {
        try
        {
            final Document doc = getDocument(url);
            final NodeList listFields = doc.getElementsByTagName("entry");
            for(int ss = 0; ss<listFields.getLength(); ss++)
            {
                Item item = null;
                final Node fieldNode = listFields.item(ss);
                if(fieldNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    final Element fieldElement = (Element) fieldNode;
                    final DateInfo di = getCreated(fieldElement, new String[]{"published","updated"});
                    item = new Item(di.getTimestamp(), getValue(fieldElement, "title"), getValue(fieldElement, "content"), itemcreatorname, type);
                    item.setFoundrsscreated(di.isFoundCreated());
                    item.setSummary(getValue(fieldElement, "summary"));
                    item.setLink(getValue(fieldElement, "id"));
                    item.setSource(itemcreatorname);
                }
                items.add(item);
            }
        }
        catch(Exception ex)
        {
            Control.L.log(Level.WARNING, "Error reading {0} {1}", new Object[]{url, ex.getMessage()});
            System.err.println("Error reading "+url+" "+ex.getMessage());
            return false;
        }
        finally
        {
            closeStream();
        }
        return true;
    }
}
