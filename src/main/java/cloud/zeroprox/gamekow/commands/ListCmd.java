package cloud.zeroprox.gamekow.commands;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.game.Game;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;

public class ListCmd implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        List<Text> arenas = new ArrayList<>();
        for (Game game: GameKow.getGameManager().games) {
            arenas.add(Text.builder(game.getName()).color(game.getMode() == GameKow.Mode.DISABLED ? TextColors.RED : TextColors.GREEN).onClick(TextActions.runCommand("/kow join " + game.getName())).build());
        }

        PaginationList.builder()
                .title(Text.of(TextColors.GREEN, "KingOfTheWater Arena's"))
                .padding(Text.of(TextColors.GOLD, "="))
                .contents(arenas)
                .build()
                .sendTo(src);
        return CommandResult.success();
    }
}
