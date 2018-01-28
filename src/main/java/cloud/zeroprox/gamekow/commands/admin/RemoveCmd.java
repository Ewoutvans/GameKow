package cloud.zeroprox.gamekow.commands.admin;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.game.Game;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class RemoveCmd implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Optional<String> gameName = args.getOne(Text.of("game"));
        if (!gameName.isPresent()) {
            throw new CommandException(Text.of("Game not found"));
        }
        Optional<Game> gameOptional = GameKow.getGameManager().getGame(gameName.get());
        if (!gameOptional.isPresent()) {
            throw new CommandException(Text.of("Game not found"));
        }
        GameKow.getInstance().removeArena(gameOptional.get());
        src.sendMessage(Text.of("Removed game"));
        return CommandResult.success();
    }
}
