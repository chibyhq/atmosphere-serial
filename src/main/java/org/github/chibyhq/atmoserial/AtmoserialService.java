package org.github.chibyhq.atmoserial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.BroadcasterLifeCyclePolicyListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

@ManagedService(path = "/atmoserial/{portDescriptor: [a-zA-Z][a-zA-Z_0-9]*}")
public class AtmoserialService implements BroadcasterLifeCyclePolicyListener, SerialPortDataListener {

    ObjectMapper objectMapper = new ObjectMapper();

    SerialPort broadcastedPort;

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
     * GET : Get the list of serial ports with their description and whether
     * they are already open
     */
    @Get
    public String onGet(AtmosphereResource r) {
        List<PortInfo> portsResult = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            PortInfo portInfo = new PortInfo();
            portInfo.setOpen(port.isOpen());
            portInfo.setDescriptivePortName(port.getDescriptivePortName());
            if (port.isOpen()) {
                portInfo.setBaudRate(port.getBaudRate());
                portInfo.setDsr(port.getDSR());
                portInfo.setParity(port.getParity());
            }
            portsResult.add(portInfo);
        }
        String result = "";
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
    public void onPost(AtmosphereResource r, Broadcaster b) {
        Map<String, String[]> params = r.getRequest().getParameterMap();
        String commPort = b.getID().substring(b.getID().lastIndexOf('/') + 1);
        SerialPort port = SerialPort.getCommPort(commPort);
        
        if(!params.containsKey("baud")){
            params.put("baud", new String[]{"9600"});
        }
        
        if (port.isOpen()) {
            // If the port is open with different parameters than requested,
            // send an exception
            if(broadcastedPort.getBaudRate() != Integer.valueOf(params.get("baud")[0])){
                throw new IllegalStateException("Port "+commPort+" is already open with different baud rate");
            }
        } else {
            port.setBaudRate(Integer.valueOf(params.get("baud")[0]));
            port.openPort();
        }

        if (broadcastedPort == null) {
            broadcastedPort = port;
        }
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
