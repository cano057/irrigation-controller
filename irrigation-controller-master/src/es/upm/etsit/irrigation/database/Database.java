package es.upm.etsit.irrigation.database;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import es.upm.etsit.irrigation.exceptions.ConnectionException;

public class Database {
  private static final Logger logger = LogManager.getLogger(Database.class.getName());
  
  private static Map<DBStatements, String> preparedStatements = null;
  
  private static final String DB_DRIVER = "org.h2.Driver";
  private static String DB_CONNECTION = "jdbc:h2:";
  private static ComboPooledDataSource cpds;
  private static final int CONNECTION_ATTEMPTS = 1;
  private static boolean isConnected = false;
  private static AtomicInteger numberConnections = new AtomicInteger(0);
  
  static {
    preparedStatements = new HashMap<DBStatements, String>();
    DoPrepareStatements(); 
  }
  
  public static boolean init (String _user, String _password) throws ConnectionException {
    DB_CONNECTION = DB_CONNECTION + System.getProperty("user.home") + "/maindb";
    logger.trace("Saving DB in [{}]", System.getProperty("user.home"));
    
    try {
      Class.forName(DB_DRIVER);
    } catch (ClassNotFoundException e) {
      logger.fatal("H2 Driver hasn't been found!");
      return false;
    }
    
    
    // First check test if we're able to make a connection to db
    testConnection(_user, _password);
    
    cpds = new ComboPooledDataSource();
    try {
      // Only one attempt to make a connection
      cpds.setAcquireRetryAttempts(CONNECTION_ATTEMPTS);
      cpds.setDriverClass(DB_DRIVER);
      cpds.setJdbcUrl(DB_CONNECTION);
      cpds.setUser(_user);
      cpds.setPassword(_password);
    } catch (PropertyVetoException e) {
      logger.fatal("Error launching cpds");
      logger.throwing(e);
      return false;
    }
    
    setConnected(true);
    return true;
    
  }
  
  private static void testConnection(String _user, String _password) throws ConnectionException {
    try {
      Connection conn = DriverManager.getConnection(DB_CONNECTION, _user, _password);
      conn.close();
    } catch(SQLException e) {
      throw new ConnectionException("Bad user or password");
    }
  }
  
  /**
   * Create tables for DB. If that tables are already set, then it'll do nothing
   * @param conn
   */
  public static void checkDatabase(Connection conn) {
    
    try {
      // Mode
      PreparedStatement stmt = conn.prepareStatement(preparedStatements.get(DBStatements.MAIN_CR_MODES));
      stmt.execute();
      
      // Zones
      stmt = conn.prepareStatement(preparedStatements.get(DBStatements.MAIN_CR_ZONES));
      stmt.execute();
      
      // Days
      stmt = conn.prepareStatement(preparedStatements.get(DBStatements.MAIN_CR_DAYS));
      stmt.execute();
      
      // Schedule
      stmt = conn.prepareStatement(preparedStatements.get(DBStatements.MAIN_CR_SCHEDULES));
      stmt.execute();
      
    } catch (SQLException e) {
      logger.throwing(e);
      
    }
  }
  
  /**
   * Get the database connection. If it's closed, make a new one
   * @return the connection
   */
  public static Connection getConnection() throws SQLException {
    newConnection();
    return cpds.getConnection();
  }
  
  private static void newConnection() {
    logger.trace("Adding new connection to database. Currently there are: [{}]", numberConnections);
    numberConnections.incrementAndGet();
  }
  
