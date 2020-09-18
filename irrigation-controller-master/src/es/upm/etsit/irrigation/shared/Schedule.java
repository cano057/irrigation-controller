package es.upm.etsit.irrigation.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import es.upm.etsit.irrigation.util.DayOfWeek;
import es.upm.etsit.irrigation.util.Time;

public class Schedule implements Serializable {
  private static final long serialVersionUID = 3L;
  
  private boolean[] days = new boolean[DayOfWeek.values().length];
  private List<Time> irrigationCycles = new ArrayList<Time>();
  
  
  public Schedule(boolean[] _days, List<Time> _irrigationCycles) {
    days = _days;
    irrigationCycles = _irrigationCycles;
  }
  
  public long isTimeForIrrigation(Calendar now) {
    int today = now.get(Calendar.DAY_OF_WEEK);
    int hour = now.get(Calendar.HOUR_OF_DAY);
    int minute = now.get(Calendar.MINUTE);
    
    for (Time irrigationCycle : irrigationCycles) {
      if (irrigationCycle.isBetween(hour, minute) && isDaySelected(today-1)) {
        return irrigationCycle.getTimeout();
      }
    }
    
    return 0;
  }
  
  public boolean isDaySelected(int _day) {
    if (_day < 0 || _day > days.length)
      return false;
    
    return days[_day];
  }
  
  public boolean isDaySelected(DayOfWeek _day) {
    return isDaySelected(_day.getID());
  }
  
  public List<Time> getIrrigationCycles() {
    return irrigationCycles;
  }

  public boolean[] getDays() {
    return this.days;
  }
}
