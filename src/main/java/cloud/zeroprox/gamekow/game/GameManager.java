package cloud.zeroprox.gamekow.game;

import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GameManager {

    public List<Game> games = new ArrayList<>();

    public boolean isPlayerActive(Player player) {
        return getPlayerGame(player).isPresent();
    }

    public Optional<Game> getGame(String gameName) {
        return games.stream().filter(game -> game.getName().equalsIgnoreCase(gameName)).findFirst();
    }

    public Optional<Game> getPlayerGame(Player player) {
        return games.stream().filter(game -> game.getAllPlayers().contains(player.getUniqueId())).findFirst();
    }

    public String getDefaultName() {
        return games.size() == 0 ? "DEFAULT" : games.get(0).getName();
    }
}
