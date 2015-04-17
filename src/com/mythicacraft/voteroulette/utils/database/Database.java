package com.mythicacraft.voteroulette.utils.database;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.plugin.Plugin;

import com.mythicacraft.voteroulette.VoteRoulette;
import com.mythicacraft.voteroulette.utils.Utils;

/**
 * Abstract Database class, serves as a base for any connection method (MySQL,
 * SQLite, etc.)
 * 
 * @author -_Husky_-
 * @author tips48
 */
public abstract class Database {

	protected Connection connection;

	/**
	 * Plugin instance, use for plugin.getDataFolder()
	 */
	protected Plugin plugin;

	/**
	 * Creates a new Database
	 * 
	 * @param plugin
	 *            Plugin instance
	 */
	protected Database(Plugin plugin) {
		this.plugin = plugin;
		this.connection = null;
	}

	/**
	 * Opens a connection with the database
	 * 
	 * @return Opened connection
	 * @throws SQLException
	 *             if the connection can not be opened
	 * @throws ClassNotFoundException
	 *             if the driver cannot be found
	 */
	public abstract Connection openConnection() throws SQLException,
	ClassNotFoundException;

	/**
	 * Checks if a connection is open with the database
	 * 
	 * @return true if the connection is open
	 * @throws SQLException
	 *             if the connection cannot be checked
	 */
	public boolean checkConnection() throws SQLException {
		if(connection != null && !connection.isClosed()) {
			createVRTables();
			return true;
		} else {
			//VoteRoulette.USE_DATABASE = false;
			return false;
		}
	}

	private void createVRTables() {
		try {
			this.createTable("vr_votes", "vote_id INT AUTO_INCREMENT KEY, player_id varchar(255) NOT NULL, website varchar(255), datetime DATETIME");
			this.createTable("vr_voters", "player_id varchar(255) NOT NULL KEY, player_name varchar(255) NOT NULL, current_cycle int, lifetime_votes int, longest_vote_streak int, current_vote_streak int");
			this.createTable("vr_unclaimed_awards", "unclaimed_id INT AUTO_INCREMENT KEY, award_name varchar(255) NOT NULL, award_type int NOT NULL, player_id varchar(255) NOT NULL");
			//this.createTable("vr_delayed_commands", "command_id INT AUTO_INCREMENT KEY, command varchar(255) NOT NULL, player_name varchar(255), seconds_left int NOT NULL");
		} catch (Exception e) {
			VoteRoulette.getPlugin().getLogger().severe("Unable to create tables in database, plugin may not function as intended!");
		}

	}

	public void createTable(String name, String columns) throws SQLException {
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet tables = dbm.getTables(null, null, name, null);
		if (!tables.next()) {
			Utils.debugMessage("Table \"" + name + "\" not found, creating...");
			Statement statement = connection.createStatement();
			statement.executeUpdate("CREATE TABLE " + name + "(" + columns + ")");
		}
	}

	/**
	 * Gets the connection with the database
	 * 
	 * @return Connection with the database, null if none
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Closes the connection with the database
	 * 
	 * @return true if successful
	 * @throws SQLException
	 *             if the connection cannot be closed
	 */
	public boolean closeConnection() throws SQLException {
		if (connection == null) {
			return false;
		}
		connection.close();
		return true;
	}


	/**
	 * Executes a SQL Query<br>
	 * 
	 * If the connection is closed, it will be opened
	 * 
	 * @param query
	 *            Query to be run
	 * @return the results of the query
	 * @throws SQLException
	 *             If the query cannot be executed
	 * @throws ClassNotFoundException
	 *             If the driver cannot be found; see {@link #openConnection()}
	 */
	public ResultSet querySQL(String query) throws SQLException,
	ClassNotFoundException {
		if (!checkConnection()) {
			openConnection();
		}

		Statement statement = connection.createStatement();

		ResultSet result = statement.executeQuery(query);

		return result;
	}

	/**
	 * Executes an Update SQL Query<br>
	 * See {@link java.sql.Statement#executeUpdate(String)}<br>
	 * If the connection is closed, it will be opened
	 * 
	 * @param query
	 *            Query to be run
	 * @return Result Code, see {@link java.sql.Statement#executeUpdate(String)}
	 * @throws SQLException
	 *             If the query cannot be executed
	 * @throws ClassNotFoundException
	 *             If the driver cannot be found; see {@link #openConnection()}
	 */
	public int updateSQL(String query) throws SQLException,
	ClassNotFoundException {
		if (!checkConnection()) {
			openConnection();
		}

		Statement statement = connection.createStatement();

		int result = statement.executeUpdate(query);

		return result;
	}
}