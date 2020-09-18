package es.upm.etsit.irrigation.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.upm.etsit.irrigation.shared.Mode;
import es.upm.etsit.irrigation.shared.Schedule;
import es.upm.etsit.irrigation.shared.Zone;

public class Util {
  private static final Logger logger = LogManager.getLogger(Util.class.getName());
  
  public static void printMode(Mode mode) {
    for (Zone zone : mode.getZones()) {
      Schedule sch = zone.getSchedule();
      
      String s = "";
      for (DayOfWeek day : DayOfWeek.values()) {
        if (sch.isDaySelected(day))
          s += day.name() + " ";
      }
      
      logger.debug("Zone [{}] with name [{}] has active days [{}]",
          zone.getPinAddress(), zone.getName(), s);
    }
  }
}
