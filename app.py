from flask import Flask, request, abort
from flask_cors import CORS

import time
import json
import random

from collections import defaultdict

app = Flask(__name__)
CORS(app)

# Storage

# Log contains tuples of (simulated event time, name, success)
log = []

# upcomming_events contains tuples of (simulated event time, name)
upcomming_events = []

# names maps names to colors
colors = defaultdict(lambda:"medication")

tOffset = 0

# Constants
MAX_TAKE_TIME = 120

# Code

@app.route("/")
def hello():
  return "Hello, World at {}".format(getSimTime())


# Device API's
@app.route("/device/pill_taken")
def pill_taken():
	return log_pill(True)

@app.route("/device/pill_missed")
def pill_missed():
	return log_pill(False)

def log_pill(success):
	while len(upcomming_events) > 0 and upcomming_events[0][0]-getSimTime() < MAX_TAKE_TIME:
		# This is probably referring to the first upcomming event, check it off
		if upcomming_events[0][0]==0:
			# Keep "immediate" requests
			break
		upcomming_events = upcomming_events[1:]

	color = request.args.get('color')
	event = (getSimTime(), color, success)
	log.append(event)
	return str(event)

@app.route("/device/next_pill")
def get_next_pill():
	upcomming_events.sort()
	while upcomming_events and upcomming_events[0] < getSimTime - MAX_TAKE_TIME:
		upcomming_events = upcomming_events[1:]

	if len(upcomming_events) == 0:
		abort(404)
		return "No events scheduled"

	event = upcomming_events[0]
	# Events are sent to the device in true time, not simulated time
	return json.dumps({'time':event[0]-tOffset, 'color':event[1]})

# App API's
@app.route("/api/log")
def get_log():
	log.sort()
	return str(log)

@app.route('/api/add_medication')
def add_medication():
	name = request.args.get('name')
	if name in colors:
		return colors[name]
		
	colorslist = 'red green blue purple 5 6 7 8'.split()
	color = colorslist[len(colors)]
	colors[name] = color
	return color

# Misc utilities
def random_color():
	return random.choice('red green blue'.split())

def sample_setup():
	# Build a log
	for i in range(10):
		event = (getSimTime()-60*60*24*i, random_color(), random.random()<0.8)
		log.append(event)

	# Populate sample events
	for i in range(100):
		event = (getSimTime()+30*i, random_color())
		upcomming_events.append(event)

def getSimTime():
	return time.time() + tOffset


sample_setup()
if __name__ == "__main__":
	app.run()