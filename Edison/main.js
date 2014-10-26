/*jslint node:true, vars:true, bitwise:true, unparam:true */
/*jshint unused:true */
/*global */

/*
A simple node.js application intended to blink the onboard LED on the Intel based development boards such as the Intel(R) Galileo and Edison with Arduino breakout board.

MRAA - Low Level Skeleton Library for Communication on GNU/Linux platforms
Library in C/C++ to interface with Galileo & other Intel platforms, in a structured and sane API with port nanmes/numbering that match boards & with bindings to javascript & python.

Steps for installing MRAA & UPM Library on Intel IoT Platform with IoTDevKit Linux* image
Using a ssh client: 
1. echo "src maa-upm http://iotdk.intel.com/repos/1.1/intelgalactic" > /etc/opkg/intel-iotdk.conf
2. opkg update
3. opkg upgrade

Article: https://software.intel.com/en-us/html5/articles/intel-xdk-iot-edition-nodejs-templates
*/
var left = false;
var right = false;
var mraa = require('mraa'); //require mraa
console.log('MRAA Version: ' + mraa.getVersion()); //write the mraa version to the Intel XDK console

var bbreak = false;

var myLeftLed = new mraa.Gpio(8);
myLeftLed.dir(mraa.DIR_OUT);

var myRightLed = new mraa.Gpio(3);
myRightLed.dir(mraa.DIR_OUT);

var myBreakLed = new mraa.Gpio(4);
myRightLed.dir(mraa.DIR_OUT);

var myEmergencyBuzzer = new mraa.Gpio(7);
myEmergencyBuzzer.dir(mraa.DIR_OUT);
var buzzer = 0;

var ledState = true;

periodicActivity(); //call the periodicActivity function

function periodicActivity()
{
    if(left){
        myLeftLed.write(ledState?1:0);
        ledState = !ledState;
    } else {
        myLeftLed.write(0);
    }
    if(right) {
        myRightLed.write(ledState?1:0);
        ledState = !ledState;
    } else{
        myRightLed.write(0);
    }
    myBreakLed.write(bbreak?1:0);
    if(buzzer != 0) {
        myEmergencyBuzzer.write(1);
        buzzer --;
    } else {
        myEmergencyBuzzer.write(0);
    }
        
  setTimeout(periodicActivity,500);
}


var WebSocketServer = require('websocket').server;
var http = require('http');

var server = http.createServer(function(request, response) {
    console.log((new Date()) + ' Received request for ' + request.url);
    response.writeHead(404);
    response.end();
});
server.listen(8042, function() {
    console.log((new Date()) + ' Server is listening on port 8042');
});

var wsServer = new WebSocketServer({
    httpServer: server,
    // You should not use autoAcceptConnections for production
    // applications, as it defeats all standard cross-origin protection
    // facilities built into the protocol and the browser.  You should
    // *always* verify the connection's origin and decide whether or not
    // to accept it.
    autoAcceptConnections:false 
});

wsServer.on('request', function(request) {
    
        console.log("REQUEST");
    var connection = request.accept('echo-protocol', request.origin);
    console.log((new Date()) + ' Connection accepted.');
    connection.on('message', function(message) {
        console.log("MESSAGE");
        if (message.type === 'utf8') {
            console.log('Received Message: ' + message.utf8Data);
            connection.sendUTF(message.utf8Data);
            if(message.utf8Data == "none") {
                left = false;
                right = false;
            }
            if(message.utf8Data == "turn_left") {
                left = true;
                right = false;
            }
            if(message.utf8Data == "turn_right") {
                left = false;
                right = true;
            }
            if(message.utf8Data == "brake"){
                bbreak = ! bbreak;
            }
            if(message.utf8Data == "beep"){
                buzzer = 3;
            }
        }
    });
    connection.on('close', function(reasonCode, description) {
        console.log((new Date()) + ' Peer ' + connection.remoteAddress + ' disconnected.');
    });
});