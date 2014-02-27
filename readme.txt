Synopsis
================================================================================
RSSamantha is a command line rss/atom feed aggregator/creator written in java.
It is designed to subscribe to a batch of feeds in order to merge their items
into new feeds and write them as rss 2.0, html, or plain text to harddisk.

Additionally it has the ability to download contents of podcastfeeds,
filter feed attributes by regular expressions, preprocess the configurationfile
for rather unhandy searchterms, requesting channels via HTTP GET and add/remove
items from external processes via http POST.

License:
GPL v3 Copyright (C) 2011-2013  David Schröer <tengcomplexATgmail.com>

Features
================================================================================
* Platform independent.
* Read rss 1.0/rss 2.0/atom feeds.
* Write aggregated feeds to harddisk.
* HTTP interface for requesting dynamic feeds and adding items.
* Download podcast content. 

Usage
================================================================================
-------------------
Installation
-------------------
A java runtime environment is required. The binaries are compiled with 1.6,
source should compile and run with 1.5. Tested with 1.6 and 1.7 runtime.

Unzip rssamantha.zip, edit configuration file as needed.
There are example wrapper scripts rssamantha.bsh/rssamantha.bat and an example
configuration file rssamantha_demo.opml you can take as a start.

-------------------
General Usage
-------------------
Usage:
 java
 [-Dcom.drinschinz.rssamantha.loglevel=loglevel] (INFO)
 [-Dcom.drinschinz.rssamantha.logfolder=logfolder] (./)
 [-Dcom.drinschinz.rssamantha.configreaderdelay=number] (12000)
 [-Dcom.drinschinz.rssamantha.starterdelay=number] (3000)
 [-Dcom.drinschinz.rssamantha.itemstoragefile=itemstoragefile]
 [-Dcom.drinschinz.rssamantha.itemacceptor={true|false}]
 [-Dcom.drinschinz.rssamantha.itemacceptorport=number] (8383)
 [-Dcom.drinschinz.rssamantha.itemacceptorhost=string] (localhost)
 [-Dcom.drinschinz.rssamantha.itemacceptorthreads=number] (20)
 [-Dcom.drinschinz.rssamantha.acceptorlist=IP1,IP,I*,...IPN] (0:0:0:0:0:0:0:1)
 [-Dcom.drinschinz.rssamantha.cssfile=filename] (null)
 [-Dcom.drinschinz.rssamantha.scriptfile=filename] (null)
 [-Dcom.drinschinz.rssamantha.bodyfile=filename] (null)
 [-Dcom.drinschinz.rssamantha.onloadfile=filename] (null)
 [-Dcom.drinschinz.rssamantha.htmlfiledatetimeformat=pattern] (dd MMM yyyy HH:mm:ss)
 [-Dcom.drinschinz.rssamantha.knowndownloadsfile=filename]
 [-Dcom.drinschinz.rssamantha.downloadthreadsleep=number] (60000)
 [-Dcom.drinschinz.rssamantha.concurrentdownloads=number] (2)
 [-Dcom.drinschinz.rssamantha.addpodcastitems=number] (5)
 [-Dcom.drinschinz.rssamantha.futuredump={true|false}] (false)
 [-Dcom.drinschinz.rssamantha.titleprefix={true|false}] (true)
 [-Dcom.drinschinz.rssamantha.compression=number] (0))
 [-Dcom.drinschinz.rssamantha.preprocessconfig={true|false}] (false)
 [-Dcom.drinschinz.rssamantha.preprocessconfig.value=valuereplacedby]
 ["-Dcom.drinschinz.rssamantha.preprocessconfig.repl_<replacename>=Homer+OR+Fry"]
 -jar rssamantha.jar
 configfile

The compression argument supports values as defined in the java standard
java.util.zip.Deflater.
See http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.util.zip.Deflater.DEFAULT_COMPRESSION

-------------------
Example start
-------------------
Contains arguments for a http proxy, ignore those standard http.proxy* arguments
if you have direct internet access.

java -Dcom.drinschinz.rssamantha.loglevel=INFO \
-Dcom.drinschinz.rssamantha.showlimit=100 \
-Dcom.drinschinz.rssamantha.itemstoragefile=../rssfeedcreatoritems.dat \
-Dhttp.proxyHost=localhost\
"-Dcom.drinschinz.rssamantha.preprocessconfig.repl_1=hobo+OR+shotgun" \
-Dhttp.proxyPort=8118 \
-jar rssamantha.jar configfile.opml

