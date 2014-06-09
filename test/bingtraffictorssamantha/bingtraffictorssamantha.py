#
#  Bing to RSSamantha.
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

import json
import urllib, urllib2
from datetime import datetime, date, time
from subprocess import call

RSSAMANTHA_URL = "http://localhost:8081/"
RSSAMANTHA_CHANNEL = "test"

BING_KEY = "the_key_from_bing"
SouthLatitude = "51.160707"
WestLongitude = "6.702596" 
NorthLatitude = "51.511612"
EastLongitude = "7.496358"

def getCongestion(r):
	if "congestion" in r:
		c = r["congestion"]
		if c == "slow":
			return "langsam"
		elif c == "sluggish":
			return "stockend"	
		return c
	return ""

def getSeverity(r):
	if "severity" in r:
		s = r["severity"]
		if s == 1:
			return "vernachlaessigbar"
		elif s == 2:
			return "harmlos"
		elif s == 3:
			return "mittel"
		elif s == 4:
			return "schwer"	
	return ""

def getType(r):
	if "type" in r:
		t = r["type"]
		if t == 1:
			return "Unfall"
		elif t == 2:
			return "Stau"
		elif t == 3:
			return "Fahrzeug liegengeblieben"
		elif t == 4:
			return "Schwertransport"
		elif t == 5 or t == 6:
			return "Anderes"
		elif t == 7:
			return "Geplantes Ereignis"
		elif t == 8:
			return "Strasse kaputt"
		elif t == 9:
			return "Baustelle"
		elif t == 10:
			return "Alarm"
		elif t == 11:
			return "Wetter"
	return ""

def getDateTimeAsString(d):
	return d.replace("Date(", "").replace("/", "").replace(")", "")

def getDateTimeAsFloat(d):
	return float(getDateTimeAsString(d))

def getDateTimeAsFormattedString(d):
	dt = datetime.fromtimestamp(getDateTimeAsFloat(d)/1000)
	return dt.strftime("%Y-%m-%dT%H:%M:%S")

def sendItem(msg, d):
	msg = urllib.urlencode({'title': msg.encode('utf8')})
	call(["wget", "-qO-", "--post-data="+msg+"&channel0="+RSSAMANTHA_CHANNEL+"&created="+d, RSSAMANTHA_URL])

def getStreetName(latitude, longitude):
	try:
		response = urllib2.urlopen("http://dev.virtualearth.net/REST/v1/Locations/"+latitude+","+longitude+"?includeEntityTypes=Address&includeNeighborhood=0&key="+BING_KEY)
		data = json.load(response)
		return data["resourceSets"][0]["resources"][0]["address"]["addressLine"]
	except:
		return "street"

response = urllib2.urlopen("http://dev.virtualearth.net/REST/v1/Traffic/Incidents/"+SouthLatitude+","+WestLongitude+","+NorthLatitude+","+EastLongitude+"?t=1,2,3,4,5,6,7,8,9,10,11&s=2,3,4&key="+BING_KEY)
config = json.load(response)

for i in config["resourceSets"]:
	for r in i["resources"]:
		streetname = getStreetName(str(r["toPoint"]["coordinates"][0]), str(r["toPoint"]["coordinates"][1]))
		msg = "[Bing Traffic] "+streetname+" | "+getSeverity(r)+" | "+getType(r)+" "+r["description"]
		sendItem(msg, getDateTimeAsString(r["start"]))
		print "\n\n"


