package cloud.zeroprox.gamekow.utils;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

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

    @Setting("red")
    public Transform<World> red;

    @Setting("green")
    public Transform<World> green;

    @Setting("orange")
    public Transform<World> orange;

    @Setting("yellow")
    public Transform<World> yellow;

    @Setting("purple")
    public Transform<World> purple;

    @Setting("blue")
    public Transform<World> blue;

    public Location corner_play_1;
    public Location corner_play_2;
    public Location corner_area_1;
    public Location corner_area_2;

    public GameSerialize() {

    }
}
