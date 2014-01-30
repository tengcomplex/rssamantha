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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.w3c.dom.*;

/**
 *
 * @author teng
 */
public class Control
{
    private final static int DEFAULTSTARTERDELAY                    = 3000;
    private final static String DEFAULTITEMACCEPTORPORT             = "8383";
    private final static String DEFAULTCONCURRENTITEMACCEPTORTHREADS= "20";
    private final static int DEFAULTWRITESLEEP                      = 120000;
    private final static int DEFAULTSHOWLIMIT                       = 30;
    private final static int DEFAULTSTORELIMIT                      = 300;
    private final static int DEFAULTDOWNLOADTHRADSLEEP              = 60000;
    private final static int DEFAULTCONCURRENTDOWNLOAD              = 2;

    public final static int DEFAULTREADITEMSLEEP                    = 3600000;
    public final static boolean DEFAULTTITLEPREFIX                  = true;
    
    /** Short package name */
    public final static String PNAME                                = Control.class.getPackage().getName();
    /* Where we hold running ItemCreators during lifetime */
    private final List<ItemCreator> creators = Collections.synchronizedList(new ArrayList<ItemCreator>());
    /** If true we don't write Item's with created(pubDate) greater $now */
    private final boolean futuredump;
    /** Might be:
     * <ul>
     *  <li>9: BEST_COMPRESSION
     *  <li>1: BEST_SPEED
     *  <li>-1: DEFAULT_COMPRESSION
     *  <li>0: DEFAULT_STRATEGY
     *  <li>9: DEFLATED
     *  <li>1: FILTERED
     *  <li>2: HUFFMAN_ONLY
     *  <li>0: NO_COMPRESSION
     * </ul>
     */
    private final int compression;
    private static Deflater deflater = new Deflater();
    private static Inflater inflater = new Inflater();
    /** Just compress if string is longer than COMPRESSIONLIMIT */
    private final static int COMPRESSIONLIMIT                       = 200;
    private ChannelData[] channels;
    
    private DownloadControl downloadcontrol;
    /** We add ItemCreators when initializing, start them in ItemCreatorsStarter and get rid of this stack eventually. */
    //private LinkedList<ItemCreator> itemcreators = new LinkedList<ItemCreator>();
    private LinkedList<ItemCreatorData> itemcreators = new LinkedList<ItemCreatorData>();
    private final RSSamanthaStatistics stats = new RSSamanthaStatistics();

    public static Logger L;

    public Control(final String configfilename)
    {
        initLogger(PNAME, System.getProperty(PNAME+".logfolder", "")+"rssamantha%g.log", 1024*1024, 10, false, new RssFeedCreatorLogFormatter());
        System.out.println("Logging into "+System.getProperty(PNAME+".logfolder", ""));
        checkVersion();
        L.log(Level.INFO, "{0}{1}", new Object[]{System.getProperty("line.separator"), getAppWelcome(Main.APPNAME, Main.APPVERSION, Main.applicationproperties.getProperty("app.vendor"))});
        L.info("System properties\n");
        L.info(getPropertiesAsString(System.getProperties()));
        L.setLevel(Level.parse(System.getProperty(PNAME+".loglevel", "INFO")));
        this.futuredump =  "true".equals(System.getProperty(PNAME+".futuredump"));
        this.compression =  System.getProperties().containsKey(PNAME+".compression") ? Integer.valueOf(System.getProperty(PNAME+".compression")) : Deflater.NO_COMPRESSION;
        if(this.compression != Deflater.NO_COMPRESSION)
        {
            deflater = new Deflater(compression);
            inflater = new Inflater();
        }
        /*
         * And: google won't send stuff at a pure java client, so we mess
         * with the User-Agent sent...
         */
        if(!System.getProperties().containsKey("http.agent"))
        {
            System.setProperty("http.agent", Main.APPNAME+" "+Main.APPVERSION);
        }
        if(!initChannels(configfilename))
        {
            System.exit(1);
        }
        if(System.getProperties().containsKey(PNAME+".itemstoragefile"))
        {
            initItemStorage();
        }
    }

    private final static class ChannelData
    {
        int ix;
        String name;
        final Map<String, String> channelelements = new HashMap<String, String>(), configelements = new HashMap<String, String>();
        RssFileHandler rsswriter;
        TxtFileHandler txtwriter;
        HtmlFileHandler htmlwriter;
        ItemData itemdata;

        @Override
        public String toString()
        {
            return toShortString()+" rsswriter:["+(rsswriter != null ? rsswriter.toString() : "null")+"] txtwriter:["+(txtwriter != null ? txtwriter.toString() : "null")+"]";
        }

        String toShortString()
        {
            return "ix:"+ix+" name:"+name+" itemdata:["+itemdata.toString()+"]";
        }
    }

    /** Check app.version against http://sourceforge.net/api/file/index/project-id/596676/mtime/desc/limit/1/rss */
    private void checkVersion()
    {
//System.out.println("application.properties:"+getPropertiesAsString(Main.applicationproperties));
        RssChannelReader rcr = new RssChannelReader(Main.applicationproperties.getProperty("app.files"), "checkversion");
        if(rcr.read() && rcr.getItems().size() > 0)
        {
            final String t = rcr.getItems().get(0).getElements().getElementValue("title");
            final int fv = t.indexOf("v");
            int to = t.indexOf("/", fv);
            if(to < 0)
            {
                to = t.length();
            }
            try
            {
                final double v = Double.parseDouble(t.substring(fv+1,to));
                if(Double.parseDouble(Main.APPVERSION) < v)
                {
                    System.out.println("New version "+v+" available. See "+Main.applicationproperties.getProperty("app.home"));
                }
            }
            catch(Exception e)
            {
                System.err.println("Could not parse version numbers.");
            }
        }
        else
        {
            System.err.println("Could not read latest version number.");
        }
        rcr.getItems().clear();
        rcr = null;
    }
    
