package joserodpt.realskywars.plugin.managers;

/*
 *   _____            _  _____ _
 *  |  __ \          | |/ ____| |
 *  | |__) |___  __ _| | (___ | | ___   ___      ____ _ _ __ ___
 *  |  _  // _ \/ _` | |\___ \| |/ / | | \ \ /\ / / _` | '__/ __|
 *  | | \ \  __/ (_| | |____) |   <| |_| |\ V  V / (_| | |  \__ \
 *  |_|  \_\___|\__,_|_|_____/|_|\_\\__, | \_/\_/ \__,_|_|  |___/
 *                                   __/ |
 *                                  |___/
 *
 * Licensed under the MIT License
 * @author José Rodrigues
 * @link https://github.com/joserodpt/RealSkywars
 */

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.db.DatabaseTypeUtils;
import com.j256.ormlite.logger.LoggerFactory;
import com.j256.ormlite.logger.NullLogBackend;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import joserodpt.realskywars.api.RealSkywarsAPI;
import joserodpt.realskywars.api.config.RSWSQLConfig;
import joserodpt.realskywars.api.database.PlayerData;
import joserodpt.realskywars.api.managers.DatabaseManagerAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class DatabaseManager extends DatabaseManagerAPI {

    private final Dao<PlayerData, UUID> playerDataDao;
    private final HashMap<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final RealSkywarsAPI rsa;

    public DatabaseManager(RealSkywarsAPI rsa) throws SQLException {
        LoggerFactory.setLogBackendFactory(new NullLogBackend.NullLogBackendFactory());

        this.rsa = rsa;
        String databaseURL = getDatabaseURL();

        ConnectionSource connectionSource = new JdbcConnectionSource(
                databaseURL,
                RSWSQLConfig.file().getString("username"),
                RSWSQLConfig.file().getString("password"),
                DatabaseTypeUtils.createDatabaseType(databaseURL)
        );

        TableUtils.createTableIfNotExists(connectionSource, PlayerData.class);

        this.playerDataDao = DaoManager.createDao(connectionSource, PlayerData.class);

        // add new choosen_kit (v0.8)
        createColumnIfNotExists(connectionSource, "choosen_kit", "VARCHAR");

        getPlayerData();
    }

    public void createColumnIfNotExists(ConnectionSource cs, String columnName, String columnType) {
        try {
            if (!doesColumnExist(cs, columnName)) {
                Bukkit.getLogger().warning("[RealSkywars] RealSkywars.db: Upgrading SQL table to add choosen_kit to realscoreboard_playerdata...");
                playerDataDao.executeRaw("ALTER TABLE realscoreboard_playerdata ADD COLUMN " + columnName + " " + columnType);
                Bukkit.getLogger().warning("[RealSkywars] RealSkywars.db: Upgrade complete!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean doesColumnExist(ConnectionSource cs, String columnName) {
        try {
            DatabaseMetaData metaData = cs.getReadOnlyConnection(null).getUnderlyingConnection().getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "realscoreboard_playerdata", columnName);

            return columns.next(); // Return true if the column exists
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Database connection String used for establishing a connection.
     *
     * @return The database URL String
     */
    @Override
    @NotNull
    protected String getDatabaseURL() {
        final String driver = RSWSQLConfig.file().getString("driver").toLowerCase();

        switch (driver) {
            case "mysql":
            case "mariadb":
            case "postgresql":
                return "jdbc:" + driver + "://" + RSWSQLConfig.file().getString("host") + ":" + RSWSQLConfig.file().getInt("port") + "/" + RSWSQLConfig.file().getString("database");
            case "sqlserver":
                return "jdbc:sqlserver://" + RSWSQLConfig.file().getString("host") + ":" + RSWSQLConfig.file().getInt("port") + ";databaseName=" + RSWSQLConfig.file().getString("database");
            default:
                return "jdbc:sqlite:" + new File(rsa.getPlugin().getDataFolder(), RSWSQLConfig.file().getString("database") + ".db");
        }
    }

    @Override
    protected void getPlayerData() {
        try {
            playerDataDao.queryForAll().forEach(playerData -> playerDataCache.put(playerData.getUUID(), playerData));
        } catch (SQLException exception) {
            rsa.getLogger().severe("Error while getting the player data:" + exception.getMessage());
        }
    }

    @Override
    public PlayerData getPlayerData(Player p) {
        return playerDataCache.getOrDefault(p.getUniqueId(), new PlayerData(p));
    }

    @Override
    public void savePlayerData(PlayerData playerData, boolean async) {
        playerDataCache.put(playerData.getUUID(), playerData);
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(rsa.getPlugin(), () -> savePlayerData(playerData, false));
        } else {
            try {
                playerDataDao.createOrUpdate(playerData);
            } catch (SQLException throwables) {
                rsa.getLogger().severe("Error while saving the player data:" + throwables.getMessage());
            }
        }
    }

    @Override
    public void deletePlayerData(PlayerData playerData, boolean async) {
        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(rsa.getPlugin(), () -> deletePlayerData(playerData, false));
        } else {
            try {
                playerDataDao.delete(playerData);
            } catch (SQLException throwables) {
                rsa.getLogger().severe("Error while deleting the player data:" + throwables.getMessage());

            }
        }
    }

    @Override
    public Dao<PlayerData, UUID> getQueryDao() {
        return this.playerDataDao;
    }
}