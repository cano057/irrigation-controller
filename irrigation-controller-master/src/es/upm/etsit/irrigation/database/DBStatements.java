package es.upm.etsit.irrigation.database;

public enum DBStatements {
  /**
   * Naming standard for defines:
   * {DB}_{SEL/INS/UPD/DEL/REP/CR}_{Summary of data changed}
   * When updating more than one field, consider looking at the calling function
   * name for a suiting suffix.
   */
  
  // Create statements
  MAIN_CR_MODES,
  MAIN_CR_ZONES,
  MAIN_CR_DAYS,
  MAIN_CR_SCHEDULES,
  
  // Select statements
  MAIN_SEL_MODES,
  MAIN_SEL_ZONES_BY_MODE_ID,
  MAIN_SEL_DAYS_BY_ZONE_ID,
  MAIN_SEL_SCHEDULES_BY_ZONE_ID,
  
  // Insert statements
  MAIN_INS_MODE,
  MAIN_INS_ZONE,
  MAIN_INS_DAYS,
  MAIN_INS_SCHEDULES,
  
  // Replace statements
  
  // Delete statements
  MAIN_DEL_MODE,
  MAIN_DEL_ZONES_BY_MODE_ID,
  MAIN_DEL_DAYS_BY_ZONE_ID,
  MAIN_DEL_SCHEDULES_BY_ZONE_ID
  
  // Update statements
}