-------------------
Add/Remove external items and requesting data via HTTP.
-------------------
If an itemacceptor thread is configured, it is possible to add/remove items to a
channel from external sources via HTTP post. 

The related properties are:
* com.drinschinz.rssamantha.itemacceptor
* com.drinschinz.rssamantha.itemacceptorport
* com.drinschinz.rssamantha.acceptorlist

POST examples:
Supported keys:
channel(String, channelname, defined in config.name)
ix (Integer, the channel index)
title (String)
description (String)
created (Long, milliseconds, between the current time and midnight, January 1, 1970 UTC)
link (String)
remove (Integer, 1 means true, default false)

Add item:
wget --post-data='title=testtitle&description=testdescription&ix=0' http://host:port/ -O /dev/null
wget --post-data='title=testtitle&description=testdescription&channel=tengtest&created=$(($(date +%s%N)/1000000))' http://host:port/ -O /dev/null

Remove item:
wget --post-data='title=testtitle&description=testdescription&ix=0&created=$CREATED&remove=1' http://host:port/ -O /dev/null
wget --post-data='title=testtitle&description=testdescription&channel=tengtest&created=$CREATED&remove=1' http://host:port/ -O /dev/null

GET examples:
http://$HOST:$PORT (-> generator)
http://$HOST:$PORT/channel=$CHANNELNAME1&channel$CHANNELNAME2
http://$HOST:$PORT/ix=$CHANNELINDEX1&channel$CHANNELNAME2
http://$HOST:$PORT/channel=$CHANNELNAME&type=$TYPE&$refresh$NUMBERINSECONDSnumitems=$NUMBER&cutoff=$EPOCHTIMEINMILLIS$search_title=$REGEXP
http://$HOST:$PORT/generator
http://$HOST:$PORT/status
http://$HOST:$PORT/opml

Special argument cases:
numitem=ALL just means return all items.
cutoff=TODAY means cutoff is set to servers today date midnight, return all
items created after that.
type: Accepts xml,txt or html.
refresh: Sets a meta tag, available only if type=html is given.
search_title: Just return titles matching regular expression.

Configuration
================================================================================
Channels configuration is read from an opml configfile.

-------------------
Supported subscription types
-------------------
Note that since version 0.802 a feedtype detector was introduced.
This feature can set the most common types rss/atom automatically if a channel 
has no feedtype tag defined. 
If downloading content from a podcast feed is desired or a different parser
should be used, the feedtype is still needed.

