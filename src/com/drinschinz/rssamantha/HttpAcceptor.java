/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.drinschinz.rssamantha;

import static com.drinschinz.rssamantha.ClientThread.BR;
import static com.drinschinz.rssamantha.ClientThread.EOL;
import static com.drinschinz.rssamantha.ClientThread.HTTP_BAD_REQUEST;
import static com.drinschinz.rssamantha.ClientThread.HTTP_NOT_FOUND;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpConnectionFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.impl.DefaultBHttpServerConnectionFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLServerSocketFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.http.Header;
import org.w3c.dom.Document;

/**
 *
 * @author teng
 */
public class HttpAcceptor
{
    private final Control control;
    private final String generatorHtml, opml;
    private final AdditionalHtml additionalHtml;
    /** JavaScript validation for the generator */
    private final static String checkInput = new Scanner(ItemAcceptor.class.getResourceAsStream("checkInput.js")).useDelimiter("\\A").next();
    private final SimpleDateFormat htmlhandlerdatetimeformat = new SimpleDateFormat(System.getProperty(Control.PNAME+".htmlfiledatetimeformat", HtmlFileHandler.DEFAULTDATETIMEHTMLPATTERN));
    private final String host = System.getProperty(Control.PNAME+".itemacceptorhost", "localhost");
    private final int port;
    private final int[] channelixs;
    
    private final static SimpleDateFormat CUTOFF_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//1999-12-22 23:59:59
    public final static String ALL = "ALL";
    public final static String TODAY = "TODAY";
    
