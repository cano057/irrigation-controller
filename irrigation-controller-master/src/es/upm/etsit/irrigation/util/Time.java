package es.upm.etsit.irrigation.util;

import java.io.Serializable;

public class Time implements Serializable {
  private static final long serialVersionUID = 3L;
  
  private final LocalTime start;
  private final long timeoutInSeconds;
  
  
  public Time(LocalTime _start, long _timeout) {
    start = _start;
    timeoutInSeconds = _timeout;
  }
  
  public boolean isBetween(int hour, int minute) {
    LocalTime end = LocalTime.of(start.getHour(), start.getMinute());
    end = end.plusSeconds(timeoutInSeconds);
    
    LocalTime now = LocalTime.of(hour, minute);
    
    return now.isBefore(end) && (now.isAfter(start) || now.equals(start));
  }
  
  public LocalTime getStart() {
    return start;
  }
  
  public long getTimeout() {
    return timeoutInSeconds;
  }
}
