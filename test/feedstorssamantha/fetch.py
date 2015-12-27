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
# Usage: >python fetch.py rssamanthaurl feedname "url" channelname
#

import sys
import time
import urllib
import urllib2
import feedparser

def handleFeed(rssamanthaUrl, feedName, url, channel):
	print "Parsing url:"+url+" ..."
	d = feedparser.parse(url)
	if d['bozo'] == 1:
		print "feed is not well formed, "+str(d['bozo_exception'])
	if hasattr(d['feed'], 'title') and not (d['feed']['title'] is None):
			feedTitle = d['feed']['title']
	else:
			feedTitle = "n/a"
	if hasattr(d, 'namespaces') and not (d.namespaces is None):
			namespaces = str(d.namespaces)
	else:
			namespaces = "n/a"
	print "Parsed url:"+url+" feedtitle:"+feedTitle.encode('ascii', 'ignore')+" namespaces:"+namespaces+"\n"
	for item in d.entries:
		if hasattr(item, 'published_parsed') and not (item.published_parsed is None):
			dt = str(int(time.mktime(item.published_parsed)*1000))
		else:
			if hasattr(item, 'updated_parsed') and not (item.updated_parsed is None):
				dt = str(int(time.mktime(item.updated_parsed)*1000))
			else:
				dt = ""
		if hasattr(item, 'published'):
			p = item.published
		else:
			p = ""
		if hasattr(item, 'description'):
			desc = item.description
		else:
			desc = "n/a"
		print "title:"+item.title.encode('ascii', 'ignore')+" published:["+dt+" "+p+"] link:"+item.link.encode('ascii', 'ignore')+" description:"+desc.encode('ascii', 'ignore')
		ret = sendItem(rssamanthaUrl, ("["+feedName.decode('utf-8')+"] "+item.title), dt, desc, item.link, channel)
		print ret
	print "--------------------\n"

def sendItem(rssamanthaUrl, title, d, description, link, channel):
	values = {
		"title" :  title.encode('utf-8'),
		"channel0" : channel,
		"description" : description.encode('utf8'),
		"link" : link.encode('utf8')
	}
	if len(d) > 0:
		values["created"] = d

	data = urllib.urlencode(values)
	req = urllib2.Request(rssamanthaUrl, data)
	try:
		response = urllib2.urlopen(req)
		r = response.read()
		return r
	except urllib2.HTTPError, err:
		return "HTTP Error "+str(err.code)
#
# Do work if called standalone, with arguments
#
if len(sys.argv) == 5:
	handleFeed(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])

