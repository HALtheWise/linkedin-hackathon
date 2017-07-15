url <- "https://linkedin-whynot.herokuapp.com"
http.onrequest(function(request, response) {
    if("color" in request.query) {
        device.send("color", request.query.color);
    }
    response.send(200, "OK");
});

function taken(a) {
    server.log("yay! you took your pill");
    http.get(url + "/device/pill_taken").sendsync();
}

function notTaken(a) {
    http.get(url + "/device/pill_missed").sendsync();
}

device.on("taken", taken);

device.on("not taken", notTaken);


