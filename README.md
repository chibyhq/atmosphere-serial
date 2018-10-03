# Atmosphere Serial

A Java "Websocket to Serial port" bridge using Atmosphere and jSerialComm.
It features a small REST API to open/close serial ports and accept incoming messages, and uses atmosphere to broadcast serial port output.

## How to use it ?

1. Import the JAR into your classpath
1. Declare your atmosphere context, for instance using Spring Boot (c.f. sample project)
1. From your web UI (using for instance atmosphere-js), invoke the REST API to query the list of available serial ports, and open a connection with appropriate parameters (transmission speed, timeout etc...).
1. Send messages to the serial port / close it
