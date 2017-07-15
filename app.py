from flask import Flask, request
import time
import json
import random

app = Flask(__name__)

# Log contains tuples of (simulated event time, color, success)
log = []

tOffset = 0

@app.route("/")
def hello():
  return "Hello, World at {}".format(time.time())


# Device API's
@app.route("/device/pill_taken")
def pill_taken():
	return log_pill(True)

@app.route("/device/pill_missed")
def pill_missed():
	return log_pill(False)

def log_pill(success):
	color = request.args.get('color')
	event = (time.time()+tOffset, color, success)
	log.append(event)
	return str(event)

@app.route("/device/next_pill")
def get_next_pill():
	color = random.choice('red green blue'.split())
	if random.random() < 0.1:
		# 10% chance
		t = 0
	else:
		t = time.time() + 120 # 2min in future
	return json.dumps({'time':t, 'color':color})

# App API's
@app.route("/api/log")
def get_log():
	log.sort()
	return str(log)

if __name__ == "__main__":
  app.run()