    private final static Transformer transformer = newTransformer();
    private static Transformer newTransformer()
    {
        try
        {
            return TransformerFactory.newInstance().newTransformer();
        }
        catch(TransformerConfigurationException ex)
        {
            Control.L.log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public HttpAcceptor(final Control control, final int port, final int[] channelixs)
    {
        this.control = control;
        this.port = port;
        this.channelixs = channelixs;
        this.generatorHtml = initGeneratorHtml(this.control.getAllChannelNames());
        this.opml = initOpml(this.control.getAllChannelNames());
        additionalHtml = new AdditionalHtml();
    }
    
    public void start()
    {
        // Set up the HTTP protocol processor
        HttpProcessor httpproc = HttpProcessorBuilder.create()
                .add(new ResponseDate())
                .add(new ResponseServer("Test/1.1"))
                .add(new ResponseContent())
                .add(new ResponseConnControl()).build();

        // Set up request handlers
        UriHttpRequestHandlerMapper reqistry = new UriHttpRequestHandlerMapper();
        reqistry.register("*", new HttpHandler("/home/teng/projects/elementalwebserver/httpcomponents-core-4.3.2/tutorial/"));

        // Set up the HTTP service
        HttpService httpService = new HttpService(httpproc, reqistry);
        try
        {   
            Thread t = new RequestListenerThread(port, httpService, null);
            t.setDaemon(false);
            t.start();
        } 
        catch(IOException ex)
        {
            Logger.getLogger(HttpAcceptor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String getGeneratorHtml()
    {
        return this.generatorHtml;
    }
    
    private String initGeneratorHtml(final String[] channels)
    {
        final StringBuilder s = new StringBuilder(4096);        
        s.append("<HTML>"+ClientThread.EOL);
        s.append("<HEAD>"+ClientThread.EOL);
        s.append("<TITLE>").append(Main.APPNAME).append("</TITLE>"+ClientThread.EOL);
        s.append("</HEAD>"+ClientThread.EOL);
        s.append("<BODY>"+ClientThread.EOL);
        s.append(ClientThread.BR+ClientThread.BR+ClientThread.EOL);
        s.append(checkInput);
        s.append("<FORM name=\"generate\" action=\"/\" method=\"get\" target=\"_blank\" onsubmit=\"return checkInput()\">"+ClientThread.EOL);
        s.append("<TABLE>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>Channel:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<SELECT name=\"channel\" size=\"5\" multiple>"+ClientThread.EOL);
        for(int ii=0; ii<channels.length; ii++)
        {
            s.append("<OPTION").append(ii == 0 ? " selected>" : ">").append(channels[ii]).append("</OPTION>"+ClientThread.EOL);
        }
        s.append("</SELECT>"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>Number of Items:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT name=\"numitems\" type=\"text\" size=\"10\" maxlength=\"12\" value=\"100\">"+ClientThread.BR+ClientThread.EOL);
        s.append("<FONT size=\"1\">[Integer or ALL]</FONT>"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>Cutoff:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT name=\"cutoff\" type=\"text\" size=\"19\" maxlength=\"19\" value=\"TODAY\">"+ClientThread.BR+ClientThread.EOL);
        s.append("<FONT size=\"1\">[yyyy-mm-dd hh:mm:ss or milliseconds from epoch or TODAY]</FONT>"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>Refresh:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT name=\"refresh\" type=\"text\" size=\"19\" maxlength=\"19\" value=\"\">"+ClientThread.BR+ClientThread.EOL);
        s.append("<FONT size=\"1\">[In seconds, works for HTML]</FONT>"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>RegExp Title:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT name=\"search_title\" type=\"text\" size=\"30\" maxlength=\"100\">"+ClientThread.BR+ClientThread.EOL);
        s.append("<FONT size=\"1\">[Java compliant regexp]</FONT>"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>Unique Title:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT name=\"uniquetitle\" type=\"checkbox\">"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>Type:</TD>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT type=\"radio\" name=\"type\" value=\"xml\" checked>XML"+ClientThread.EOL);
        s.append("<INPUT type=\"radio\" name=\"type\" value=\"html\">HTML"+ClientThread.EOL);
        s.append("<INPUT type=\"radio\" name=\"type\" value=\"txt\">TXT"+ClientThread.EOL);
        s.append("</TD>"+ClientThread.EOL);
        s.append("</TR>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("<TD>"+ClientThread.EOL);
        s.append("<INPUT type=\"submit\" value=\"Generate\">"+ClientThread.EOL);
        s.append("</TD><TD></TD>"+ClientThread.EOL);
        s.append("<TR>"+ClientThread.EOL);
        s.append("</TABLE>"+ClientThread.EOL);
        s.append("</FORM>"+ClientThread.EOL);
        s.append(ClientThread.BR+ClientThread.BR+ClientThread.EOL+"Channels:<UL>"+ClientThread.EOL);
        for(String c : channels)
        {
            s.append("<LI><A HREF=\"/channel=").append(c).append("\">").append(c).append("</A></LI>"+ClientThread.EOL);
        }
        s.append(ClientThread.BR+ClientThread.EOL+"</UL>"+ClientThread.EOL);
        s.append(ClientThread.EOL+"All Channels as OPML:<UL>"+ClientThread.EOL);
        s.append("<LI><A HREF=/opml>").append(Main.APPNAME.toLowerCase()).append(".opml</A></LI>"+ClientThread.EOL);
        s.append(ClientThread.BR+ClientThread.EOL+"</UL>"+ClientThread.EOL);
        s.append("</BODY>"+ClientThread.EOL);
        s.append("</HTML>");
        return s.toString();
    }
    
    private String initOpml(final String[] channels)
    {
        final StringBuilder s = new StringBuilder(512);
        s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        s.append("<opml>");
        s.append("<head>");
        s.append("<title>").append(Main.APPNAME).append("</title>");
        s.append("</head>");
        s.append("<body>");
        for(String c : channels)
        {
            s.append("<outline type=\"rss\" text=\"").append(c).append("\" xmlUrl=\"http://").append(host).append(":").append(port).append("/channel=").append(c).append("\" />");
        }
        s.append("</body>");
        s.append("</opml>");
        return s.toString();
    }
    
    public static class AdditionalHtml
    {
        private final String css, script, onload, body;
        
        private AdditionalHtml()
        {
            css = init("cssfile");
            script = init("scriptfile");
            onload = init("onloadfile");
            body = init("bodyfile");
        }
        
        private String init(final String what)
        {
            if(System.getProperties().containsKey(Control.PNAME+"."+what))
            {   
                final StringBuilder s = new StringBuilder();
                for(String ln : Control.readFile(System.getProperties().getProperty(Control.PNAME+"."+what).toString()))
                {
                    s.append(ln);
                    s.append(Control.LINESEP);
                }
                if(s.length() == 0)
                {
                    Control.L.log(Level.WARNING, "{0} was not available or empty", what);
                }
                else
                { 
                    Control.L.log(Level.INFO, "{0} initialized, length:{1}", new Object[]{what, s.length()});
                    return s.toString();
                }
            }
            return null;
        }
        
        public String getCss()
        {
            return css;
        }

        public String getScript()
        {
            return script;
        }

        public String getOnload()
        {
            return onload;
        }

        public String getBody()
        {
            return body;
        }
    }

    class HttpHandler implements HttpRequestHandler
    {

        private final String docRoot;
        private HttpResponse response;
        private String target;

        public HttpHandler(final String docRoot)
        {
            super();
            this.docRoot = docRoot;
        }

        @Override
        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException
        {
            this.response = response;
System.out.println("context: "+context);            
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if(!method.equals("GET") && !method.equals("POST"))
            {
                throw new MethodNotSupportedException(method + " method not supported");
            }
            target = request.getRequestLine().getUri();
System.out.println("target: "+target);
            Header[] hd = request.getAllHeaders();
            for(Header h : hd)
            {
                System.out.println("header:"+h.getName()+" "+h.getValue());
            }
            Map<String,String> args = Collections.emptyMap();
            if(request instanceof HttpEntityEnclosingRequest)
            {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                byte[] entityContent = EntityUtils.toByteArray(entity);
                System.out.println("Incoming entity content (bytes): " + entityContent.length);
                args = getArgsFromUrl(new String(entityContent));
            }
            else
            {
                args = getArgsFromUrl(target.substring(1));
            }
            System.out.println("args:"+args.toString());
            if(method.equals("POST"))
            {
                //handlePOST(args);
            }
            else
            {
                handleGET(args);
            }
        }
        /**
         * TODO: It seems this can be slightly optimized.
         * @param s
         * @return
         * @throws Exception 
         */
        private Map<String,String> getArgsFromUrl(final String s)
        {
            final String [] el = (s.startsWith("?") ? s.substring(1) : s).split("&");
            final HashMap<String, String> hm = new HashMap<>();
            final int[] channelix = new int[2];
            for(int ii=0; ii<el.length; ii++)
            {
                try
                {
                    final String t = URLDecoder.decode(el[ii], "UTF-8");
                    if(t.indexOf("=") != -1)
                    {
                        final String[] tok = t.split("=");
                        if("channel".equals(tok[0]))
                        {
                            tok[0]+=(channelix[0]++);
                        }
                        else if("ix".equals(tok[0]))
                        {
                            tok[0]+=(channelix[1]++);
                        }
                        hm.put(tok[0], tok.length > 1 ? tok[1] : "");
                    }
                    else if(t.length() > 0)
                    {
                        hm.put(t, "");
                    }
                } 
                catch (UnsupportedEncodingException ex)
                {
                    Logger.getLogger(HttpAcceptor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return hm;
        }
        
          /**
          * 
          * @param hm input GET arguments. channel and ix keys indexed.
          * @return 
          */
         private int[] getChannels(final Map<String, String> hm)
         {   
             if(hm.containsKey("allchannels"))
             {
                 Control.L.log(Level.FINEST, "returning all {0} channels", new Object[]{channelixs.length});
                 return channelixs;
             }
             final List<Integer> tmp = new ArrayList<>(control.getChannelCount());
             for(int ii=0; ii<control.getChannelCount(); ii++)
             {
                 int addix = control.getChannelIndex(hm.get("channel"+ii)); // returns -1 for an invalid
                 if(addix != -1 && !tmp.contains(addix))
                 {
                     tmp.add(addix);
                 }
                 try
                 {
                     addix = Integer.parseInt(hm.get("ix"+ii)); 
                     if(addix >= 0 && addix < control.getChannelCount() && !tmp.contains(addix))
                     {
                         tmp.add(addix);
                     }
                 }
                 catch(NumberFormatException n)
                 {
                     // ignore
                 }
             }
             if(tmp.isEmpty())
             {
                 Control.L.log(Level.FINEST, "returning no channels");
                 return new int[0];
             }
             final int[] ret = new int[tmp.size()];
             for(int ii=0; ii<tmp.size(); ii++)
             {
                 ret[ii] = tmp.get(ii);
             }
             Control.L.log(Level.FINEST, "returning {0} ix:{1}", new Object[]{ret.length, Arrays.toString(ret)});
             return ret;
         }
        
        private void handleGET(final Map<String, String> hm)
        {
            if(hm.isEmpty() || hm.containsKey("generator"))
            {
                doGenerator();
                return;
            }
            if(hm.size() == 1 && hm.containsKey("favicon.ico"))
            {
                httpAnswer(HttpStatus.SC_NOT_FOUND, "File not found", Main.APPNAME);
                Control.L.log(Level.FINEST, "No favicon support");
                return;
            }
            if(hm.containsKey("status"))
            {
                //final String ha = socket.getInetAddress().getHostAddress();
                String ha = "127.0.0.1";
                if("0:0:0:0:0:0:0:1".equals(ha) || "127.0.0.1".equals(ha))
                {
                    doStatus();
                }
                else
                {
                    doGenerator();
                }
                return;
            }
            if(hm.containsKey("opml"))
            {
                doOpml();
                return;
            }
            final int[] cis = getChannels(hm);
            if(cis.length == 0)
            {
                httpAnswer(HTTP_BAD_REQUEST, "Bad GET Request, invalid channel(s) "+target+BR+BR+getHttpUsage(true), Main.APPNAME);
                return;
            }
            int numitems = -1;
            if(hm.containsKey("numitems"))
            {
                numitems = ALL.equals(hm.get("numitems").toUpperCase()) ? Integer.MAX_VALUE : Integer.parseInt(hm.get("numitems"));
            }
            long cutoff = -1;
            if(hm.containsKey("cutoff") && hm.get("cutoff").length() > 0)
            {
                if(TODAY.equals(hm.get("cutoff").toUpperCase()))
                {
                    final Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    cutoff = cal.getTimeInMillis();
                }
                else
                {
                    synchronized(CUTOFF_FORMATTER)
                    {
                        try
                        {
                            cutoff = CUTOFF_FORMATTER.parse(hm.get("cutoff")).getTime();
                        }
                        catch(ParseException pe)
                        {
                            try
                            {
                                cutoff = Long.parseLong(hm.get("cutoff")); // if we fail here we throw in caller
                            }
                            catch(NumberFormatException nfe)
                            {
                                //throw new Exception("Unable to parse cutoff "+hm.get("cutoff"));
                            }
                        }
                    }
                }
            }
            final String type = hm.containsKey("type") ? hm.get("type") : "xml";
            if(!"xml".equals(type) && !"html".equals(type) && !"txt".equals(type))
            {
                httpAnswer(HTTP_BAD_REQUEST, "Bad GET Request, unknown type:"+hm.toString()+BR+BR+getHttpUsage(true), Main.APPNAME);
                Control.L.log(Level.WARNING, "Unknown type:{0}", type);
                return;
            }
            String refresh = Control.DEFAULT_HTTP_REFRESH;
            if(hm.containsKey("refresh") && hm.get("refresh").length() > 0)
            {
                refresh = hm.get("refresh");
                try
                {
                    Integer.parseInt(refresh);
                }
                catch(NumberFormatException e)
                {
                    httpAnswer(HTTP_BAD_REQUEST, "Bad GET Request, invalid refresh:"+hm.toString()+BR+BR+getHttpUsage(true), Main.APPNAME);
                    Control.L.log(Level.WARNING, "Invalid refresh:{0}", refresh);
                    return;
                }
            }
            boolean uniqueTitle = false;
            if(hm.containsKey("uniquetitle"))
            {
                final String ut = hm.get("uniquetitle");
                if("1".equals(ut) || "on".equals(ut) || "true".equals(ut))
                {
                    uniqueTitle = true;
                }
            }
            Pattern pt_title = null;
            if(hm.containsKey("search_title") && hm.get("search_title").length() > 0)
            {
                try
                {
    //System.out.println("compile pattern:"+hm.get("search_title"));                
                    pt_title = Pattern.compile(hm.get("search_title"));
                }
                catch(PatternSyntaxException e)
                {
                    httpAnswer(HTTP_BAD_REQUEST, "Bad GET Request, invalid search pattern:"+hm.get("search_title")+BR+BR+getHttpUsage(true), Main.APPNAME);
                    Control.L.log(Level.WARNING, "Invalid pattern:{0}", hm.get("search_title"));
                    return;
                }
            }
            final List<Item> items = control.getSortedItems(cis, cutoff, numitems, pt_title, "xml".equals(type), uniqueTitle);
    //System.out.println("numitems:"+items.size());
            response.setStatusCode(HttpStatus.SC_OK);
            StringBuilder answer = new StringBuilder(512);
            
//            out.print("HTTP/1.0 "+HTTP_OK+" OK"+EOL);
//            out.print("Content-type: text/"+type+"; charset=utf-8"+EOL);
//            out.print("Server: "+Main.APPNAME+"/"+Main.APPVERSION+EOL);
//            final Date dt = new Date();
//            out.print("Date: "+dt+EOL);
//            synchronized(HTTP_RESPONSE_FORMATTER)
//            {
//                out.print("Last-Modified: "+HTTP_RESPONSE_FORMATTER.format(dt)+EOL+EOL);
//            }
            if("html".equals(type))
            {
                answer.append((new HtmlFileHandler(control, cis, null, 0, htmlhandlerdatetimeformat)).getContentAsString(items, refresh, additionalHtml));
            }
            else if("txt".equals(type))
            {
                answer.append((new TxtFileHandler(control, cis, null, 0, TxtFileHandler.DEFAULT_DATETIME_TXT_PATTERN)).getContentAsString(items));
            }
            else
            {
                final Document doc = (new RssFileHandler(control, cis, null, 0)).getDocument(items);
                synchronized(transformer)
                {
                    try
                    {
                        transformer.reset();
                        transformer.setOutputProperty("indent", "yes");
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        transformer.transform(new DOMSource(doc), new StreamResult(os));
                        answer.append(new String(os.toByteArray(), "UTF-8"));
                    } 
                    catch(TransformerException | UnsupportedEncodingException ex)
                    {
                        Logger.getLogger(HttpAcceptor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            StringEntity entity = new StringEntity(
                        answer.toString(),
                        ContentType.create("text/"+type, "UTF-8"));
            response.setEntity(entity);
            Control.L.log(Level.INFO, "Served channel:{0} type:{1} number of items:{2}", new Object[]{control.getChannelName(cis), type, items.size()});
            items.clear();
        }
        
        private void handlePOST(final Map<String, String> hm)
        {
            
        }
        
        private void doStatus()
        {
            final String msg = "java version:"+System.getProperty("java.runtime.version")+BR
                    +"app version:"+Main.APPNAME+" "+Main.APPVERSION+BR
                    +"ignorefutureitems:"+Control.ignorefutureitems+BR
                    +"compression:"+control.getCompression()+BR
                    +"loglevel:"+Control.L.getLevel()+"</b>"+BR+BR
                    +"Status:"+BR+control.getStatus();
            httpAnswer(HttpStatus.SC_OK, msg, Main.APPNAME);
        }
        
        private void doOpml()
        {
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity entity = new StringEntity(
                        opml,
                        ContentType.create("text/xml", "UTF-8"));
            response.setEntity(entity);
            System.out.println("Served opml:"+opml);
        }
        
        private void doGenerator()
        {
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity entity = new StringEntity(
                        generatorHtml,
                        ContentType.create("text/html", "UTF-8"));
            response.setEntity(entity);
            System.out.println("Served generator");
        }
        
        private String getHttpUsage(final boolean get)
        {
            final StringBuilder ret = new StringBuilder("Usage:"+EOL);
            if(get)
            {
                ret.append("<OL>"+EOL);
                    ret.append("<LI>http://myhost/status</LI>"+EOL);
                    ret.append("<LI>http://myhost/opml</LI>"+EOL);
                    ret.append("<LI>http://myhost/[channel=$NAME1&channel$NAMEn][ix=$IX1&ix$IXn][&numitems={ALL|number}][&type={xml|html|txt}][&refresh={seconds}][&cutoff={TODAY|yyyy-MM-dd HH:mm:ss|epochtimeinmillis}][&uniquetitle={1|0}]"+BR+EOL);
                        ret.append("<SMALL>Available channels:").append(Arrays.toString(control.getAllChannelNames())).append("</SMALL>");
                    ret.append("</LI>"+EOL);
                ret.append("</OL>"+EOL);
            }
            else
            {
                ret.append("<UL>"+EOL);
                    ret.append("<LI>channel(String, channelname, defined in config.name)</LI>"+EOL);
                    ret.append("<LI>ix (Integer, the channel index)</LI>"+EOL);
                    ret.append("<LI>title (String)</LI>"+EOL);
                    ret.append("<LI>description (String)</LI>"+EOL);
                    ret.append("<LI>created (Long, milliseconds, between the current time and midnight, January 1, 1970 UTC)</LI>"+EOL);
                    ret.append("<LI>link (String)</LI>"+EOL);
                    ret.append("<LI>remove (Integer, 1 means true, default 0)</LI>"+EOL);
                    ret.append("<LI></LI>"+EOL);
                ret.append("</UL>"+EOL);
            }
            return ret.toString();
        }
        
        private void httpAnswer(final int returncode, final boolean hr_returncode, final String body, final String title)
        {
            final StringBuilder answer = new StringBuilder(64);
            answer.append("<HTML>");
            answer.append("<HEAD>");
            answer.append("<TITLE>").append(title).append("</TITLE>");
            answer.append("</HEAD>");
            answer.append("<BODY>");
            answer.append(hr_returncode ? "<h3>HTTP/1.0 "+returncode +"</h3>" : "");
            answer.append(body);
            answer.append("</BODY>");
            answer.append("</HTML>");
            StringEntity entity = new StringEntity(
                        answer.toString(),
                        ContentType.create("text/html", "UTF-8"));
            response.setEntity(entity);
        }
        
        private void httpAnswer(final int returncode, final String body, final String title)
        {
            httpAnswer(returncode, true, body, title);
        }
    }

    static class RequestListenerThread extends Thread
    {

        private final HttpConnectionFactory<DefaultBHttpServerConnection> connFactory;
        private final ServerSocket serversocket;
        private final HttpService httpService;

        public RequestListenerThread(
                final int port,
                final HttpService httpService,
                final SSLServerSocketFactory sf) throws IOException
        {
            this.connFactory = DefaultBHttpServerConnectionFactory.INSTANCE;
            this.serversocket = sf != null ? sf.createServerSocket(port) : new ServerSocket(port);
            this.httpService = httpService;
        }

        @Override
        public void run()
        {
            System.out.println("Listening on port " + this.serversocket.getLocalPort());
            while (!Thread.interrupted())
            {
                try
                {
                    // Set up HTTP connection
                    Socket socket = this.serversocket.accept();
                    System.out.println("Incoming connection from " + socket.getInetAddress());
                    HttpServerConnection conn = this.connFactory.createConnection(socket);

                    // Start worker thread
                    Thread t = new WorkerThread(this.httpService, conn);
                    t.setDaemon(true);
                    t.start();
                } catch (InterruptedIOException ex)
                {
                    break;
                } catch (IOException e)
                {
                    System.err.println("I/O error initialising connection thread: "
                            + e.getMessage());
                    break;
                }
            }
        }
    }

    static class WorkerThread extends Thread
    {

        private final HttpService httpservice;
        private final HttpServerConnection conn;

        public WorkerThread(
                final HttpService httpservice,
                final HttpServerConnection conn)
        {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }

        @Override
        public void run()
        {
            System.out.println("New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try
            {
                while (!Thread.interrupted() && this.conn.isOpen())
                {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex)
            {
                System.err.println("Client closed connection");
            } catch (IOException ex)
            {
                System.err.println("I/O error: " + ex.getMessage());
            } catch (HttpException ex)
            {
                System.err.println("Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally
            {
                try
                {
                    this.conn.shutdown();
                } catch (IOException ignore)
                {
                }
            }
        }

    }
}
