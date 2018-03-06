package cloud.zeroprox.gamekow.commands.admin;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.game.IGame;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class ClearStatsCmd implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Optional<String> gameName = args.getOne(Text.of("game"));
        if (!gameName.isPresent()) {
            throw new CommandException(Text.of("Game not found"));
        }
        Optional<IGame> gameOptional = GameKow.getGameManager().getGame(gameName.get());
        gameOptional.ifPresent(game -> game.clearScores());
        src.sendMessage(Text.of("Game scores cleared."));
        return CommandResult.success();
    }
}
