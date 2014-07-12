/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.drinschinz.rssamantha;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.apache.http.Header;

/**
 *
 * @author teng
 */
public class HttpAcceptor
{
    final Control control;
    private final String generatorHtml;
    /** JavaScript validation for the generator */
    private final static String checkInput = new Scanner(ItemAcceptor.class.getResourceAsStream("checkInput.js")).useDelimiter("\\A").next();
    
    public HttpAcceptor(final Control control)
    {
        this.control = control;
        this.generatorHtml = initGeneratorHtml(this.control.getAllChannelNames());
    }
    
    public void start(final int port)
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

    class HttpHandler implements HttpRequestHandler
    {

        private final String docRoot;
        private HttpResponse response;

        public HttpHandler(final String docRoot)
        {
            super();
            this.docRoot = docRoot;
        }

        @Override
        public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context) throws HttpException, IOException
        {
            this.response = response;
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if(!method.equals("GET") && !method.equals("POST"))
            {
                throw new MethodNotSupportedException(method + " method not supported");
            }
            String target = request.getRequestLine().getUri();
            System.out.println("target: "+target);
            Header[] hd = request.getAllHeaders();
            for(Header h : hd)
            {
                System.out.println("header:"+h.getName()+" "+h.getValue());
            }
            if (request instanceof HttpEntityEnclosingRequest)
            {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                byte[] entityContent = EntityUtils.toByteArray(entity);
                System.out.println("Incoming entity content (bytes): " + entityContent.length);
            }
            if(method.equals("POST"))
            {
                //handlePOST(getArgsFromUrl(content));
                
            }
            else
            {
                handleGET(new HashMap<String, String>());
            }
        }
        
        private void handleGET(final Map<String, String> hm)
        {
            if(hm.isEmpty() || hm.containsKey("generator"))
            {
                doGenerator();
                return;
            }
        }
        
        private void doGenerator()
        {
            //out.println("hallo");
            response.setStatusCode(HttpStatus.SC_OK);
            StringEntity entity = new StringEntity(
                        generatorHtml,
                        ContentType.create("text/html", "UTF-8"));
            response.setEntity(entity);
            System.out.println("Served generator");
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