    private static final class ItemCreatorData
    {
        String title = "", feedtype = "", feedurl = "";
        String[] days, hours;
        boolean appenddescription, titleprefix;
        long sleep;
        int channelindex;
        HashMap<String, Matcher> pattern;
        
        void initItemCreator(final NamedNodeMap nnmfeeds) throws Exception
        {
            sleep = nnmfeeds.getNamedItem("delay") != null && nnmfeeds.getNamedItem("delay").getNodeValue().length() > 0 ? Long.valueOf(nnmfeeds.getNamedItem("delay").getNodeValue()) : DEFAULTREADITEMSLEEP;
            titleprefix = nnmfeeds.getNamedItem("titleprefix") != null && nnmfeeds.getNamedItem("titleprefix").getNodeValue().length() > 0 ? Boolean.valueOf(nnmfeeds.getNamedItem("titleprefix").getNodeValue()) : DEFAULTTITLEPREFIX;
            days = nnmfeeds.getNamedItem("dayofweek") != null && nnmfeeds.getNamedItem("dayofweek").getNodeValue().length() > 0 ? nnmfeeds.getNamedItem("dayofweek").getNodeValue().split(",") : null;
            hours = nnmfeeds.getNamedItem("hourofday") != null && nnmfeeds.getNamedItem("hourofday").getNodeValue().length() > 0 ? nnmfeeds.getNamedItem("hourofday").getNodeValue().split(",") : null;
            appenddescription = nnmfeeds.getNamedItem("appenddescription") != null && "true".equals(nnmfeeds.getNamedItem("appenddescription").getNodeValue());
            for(int ii=0; ii<nnmfeeds.getLength(); ii++)
            {
                final String s = nnmfeeds.item(ii).getNodeName();
                if(s == null || s.length() == 0)
                {
                    continue;
                }
                final String value = nnmfeeds.item(ii).getNodeValue();
                if(s.startsWith("matchpattern_"))
                {
                    initPattern(s.substring(s.indexOf("_")+1), value);
                }
            }
        }
        
        void initPattern(final String key, final String s)
        {
            if(s == null || s.length() == 0)
            {
                return;
            }
            if(pattern == null)
            {
                this.pattern = new HashMap<String, Matcher>();
            }
            final Pattern p = Pattern.compile(s);
            this.pattern.put(key, p.matcher(""));
        }
    }
    
