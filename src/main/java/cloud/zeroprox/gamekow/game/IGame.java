package cloud.zeroprox.gamekow.game;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.stats.PlayerStats;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IGame {

    GameKow.Mode getMode();

    String getName();

    Collection<UUID> getAllPlayers();

    TextColor getFreeColor();

    Transform<World> getRandomSpawn();

    Transform<World> getLobby();

    boolean isInsideArea(Location<World> location);

    boolean isInsidePlayGround(Player player);

    Optional<PlayerStats> getPlayerStats(Player player);

    Optional<PlayerStats> getPlayerStats(UUID uuid);

    void showStats(Player player);

    void showStatsOfPlayer(Player player, Player target);

    void toggleStatus();

    void manageLoss(Player player, GameKow.LossType lossTypes);

    void addPlayer(Player player);

    void leavePlayer(Player player, boolean resetStats);

    void clearScores();

    boolean containsPlayer(Player attacker);
}