rss (Using DOM parser from sun libs)
simplerss (Using a SAX like parser from qdmxl classes,
  thanks to Steven R. Brandt,
  see http://www.javaworld.com/javatips/jw-javatip128.html?page=1)
rssidentica
atom
podcast (Note that -Dknowndownloadsfile=filename must be set up, otherwise the
downloadcontrol thread is not started)

Written and HTTP requested channels are published in RSS 2.0.

-------------------
Feed configuation
-------------------
Example:
<source title="title" feedtype="type" feedUrl="url" delay="60000" matchpattern_key="patternA" matchpattern_key_name="patternB" dayofweek="x,y,z" hourofday="x,y" appenddescription="true|false" titleprefix="true|false"/>

feedUrl:
The actual URL of the feed we want to subscribe.

delay:
In ms. Once a day means 86400000, once an hour 3600000.

dayofweek/hourofday:
Commaseparated list of reading days/hours.
See http://docs.oracle.com/javase/6/docs/api/constant-values.html#java.util.Calendar.DAY_OF_WEEK

appenddescripton:
If true we add the full description to the title of an item. Default false.

titleprefix:
If false we don't prefix he title with [$title]. Default true.

matchpattern_key[_patternname]:
A pattern can be applied to the title and/or another part like the category.
(See simple example) If there are more than one patterns defined, the item is
thrown away if one condition doesn't match. So if you want just matching some
conditions, let's say you want to monitor a source for "amy wong naked" in title
--or-- in description, you have to set this up by two sources.
If you want to match a title for not starting with "Bender" and not starting
with "Fry" and not containing "Homer" you can do so by multiple patterns on the
same attribute.
Example:
* matchpattern_title="^(?!.*(Homer)).*$"
* matchpattern_title_nobender="^(?!Bender).*"
* matchpattern_title_nofry="^(?!Fry).*"

Matchpattern is based on java regular expressions, example usage:
a. Find strings that 'starts with' "STR": use "^STR.*"
b. Find strings that 'does not start with' "STR": use "^(?!STR).*"

c. Find strings that 'ends with' "STR": use ".*STR$"
d. Find strings that 'does not end with' "STR": use ".*(?<!STR)$"

e. Find strings that 'contains' "STR" in the middle : use ".+STR.+"
f. Find strings that 'contains' "STR" somewhere : use ".*STR.*"
g. Find strings that 'does not contain' "STR": use "^(?!.*(STR)).*$"

h. Find strings that 'equal to' "STR": use "^STR$"
i. Find strings that 'not equal to' "STR": use "^(?!STR$).*"

Logical operators
j. Find String that 'starts with' "STRA" or 'starts with' "STRB": use "^STRA.*|^STRB.*"

See http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html

-------------------
Channel output feed values
-------------------
[rsschannel.title=title]
[rsschannel.link=link]
[rsschannel.description=description]
[rsschannel.comment=comment]
[rsschannel.yourvalue=value]

-------------------
Channel config values
-------------------
[config.rsswritesleep=sleep{ms}(3600000)]
[config.txtwritesleep=sleep{ms}(3600000)]
[config.htmlwritesleep=sleep{ms}(3600000)]
[config.showlimit=limit]
[config.storelimit=limit]
[config.rssfilename=rssfilename]
[config.txtfilename=txtfilename]
[config.htmlfilename=htmlfilename]
[config.downloadfolder=folder]

-------------------
Simple example
-------------------
See rssamantha_demo.opml example file.

-------------------
Add new items
-------------------
Add a source tag within the desired channel outline tag, save opml and restart
RSSamantha.


Changes
================================================================================
-------------------
Version: 0.802
Release: soon
------------------
- Upgrade to java 1.7 compiler, no longer supports 1.6.
- Add automatic feedtype detection.
- Add additional html output capabilities.
- Just set "http.agent" property with APPNAME if respective system property 
  empty.
- Clean titles, remove newline when reading items.
- Add support for datetime format "E',' dd MMM yyyy KK:mm a z".
- Add type="rss" to channels.opml output, making Liferea happy.
- Introduce CountEvent.PROCESSED, representing the sum of every incoming item.
- Improve entrypage generation, writing directly to the stream.
- Remove type rsstwitter. (Twitter stopped public RSS support in summer 2013)
    Alternatively it's possible to use twitter API and adding items from an 
    external process.
    See https://github.com/tengcomplex/twittertorssamantha
- Set hard log entry datetime pattern "yyyy-MM-dd'T'HH:mm:ss.S", introducing
  milliseconds.
- Don't decompress item description when serving html/txt via http, reduces
  memory consumption.

-------------------
Version: 0.801
Release: 20131117
------------------
- Tiny performance improvements:
   * Item.clone()
   * Log.isLoggable()
   * Control: Remove synchronized keyword in additional getSortedItems() 
     methods, all are calling synchronized method with full signature anyway.
   * Control.getChannelIndex()
- In HTML output, open item title hrefs in new tabs/windows.
- Log warning in readFile() if file not exists. 

-------------------
Version: 0.800
Release: 20130725
------------------
- Fix version number parsing.

-------------------
Version: 0.799
Release: 20130721
------------------
- Fix HTTP POST channel parsing.
- Small optimization in HTTP POST item creation.
- Introduce global APPNAME,APPVERSION variables, avoiding a bunch of access
  to application.properties set.

-------------------
Version: 0.798
Release: 20130630
-------------------
- Improve known downloads cache, endless growth safe.
- Set "rssamantha {version}" as http.agent.
- Improve parsing of exotic datetime formats.
- Change multiple ix/channel arguments in HTTP GET interface.
  Now channel=c1&channel=c2 instead of channel=c1,c2.
- Add multiple select input on generator HTML page.

-------------------
Version: 0.797
Release: 20130427
-------------------
- Add optional system property "com.drinschinz.rssamantha.cssfile". If set use
  it in HtmlFileHandler.getAsString().
- Add optional system property "htmlfiledatetimeformat". If set use it in 
  HtmlFileHandler.getAsString(). Default "dd MMM yyyy HH:mm:ss".
- Add dynamic channel merging when requesting data via GET.
  Example: http://host/?channel=A,B,C
- Add dateformat "E',' dd MMM',' yyyy HH:mm:ss Z".
- Add dateformat "yyyy-MM-dd'T'HH:mmZ".
- Improve Html file writer performance.
- Write known downloads file after ever successful download to disk. Avoid 
  re-download after unplanned shutdown.
- Fix timestamp ending on "HH:mm:ssZ", replace with "HH:mm:ss+0000".
- Fix timestamp ending on "+HH:MM", replace with "+HHmm".
- Fix timestamp ending on "HH:mm:ss UT", replace with "HH:mm:ss GMT".

-------------------
Version: 0.796
Release: 20121027
-------------------
- Just allow GET status command for requests from localhost.
- Bugfix. Prefix "source" value to title in case of PODCASTFEED items.
- Bugfix. Initialize Item.date.formatter with US locale. Prevents unreadable 
  "pubDate" values in exported feeds on system with non english default locale.
- Example file rename to rssamantha_demo.opml.

-------------------
Version: 0.795
Release: 20120902
-------------------
- Print listen port to stdout if starting HTTP acceptor.
- Add charset to HTTP header in any case.
- Add GET generator command which is the new default.
  Command status now without channel links.
- Add HTTP GET search_title regexp argument.
- Accept no HTTP arguments, treat like "generator".
- Add URL generator form to status view.
- Add dateformat "MM/dd/yyyy HH:mm".
- Replace newline character by space and smart trim when appending description. 
  to title in ItemCreator.

-------------------
Version: 0.794
Release: 20120602
-------------------
- Bugfix. Remove <titles> hashcode of the item to be removed, not of the one to 
  be added. Relevant for feeds without serving a $created timestamp.
- Introduced Control.CountEvent.FINISHEDDOWNLOAD.
- Don't lie about number of written items in Main.ShutdownThread.
- Added optional HTTP GET cutoff argument.
- Added html filewriter and HTTP GET support.
- Added txt HTTP GET support.

-------------------
Version: 0.793
Release: 20120428
-------------------
- Bugfix. Read the $created of an item with an array of tagnames,
  trying "updated and "published" for atomfeeds. First match leads to value.
  Trying "pubDate" and "dc_date" for rssfeeds.
- Added version check at startup.
- Optimized SimpleDateFormat array order in ChannelReader.
- Minor improvements, Control.existsFile(), Control.getAppWelcome().
- Renamed some classes and defaultvariables in Control.
- Added Control.CountEvent HTTP fields.

-------------------
Version: 0.792
Release: 20120325
-------------------
- Added optional property "titleprefix=true|false" per source in order to
  control title style, prefixed or not.
- Added optional compression of internal data, just Item.description longer 200
  characters is hold compressed in memory.
- Added optional system property "itemacceptorhost", default "localhost".
- Added HTTP GET opml command.
- Added known downloads size, href list of known channels and a href to opml to
  HTTP GET status command output.
- Added HTTP usage output in case of bad request.
- Unroll channels after initialization FIFO.
- Cap number of ItemAcceptor threads, new property "itemacceptorthreads",
  default 20.
- Added example windows .bat starter file.

-------------------
Version: 0.791
Release: 20120108
-------------------
- Added feed request via http GET, resulting in on the fly XML RSS 2.0 response.
- Added item remove via HTTP POST.
- Added enhancement regarding logging statistic data.
- Renamed Itemacceptor.BrowserClientThread to Itemacceptor.ClientThread.
- Read CDATA coalescing, ignore comments and elementcontent whitespace when
  reading xml.
- Added optional setting "config.txtdatetimeformat", default "HH:mm:ss".
- Added dateformat "E',' dd. MMM yyyy HH:mm:ss Z".

-------------------
Version: 0.79
Release: 20111204
-------------------
- Deactivated translation support.
  NOTE: Google translate API v1 was shut down.
  Now google tranlation lib supports Version 2 of the API and is available
  as a paid service.
  See:
  http://code.google.com/apis/language/translate/v2/getting_started.html
  http://code.google.com/apis/language/translate/v2/pricing.html

  If you are willing to pay for translation or have an easy alternative,
  let me know.

- Minor optimizations. (ChannelReader.DateIndex)
- Various renamings.

-------------------
Version: 0.789
Release: 20111001
-------------------
- Introduced application.properties.
- Item matching now using matches() instead find().
- Rename to rssamantha.

-------------------
Version: 0.788
Release: 20110916
-------------------
- Added dateformat "E MMM dd HH:mm:ss Z yyyy".
- Added dateformat "E',' dd MMM yyyy".
- Added dateformat "E','dd MMM yyyy HH:mm:ss Z".

-------------------
Version: 0.787
Release: 20110618
-------------------
- Support multiple matchpattern for the same attribute by optional suffixes.

-------------------
Version: 0.786
Release: 20110613
-------------------
- ItemAcceptor.readRequest() now using StringBuilder.
- Outsourced Statistics, improved Control.RssFeddCreatorStatistics.
- Just collect the hashcode of Items with foundrsscreated=false in ItemData.
- TxtFileHandler using a DateFomat for the timestamps.
- Minor improvements.
- Fixed exception before trying to translate empty title.
- No longer using toolwit library.
- Upgrade to google translate API version 0.95.

-------------------
Version: 0.785
Release: 20110122
-------------------
- Added support for spain dateformat "E',' dd MMM yyyy HH:mm:ss Z".
- Make ChannelReader.dateformats final.

-------------------
Version: 0.784
Release: 20110116
-------------------
- Added dateformat "MM/dd/yyyy hh:mm:ss a".

-------------------
Version: 0.783
Release: 20101231
-------------------
- Introduced Control.AddItemResult enum.
- Bugfix, we did always return already known item if two items hav the exact
  same created timestamp. Now we compare title as well in Item.compareTo
  if created timestamps are equal.
- Bugfix, sort read podcastitems after reading them in order to add
  the youngest number of "config.adddownloaditems".
- Itemacceptor returns more information.
- Main prints welcome message when starting on stdout.
- Loglevel changed to FINE in FileHandler.hasChanged if nothing has actually
  changed.
- Upgrade to google-api-translate-java-0.94.jar lib.

-------------------
Version: 0.74
Release: 20101225
-------------------
- Minor improvements.

-------------------
Version: 0.72
Release: 20101106
-------------------
- Minor cleanups.

-------------------
Version: 0.72
Release: 20101104
-------------------
- Optional preprocessing of the opml configfile.
- Logging improvements.
- HTML decode improvements.

-------------------
Version: 0.71
Release: 20101024
-------------------
- Accept link via http.
- Minor improvements in ItemAcceptor.

-------------------
Version: 0.70
Release: 20101017
-------------------
- Accept created via http.

-------------------
Version: 0.69
Release: 20100905
-------------------
- Configuration in one single opml file.
- Support multiple matchingpattern.(i.e. matchpattern_key).
- Added translation support.
- All system property now starting with the central packagename.
- Various optimizations and better errorhandling.

-------------------
Version: 0.68
Release: 20100530
-------------------
- Replaced startswith and startsnotwith by matching pattern.
- Trim description before appending it, clean it from html and divide
  it by a "|" character.

-------------------
Version: 0.67
Release: 20100420
-------------------
- Minor optimizations.

-------------------
Version: 0.66
Release: 20100322
-------------------
- Added systemproperty futuredump, if true we don't write items published in
  future.
- Minor optimizations.

-------------------
Version: 0.65
Release: 20100318
-------------------
- Just start downloadcontrol if -Dknowndownloadsfile is defined.
- Minor optimizations.

-------------------
Version: 0.64
Release: 20100314
-------------------
- Added type podcastfeed and downloadmanager.

-------------------
Version: 0.62
Release: 20100307
-------------------
- Added http itemacceptor.

-------------------
Version: 0.61
Release: 20100303
-------------------
- Added datetime support for "yyyy-MM-dd".
- Fixed title removal.

-------------------
Version: 0.6
Release: 20100215
-------------------
- Supporting multiple channels.

-------------------
Version: 0.5
Release: 20100130
-------------------
- Added hours, changed order configitems.
- Just writing items if hashcode has changed.

-------------------
Version: 0.4
Release: 20100116
-------------------
- Added startsnotwith.

TODO
===============================================================================
- Make status avaible as rss feed itself.
- It may be possible to smart trim every description at read() time.
- Jun 27, 2011 6:35:19 AM com.drinschinz.rssfeedcreator.ChannelReader getCreated SEVERE: Error reading http://feeds.feedburner.com/francetv-sports?format=xml Couldn't parse dim 26 juin 2011 22:13:51 +0100
    Seems wrong, french guys using weird days are not supported as it seems.
- Jul 11, 2011 6:11:15 AM com.drinschinz.rssfeedcreator.ChannelReader getCreated SEVERE: Error reading http://rss.nrg.co.il/news/ Couldn't parse Sun,10 Jul 2011 18:36:05 +0200
- Add source attributes to readme.
- Add $after datetime POST argument support.

- Add xsd validation for config.opml file, see: http://en.wikipedia.org/wiki/Xsd
- Add pattern cache, could lead to performance improvement in 
  ItemAcceptor.handleGET().