#require "LIS3DH.class.nut:1.3.0"
#require "WS2812.class.nut:3.0.0"


spi <- hardware.spi257;
spi.configure(MSB_FIRST, 7500);
pixels <- WS2812(spi, 1);

timeToTakePill <- false;
lifted <- true;
putBack <- true;

buzz <- hardware.pin2;
startTime <- 0;
timeOut <- false;

buzz.configure(DIGITAL_OUT, 1);


function noise(time) {
    local buzztime = time/(0.002);
    for(local i = 0; i < buzztime; ++i) {
        buzz.write(1);
        imp.sleep(0.001);
        buzz.write(0);
        imp.sleep(0.001);
    }
}


function check() {
    if(timeToTakePill) {
        if(!lifted) {
            if(doubleCheck()) {
                lifted = true;
                timeOut = false;
                server.log("First");
                imp.sleep(1.5);
                return false;
            }
        }
        else if(!putBack) {
            if(doubleCheck()) {
                putBack = true;
                pixels.set(0, [0, 0, 0]).draw();
                agent.send("taken", "ok");
                return true;
            }
        }
    }
    return false;
}
function doubleCheck() {
    local count = 0;
    for(local i = 0; i < 100; ++i) {
        count += (accel.getInterruptTable().click ? 1 : 0);
        imp.sleep(0.001);
    }
    return (count > 40);
}

function blink() {
    hardware.pin1.configure(DIGITAL_OUT, 1);
    pixels.set(0, [255, 255, 255]).draw();
    imp.sleep(2);
    pixels.set(0, [0, 0 ,0 ]).draw();
    imp.sleep(0.5);
    hardware.pin1.configure(DIGITAL_IN, check);
}

function takePill(color) {
    lifted = false;
    putBack = false;
    hardware.pin1.configure(DIGITAL_OUT, 1);
    local col = [0, 0, 0];
    if(color == "white") {
        col = [255, 255, 255];
    }
    else if(color == "yellow") {
        col = [255, 255, 0];
    }
    else if(color == "blue") {
        col = [0, 0, 255];
    }
    else if(color == "red") {
        col = [255, 0, 0];
    }
    else if(color == "green") {
        col = [0, 255, 0];
    }
    else {
        return;
    }
    pixels.set(0, col).draw();
    
    timeToTakePill = true;
    startTime <- time();
    timeOut <- true;
    noise(2);
    poll();
}

function poll() {
    if((time() - startTime) > 6 && timeOut) {
        agent.send("not taken", "");
        pixels.set(0, [0, 0, 0]).draw();
        server.log("timeout");
    }
    else if(!check()) {
        imp.wakeup(0, poll);
    }
}

i2c <- hardware.i2c89;
i2c.configure(CLOCK_SPEED_400_KHZ);

intPin <- hardware.pin1;
intPin.configure(DIGITAL_IN, check);

accel <- LIS3DH(i2c, 0x32);

accel.init();
accel.enable(true);
accel.setDataRate(100);

accel.configureClickInterrupt(true, LIS3DH.SINGLE_CLICK, 0.2);
i2c.write(0x32, "\x3A" + "\x0f");
accel.configureInterruptLatching(true);


agent.on("color", takePill);