    private boolean initChannels(final String s)
    {   
        ItemCreatorData icd = null;
        try
        {
            Document doc = null;
            final DocumentReader dr = new DocumentReader();
            if("true".equals(System.getProperty(PNAME+".preprocessconfig")))
            {
                final HashMap<String, String> repl = new HashMap<String, String>();
                for(Iterator<Object> iter = System.getProperties().keySet().iterator(); iter.hasNext();)
                {
                    final String next = iter.next().toString();
                    if(next.startsWith(PNAME+".preprocessconfig."))
                    {
                        repl.put(next.substring(next.lastIndexOf(".")+1), System.getProperty(next));
                    }
                }
                doc = dr.getPreprocessedDocument(s, repl);
            }
            else
            {
                doc = dr.getDocument(s);
            }
            final NodeList listoutlines = doc.getElementsByTagName("outline");
            this.channels = new ChannelData[listoutlines.getLength()];
//System.out.println("listoutlines.getLength():"+listoutlines.getLength());
            
            for(int ii=0; ii<listoutlines.getLength(); ii++)
            {
                final Node fieldNode = listoutlines.item(ii);
//System.out.println("fieldnode:"+fieldNode.toString());
                if(fieldNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    this.channels[ii] = new ChannelData();
                    this.channels[ii].ix = ii;
//System.out.println("fieldnode:"+fieldNode.toString());
                    final NamedNodeMap nnm = fieldNode.getAttributes();
                   
                    this.channels[ii].name = nnm.getNamedItem("config.name").getNodeValue();
                    this.channels[ii].itemdata = new ItemData(Integer.valueOf(nnm.getNamedItem("config.showlimit") != null ? nnm.getNamedItem("config.showlimit").getNodeValue() : String.valueOf(DEFAULTSHOWLIMIT)),
                                                       Integer.valueOf(nnm.getNamedItem("config.storelimit") != null ? nnm.getNamedItem("config.storelimit").getNodeValue() : String.valueOf(DEFAULTSTORELIMIT)));
                    for(int zz=0; zz<nnm.getLength(); zz++)
                    {
                        final Node n = nnm.item(zz);
//System.out.println("n:"+n.toString());
                        if(n.getNodeName().startsWith("rsschannel."))
                        {
                            this.channels[ii].channelelements.put(n.getNodeName().substring("rsschannel.".length()), n.getNodeValue());
                        }
                        else if(n.getNodeName().startsWith("config."))
                        {
                            this.channels[ii].configelements.put(n.getNodeName().substring("config.".length()), n.getNodeValue());
                        }
                    }
                    if(nnm.getNamedItem("config.rssfilename") != null)
                    {
                        this.channels[ii].rsswriter = new RssFileHandler(this, new int[]{ii},
                                                                    nnm.getNamedItem("config.rssfilename").getNodeValue(),
                                                                    Integer.valueOf(nnm.getNamedItem("config.rsswritesleep") != null ? nnm.getNamedItem("config.rsswritesleep").getNodeValue() : String.valueOf(DEFAULTWRITESLEEP)));
                    }
                    if(nnm.getNamedItem("config.txtfilename") != null)
                    {
                        this.channels[ii].txtwriter = new TxtFileHandler(this, new int[]{ii},
                                                                    nnm.getNamedItem("config.txtfilename").getNodeValue(),
                                                                    Integer.valueOf(nnm.getNamedItem("config.txtwritesleep") != null ? nnm.getNamedItem("config.txtwritesleep").getNodeValue() : String.valueOf(DEFAULTWRITESLEEP)),
                                                                    nnm.getNamedItem("config.txtdatetimeformat") != null ? nnm.getNamedItem("config.txtdatetimeformat").getNodeValue() : TxtFileHandler.DEFAULTDATETIMETXTPATTERN);
                    }
                    if(nnm.getNamedItem("config.htmlfilename") != null)
                    {
                        this.channels[ii].htmlwriter = new HtmlFileHandler(this, new int[]{ii},
                                                                    nnm.getNamedItem("config.htmlfilename").getNodeValue(),
                                                                    Integer.valueOf(nnm.getNamedItem("config.htmlwritesleep") != null ? nnm.getNamedItem("config.htmlwritesleep").getNodeValue() : String.valueOf(DEFAULTWRITESLEEP)),
                                                                    nnm.getNamedItem("config.htmldatetimeformat") != null ? nnm.getNamedItem("config.htmldatetimeformat").getNodeValue() : HtmlFileHandler.DEFAULTDATETIMEHTMLPATTERN);
                    }
                    final NodeList feeds = fieldNode.getChildNodes();
                    /* obtain ItemCreatorData */
                    for(int zz=0; zz<feeds.getLength(); zz++)
                    {
                        final Node n = feeds.item(zz);
                        if(n.getNodeType() == Node.ELEMENT_NODE)
                        {
//System.out.println("n:"+n.toString());
                            final NamedNodeMap nnmfeeds = n.getAttributes();
                            icd = new ItemCreatorData();
                            icd.title = nnmfeeds.getNamedItem("title").getNodeValue();
                            icd.feedurl = nnmfeeds.getNamedItem("feedUrl").getNodeValue();
                            icd.feedtype = nnmfeeds.getNamedItem("feedtype") != null ? nnmfeeds.getNamedItem("feedtype").getNodeValue() : null;
                            icd.initItemCreator(nnmfeeds);
                            icd.channelindex = ii;
                            itemcreators.push(icd);
                        }
                    }
//System.out.println("channel:"+this.channels[ii].toString());
                }
            }
        }
        catch(Exception ex)
        {
            final String err = "Error reading configfile:"+s
                    +(icd != null && icd.title != null ? " title:"+icd.title : "")
                    +(icd != null && icd.feedtype != null ? " feedtype:"+icd.feedtype : "")
                    +(icd != null && icd.feedurl != null ? " feedurl:"+icd.feedurl : "")
                    +" "+ex.getMessage();
            L.warning(err);
            System.err.println(err);
            ex.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    public int getCompression()
    {
        return this.compression;
    }

    public boolean isFutureDump()
    {
        return this.futuredump;
    }

    /** Helper for  @Itemacceptor*/
    public String[] getAllChannelNames()
    {
        final String[] ret = new String[channels.length];
        for(int ii=0; ii<ret.length; ii++)
        {
            ret[ii] = channels[ii].name;
        }
        return ret;
    }
    
     /** Helper for  @Itemacceptor*/
    public int getChannelCount()
    {
        return this.channels.length;
    }

    /** Helper for  @Itemacceptor*/
    public String getChannelName(final int ix)
    {
        return isValidChannelIndex(ix) ? channels[ix].name : "invalid channel";
    }
    
    /**
     * Helper for HtmlFileHandler title creation.
     * @param ixs The channel indices.
     * @return Channelnames as commaseparated String.
     */
    public String getChannelName(final int[] ixs)
    {
        final StringBuilder ret = new StringBuilder("");
        for(int i : ixs)
        {
            ret.append(getChannelName(i)).append(",");
        }
        ret.deleteCharAt(ret.length()-1);
        return ret.toString();
    }

    /** Helper for @initItemStorage() and @Itemacceptor*/
    public int getChannelIndex(final String name)
    {
        if(name != null && name.length() > 0)
        {
            for(int ii=0; ii<channels.length; ii++)
            {
                if(channels[ii].name.equals(name))
                {
                    return ii;
                }
            }
        }
        return -1;
    }

    public String getStatus()
    {
        return this.stats.getStatus();
    }
    
    private void initItemStorage()
    {
        if(!existsFile(System.getProperty(PNAME+".itemstoragefile")))
        {
            L.log(Level.FINE, "{0} is not existant", System.getProperty(PNAME+".itemstoragefile"));
            return;
        }
        final HashMap<String, SortedSet<Item>> tmp = (HashMap<String, SortedSet<Item>>)readObject(System.getProperty(PNAME+".itemstoragefile"));
        if(tmp != null && tmp.size() > 0)
        {
            for(Iterator<String> iter = tmp.keySet().iterator(); iter.hasNext();)
            {
                final String key = iter.next();
                final int channelindex = getChannelIndex(key);
                if(channelindex == -1)
                {
                    L.log(Level.INFO, "Channel name {0} is unknown, skipping", key);
                    continue;
                }
                final SortedSet<Item> items = tmp.get(key);
                for(Item i : items)
                {
                    addItem(i, channelindex);
                }
                L.log(Level.INFO, "Initialized {0} items from key:{1}", new Object[]{items.size(), key});
            }
        }
        else
        {
            L.log(Level.INFO, "Didn't initialize any items from {0}", System.getProperty(PNAME+".itemstoragefile"));
        }
    }

    /** For logging. */
    public String getChannelDataToShortString(final int ix)
    {
        return channels[ix].toShortString();
    }
    
    public String getChannelDataToShortString(final int[] channelindices)
    {
        final StringBuilder s = new StringBuilder("");
        for(int i : channelindices)
        {
            s.append(getChannelDataToShortString(i)).append(" ");
        }
        s.deleteCharAt(s.length()-1);
        return s.toString();
    }

    public synchronized int getNumberOfAllItems()
    {
        int ret = 0;
        for(int ii=0; ii<channels.length; ii++)
        {
            ret += channels[ii].itemdata.getNumberOfItems();
        }
        return ret;
    }

    protected synchronized Map<String, String> getChannelElements(final int ix)
    {
        return this.channels[ix].channelelements;
    }
    
    public DownloadControl getDownloadControl()
    {
        return this.downloadcontrol;
    }

    private class ItemCreatorsStarter extends Thread
    {
        /**
         *
         * @param url
         * @return Default #ItemCreator.ItemCreatorType.UNKNOWN
         */
        private ItemCreator.ItemCreatorType detectFeedType(final String url)
        {
            BufferedReader in;
            int numlines = 0;
            ItemCreator.ItemCreatorType ret = ItemCreator.ItemCreatorType.UNKNOWN;
            try
            {
                in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
                String inputLine;
                while((inputLine = in.readLine()) != null)
                {
                    //System.out.println(inputLine);
                    if(inputLine.indexOf("</item>") != -1)
                    {
                        ret = ItemCreator.ItemCreatorType.RSSFEED;
                        break;
                    }
                    if(inputLine.indexOf("</entry>") != -1)
                    {
                        ret = ItemCreator.ItemCreatorType.ATOMFEED;
                        break;
                    }
                    if(numlines++ > 1024)
                    {
                        /* Return if some source giving us gigabytes of text */
                        break;
                    }
                }
                in.close();
                L.log(Level.INFO, "Detected feed type:{0} from URL:{1}", new Object[]{ret, url});
            } 
            catch(Exception ex)
            {
                //ignore
            }
            return ret;
        }

        @Override
        @SuppressWarnings("SleepWhileHoldingLock")
        public void run()
        {
            final int sleep = System.getProperties().containsKey(Control.this.getClass().getPackage().getName() + ".starterdelay") ? Integer.parseInt(System.getProperty(Control.this.getClass().getPackage().getName() + ".starterdelay")) : DEFAULTSTARTERDELAY;
            L.log(Level.INFO, "Starting {0} itemcreators, delay:{1}ms", new Object[]{itemcreators.size(), String.valueOf(sleep)});
            try
            {
                while(!itemcreators.isEmpty())
                {
                    ItemCreator itemcreator = null;
                    ItemCreatorData icd = itemcreators.removeLast();
                    if(icd.feedtype == null || icd.feedtype.length() == 0)
                    {
                        icd.feedtype = detectFeedType(icd.feedurl).name;
                    } 
                    else
                    {
                        icd.feedtype = icd.feedtype.toLowerCase();
                    }
                    if(ItemCreator.ItemCreatorType.RSSFEED.name.equals(icd.feedtype) || ItemCreator.ItemCreatorType.UNKNOWN.name.equals(icd.feedtype))
                    {
                        itemcreator = new RssFeedItemCreator(Control.this, icd.feedurl, icd.title);
                        itemcreator.setType(ItemCreator.ItemCreatorType.RSSFEED);
                    } 
                    else if(ItemCreator.ItemCreatorType.SIMPLERSSFEED.name.equals(icd.feedtype))
                    {
                        itemcreator = new SimpleRssFeedItemCreator(Control.this, icd.feedurl, icd.title);
                        itemcreator.setType(ItemCreator.ItemCreatorType.SIMPLERSSFEED);
                    } 
                    else if(ItemCreator.ItemCreatorType.ATOMFEED.name.equals(icd.feedtype))
                    {
                        itemcreator = new AtomFeedItemCreator(Control.this, icd.feedurl, icd.title);
                        itemcreator.setType(ItemCreator.ItemCreatorType.ATOMFEED);
                    } 
                    else if(ItemCreator.ItemCreatorType.RSSIDENTICAFEED.name.equals(icd.feedtype))
                    {
                        itemcreator = new RssFeedItemCreator(Control.this, icd.feedurl, icd.title);
                        itemcreator.setType(ItemCreator.ItemCreatorType.RSSIDENTICAFEED);
                    } 
                    else if(ItemCreator.ItemCreatorType.PODCASTFEED.name.equals(icd.feedtype))
                    {
                        itemcreator = new PodcastFeedItemCreator(Control.this, icd.feedurl, icd.title);
                        itemcreator.setType(ItemCreator.ItemCreatorType.PODCASTFEED);
                        if(channels[icd.channelindex].configelements.containsKey("adddownloaditems"))
                        {
                            ((PodcastFeedItemCreator) itemcreator).setAddLimit(Integer.parseInt(channels[icd.channelindex].configelements.get("adddownloaditems")));
                        }
                    }
                    itemcreator.setSleep(icd.sleep);
                    itemcreator.setTitlePrefix(icd.titleprefix);
                    itemcreator.setDays(icd.days);
                    itemcreator.setHours(icd.hours);
                    itemcreator.setAppendDescription(icd.appenddescription);
                    itemcreator.setPattern(icd.pattern);
                    itemcreator.setChannelindex(icd.channelindex);
                    addItemCreator(itemcreator);
                    Thread.sleep(sleep);
                }
            } 
            catch(Exception ex)
            {
                L.log(Level.SEVERE, "Error during itemcreators startup ", ex);
                ex.printStackTrace(System.err);
                System.exit(1);
            }
            itemcreators = null;
            L.info("Rolled up the itemcreators, runnning a garbage collection.");
            System.gc();
        }
    }

    public void doRss()
    {
        /*
         * Start ItemAcceptor
         */
        if("true".equals(System.getProperty(PNAME+".itemacceptor")))
        {
            final int port = Integer.parseInt(System.getProperty(PNAME+".itemacceptorport",DEFAULTITEMACCEPTORPORT));
            final int[] chix = new int[channels.length];
            int ix = 0;
            for(int ii=0; ii<chix.length; ii++)
            {
                chix[ii] = ix++;
            }
            (new Thread(new ItemAcceptor(this, 
                                        port,
                                        Integer.parseInt(System.getProperty(PNAME+".itemacceptorthreads",DEFAULTCONCURRENTITEMACCEPTORTHREADS)),
                                        chix))).start();
            System.out.println("Accepting HTTP requests on port "+port);
        }
        /*
         * Start DownloadControl
         */
        if(System.getProperties().containsKey(PNAME+".knowndownloadsfile"))
        {
            downloadcontrol = new DownloadControl(System.getProperty(PNAME+".knowndownloadsfile"), 
                                Integer.valueOf(System.getProperty(PNAME+".downloadthreadsleep", String.valueOf(DEFAULTDOWNLOADTHRADSLEEP))), 
                                Integer.valueOf(System.getProperty(PNAME+".concurrentdownloads", String.valueOf(DEFAULTCONCURRENTDOWNLOAD))),
                                this);
            downloadcontrol.setPriority(Thread.MIN_PRIORITY);
            downloadcontrol.start();
        }
        /*
         * Start ItemCreator
         */
        new ItemCreatorsStarter().start();
        /*
         * Start filehandler
         */
        for(int ii=0; ii<channels.length; ii++)
        {
            L.log(Level.FINEST, "channel:{0}", channels[ii].toString());
            if(channels[ii].rsswriter != null)
            {  
                channels[ii].rsswriter.start();
            }
            if(channels[ii].txtwriter != null)
            {
                channels[ii].txtwriter.start();
            }
            if(channels[ii].htmlwriter != null)
            {
                channels[ii].htmlwriter.start();
            }
        }
    }
    
    public synchronized int getStoreLimit(final int[] ixs)
    {
        int ret = 0;
        for(int i : ixs)
        {
            ret += channels[i].itemdata.getStoreLimit();
        }
        return ret;
    }
    
    public synchronized int getShowLimit(final int[] ixs)
    {
        int ret = 0;
        for(int i : ixs)
        {
            ret += channels[i].itemdata.getShowLimit();
        }
        return ret;
    }
    
    protected synchronized int getNumberOfStoreItems(final int[] ixs)
    {
        int ret = 0;
        for(int i : ixs)
        {
            ret += channels[i].itemdata.storesize();
        }
        return ret;
    }

    /*  */
    protected synchronized void addItemCreator(final ItemCreator ic)
    {
        /* Set threadname as creators name for better overview in a profiler */
        ic.setName(ic.getCreatorname());
        L.log(Level.INFO, "Adding and starting {0}", ic.toString());
        ic.start();
        this.creators.add(ic);
    }

    protected synchronized int getNumberOfChannels()
    {
        return channels.length;
    }

    protected synchronized int getNumberOfItemCreators()
    {
        return creators.size();
    }
    
    public synchronized List<Item> getSortedItems(final int[] ixs, final long cutoff, final int numitems, final Pattern pt_title)
    {
        final List<Item> ret = new ArrayList<Item>();
        final boolean defsize = numitems == -1;
        final boolean maxsize = numitems == Integer.MAX_VALUE;
        int limit = defsize || !maxsize ? numitems : Integer.MAX_VALUE;
        for(int ii=0; ii<ixs.length; ii++)
        {
            limit += defsize ? channels[ixs[ii]].itemdata.getShowLimit() : 0;
            ret.addAll(channels[ixs[ii]].itemdata.getSortedItems(defsize ? channels[ixs[ii]].itemdata.getShowLimit() : maxsize ? channels[ixs[ii]].itemdata.getNumberOfItems() : numitems, cutoff, pt_title));
        }
        Collections.sort(ret);
        for(int jj=ret.size()-1; jj>=0; jj--)
        {
            if(ret.size() <= limit)
            {
                break;
            }
            ret.remove(jj);
        }
        if(this.compression != Deflater.NO_COMPRESSION)
        {
            for(Item i : ret)
            {
                if(i.getDescriptionAsBytes() != null)
                {
                    i.putElement("description", extractBytes(i.getDescriptionAsBytes()));
                    i.setDescriptionAsBytes(null);
                }
            }
        }
        return ret;
    }
    
    public List<Item> getSortedItems(final int ix, final long cutoff, final int numitems, final Pattern pt_title)
    {
        return getSortedItems(new int[]{ix}, cutoff, numitems, pt_title);
    }

    public List<Item> getSortedItems(final int ix)
    {
        return getSortedItems(ix, channels[ix].itemdata.getShowLimit(), -1, null);
    }
    
    public List<Item> getSortedItems(final int[] ixs)
    {
        return getSortedItems(ixs, -1, -1, null);
    }

    /* For the shutdownhook in Main */
    public synchronized Map<String, SortedSet<Item>> getAllItems()
    {
        final Map<String, SortedSet<Item>> ret = new HashMap<String, SortedSet<Item>>();
        for(int ii=0; ii<channels.length; ii++)
        {
            ret.put(channels[ii].name, channels[ii].itemdata.getAllItems());
        }
        return ret;
    }

    protected boolean isValidChannelIndex(final int ix)
    {
        return (ix >= 0 && ix <= channels.length+1);
    }

    public enum CountEvent
    {
        ADDED(0),
        INVALID(1),
        TOOOLD(2),
        ALREADYKNOWN(3),
        ADDEDTODOWNLOADQUEUE(4),
        NODOWNLOADCONTROL(5),
        ALREADYINDOWNLOADPROGRESS(6),
        ALREADYKNOWNDOWNLOAD(7),
        DOWNLOADERROR(8),
        FINISHEDDOWNLOAD(9),
        HTTP_GET(10),
        HTTP_POST(11),
        HTTP_NOACCEPT(12),
        PROCESSED(13);

        private int ix = 0;

        private CountEvent(final int i)
        {
            this.ix = i;
        }

        public int getIndex()
        {
            return this.ix;
        }
    }

    /**
     *
     */
    private CountEvent isAddable(final Item i, final int ix)
    {
        stats.count(CountEvent.PROCESSED);
        if(i.getElements().getElementValue("title") == null || i.getElements().getElementValue("title").length() == 0)
        {   
            stats.count(CountEvent.INVALID);
            return CountEvent.INVALID;
        }
        if(channels[ix].itemdata.isTooOld(i))
        {
            stats.count(CountEvent.TOOOLD);
            return CountEvent.TOOOLD;
        }
        if(channels[ix].itemdata.isKnown(i))
        {
            stats.count(CountEvent.ALREADYKNOWN);
            return CountEvent.ALREADYKNOWN;
        }
        stats.count(CountEvent.ADDED);
        return CountEvent.ADDED;
    }

    protected synchronized CountEvent addDownloadItem(final Item i, final int ix)
    {
        final CountEvent atr = isAddable(i, ix);
        if(CountEvent.ADDED != atr)
        {
            return atr;
        }
        if(downloadcontrol == null)
        {
            L.log(Level.SEVERE, "Reference to DownloadControl is null. Consider using -D{0}.knowndownloadsfile. Cannot add downloaditem {1}", new Object[]{this.getClass().getPackage().getName(), i.toShortString()});
            stats.count(CountEvent.NODOWNLOADCONTROL);
            return CountEvent.NODOWNLOADCONTROL;
        }
        if(downloadcontrol.containsItem(i))
        {
            L.log(Level.FINE, "{0}. Not adding already in downloadprogress item {1}", new Object[]{channels[ix].toShortString(), i.toShortString()});
            stats.count(CountEvent.ALREADYINDOWNLOADPROGRESS);
            return CountEvent.ALREADYINDOWNLOADPROGRESS;
        }
        if(downloadcontrol.isKnownFile(i.getElements().getElementValue("contenturl")))
        {
            L.log(Level.FINE, "Already known download {0}", i.getElements().getElementValue("contenturl"));
            stats.count(CountEvent.ALREADYKNOWNDOWNLOAD);
            return CountEvent.ALREADYKNOWNDOWNLOAD;
        }
        i.putElement("contentfolder", channels[ix].configelements.get("downloadfolder"));
        downloadcontrol.addItem(i);
        stats.count(CountEvent.ADDEDTODOWNLOADQUEUE);
        return CountEvent.ADDEDTODOWNLOADQUEUE;
    }

    protected synchronized boolean removeItem(final String title, final long created, final int ix)
    {
        return channels[ix].itemdata.removeItem(title, created);
    }

    public static byte[] compressBytes(final String data)
    {
        try
        {
            deflater.reset();
            byte[] input = data.getBytes("UTF-8");  //the format... data is the total string
            deflater.setInput(input);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);   //we write the generated byte code in this array
            deflater.finish();
            final byte[] buff = new byte[1024];   //segment segment pop....segment set 1024
            while(!deflater.finished())
            {
                final int count = deflater.deflate(buff);       //returns the generated code... index
                baos.write(buff, 0, count);     //write 4m 0 to count
            }
            baos.close();
            final byte[] output = baos.toByteArray();
//System.out.println("Original: "+input.length+" Compressed: "+output.length);
            L.log(Level.FINE, "Original: {0} Compressed: {1}", new Object[]{input.length, output.length});
            return output;
        }
        catch(Exception ex)
        {
            L.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return "".getBytes();
    }

    public static String extractBytes(final byte[] input)
    {
        try
        {
            inflater.reset();
            inflater.setInput(input);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
            final byte[] buff = new byte[1024];
            while(!inflater.finished())
            {
                final int count = inflater.inflate(buff);
                baos.write(buff, 0, count);
            }
            baos.close();
            return new String(baos.toByteArray());
        }
        catch(Exception ex)
        {
            L.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return "decompression failed";
    }

    protected synchronized CountEvent addItem(final Item i, final int ix)
    {
//if(i.getElements().getElementValue("title").startsWith("[SZSE]"))
//{
//    System.out.println("DEBUG");
//}
        final CountEvent atr = isAddable(i, ix);
        if(L.isLoggable(Level.FINE))
        {
            L.log(Level.FINE, "Returnvalue:{0} Channel:{1} Item:{2}", new Object[]{atr.toString(), channels[ix].toShortString(), i.toShortString()});
        }
        if(CountEvent.ADDED != atr)
        {
            return atr;
        }
        if(this.compression != Deflater.NO_COMPRESSION && i.getDescriptionAsBytes() == null && i.getElements().getElementValue("description").length() > COMPRESSIONLIMIT)
        {
            i.setDescriptionAsBytes(compressBytes(i.getElements().removeElementValue("description")));
        }
        channels[ix].itemdata.addItem(i);
        return atr;
    }

    public RSSamanthaStatistics getStatistics()
    {
        return this.stats;
    }

    public final class RSSamanthaStatistics extends Statistics
    {
        private AtomicInteger[] countresults = new AtomicInteger[CountEvent.values().length];

        public RSSamanthaStatistics()
        {
            for(int ii=0; ii<countresults.length; ii++)
            {
                countresults[ii] = new AtomicInteger();
            }
        }

        public int count(final CountEvent air)
        {
            return this.countresults[air.getIndex()].incrementAndGet();
        }

        /**
         * For logging.
         */
        @Override
        public synchronized String getStatus()
        {
            int storelimit = 0;
            for(int ii=0; ii<channels.length; ii++)
            {
                storelimit += channels[ii].itemdata.getStoreLimit();
            }
            final StringBuilder str = new StringBuilder(getNumberOfItemCreators()+" itemcreators in "+getNumberOfChannels()+" channels running. ");
            for(int ii=0; ii<channels.length; ii++)
            {
                str.append("ix:").append(ii).append(" ").append(channels[ii].name).append(" ").append(channels[ii].itemdata.getNumberOfItems()).append("/").append(channels[ii].itemdata.getStoreLimit()).append(" items (").append(formatPercentage(channels[ii].itemdata.getNumberOfItems(), channels[ii].itemdata.getStoreLimit())).append(") ");
            }
            str.append("Holding summary ").append(getNumberOfAllItems()).append("/").append(storelimit).append(" items (").append(formatPercentage(getNumberOfAllItems(), storelimit)).append(" full at total)");
            str.append(downloadcontrol != null ? " known downloads:" + downloadcontrol.getKnownDownloads().size() : "");
            str.append(" ").append(super.getStatus());
            str.append(" numoperations:[");
            for(int ii=0; ii<CountEvent.values().length; ii++)
            {
                str.append(CountEvent.values()[ii]).append(":").append(countresults[CountEvent.values()[ii].getIndex()].get()).append(" ");
            }
            str.deleteCharAt(str.length()-1);
            str.append("] ");
            return str.toString();
        }
    }

    /**
     * 
     * @param s
     * @return
     */
    public synchronized static String decodeHtml(final String s)
    {
//System.out.println("s:"+s);
        return replaceHtmlCharacter(removeHtmlTags(s));
    }

    public synchronized static String removeHtmlTags(final String s)
    {
        return s.trim().replaceAll("\\<.*?\\>", "");
    }

    /** Umlauts etc. */
    public synchronized static String replaceHtmlCharacter(final String s)
    {
        return s.trim().replaceAll("&nbsp;", " ").replaceAll("&auml;", "ä").replaceAll("&Auml;", "Ä").replaceAll("&ouml;", "ö").replaceAll("&Ouml;", "Ö").replaceAll("&uuml;", "ü").replaceAll("&Uuml;", "Ü").replaceAll("&szlig;", "ß").replaceAll("&amp;", "&");
    }

    public synchronized static String writeFile(final String filename, final String content, final boolean append, final String charset)
    {
        if(content == null || content.length() == 0)
        {
            L.fine("No content to write");
            return "empty content";
        }
        L.log(Level.FINE, "Writing filename:{0} content.length():{1} charset:{2}", new Object[]{filename, content.length(), charset});
        PrintWriter pw = null;
        try
        {
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, append), charset)));
            pw.print(content);
            pw.flush();

        }
        catch(IOException ex)
        {
            L.log(Level.SEVERE, "", ex);
            return ex.getMessage();
        }
        finally
        {
            if(pw != null)
            {
                pw.close();
            }
        }
        return null;
    }

    public synchronized static void writeObject(final Object obj, final String filename)
    {
        final long pre = System.currentTimeMillis();
        try
        {
            final FileOutputStream fos = new FileOutputStream(filename);
            final ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
        }
        catch (IOException ex)
        {
            L.log(Level.SEVERE, "", ex);
        }
        L.log(Level.FINE, "Written filename:{0} duration:{1}ms", new Object[]{filename, System.currentTimeMillis()-pre});
    }
    
    public synchronized static List<String> readFile(final File file)
    {
        final List<String> ret = new ArrayList<String>();
        try
        {
            if(!file.exists())
            {
                L.log(Level.WARNING, "File {0} does not exist", file.toString());
                return ret;
            }
            final BufferedReader in = new BufferedReader(new FileReader(file));
            String line = "";
            while((line = in.readLine()) != null)
            {
                ret.add(line);
            }
            in.close();
        }
        catch(Exception ex)
        {
             L.log(Level.SEVERE, "", ex);
        }
        return ret;
    }

    public synchronized static List<String> readFile(final String fileName)
    {
        return readFile(getFile(fileName));
    }

    public static Object readObject(final String filename)
    {
        final long pre = System.currentTimeMillis();
        Object ret = null;
        try
        {
            final FileInputStream fs = new FileInputStream(new File(filename));
            final ObjectInputStream is = new ObjectInputStream(fs);
            ret = is.readObject();
            is.close();
        }
        catch(IOException ex)
        {
             L.log(Level.SEVERE, "", ex);
        }
        catch(ClassNotFoundException cex)
        {
             L.log(Level.SEVERE, "", cex);
        }
        L.log(Level.FINE, "Read filename:{0} done. duration:{1}ms", new Object[]{filename, System.currentTimeMillis()-pre});
        return ret;
    }

    public static File getFile(final String fileName)
    {
        return new File(fileName);
    }

    public static boolean existsFile(final String fileName)
    {
        return fileName != null && getFile(fileName).exists();
    }

    public static String getAppWelcome(final String appname, final String appversion, final String appvendor)
    {
        final StringBuilder ret =  new StringBuilder("");
        ret.append("****************************************************************").append(LINESEP);
        ret.append("*** Application:         ").append(appname).append(LINESEP);
        ret.append("*** Application Version: ").append(appversion).append(LINESEP);
        ret.append("*** Application Vendor:  ").append(appvendor).append(LINESEP);
        ret.append("*** Starttime:           ").append(new SimpleDateFormat().format(Calendar.getInstance().getTime())).append(LINESEP);
        ret.append("*** JVM:                 ").append(System.getProperty("java.vm.name")).append(LINESEP);
        ret.append("*** JVM Vendor:          ").append(System.getProperty("java.vm.vendor")).append(LINESEP);
        ret.append("*** JVM Version:         ").append(System.getProperty("java.vm.version")).append(LINESEP);
        ret.append("*** OS:                  ").append(System.getProperty("os.name")).append(LINESEP);
        ret.append("*** OS Version:          ").append(System.getProperty("os.version")).append(LINESEP);
        ret.append("*** Java Version:        ").append(System.getProperty("java.version")).append(LINESEP);
        ret.append("*** Java Runtime Version:").append(System.getProperty("java.runtime.version")).append(LINESEP);
        ret.append("*** Java Launcher:       ").append(System.getProperty("sun.java.launcher")).append(LINESEP);
        ret.append("*** Java Home:           ").append(System.getProperty("java.home")).append(LINESEP);
        ret.append("*** Max Memory:          ").append(Runtime.getRuntime().maxMemory()).append(LINESEP);
        ret.append("*** Free Memory:         ").append(Runtime.getRuntime().freeMemory()).append(LINESEP);
        ret.append("*** User Home:           ").append(System.getProperty("user.home")).append(LINESEP);
        ret.append("*** User Name:           ").append(System.getProperty("user.name")).append(LINESEP);
        ret.append("*** User Country:        ").append(System.getProperty("user.country")).append(LINESEP);
        ret.append("*** User IP:             ");
        try
        {
            ret.append(InetAddress.getLocalHost().getHostAddress()).append(LINESEP);
        }
        catch(UnknownHostException ue)
        {
            ret.append("Unknown").append(LINESEP);
        }
        ret.append("****************************************************************");
        return ret.toString();
    }

    public final static String LINESEP = System.getProperty("line.separator");
    public final static String LOGLINEDIVIDER = "----------------------------------------------------------------"+LINESEP;
    
    public static String getPropertiesAsString(final Properties p)
    {
        if(p == null || p.size() == 0)
        {
            return (p == null ? "NULL" : "No")+ " properties";
        }
        final StringBuilder ret = new StringBuilder(LOGLINEDIVIDER);
        ret.append(p.size()).append(" propertie").append(p.size() > 1 ? "s" : "").append(LINESEP);
        for(final Iterator<Object> iter = p.keySet().iterator(); iter.hasNext();)
        {
            final String key = iter.next().toString();
            ret.append(key).append("\t").append(p.getProperty(key)).append(iter.hasNext() ? LINESEP : "");
        }
        ret.append(LINESEP).append(LOGLINEDIVIDER);
        return ret.toString();
    }
    
    public static boolean initLogger(final String name, final String filename, final int limit, final int count, final boolean parenthandlers, final Formatter formatter)
    {
        try
        {
            L =  Logger.getLogger(name);
            java.util.logging.FileHandler fh = null;
            if(filename != null)
            {
                fh = new java.util.logging.FileHandler(filename, limit, count, true);
                fh.setFormatter(formatter);
                Handler [] hdl = L.getHandlers();
                for(int ii=0; ii<hdl.length; ii++)
                {
                    L.removeHandler(hdl[ii]);
                }
                if(!parenthandlers)
                {
                    hdl = L.getParent().getHandlers();
                    for(int ii=0; ii<hdl.length; ii++)
                    {
                        L.getParent().removeHandler(hdl[ii]);
                    }
                }
                if(fh != null)
                {
                    L.addHandler(fh);
                }
                L.info("just filehandler from here");
            }
            L.setLevel(Level.FINEST);
        }
        catch(IOException e)
        {
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    private static class RssFeedCreatorLogFormatter extends Formatter
    {
        private final Date dat = new Date();
        private final static String format = "{0,date} {0,time}";
        private MessageFormat formatter = new MessageFormat(format);

        private Object args[] = new Object[1];
        private String lineSeparator = System.getProperty("line.separator");

        /**
         * Format the given LogRecord.
         * @param record the log record to be formatted.
         * @return a formatted log record
         */
        public synchronized String format(final LogRecord record)
        {
            final StringBuilder sb = new StringBuilder();
            // Minimize memory allocations here.
            dat.setTime(record.getMillis());
            args[0] = dat;
            final StringBuffer text = new StringBuffer();
            formatter.format(args, text, null);
            sb.append(text);
            sb.append(" ");
            if(record.getSourceClassName() != null) 
            {
                sb.append(record.getSourceClassName());
            } 
            else 
            {
                sb.append(record.getLoggerName());
            }
            if(record.getSourceMethodName() != null) 
            {
                sb.append(" ");
                sb.append(record.getSourceMethodName());
            }
            sb.append(" ");
            final String message = formatMessage(record);
            sb.append(record.getLevel().getLocalizedName());
            sb.append(": ");
            sb.append(message);
            sb.append(lineSeparator);
            if(record.getThrown() != null)
            {
                try
                {
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    sb.append(sw.toString());
                }
                catch(Exception ex)
                {
                    /* ignore */
                }
            }
            return sb.toString();
        }
    }
}


