package es.upm.etsit.irrigation;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.exception.UnsupportedBoardType;
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.DataBits;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.StopBits;

import es.upm.etsit.irrigation.database.DataMgr;
import es.upm.etsit.irrigation.shared.Mode;
import es.upm.etsit.irrigation.shared.Sensor;
import es.upm.etsit.irrigation.shared.Zone;

public class Controller {
  private final Logger logger = LogManager.getLogger(this.getClass().getName());
  
  private final long MILLISECONDS = 1000;
  public static final int MAX_ZONES = 32;
  
  private Mode mode;
  
  // It's a identifier for each town/village in AEMET opendata.
  private int location;
  
  private GpioController gpio = GpioFactory.getInstance();
  private Serial serial = null;

  
  private Map<Zone, GpioPinDigitalOutput> zonesPin = new HashMap<Zone, GpioPinDigitalOutput>();
  
  public Controller(Mode _mode) {
    loadingMode(_mode);
    location = -1;
    
    initSensorConnection();
  }
  
  
  private void initSensorConnection() {
    serial = SerialFactory.createInstance();
    
    serial.addListener(new SerialDataEventListener() {
      @Override
      public void dataReceived(SerialDataEvent event) {
        
        try {
          String data = event.getAsciiString();
          String[] dataSplitted = data.split("\\-");
          
          if (dataSplitted.length == 2) {
            int ID = -1;
            
            try {
             ID = Integer.parseInt(dataSplitted[0]); 
            } catch(NumberFormatException e) {
              logger.error("Error getting ID [{}], data [{}]", dataSplitted[0], data);
            }
            
            String info = dataSplitted[1];
            
            logger.info("Getting data [{}] and id [{}] and info [{}]", data, ID, info);
            for (Zone zone : mode.getZones()) {
              Sensor sensor = zone.getMySensor();
              
              if (sensor != null && sensor.getID() == ID) {
                
                String[] infoSplitted = info.split("/");
                
                if (infoSplitted.length == 3) {
                  double groundHumidity;
                  double humidity;
                  double temperature;
                  
                  try {
                    groundHumidity = Double.parseDouble(infoSplitted[0]);
                    humidity = Double.parseDouble(infoSplitted[1]);
                    temperature = Double.parseDouble(infoSplitted[2]);
                    
                    sensor.setGroundHumidity(groundHumidity);
                    sensor.setHumidity(humidity);
                    sensor.setTemperature(temperature);
                    
                  } catch(NumberFormatException e) {
                    logger.error("Error parsing info", info, data);
                  }
                } else {
                  logger.error("Error getting info [{}], data [{}]", info, data);
                }
                
                break;
              }
            }
            
          } else {
            logger.error("Error getting data [{}]", data);
          }
          
        } catch (IOException e) {
          e.printStackTrace();
        }
        
        
      }
    });
    
    // create serial config object
    SerialConfig config = new SerialConfig();

    // set default serial settings (device, baud rate, flow control, etc)
    try {
      config.device("/dev/serial0")
            .baud(Baud._9600)
            .dataBits(DataBits._8)
            .parity(Parity.NONE)
            .stopBits(StopBits._1)
            .flowControl(FlowControl.NONE);
      
      // open the default serial device/port with the configuration settings
      serial.open(config);
    } catch (UnsupportedBoardType | IOException e) {
      e.printStackTrace();
    }
  }
  
  public void checkAndStartIrrigationCycles() {
    // Safety check
    if (mode == null)
      return;
    
    Calendar now = GregorianCalendar.getInstance();
    now.setTimeInMillis(System.currentTimeMillis());
    
    for (Zone zone : mode.getZones()) {
      long timeout = 0;
      if (!zone.isWatering() && (timeout = zone.getSchedule().isTimeForIrrigation(now)) > 0 /*&&
          Weather.shouldIrrigateNow()*/) {
        logger.trace("Starting watering in zone " + zone.getPinAddress());
        activeElectrovalve(zone, timeout*MILLISECONDS);
      }
    }
  }
  
  public void checkInactivePorts() {
    for (Zone zone : zonesPin.keySet()) {
      if (zonesPin.get(zone).isLow()) {
        zone.setWatering(false);
      }
    }
  }
  
  private void makePin(Zone zone) {
    GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(
        RaspiPin.getPinByAddress(zone.getPinAddress()));
    pin.setShutdownOptions(true, PinState.LOW);
    zonesPin.put(zone, pin);
  }
  
  public void activeElectrovalve(Zone zone, long timeout) {
    if (zonesPin.get(zone) == null) {
      logger.error("This shouldn't happen");
      makePin(zone);
    }
    
    zonesPin.get(zone).pulse(timeout);
    zone.setWatering(true);
  }
  
  
  public void setNewActiveMode(Mode newMode) {
    for (Zone zone : zonesPin.keySet()) {
      zone.setWatering(false);
      zonesPin.get(zone).low();
    }
    
    zonesPin.clear();
    
    DataMgr.removeMode(mode);
    
    loadingMode(newMode);
      
    if (newMode != null)
      DataMgr.addModeToDB(newMode); 
  }
  
  private void loadingMode(Mode newMode) {
   mode = newMode;
    
    if (newMode != null) {
      // Make pins for zones.
      for(Zone zone : mode.getZones()) {
        logger.trace("Added new zone " + zone.getPinAddress());
        
        GpioPin gpioPin = gpio.getProvisionedPin(RaspiPin.getPinByAddress(zone.getPinAddress()));
        if (gpioPin != null) {
          if (gpioPin instanceof GpioPinDigitalOutput) {
            GpioPinDigitalOutput pin = (GpioPinDigitalOutput) gpioPin;
            zonesPin.put(zone, pin);
            
          } else {
            logger.error("Pin is not output but exists. Reset gpio");
            gpio.shutdown();
            gpio = GpioFactory.getInstance();
            setNewActiveMode(newMode);
          }
          
        } else {
          logger.debug("The first time we launch this pin");
          makePin(zone);
        }
      }
    }
  }
  
  public Zone getZoneByPinAddress(int pin) {
    for (Zone zone : zonesPin.keySet()) {
      if (zone.getPinAddress() == pin)
        return zone;
    }
    
    return null;
  }
  
  public Boolean[] getCurrentZoneStatus() {
    Boolean[] isWateringZone = new Boolean[MAX_ZONES];
    
    // Set all array to null
    for (int i = 0; i < isWateringZone.length; i++) {
      isWateringZone[i] = null;
    }
    
    // Update it with current pins.
    for (Zone zone : zonesPin.keySet()) {
      isWateringZone[zone.getPinAddress()] = zone.isWatering();
    }
    
    return isWateringZone;
  }

  public int getLocation() {
    return location;
  }

  public void setLocation(int location) {
    this.location = location;
  }
  
  
  public Serial getSerial() {
    return serial;
  }
  
}
