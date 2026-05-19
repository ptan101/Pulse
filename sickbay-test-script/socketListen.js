'use strict';

var SOCKET_IO_PORT = 3001;
var io = require('socket.io');
var http = require('http');
var os = require('os');

var create_SocketIO_Server = function () {
    var serverIO = http.createServer();
    var browserSocket = io(serverIO);

    serverIO.listen(SOCKET_IO_PORT, '0.0.0.0');
    console.log("Socket.IO server listening on port " + SOCKET_IO_PORT);

    // Print all LAN IPs so you know which one to enter in the app and test.html
    var ifaces = os.networkInterfaces();
    Object.keys(ifaces).forEach(function (name) {
        ifaces[name].forEach(function (iface) {
            if (iface.family === 'IPv4' && !iface.internal) {
                console.log("  LAN IP [" + name + "]: " + iface.address);
            }
        });
    });
    console.log("  Loopback (Phase 1 / same-machine test): 127.0.0.1");

    // Log every raw HTTP request so we can tell if phone requests arrive at all
    serverIO.on('request', function (req) {
        console.log('HTTP ' + req.method + ' ' + req.url + '  from ' + req.socket.remoteAddress);
    });

    browserSocket.on('connection', function (socket) {
        console.log("Client connected.");

        socket.on('disconnect', function () {
            delete this.socket;
        });

        socket.on('error', function (err) {
            console.error('SOCKET ERR: ' + err.toString());
        });

        // Android app emits this event (SickbayPushService.attemptSend)
        socket.on('NewWebsocketData_serverside_timestamp', function (data) {
            console.log('-----DATA RECEIVED at ' + new Date().toISOString() + '-----');
            console.log(JSON.stringify(data, null, 2));

            // Basic payload validation
            var d = data && data.data;
            if (!d) { console.warn('  [WARN] Missing "data" wrapper'); return; }
            var missing = ['channel', 'ns', 't0', 'dt', 'signals'].filter(function (k) { return d[k] === undefined; });
            if (missing.length) { console.warn('  [WARN] Missing fields: ' + missing.join(', ')); return; }
            var sigCount = Object.keys(d.signals).length;
            console.log('  channel=' + d.channel + '  ns=' + d.ns + '  signals=' + sigCount + '  dt=' + d.dt + 's');
        });
    });
};

create_SocketIO_Server();
