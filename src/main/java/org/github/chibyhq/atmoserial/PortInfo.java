package org.github.chibyhq.atmoserial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortInfo {
   boolean open;
   int baudRate;
   String descriptor;
   String descriptivePortName;
   int parity;
   String systemPortName;
   boolean dsr;
}
