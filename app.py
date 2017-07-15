from flask import Flask, request, abort
from flask_cors import CORS

import time
import json
import random
import threading
import http.client

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
	global upcomming_events

	firstUpcomming = upcomming_events[0]

	if len(upcomming_events) > 0 and upcomming_events[0][0]-getSimTime() < MAX_TAKE_TIME:
		# This is probably referring to the first upcomming event, check it off
		upcomming_events = upcomming_events[1:]

	if 'color' in request.args:
		color = request.args.get('color')
	else:
		color = getcolor(firstUpcomming[1])

	event = (getSimTime(), color, success)
	log.append(event)
	return str(event)

@app.route("/device/next_pill")
def get_next_pill():
	remove_missed_events()

	if len(upcomming_events) == 0:
		abort(404)
		return "No events scheduled"

	event = upcomming_events[0]
	name = event[1]

	# Events are sent to the device in true time, not simulated time
	return json.dumps({'time':event[0]-tOffset, 'color':getcolor(name)})

def remove_missed_events():
	global upcomming_events
	upcomming_events.sort()
	while upcomming_events and upcomming_events[0][0] < getSimTime() - MAX_TAKE_TIME:
		if upcomming_events[0][0] == 0:
			# This is an "immediate" request
			break
		upcomming_events = upcomming_events[1:]

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

	if 'color' in request.args:
		color = request.args.get('color')
		
		if color == "":
			if name in colors:
				del colors[name]
			return "[deleted]"

		colors[name]=request.args.get('color')


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
def api_schedule():
	name = request.args.get('name')
	time = request.args.get('time')
	if not name or not time:
		abort(400)
	return schedule(name, time)

@app.route('/api/take_now')
def api_take_now():
	name = request.args.get('name')
	if not name:
		abort(400)

	return schedule(name, 0)

def schedule(name, time):
	event = (float(time), name)
	upcomming_events.append(event)
	upcomming_events.sort()
	print("Event scheduled: " + str(event))
	return "Event scheduled: " + str(event)

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

# Dialout


def start_process():
	thread = threading.Thread(target=handle_dialout)
	thread.start()

def handle_dialout():
	last_time = time.time()
	global upcomming_events

	while True:
		# print('thinking...' + str(random.random()))
		time.sleep(0.5)
		remove_missed_events()
		if len(upcomming_events) == 0:
			continue

		event = upcomming_events[0]
		t = event[0]
		name = event[1]

		if t==0:
			print("Medicating immediately for event {}".format(event))
			color = getcolor(name)
			startlen = len(log)
			medicate(color)

			tstart = time.time()
			while time.time() < tstart + MAX_TAKE_TIME and len(log) == startlen:
				time.sleep(0.1)

			upcomming_events = upcomming_events[1:]
			continue

		if t > last_time and t < getSimTime():
			print("Medicating for event {}".format(event))
			color = getcolor(name)
			medicate(color)

			last_time = t
			continue


REMOTE_HOST = "example.com"
REMOTE_PATH = "/medicate"

def medicate(color):
	conn = http.client.HTTPConnection(REMOTE_HOST)
	conn.request('GET', REMOTE_PATH+'?color={}'.format(color))


sample_setup()
start_process()
if __name__ == "__main__":
	app.run()