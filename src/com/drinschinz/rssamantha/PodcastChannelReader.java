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

import java.util.Collections;
import java.util.logging.Level;
import org.w3c.dom.*;

/**
 *
 * @author teng
 */
public class PodcastChannelReader extends RssChannelReader
{
    public PodcastChannelReader(final String url, final String itemcreatorname)
    {
        super(url, itemcreatorname);
    }

    /**
     * Because some feeds add new media on bottom and we could use
     * config.adddownloaditems in this channel we have to sort
     * in order to get youngest on top before adding.
     */
    public void sortItems()
    {
        Collections.sort(items);
    }

    @Override
    public boolean read()
    {
        try
        {
            final Document doc = getDocument(url);
            final NodeList listFields = doc.getElementsByTagName("item");
            for(int ss = 0; ss<listFields.getLength(); ss++)
            {
                Item item = null;
                final Node fieldNode = listFields.item(ss);
                if(fieldNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    final Element fieldElement = (Element) fieldNode;
                    final DateInfo di = getCreated(fieldElement, new String[]{"pubDate"});
                    item = new Item(di.getTimestamp(), getValue(fieldElement, "title"), getValue(fieldElement, "description"), itemcreatorname, type);
                    item.setFoundrsscreated(di.isFoundCreated());
                    item.setLink(getValue(fieldElement, "link"));
                    item.setCategory(getValue(fieldElement, "category"));
                    //item.putElement("guid", getValue(fieldElement, "guid"));
                    final NodeList trs = fieldElement.getElementsByTagName("enclosure");
                    final Element enel = ((Element) trs.item(0));
                    if(enel != null)
                    {
                        item.setContentType(enel.getAttribute("type"));
                        item.setContentUrl(enel.getAttribute("url"));
                        item.setContentLength(enel.getAttribute("length"));
                    }
                    item.setSource(itemcreatorname);
                }
                if(item.getContentUrl() != null) // TODO, this be done different. Check url earlier.
                {
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
