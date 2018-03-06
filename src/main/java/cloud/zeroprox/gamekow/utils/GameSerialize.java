package cloud.zeroprox.gamekow.utils;

import cloud.zeroprox.gamekow.GameKow;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;

@ConfigSerializable
public class GameSerialize {

    @Setting("name")
    public String name;

    @Setting("lobby")
    public Transform<World> lobby;

    @Setting("area")
    public AABBSerialize area;

    @Setting("playground")
    public AABBSerialize playground;

    @Setting("spawns")
    public List<Transform<World>> spawns;

    @Setting("gametype")
    public GameKow.GameType gameType;

    public Location corner_play_1;
    public Location corner_play_2;
    public Location corner_area_1;
    public Location corner_area_2;



    public GameSerialize() {

    }
}
