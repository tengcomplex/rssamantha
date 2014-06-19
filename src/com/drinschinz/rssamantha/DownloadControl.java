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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

/**
 *
 * @author teng
 */
public class DownloadControl extends Thread implements Observer
{
    /** Files we downloaded older 300 days are going to be removed from cache.<br> 
     * @see #knownDownloads
     * @see #update(java.util.Observable, java.lang.Object) 
     */
    private final static long MAX_OLD_KNOWNDOWNLOADS = 1000l * 60l * 60l * 24l * 300l;
    
    private final LinkedList<Item> dlqueue = new LinkedList<>();
    private final LinkedList<Observable> dlactive = new LinkedList<>();
    private final long sleep;
    private final int concurrentlimit;
    private final String knownDownloadsFilename;
    /** Filename->lastaccessed */
    private Map<String, Long> knownDownloads;
    private final Control control;

    public DownloadControl(final String knownDownloadsFilename, final int sleep, final int concurrentlimit, final Control control)
    {
        this.knownDownloadsFilename = knownDownloadsFilename;
        this.sleep = sleep;
        this.concurrentlimit = concurrentlimit;
        this.control = control;
        initKnownDownloads();
    }

    private void initKnownDownloads()
    {
        if(!Control.existsFile(knownDownloadsFilename))
        {
            Control.L.log(Level.FINE, "{0} is not existant", knownDownloadsFilename);
            knownDownloads = new HashMap<>();
            return;
        }
        knownDownloads = (HashMap<String, Long>)Control.readObject(knownDownloadsFilename);
        Control.L.log(Level.INFO, "Initialized {0} known downloads.", new Object[]{knownDownloads.size()});
        Control.L.log(Level.FINEST, "Known downloads:{0}", Arrays.toString(knownDownloads.keySet().toArray()));
    }

    public Map<String, Long> getKnownDownloads()
    {
        return this.knownDownloads;
    }

    public void addItem(final Item i)
    {
        dlqueue.add(i);
    }

    public boolean isKnownFile(final String filename)
    {
        if(knownDownloads.containsKey(filename))
        {
            knownDownloads.put(filename, System.currentTimeMillis());
            Control.L.log(Level.FINEST, "Reset last accessed known filename {0}", new Object[]{filename});            
            return true;
        }
        return false;
    }

    private void startDownload(final Item i) throws Exception
    {
        final Download dl = new Download(i);
        dl.addObserver(this);
        dlactive.add(dl);
//System.out.println("started download "+url+" "+targetfolder);
        Control.L.log(Level.INFO, "Started download {0} {1} dlactivequeuesize:{2} dlqueuesize:{3}", new Object[]{i.getElements().getElementValue("contenturl"), i.getElements().getElementValue("contentfolder"), dlactive.size(), dlqueue.size()});
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run()
    {
        Control.L.info("Started");
        for(;;)
        {
            try
            {
                Thread.sleep(sleep);
//System.out.println("dlqueue.size():"+dlqueue.size()+" dlactive.size():"+dlactive.size());
                if(dlqueue.size() != 0 && dlactive.size() < concurrentlimit)
                {
                    final Item i = dlqueue.removeFirst();
                    startDownload(i);
                }
            }
            catch(Exception ex)
            {
                control.getStatistics().count(Control.CountEvent.DOWNLOADERROR);
                Control.L.log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean containsItem(final Item i)
    {
        if(dlqueue.contains(i))
        {
            return true;
        }
        for(int ii=0; ii<dlactive.size(); ii++)
        {
            final Download d = (Download)dlactive.get(ii);
            if(d.getItem().equals(i))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Called on complete download.
     */
    private void removeOldKnownDownloads()
    {
        final long now = System.currentTimeMillis();
        for(Iterator<String> iter = knownDownloads.keySet().iterator(); iter.hasNext();)
        {
            final String fn = iter.next();
//System.out.println("fn:"+fn+" la:"+knownDownloads.get(fn)+" now:"+now+" diff:"+(now - knownDownloads.get(fn))+" MAX_OLD_KNOWNDOWNLOADS:"+MAX_OLD_KNOWNDOWNLOADS);                    
            if(now-knownDownloads.get(fn) > MAX_OLD_KNOWNDOWNLOADS)
            {
                iter.remove();
                Control.L.log(Level.FINE, "Removed known download {0}", new Object[]{fn}); 
            }
        }
    }
    
    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Override
    public void update(final Observable arg0, final Object arg1)
    {
        final Download dl = (Download) arg0;
        if(dl.getStatus() == Download.COMPLETE)
        {
            dlactive.remove(dl);
            synchronized(knownDownloads)
            {
                knownDownloads.put(dl.getUrl(), System.currentTimeMillis());
                Control.L.log(Level.FINEST, "Put to known downloads {0}", new Object[]{dl.getUrl()});
                removeOldKnownDownloads();
            }
            control.getStatistics().count(Control.CountEvent.FINISHEDDOWNLOAD);
            Control.L.log(Level.INFO, "Finished download {0} size:{1} adding item {2}", new Object[]{dl.getUrl(), dl.getSize(), dl.getItem().toShortString()});
            this.control.addItem(dl.getItem(), Integer.parseInt(dl.getItem().getElements().getElementValue("itemindex")));
            Control.writeObject(this.knownDownloads, knownDownloadsFilename);
            Control.L.log(Level.INFO, "Written {0} known downloads.", new Object[]{knownDownloads.size()});
        }
        else if(dl.getStatus() == Download.ERROR)
        {
            dlactive.remove(dl);
            control.getStatistics().count(Control.CountEvent.DOWNLOADERROR);
            Control.L.log(Level.SEVERE, "Error at download {0} size:{1}", new Object[]{dl.getUrl(), dl.getSize()});
        }
    }
}
