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

import java.util.logging.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 *
 * @author teng
 */
public class RssChannelReader extends ChannelReader
{
    public RssChannelReader(final String url, final String itemcreatorname)
    {
        super(url, itemcreatorname);
    }
    
    /**
     * 
     * @return 
     */
    @Override
    public boolean read()
    {
        try
        {
            final Document doc = getDocument(url);
            final NodeList listFields = doc.getElementsByTagName("item");
            for(int ss = 0; ss<listFields.getLength(); ss++)
            {
                final Node fieldNode = listFields.item(ss);
                if(fieldNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    final Element fieldElement = (Element) fieldNode;
                    final DateInfo di = getCreated(fieldElement, new String[]{"pubDate", "dc:date"});
                    final Item item = new Item(di.getTimestamp(), getValue(fieldElement, "title"), getValue(fieldElement, "description"), itemcreatorname, type);
                    item.setFoundrsscreated(di.isFoundCreated());
                    item.setLink(getValue(fieldElement, "link"));
                    item.setCategory(getValue(fieldElement, "category"));
                    if(!"rssfilereader".equals(itemcreatorname))
                    {
                        item.setSource(itemcreatorname);
                    }
                    else
                    {
                        item.setSource(getValue(fieldElement, "source"));
                    }
                    items.add(item);
                }
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
