package es.upm.etsit.irrigation.util;

public enum DayOfWeek {
  SUNDAY(0),
  MONDAY(1),
  TUESDAY(2),
  WEDNESDAY(3),
  THURSDAY(4),
  FRIDAY(5),
  SATURDAY(6);
  
  private int ID;
  
  DayOfWeek(int _id) {
    ID = _id;
  }
  
  public int getID() {
    return ID;
  }
  
  public static DayOfWeek getFromID(int id) {
    for (DayOfWeek day : DayOfWeek.values())
      if (day.getID() == id)
        return day;
    return null;
  }
}
