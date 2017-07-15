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
colors = dict()

tOffset = 0

# Constants
MAX_TAKE_TIME = 20

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
	color = request.args.get('color')
	if not color:
		abort(400)
		return

	while len(upcomming_events) > 0 and upcomming_events[0][0]-getSimTime() < MAX_TAKE_TIME:
		# This is probably referring to the first upcomming event, check it off
		if upcomming_events[0][0]==0:
			# Keep "immediate" requests
			break
		upcomming_events = upcomming_events[1:]

	event = (getSimTime(), color, success)
	log.append(event)
	return str(event)

@app.route("/device/next_pill")
def get_next_pill():
	global upcomming_events
	upcomming_events.sort()
	while upcomming_events and upcomming_events[0][0] < getSimTime() - MAX_TAKE_TIME:
		upcomming_events = upcomming_events[1:]

	if len(upcomming_events) == 0:
		abort(404)
		return "No events scheduled"

	event = upcomming_events[0]
	name = event[1]

	# Events are sent to the device in true time, not simulated time
	return json.dumps({'time':event[0]-tOffset, 'color':getcolor(name)})

@app.route("/device/all_upcomming")
def get_all_upcomming():
	return str(upcomming_events)

# App API's
@app.route("/api/log")
def get_log():
	log.sort()
	return str(log)

@app.route('/api/add_medication')
def add_medication():
	name = request.args.get('name')
	if not name:
		abort(400)
	return getcolor(name)

# getcolor gets a color from the mapping, creating it if it doesn't exist.
def getcolor(name):

	if name in colors:
		return colors[name]
		
	colorslist = 'red green blue'.split()
	if len(colors) < len(colorslist):
		color = colorslist[len(colors)]
	else:
		color = 'color'+str(len(colors)+1)
	colors[name] = color
	return color

@app.route('/api/list_medications')
def list_medications():
	return json.dumps(colors)

@app.route('/api/schedule')
def schedule():
	name = request.args.get('name')
	time = request.args.get('time')
	if not name or not time:
		abort(400)

	event = (float(time), name)
	upcomming_events.append(event)
	upcomming_events.sort()
	print("Event scheduled: " + str(event))
	return ""

# Misc utilities
def random_name():
	return random.choice('hello world'.split())

def sample_setup():
	# Build a log
	for i in range(10):
		event = (getSimTime()-60*60*24*i, random_name(), random.random()<0.8)
		log.append(event)

	# Populate sample events
	for i in range(100):
		event = (getSimTime()+30*i, random_name())
		upcomming_events.append(event)

def getSimTime():
	return time.time() + tOffset


sample_setup()
if __name__ == "__main__":
	app.run()