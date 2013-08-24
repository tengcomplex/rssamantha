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

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import java.util.List;
import java.util.logging.Level;
import org.w3c.dom.*;

import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;



/**
 *
 * @author teng
 */
public class RssFileHandler extends FileHandler
{
    private final RssChannel channel;
    private final static SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z");

    public RssFileHandler(final Control c, final int[] channelindices, final String rssfilename, final long sleep)
    {
        super(c, channelindices, rssfilename, sleep);
        this.channel = new RssChannel();
        for(Iterator<String> iter = control.getChannelElements(channelindices[0]).keySet().iterator(); iter.hasNext();)
        {
            final String key = iter.next();
            channel.putElement(key, control.getChannelElements(channelindices[0]).get(key));
        }
        if(!channel.containsElement("title"))
        {
            channel.putElement("title", Main.APPNAME);
        }
        if(!channel.containsElement("link"))
        {
            channel.putElement("link", "http://configure.your.link");
        }
        if(!channel.containsElement("description"))
        {
            channel.putElement("description", "Configure your description");
        }
        if(!channel.containsElement("generator"))
        {
            channel.putElement("generator", Main.APPNAME+" "+Main.APPVERSION);
        }
        if(channelindices.length > 1)
        {
            channel.putElement("title", control.getChannelName(channelindices));
            channel.putElement("link", "Aggregated channel");
            channel.putElement("description", "Aggregated channel");
        }
    }

    protected void putChannelElement(final String k, final String v)
    {
        channel.putElement(k, v);
    }

    protected Document getDocument(final List<Item> items)
    {
        final Calendar now = Calendar.getInstance();
        synchronized(formatter)
        {
            channel.putElement("lastBuildDate", formatter.format(now.getTime()));
        }
        channel.setItems(items);
        try
        {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.newDocument();
            final Element root = document.createElement("rss");
            root.setAttribute("version", "2.0");
            document.appendChild(root);

            final Element channelel = document.createElement("channel");
            root.appendChild(channelel);

            Element el = null;
            Text text = null;

            for(Iterator<String> iter = channel.getElements().getElementKeys(); iter.hasNext();)
            {
                final String next = iter.next();
                el = document.createElement(next);
                text = document.createTextNode(channel.getElements().getElementValue(next));
                el.appendChild(text);
                channelel.appendChild(el);
            }
            for(int ii=0; ii<items.size(); ii++)
            {
                final Item item = items.get(ii);
                if(isFutureDump(item, now))
                {
                    continue;
                }
                el = document.createElement("item");
                for(Iterator<String> iter = item.getElements().getElementKeys(); iter.hasNext();)
                {
                    final String n = iter.next();
                    final Element itemel = document.createElement(n);
                    final String value = item.getElements().getElementValue(n);
                    text = document.createTextNode(value);
                    itemel.appendChild(text);
                    el.appendChild(itemel);
                }
                channelel.appendChild(el);
            }
            return document;
//System.out.println("lastwrittenhashcode:"+lastwrittenhashcode);
        }
        catch(Exception ex)
        {
            Control.L.log(Level.SEVERE, "Error writing {0} {1}", new Object[]{filename, ex.getMessage()});
            //ex.printStackTrace(System.err);
        }
        return null;
    }

    protected void write()
    {
        List<Item> items = control.getSortedItems(channelindices);
//System.out.println("RssFileHandler.write(), writing to "+filename+" number of items: " + items.size());
/*
System.out.print("RssFileHandler.write(), number of items: " + items.size() + " ");
for (int ii = 0; ii < items.size(); ii++)
{
    System.out.print("item " + (ii) + ": [" + items.get(ii).toString() + "] ");
}
System.out.print("\r\n");
*/      
        final int hash = items.hashCode();
        if(!hasChanged(hash))
        {
            return;
        }
        final Calendar now = Calendar.getInstance();
        synchronized(formatter)
        {
            channel.putElement("lastBuildDate", formatter.format(now.getTime()));
        }
        channel.setItems(items);
        try
        {
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("indent", "yes");
            Document doc = getDocument(items);
            final DOMSource source = new DOMSource(doc);
            final FileOutputStream os = new FileOutputStream(new File(filename));
            final StreamResult result = new StreamResult(os);
            transformer.transform(source, result);
            os.close();
            lastwrittenhashcode = hash;
            items.clear();
            items = null;
            doc = null;
//System.out.println("lastwrittenhashcode:"+lastwrittenhashcode);
        } 
        catch(Exception ex)
        {
            Control.L.log(Level.SEVERE, "Error writing {0} {1}", new Object[]{filename, ex.getMessage()});
            ex.printStackTrace(System.err);
        }
    }

    @Override
    public void run()
    {
        for(;;)
        {
            try
            {
                Thread.sleep(sleep);
            }
            catch(InterruptedException ie)
            {
                ie.printStackTrace(System.err);
            }
            if(isInterrupted())
            {
                return;
            }
            write();
        }
    }
}
