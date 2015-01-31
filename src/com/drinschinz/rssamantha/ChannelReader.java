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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author teng
 */
public abstract class ChannelReader extends DocumentReader
{
    protected String url, itemcreatorname;
    protected ItemCreator.ItemCreatorType type;
    protected final ArrayList<Item> items;
    /** Optimization, we try the same dateformat index as we had success with before */
    protected int preferreddateformatix = -1;
    /** Optimization, if true we try to fix "wrong" datetime strings when parsing.*/
    protected boolean preprocessdatetime = true;
    /** Order here matters, it may hit too early. */
    protected final static SimpleDateFormat [] dateformats = new SimpleDateFormat[]
    {
        new SimpleDateFormat("E',' dd MMM yyyy HH:mm:ss Z", Locale.US),
        new SimpleDateFormat("E',' dd MMM',' yyyy HH:mm:ss Z", Locale.US),
        new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S Z", Locale.US), // 2013-06-02T16:04:00.000 -0700
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        new SimpleDateFormat("yyyy-MM-dd", Locale.US),
        new SimpleDateFormat("MMM d',' yyyy HH:mm:ss aaa Z", Locale.US),
        new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US),
        new SimpleDateFormat("E',' MMM d',' yyyy HH:mm:ss a Z", Locale.US),
        new SimpleDateFormat("E',' d MMM yyyy HH:mm Z", Locale.US),
        new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US),
        new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US),
        new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy", Locale.US),
        new SimpleDateFormat("E','dd MMM yyyy HH:mm:ss Z", Locale.US),
        new SimpleDateFormat("E',' dd. MMM yyyy HH:mm:ss Z", Locale.US), //Sun, 28. Jun 2009 20:00:35 +0100
        new SimpleDateFormat("E',' dd MMM yyyy KK:mm a z", Locale.US), // Tue, 18 Feb 2014 6:50 am CET
        new SimpleDateFormat("E',' dd MMM yyyy", Locale.US),
        new SimpleDateFormat("E',' dd MMM yyyy HH:mm:ss Z", new Locale("es")),
        new SimpleDateFormat("E',' dd MMM yyyy HH:mm:ss Z", Locale.GERMAN), // Di, 30 Okt 2012 16:46:09 +0100
        new SimpleDateFormat("d MMM yyyy", Locale.US),
        new SimpleDateFormat("dd.MM.yy", Locale.US), // 29.05.14
    };
    
    public ChannelReader()
    {
        this.items = new ArrayList<>();
    }

    public ChannelReader(final String url, final String itemcreatorname)
    {
        this();
        this.url = url;
        this.itemcreatorname = itemcreatorname;
    }

    protected void setType(final ItemCreator.ItemCreatorType type)
    {
        this.type = type;
    }

    /**
     * Simple wrapper for a Date and the index of a successful parse.<br>
     * @see ChannelReader.dateformats
     */
    public static class DateInfo
    {
        private Date date = null;
        private int ix;
        private boolean preprocessdatetime;
        
        public DateInfo()
        {   
        }
        
        public DateInfo(final boolean preprocessdatetime, final int i)
        {
            this.preprocessdatetime = preprocessdatetime;
            this.ix = i;
        }
        
        public long getTimestamp()
        {
            return isFoundCreated() ? this.date.getTime() : System.currentTimeMillis();
        }
        
        public boolean isFoundCreated()
        {
            return this.date != null;
        }
        
        public Date getDate() 
        {
            return this.date;
        }

        public int getIx() 
        {
            return this.ix;
        }

        public boolean isPreprocessdatetime() 
        {
            return this.preprocessdatetime;
        }
    }

    private static boolean isWhitespace(final char c)
    {
        return (c == ' ' || c == '\n' || c == '\r' || c == '\t');
    }

    /**
     * Removes all whitespace from start and end of given String.
     * @param str
     * @return
     */
    public static String smartTrim(final String str)
    {
        if(str == null || "".equals(str) || (!isWhitespace(str.charAt(0)) && !isWhitespace(str.charAt(str.length()-1))))
        {
            return str;
        }
        final StringBuilder s = new StringBuilder(str);
        while(s.length() > 0)
        {
            if(isWhitespace(s.charAt(0)))
            {
                s.deleteCharAt(0);
            }
            else if(isWhitespace(s.charAt(s.length()-1)))
            {
                s.deleteCharAt(s.length()-1);
            }
            else
            {
                break;
            }
        }
        return s.toString();
    }

    /**
     * 
     * @param fieldElement
     * @param tagnames
     * @return A DateInfo object.<br>
     * @see Item.foundrsscreated<br>
     * @see DateInfo
     * 
     */
    protected DateInfo getCreated(final Element fieldElement, final String[] tagnames)
    {
        DateInfo ret = new DateInfo();
        NodeList trs_pd = null;
        for(String tn : tagnames)
        {
            trs_pd = fieldElement.getElementsByTagName(tn);
            if(trs_pd != null && trs_pd.getLength() > 0)
            {
                break;
            }
        }
        if(trs_pd != null && trs_pd.item(0) != null && trs_pd.item(0).getFirstChild() != null)
        {
            final String value = smartTrim(((Element) trs_pd.item(0)).getFirstChild().getNodeValue());
//System.out.println("url:"+url+" value:"+value);
            ret = parseDate(preprocessdatetime, preferreddateformatix, value);
            preferreddateformatix = ret.ix;
            preprocessdatetime = ret.preprocessdatetime;
            if(!ret.isFoundCreated())
            {
                Control.L.log(Level.SEVERE, "Error reading {0} Could not parse {1}", new Object[]{url, value}); 
            }
        }
        return ret;
    }
    
    /**
     * @param s
     * @param sdf
     * @return 
     */
    private static Date getDate(final String s, final SimpleDateFormat sdf)
    {
        try
        {
            final Date d = sdf.parse(s);
            if(Control.L.isLoggable(Level.FINEST))
            {
                Control.L.log(Level.FINEST, "parsed {0} with format:{1} return {2}", new Object[]{s, sdf.toPattern(), d.toString()});
            }
            return d;
        }
        catch(ParseException ex)
        {
//System.err.println(ex.getMessage()+" tried format:"+dateformats[zz].toPattern());
            return null;
        }
    }
    
    /* TODO: this could be done nicer, maybe a loop? */
    private final static Pattern pt_a = Pattern.compile(".*\\d{2}:\\d{2}:\\d{2}Z$");
    private final static Pattern pt_b = Pattern.compile(".*\\.\\d{3}[\\+-]\\d{2}:\\d{2}$");
    private final static Pattern pt_c = Pattern.compile(".*[\\+-]\\d{2}:\\d{2}$");
    private final static Pattern pt_d = Pattern.compile(".*\\d{2}:\\d{2}:\\d{2} UT$");
    private final static Pattern pt_epoch = Pattern.compile("^\\d{10,12}$"); // should last until Fri, 27 Sep 33658 01:46:39 GMT
    
    private final static Matcher datematcher = pt_a.matcher("");

    public synchronized static DateInfo parseDate(final boolean preprocessdatetime, final int preferreddateformatix, String s)
    {
        final DateInfo ret = new DateInfo(preprocessdatetime, preferreddateformatix);
        if(preprocessdatetime)
        {
            if(datematcher.usePattern(pt_a).reset(s).matches())
            {
                /* ts looks like 2013-04-12T19:19:04Z and should be in UTC -> We replace to 2013-04-12T19:19:04+0000 */
                s = s.substring(0, s.length()-1)+"+0000";
            }
            else if(datematcher.usePattern(pt_b).reset(s).matches())
            {
                /* ts looks like 2013-06-02T16:06:55.961-07:00 -> We replace to 2013-06-02T16:06:55.961 -0700 */
                final int ix = s.lastIndexOf(":");
                s = s.substring(0, ix-3)+" "+s.substring(ix-3, ix)+s.substring(ix+1);
            }
            else if(datematcher.usePattern(pt_c).reset(s).matches())
            {
                /* ts looks like 2013-04-11T16:21:25+00:00 -> We replace to 2013-04-11T16:21:25+0000 */
                s = s.substring(0, s.lastIndexOf(":"))+s.substring(s.lastIndexOf(":")+1);
            }
            else if(datematcher.usePattern(pt_d).reset(s).matches())
            {
                /* ts looks like Fri, 31 Aug 2012 17:59:19 UT -> We replace to Fri, 31 Aug 2012 17:59:19 GMT */
                s = s.substring(0, s.length()-2)+" GMT";
            }
            else if(datematcher.usePattern(pt_epoch).reset(s).matches())
            {
                /* ts looks like 1397512800 -> We replace to E',' dd MMM yyyy HH:mm:ss Z */
                s = dateformats[0].format(new Date(Long.parseLong(s) * 1000));
            }
            else
            {
                ret.preprocessdatetime = false;
            }
        }
        if(preferreddateformatix != -1)
        {
            ret.date = getDate(s, dateformats[preferreddateformatix]);
            if(ret.date != null)
            {
                return ret;
            }
        }
        for(int zz=0; zz<dateformats.length; zz++)
        {
            ret.date = getDate(s, dateformats[zz]);
            if(ret.date != null)
            {
                ret.ix = zz;
                return ret;
            }
        }
        return ret;
    }

    public ArrayList<Item> getItems()
    {
        return this.items;
    }

    public abstract boolean read();
}
