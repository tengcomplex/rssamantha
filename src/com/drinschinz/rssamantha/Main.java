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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

/**
 *
 * @author teng
 */
public class Main
{
    public final static Properties applicationproperties = new Properties();
    static
    {
        try(InputStream is = Main.class.getResourceAsStream("application.properties"))
        {
            applicationproperties.load(is);
        }
        catch(IOException e)
        {
            // Shouldn't happen, unless we rename our properties file.
            e.printStackTrace(System.err);
        }
    }
    /** Global access. Properties @see Main.applicationproperties is loaded at this point. */
    public final static String APPNAME = applicationproperties.getProperty("app.name");
    public final static String APPVERSION = applicationproperties.getProperty("app.version");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {   
        if(args.length != 1 || ((args.length == 1 && ("-?".equals(args[0]) || "-h".equals(args[0]) || "--help".equals(args[0])))))
        {
            System.out.println(APPNAME+" "+APPVERSION);
            System.out.println(applicationproperties.getProperty("app.usage"));
            System.exit(0);
        }
        else if((args.length == 1 && ("-version".equals(args[0]) || "--version".equals(args[0]))))
        {
            System.out.println(APPNAME+" "+APPVERSION);
            System.exit(0);
        }
        else
        {
            System.out.println(Control.LINESEP+Control.getAppWelcome(APPNAME, APPVERSION, applicationproperties.getProperty("app.vendor"))+Control.LINESEP+"Running...");
            final Control control = new Control(args[0]);
            try
            {
                Runtime.getRuntime().addShutdownHook(new ShutdownThread(control));
            }
            catch(Throwable t)
            {
                // we get here when the program is run with java
                // version 1.2.2 or older
                System.err.println("[Main thread] Could not add Shutdown hook. Probably JVM is too old or wrong. "+t.getMessage());
            }
            control.doRss();
        }
    }
}

/**
 * The ShutdownThread is the thread we pass to the addShutdownHook method
 */
class ShutdownThread extends Thread
{
    private final Control control;

    public ShutdownThread(final Control control)
    {
        super();
        this.control = control;
    }

    @Override
    public void run()
    {
        System.out.println("Shutting down");
        if(System.getProperties().containsKey(Control.PNAME+".itemstoragefile"))
        {
            final Map<String, SortedSet<Item>> hm = control.getAllItems();
            Control.writeObject(hm, System.getProperty(Control.PNAME+".itemstoragefile"));
            System.out.println("Wrote items to "+System.getProperty(Control.PNAME+".itemstoragefile"));
        }
        if(control.getDownloadControl() != null && control.getDownloadControl().getKnownDownloads().size() > 0)
        {
            Control.writeObject(control.getDownloadControl().getKnownDownloads(), System.getProperty(Control.PNAME+".knowndownloadsfile"));
            System.out.println("Wrote known downloads to "+System.getProperty(Control.PNAME+".knowndownloadsfile"));
        }
        System.out.println("Have a nice day");
    }
}
