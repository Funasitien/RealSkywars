package joserodpt.realskywars.api.managers.world.engines;

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

import joserodpt.realskywars.api.Debugger;
import joserodpt.realskywars.api.RealSkywarsAPI;
import joserodpt.realskywars.api.managers.WorldManagerAPI;
import joserodpt.realskywars.api.managers.world.RSWWorld;
import joserodpt.realskywars.api.managers.world.SWWorldEngine;
import joserodpt.realskywars.api.map.RSWMap;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.Objects;

public class SWWorldDefaultEngine implements SWWorldEngine {

    private final WorldManagerAPI wm = RealSkywarsAPI.getInstance().getWorldManagerAPI();
    private World world;
    private final RSWMap gameRoom;
    private final String worldName;

    public SWWorldDefaultEngine(World w, RSWMap gameMode) {
        this.worldName = w.getName();
        this.world = w;
        this.world.setAutoSave(false);
        this.gameRoom = gameMode;
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public void resetWorld(RSWMap.OperationReason rr) {
        Debugger.print(SWWorldDefaultEngine.class, "Resetting " + this.getName() + " - type: " + this.getType().name());

        if (Objects.requireNonNull(rr) == RSWMap.OperationReason.SHUTDOWN) {//delete world
            this.deleteWorld(RSWMap.OperationReason.SHUTDOWN);
        } else {
            this.deleteWorld(RSWMap.OperationReason.RESET);
            //Copy world
            this.wm.copyWorld(this.getName(), WorldManagerAPI.CopyTo.ROOT);

            //Load world
            this.world = this.wm.createEmptyWorld(this.getName(), World.Environment.NORMAL);
            if (this.world != null) {
                WorldBorder wb = this.world.getWorldBorder();

                wb.setCenter(this.gameRoom.getArena().getCenter());
                wb.setSize(this.gameRoom.getBorderSize());

                this.gameRoom.setState(RSWMap.MapState.AVAILABLE);
            } else {
                RealSkywarsAPI.getInstance().getLogger().severe("ERROR! Could not load " + this.getName());
            }
        }
    }

    @Override
    public void deleteWorld(RSWMap.OperationReason rr) {
        switch (rr) {
            case LOAD:
                break;
            case SHUTDOWN:
            case RESET:
                this.wm.deleteWorld(this.getName(), true);
                break;
        }
    }

    @Override
    public void setTime(long l) {
        this.world.setTime(l);
    }

    @Override
    public String getName() {
        return this.world != null ? this.world.getName() : this.worldName;
    }

    @Override
    public RSWWorld.WorldType getType() {
        return RSWWorld.WorldType.DEFAULT;
    }

    @Override
    public void save() {
        this.world.save();
    }
}
