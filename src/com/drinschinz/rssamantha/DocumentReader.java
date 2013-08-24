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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.regex.Matcher;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author teng
 */
public class DocumentReader
{
    private InputStream stream;

    public DocumentReader()
    {
        super();
    }

    protected void closeStream()
    {
        if(stream != null)
        {
            try
            {
                this.stream.close();
                this.stream = null;
            }
            catch (IOException ex)
            {
                Control.L.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * 
     * @param s Config filename
     * @param repl Map toreplace -> replace with
     * @return Document
     * @throws Exception
     */
    protected Document getPreprocessedDocument(final String s, final HashMap<String, String> repl) throws Exception
    {
        final ArrayList<String> tmp = Control.readFile(s);
        final StringBuilder buffer = new StringBuilder();
        for(int ii=0; ii<tmp.size(); ii++)
        {
            String str = tmp.get(ii);
            for(Iterator<String> iter = repl.keySet().iterator(); iter.hasNext();)
            {
                final String next = iter.next();
                if(str.indexOf("${"+next+"}") != -1)
                {
                    Control.L.log(Level.INFO, "Replacing all $'{'{0}"+"}"+" in "+"{1} with {2}", new Object[]{next, str, repl.get(next)});
                    str = str.replaceAll("\\$\\{"+next+"\\}", Matcher.quoteReplacement(repl.get(next)));
                }
            }
            buffer.append(str);
        }
        return getDocument(new ByteArrayInputStream(buffer.toString().getBytes()));
    }

    private Document getDocument(final InputStream is) throws Exception
    {
        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setCoalescing(true);
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        final Document doc = docBuilder.parse(is);
        doc.getDocumentElement().normalize();
        return doc;
    }

    protected Document getDocument(final String s) throws Exception
    {
        if(s.startsWith("http://") || s.startsWith("https://"))
        {
            this.stream = new URL(s).openStream();
        }
        else
        {
            this.stream = new FileInputStream(new File(s));
        }
        return getDocument(this.stream);
    }

    protected String getValue(final Element fieldElement, final String elementName)
    {
        final NodeList trs = fieldElement.getElementsByTagName(elementName);
        try
        {
            final String ret = ((Element)trs.item(0)).getFirstChild().getNodeValue();
            return ret == null ? "n/a" : ret;
        }
        catch(Exception ex)
        {
            return "n/a";
        }
    }
}
