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

import com.drinschinz.rssamantha.ChannelReader.DateInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import qdxml.DocHandler;
import qdxml.QDParser;

/**
 *
 * @author teng
 */
public class SimpleRssFeedItemCreator extends ItemCreator implements DocHandler
{
    private String url;
    protected int preferreddateformatix = -1;
    protected boolean preprocessdatetime = true;

    private final HashSet<String> supportedtags = new HashSet<String>();

    public SimpleRssFeedItemCreator(final Control c, final String name)
    {
        super(c, name);
        supportedtags.add("title");
        supportedtags.add("description");
        supportedtags.add("link");
        supportedtags.add("category");
        supportedtags.add("guid");
    }

    public SimpleRssFeedItemCreator(final Control c, final String url, final String name)
    {
        this(c, name);
        this.url = url;
        Control.L.log(Level.FINE, "Created {0} url:{1} name:{2}", new Object[]{this.getClass().getName(), url, name});
    }

    @Override
    public boolean read()
    {
        // This is all the code we need to parse
        // a document with our DocHandler.
        BufferedReader fr = null;
        try
        {
            URL u = new URL(url);
            fr = new BufferedReader(new InputStreamReader(u.openStream()));
            String fl = fr.readLine().replaceAll("'", "\"");
            int ix1 = fl.indexOf("encoding");
            int ix2 = fl.indexOf("\"", ix1);
            int ix3 = fl.indexOf("\"", ix2+1);
            String enc = ix1 != -1 ? fl.substring(ix2+1, ix3) : "UTF-8";
            fr.close();
            Control.L.log(Level.FINEST, "Detected {0}, opening stream from url:{1} creatorname:{2}", new Object[]{enc, url, creatorname});
            fr = new BufferedReader(new InputStreamReader(u.openStream(), enc));
            QDParser.parse(this,fr);
        }
        catch (Exception ex)
        {
            Control.L.log(Level.SEVERE, "Error reading "+url, ex);
        }
        finally
        {
            if(fr != null)
            {
                try
                {
                    fr.close();
                }
                catch(IOException e)
                {
                    // ignore 
                }
            }
        }
        return this.items.size() > 0;
    }

    Item nextitem = null;

    public void startDocument()
    {
        //System.out.println("  start document");
    }
    
    public void endDocument()
    {
        //System.out.println("  end document");
    }

    public void startElement(final String elem, final Hashtable h)
    {
        //System.out.println("    start elem: "+elem);
        if("item".equals(elem))
        {
            nextitem = new Item(type, null);
            nextitem.setSource(creatorname);
        }
        else
        {
            actelement = elem;
        }
        /*
        Enumeration e = h.keys();
        while(e.hasMoreElements())
        {
          String key = (String)e.nextElement();
          String val = (String)h.get(key);
          System.out.println("      "+key+" = "+val);
        }
        
         */
    }

    String actelement;

    public void endElement(String elem)
    {
        //System.out.println("    end elem: "+elem);
        if("item".equals(elem))
        {
            //System.out.println("End element item "+nextitem.toString());
            this.items.add(nextitem);
            nextitem = null;
        }
    }
  
    public void text(String text)
    {
        //System.out.println("        text: "+text);
        if(text == null || text.trim().length() == 0 || "\n".equals(text))
        {
            return;
        }
        if("pubDate".equals(actelement) && nextitem != null)
        {
            final DateInfo oo = ChannelReader.parseDate(preprocessdatetime, preferreddateformatix, text);

            Date dt = oo.getDate();
            preferreddateformatix = oo.getIx();
            preprocessdatetime = oo.isPreprocessdatetime();
            if(dt == null || dt.getTime() == 0)
            {
                dt = new Date(System.currentTimeMillis());
                nextitem.setFoundrsscreated(false);
            }
            nextitem.setCreated(dt.getTime());
        }
        else
        {
            if(nextitem != null && supportedtags.contains(actelement))
            {
                // TODO FIXME
                //nextitem.putElement(actelement, text);
                
            }
        }
    }
}
