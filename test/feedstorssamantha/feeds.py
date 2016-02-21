# -*- coding: utf-8 -*-
#
#  Feeds to RSSamantha.
#  Copyright (C) 2011-2015  David Schröer <tengcomplexATgmail.com>
#
#
#  This file is part of RSSamantha.
#
#  RSSamantha is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  RSSamantha is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with RSSamantha.  If not, see <http://www.gnu.org/licenses/>.
#
#
#
# Usage: >python feeds.py feeds.opml
#
import sys
from fetch import handleFeed
import xml.dom.minidom
from time import sleep
import random

RSSAMANTHA_URL = "http://localhost:8081/"

if not len(sys.argv) == 2:
	print "Error: no opml feedfilename given."
	sys.exit(1)
feedFileName = sys.argv[1]
print "Reading feed from "+feedFileName
#
# Do work
#
DOMTree = xml.dom.minidom.parse(feedFileName)
collection = DOMTree.documentElement
channels = collection.getElementsByTagName("outline")
for channel in channels:
	channelName = channel.getAttribute("config.name")
	print "*****Channel*****"
	print "Channelname: %s" % channelName	
	feeds = channel.getElementsByTagName("source")
	for feed in feeds:
		title = str(feed.getAttribute("title").encode('utf-8'))
		url = str(feed.getAttribute("feedUrl"))
		pattern = str(feed.getAttribute("matchpattern_title"))
		print "Title: "+title+" Url: "+url+" pattern:"+pattern
		sleep(random.randint(3,11))
		if pattern is None:
			handleFeed(RSSAMANTHA_URL, title, url, channelName)
		else:
			handleFeed(RSSAMANTHA_URL, title, url, channelName, pattern)

