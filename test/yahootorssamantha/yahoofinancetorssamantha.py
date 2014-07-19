# -*- coding: utf-8 -*-
#
#  Yahoo Finance to RSSamantha.
#  Copyright (C) 2011-2014  David Schröer <tengcomplexATgmail.com>
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

import time
import urllib
import urllib2

YAHOO_FINANCE_URL = "http://finance.yahoo.com/d/quotes.csv"

RSSAMANTHA_URL = "http://localhost:8081/"
RSSAMANTHA_CHANNEL = "test"

symboldata = [
	["^GDAXI", "DAX"],
	["^FTSE", "FTSE"],
	["^N225", "Nikkei"],
	["^IXIC", "NasdaqComposite"],
	["^GSPC", "S&P500"],
	["^FCHI", "CAC"],
	["^HSI", "HangSeng"]
]
# We want to request things in given order, 
# that's why we copy over the symbols from a list to map.
symbolmap = {}

def getSymbolList():
	symbollist = ""
	while len(symboldata) > 0:
		s = symboldata.pop(0)		
		symbollist += s[0]+","
		symbolmap[s[0]] = s[1]
	symbollist = symbollist[:-1]
	return symbollist
	
def sendItem(msg, d):
	values = {
		"title" :  msg.encode('utf8'),
		"channel0" : RSSAMANTHA_CHANNEL,
		"created" : d
	}
	data = urllib.urlencode(values)
	req = urllib2.Request(RSSAMANTHA_URL, data)
	response = urllib2.urlopen(req)
	r = response.read()
	return r

def getMsgFromYahoo(symbollist):
	print "getMsgFromYahoo(), symbollist:"+symbollist
	req = urllib2.Request(YAHOO_FINANCE_URL+"?s="+symbollist+"&f=sl1")
	response = urllib2.urlopen(req)
	r = response.read()
	v = r.rstrip().split('\n')
	msg = "[Yahoo Quotes] "
	for m in v:
		d = m.strip().replace("\"", "").split(",")
		msg = "".join([msg, symbolmap[d[0]], " ", str(d[1]), "  "])
	return msg
#
# Do work
#
msg = getMsgFromYahoo(getSymbolList());
print "send to "+RSSAMANTHA_URL+" msg:"+msg
print "response:"+sendItem(msg, str(abs(int(time.time())*1000)))