  public static void closeConnection(Connection conn) {
    logger.trace("Closing connection to database. Currently there are: [{}]", numberConnections);
    numberConnections.decrementAndGet();
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        logger.error("Error closing connection");
        logger.throwing(e);
      }
    }
  }
  
  public static boolean areConnectionsOpened() {
    return numberConnections.get() > 0;
  }
  
  /**
   * Get cpds with conf and all the setup.
   * @return
   */
  public static synchronized ComboPooledDataSource getCpds() {
    return cpds;
  }
  
  /**
   * @return the isConnected
   */
  public static boolean isConnected() {
    return isConnected;
  }

  /**
   * @param isConnected the isConnected to set
   */
  public static void setConnected(boolean isConnected) {
    Database.isConnected = isConnected;
  }

  
  public static void close() {
    if (Database.isConnected())
      cpds.close();
    isConnected = false;
  }
  
  /**
   * @return the preparedStatement selected by databaseStatement
   */
  public static String getPreparedStatement(DBStatements databaseStatement) {
    if (preparedStatements.containsKey(databaseStatement))
      return preparedStatements.get(databaseStatement);
    logger.error("This DatabaseStatement {} doesn't exist in preparedStatement map", databaseStatement);
    return null;
  }
  
  /**
   * Insert the prepare statements into a map variable for later use.
   * When you are going to use one prepare statement you must get it from preparedStatements and if it
   * doesn't exist in the map variable you must create it with an appropriate name
   */
  private static void DoPrepareStatements() {
    preparedStatements.put(DBStatements.MAIN_CR_MODES, "CREATE TABLE IF NOT EXISTS `modes` ("
        + "`ID` int not null AUTO_INCREMENT,"
        + "`name` varchar(150) not null default '',"
        + "PRIMARY KEY (`ID`, `name`)) DEFAULT CHARSET=UTF8");
    
    preparedStatements.put(DBStatements.MAIN_CR_ZONES, "CREATE TABLE IF NOT EXISTS `zones` ("
        + "`pinAddress` int not null,"
        + "`modeID` int not null,"
        + "`name` varchar(150) not null default '',"
        + "`shouldTakeWeather` boolean not null default '0',"
        + "PRIMARY KEY(`pinAddress`, `modeID`)) DEFAULT CHARSET=UTF8");
    
    preparedStatements.put(DBStatements.MAIN_CR_DAYS, "CREATE TABLE IF NOT EXISTS `days` ("
        + "`zoneID` int not null,"
        + "`monday` boolean not null default '0',"
        + "`tuesday` boolean not null default '0',"
        + "`wednesday` boolean not null default '0',"
        + "`thursday` boolean not null default '0',"
        + "`friday` boolean not null default '0',"
        + "`saturday` boolean not null default '0',"
        + "`sunday` boolean not null default '0',"
        + "PRIMARY KEY(`zoneID`)) DEFAULT CHARSET=UTF8");
    
    preparedStatements.put(DBStatements.MAIN_CR_SCHEDULES, "CREATE TABLE IF NOT EXISTS `schedules` ("
        + "`zoneID` int not null,"
        + "`startHour` int not null,"
        + "`startMinute` int not null,"
        + "`timeout` bigint not null,"
        + "PRIMARY KEY(`zoneID`, `startHour`, `startMinute`)) DEFAULT CHARSET=UTF8");
    
    
    preparedStatements.put(DBStatements.MAIN_SEL_MODES, "SELECT ID, name FROM `modes`");
    preparedStatements.put(DBStatements.MAIN_SEL_ZONES_BY_MODE_ID, "SELECT pinAddress, name, shouldTakeWeather FROM `zones` WHERE `modeID`=?");
    preparedStatements.put(DBStatements.MAIN_SEL_DAYS_BY_ZONE_ID, "SELECT monday, tuesday, wednesday, thursday, friday, saturday, sunday FROM `days` WHERE `zoneID`=?");
    preparedStatements.put(DBStatements.MAIN_SEL_SCHEDULES_BY_ZONE_ID, "SELECT startHour, startMinute, timeout FROM `schedules` WHERE `zoneID`=?");

    preparedStatements.put(DBStatements.MAIN_INS_MODE, "INSERT INTO `modes` "
        + "(name) VALUES (?)");
    preparedStatements.put(DBStatements.MAIN_INS_ZONE, "INSERT INTO `zones` "
        + "(pinAddress, modeID, name, shouldTakeWeather) VALUES (?,?,?,?)");
    preparedStatements.put(DBStatements.MAIN_INS_DAYS, "INSERT INTO `days` "
        + "(zoneID, monday, tuesday, wednesday, thursday, friday, saturday, sunday) VALUES (?,?,?,?,?,?,?,?)");
    preparedStatements.put(DBStatements.MAIN_INS_SCHEDULES, "INSERT INTO `schedules` "
        + "(zoneID, startHour, startMinute, timeout) VALUES (?,?,?,?)");
    
    preparedStatements.put(DBStatements.MAIN_DEL_MODE, "DELETE FROM `modes` "
        + "WHERE `ID` = ?");
    preparedStatements.put(DBStatements.MAIN_DEL_ZONES_BY_MODE_ID, "DELETE FROM `zones` "
        + "WHERE `modeID` = ?");
    preparedStatements.put(DBStatements.MAIN_DEL_DAYS_BY_ZONE_ID, "DELETE FROM `days` "
        + "WHERE `zoneID` = ?");
    preparedStatements.put(DBStatements.MAIN_DEL_SCHEDULES_BY_ZONE_ID, "DELETE FROM `schedules` "
        + "WHERE `zoneID` = ?");
  }
}
