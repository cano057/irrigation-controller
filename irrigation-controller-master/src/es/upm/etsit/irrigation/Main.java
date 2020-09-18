package es.upm.etsit.irrigation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.upm.etsit.irrigation.database.DBStatements;
import es.upm.etsit.irrigation.database.Database;
import es.upm.etsit.irrigation.exceptions.ConnectionException;
import es.upm.etsit.irrigation.shared.Controlador;
import es.upm.etsit.irrigation.shared.Mode;
import es.upm.etsit.irrigation.shared.Schedule;
import es.upm.etsit.irrigation.shared.Zone;
import es.upm.etsit.irrigation.socket.SocketHandler;
import es.upm.etsit.irrigation.util.DayOfWeek;
import es.upm.etsit.irrigation.util.LocalTime;
import es.upm.etsit.irrigation.util.Time;

public class Main {
  
  private static final Logger logger = LogManager.getLogger(Main.class.getName());
  
  // 1 second
  private final static long WAITING_TIME = 1000;
  
  // 1 min
  private final static long UPDATE_TIME = 1000;
  private static long updateTime = 0;
  
  // 15 minutes
  private final static long SENSOR_TIME = 15*60000;
  private static long sensorTime = 0; 
  
  // 1 hour
  private final static long WEATHER_TIME = 60000*60;
  private static long weatherTime = 0;
  
  private static Controller controller;
  private static Controlador otroControlador;
  
  private static final String USER = "ELCO";
  private static final String PASSWORD = "ELCO";
  
  // TODO: Move this to a external file
  private static final String RASPI_ID = "ELCORASPI";
  
  public static void main(String[] args) {
    // load database
    try {
      Database.init(USER, PASSWORD);
    } catch (ConnectionException e) {
      logger.throwing(e);
    }
    
    
    Connection conn = null;
    
    try {
      conn = Database.getConnection();
    } catch (SQLException e) {
      logger.throwing(e);
    }
    
    if (conn == null)
      System.exit(0);
    
    Database.checkDatabase(conn);
    
    Mode mode = null;
    try {
      mode = loadDatabase(conn);
    } catch (SQLException e) {
      logger.throwing(e);
    }
    
    Database.closeConnection(conn);
    
    controller = new Controller(mode);
    
    while (true) {
      logger.trace("Working...");
      
      if (System.currentTimeMillis() > updateTime) {
        // Ask new mode if exists.
        Controlador newController = SocketHandler.askController(RASPI_ID);
        
        if (otroControlador == null ||(newController != null && newController.getVersion() > otroControlador.getVersion())) {
          logger.trace("Added new controller");
          
          // DEBUG
         //  Util.printMode(newController.getActiveMode());
          
          controller.setNewActiveMode(newController.getActiveMode());
          
          int location = -1;
          
          try {
            location = Integer.parseInt(newController.getMunicipio());
          } catch (NumberFormatException e) {
            logger.error("Error getting controller", e);
          }
          
          controller.setLocation(location);
          
          otroControlador = newController;
        }
        
        // Ask if some zone should irrigate now
        Integer[] portIrrigationTimes = SocketHandler.shouldIrrigateNow(RASPI_ID);
        
        for (int i = 0; i < portIrrigationTimes.length; i++) {
          Integer time = portIrrigationTimes[i];
          if (time != null && time > 0) {
            time *= 60 * 1000;
            logger.info("Activated port [{}] for [{}]", i, time);
            if (controller.getZoneByPinAddress(i) != null)
              controller.activeElectrovalve(controller.getZoneByPinAddress(i), time);
            else
              logger.error("User wants us to irrigate a zone we don't have");
          }
        }
        
        // Send current port status
        SocketHandler.sendPortStatus(RASPI_ID, controller.getCurrentZoneStatus());
        
        updateTime = System.currentTimeMillis() + UPDATE_TIME;
      }
      
      if (System.currentTimeMillis() > weatherTime) {
        logger.trace("Launching get weeather");
        Thread thread = new Thread_GetWeather(controller.getLocation());
        thread.start();
        
        weatherTime = System.currentTimeMillis() + WEATHER_TIME;
      }
      
      if (System.currentTimeMillis() > sensorTime) {
        
        sensorTime = System.currentTimeMillis() + SENSOR_TIME;
      }
      
      
      controller.checkAndStartIrrigationCycles();
      controller.checkInactivePorts();
      
      waiting();
    }
  }
  
  private static Mode loadDatabase(Connection conn) throws SQLException {
    PreparedStatement stmt = null;
    ResultSet result = null;
    
    // Load modes info
    stmt = conn.prepareStatement(Database.getPreparedStatement(DBStatements.MAIN_SEL_MODES));
    result = stmt.executeQuery();
    
    Mode mode = null;
    // Only if we have any mode load it.
    if (result.next()) {
      logger.trace("There is a mode saved in DB");
      
      int ID = result.getInt("ID");
      String name = result.getString("name");
      
     mode = new Mode(ID, name);
     logger.trace("Mode id [{}] and name [{}]", ID, name);
     
     stmt = conn.prepareStatement(Database.getPreparedStatement(DBStatements.MAIN_SEL_ZONES_BY_MODE_ID));
     stmt.setInt(1, mode.getID());
     result = stmt.executeQuery();
     
     while(result.next()) {
       int pinAddress = result.getInt("pinAddress");
       String zoneName = result.getString("name");
       boolean shouldTakeWeather = result.getBoolean("shouldTakeWeather");
       
       logger.trace("Zone [{}, {}]",pinAddress, zoneName);
       Zone zone = new Zone(zoneName, pinAddress);
       zone.setShouldTakeWeather(shouldTakeWeather);
       mode.getZones().add(zone);
       
       PreparedStatement stmt2 = conn.prepareStatement(Database.getPreparedStatement(DBStatements.MAIN_SEL_DAYS_BY_ZONE_ID));
       stmt2.setInt(1, zone.getPinAddress());
       ResultSet result2 = stmt2.executeQuery();
       result2.next();
       
       boolean[] days = new boolean[DayOfWeek.values().length];
       for (int i = 1; i < DayOfWeek.values().length + 1; i++) {
         days[i-1] = result2.getBoolean(i);
         logger.trace("Day [{}] is {}", DayOfWeek.getFromID(i-1), days[i-1]);
       }
       
       stmt2 = conn.prepareStatement(Database.getPreparedStatement(DBStatements.MAIN_SEL_SCHEDULES_BY_ZONE_ID));
       stmt2.setInt(1, zone.getPinAddress());
       result2 = stmt2.executeQuery();
       
       List<Time> irrigationCycle = new ArrayList<Time>();
       while(result2.next()) {
         int startHour = result2.getInt("startHour");
         int startMinute = result2.getInt("startMinute");
         long timeout = result2.getLong("timeout");
         
         Time time = new Time(LocalTime.of(startHour, startMinute), timeout);
         irrigationCycle.add(time);
       }
       
       Schedule schedule = new Schedule(days, irrigationCycle);
       zone.setSchedule(schedule);
       
       
     }
    } else {
      logger.trace("No mode in DB");
    }
    
    return mode;
  }
  
  
  private static void waiting() {
    // Wait some time
    try {
      Thread.sleep(WAITING_TIME);
    }catch(Exception e) {
      logger.throwing(e);
    }
  }
  
  
  
}
