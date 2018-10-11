package org.github.chibyhq.atmoserial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.inject.Inject;

import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.config.service.Put;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.BroadcasterLifeCyclePolicyListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import lombok.extern.java.Log;

@ManagedService(path = "/atmoserial/{portDescriptor: [a-zA-Z][a-zA-Z_0-9]*}")
@Log
public class AtmoserialService implements BroadcasterLifeCyclePolicyListener, SerialPortDataListener {

    ObjectMapper objectMapper = new ObjectMapper();

    SerialPort broadcastedPort;

    final public String LIST_ALL_PORTS = "ListAllPorts";

    @Inject
    BroadcasterFactory factory;

    @Override
    public void onDestroy() {
        if (broadcastedPort != null) {
            if (broadcastedPort.closePort()) {
                broadcastedPort = null;
            }
        }
    }

    @Override
    public void onEmpty() {
    }

    @Override
    public void onIdle() {
    }

    /**
     * PUT : Get the list of serial ports with their description and whether
     * they are already open
     */
    @Put
    public void onPut(AtmosphereResource r) {
        r.getBroadcaster().broadcast(getListOfPortsAsJson(), r);
    }

    @Ready
    public void onReady(AtmosphereResource r) {
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.putAll(r.getRequest().getParameterMap());
        String commPort = getPortFromBroadcaster(r.getBroadcaster());
        

        if (getPortFromBroadcaster(r.getBroadcaster()).equals(LIST_ALL_PORTS)) {
            r.getBroadcaster().broadcast(getListOfPortsAsJson(), r);
        } else {
            SerialPort port = SerialPort.getCommPort(commPort);
            if (port == null) {
                try {
                    r.getResponse().sendError(404, "Serial port " + commPort + " does not exist");
                    // Close the connection, as the port does not exist
                    r.resume();
                } catch (IOException e) {
                    log.log(Level.WARNING, "Could not report non-existing CommPort to newly subscribed resource", e);
                }
            } else {
                if (!params.containsKey("baud")) {
                    params.put("baud", new String[] { "9600" });
                }

                if (port.isOpen()) {
                    // If the port is open with different parameters than
                    // requested,
                    // send an exception
                    if (broadcastedPort.getBaudRate() != Integer.valueOf(params.get("baud")[0])) {
                        throw new IllegalStateException(
                                "Port " + commPort + " is already open with different baud rate");
                    }
                } else {
                    port.setBaudRate(Integer.valueOf(params.get("baud")[0]));
                    port.openPort();
                }

                if (broadcastedPort == null) {
                    broadcastedPort = port;
                }
            }
        }

    }

    private static String getPortFromBroadcaster(Broadcaster b) {
        return b.getID().substring(b.getID().lastIndexOf('/') + 1);
    }

    private String getListOfPortsAsJson() {
        String result = "";
        List<PortInfo> portsResult = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            PortInfo portInfo = new PortInfo();
            portInfo.setSystemPortName(port.getSystemPortName());
            portInfo.setOpen(port.isOpen());
            portInfo.setDescriptivePortName(port.getDescriptivePortName());
            if (port.isOpen()) {
                portInfo.setBaudRate(port.getBaudRate());
                portInfo.setDsr(port.getDSR());
                portInfo.setParity(port.getParity());
            }
            portsResult.add(portInfo);
        }
        try {
            result = objectMapper.writeValueAsString(portsResult);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return result;
    }

    /**
     * POST : Open a serial port (if already open with different parameters,
     * reply with an error code and string). Parameters are : name of the
     * portDescriptor (broadcaster name), baudRate
     * 
     * @param r
     */
    @Post
    public void onPost(AtmosphereResource r) {

    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
            return;
        byte[] newData = new byte[broadcastedPort.bytesAvailable()];
        int numRead = broadcastedPort.readBytes(newData, newData.length);
        if (numRead > 0) {
            factory.get("/atmoserial/" + event.getSerialPort().getSystemPortName()).broadcast(new String(newData));
        }
    }
}
