package cloud.zeroprox.gamekow.commands;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

public class HelpCmd implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        PaginationList.builder()
                .title(Text.of(TextColors.GREEN, "Game KingOfTheWater commands"))
                .padding(Text.of(TextColors.GOLD, "="))
                .contents(
                        Text.builder("/kow join ").color(TextColors.GREEN).append(Text.of(TextColors.WHITE, "[area]")).onClick(TextActions.suggestCommand("/kow join ")).build(),
                        Text.builder("/kow leave").color(TextColors.GREEN).onClick(TextActions.runCommand("/kow leave")).build(),
                        Text.builder("/kow list").color(TextColors.GREEN).onClick(TextActions.runCommand("/kow list")).build(),
                        Text.builder("/kow admin").color(src.hasPermission("gamekow.admin") ? TextColors.GREEN : TextColors.RED).onClick(TextActions.runCommand("/kow admin")).build()
                )
                .build()
                .sendTo(src);
        return CommandResult.success();
    }
}
