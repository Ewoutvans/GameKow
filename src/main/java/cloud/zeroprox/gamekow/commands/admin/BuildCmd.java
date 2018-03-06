package cloud.zeroprox.gamekow.commands.admin;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.utils.AABBSerialize;
import cloud.zeroprox.gamekow.utils.GameSerialize;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class BuildCmd implements CommandExecutor {

    GameSerialize gameSerialize;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            throw new CommandException(Text.of(TextColors.RED, "You need to be a player"));
        }
        Optional<GameKow.AdminBuildTypes> adminOptional = args.getOne(Text.of("type"));
        if (!adminOptional.isPresent()) {
            showProgress(src);
            return CommandResult.empty();
        }
        GameKow.AdminBuildTypes adminType = adminOptional.get();
        if (adminType.equals(GameKow.AdminBuildTypes.SAVE)) {
            //SAVE
            gameSerialize.playground = new AABBSerialize(gameSerialize.corner_play_1.getBlockX(),
                    gameSerialize.corner_play_1.getBlockY(),
                    gameSerialize.corner_play_1.getBlockZ(),
                    gameSerialize.corner_play_2.getBlockX(),
                    gameSerialize.corner_play_2.getBlockY(),
                    gameSerialize.corner_play_2.getBlockZ());

            gameSerialize.area = new AABBSerialize(gameSerialize.corner_area_1.getBlockX(),
                    gameSerialize.corner_area_1.getBlockY(),
                    gameSerialize.corner_area_1.getBlockZ(),
                    gameSerialize.corner_area_2.getBlockX(),
                    gameSerialize.corner_area_2.getBlockY(),
                    gameSerialize.corner_area_2.getBlockZ());


            GameKow.getInstance().addArena(gameSerialize);
            src.sendMessage(Text.of("Saved"));
            return CommandResult.success();
        }
        if (adminType.equals(GameKow.AdminBuildTypes.NAME)) {
            Optional<String> name = args.getOne(Text.of("name"));
            this.gameSerialize = new GameSerialize();
            this.gameSerialize.gameType = GameKow.GameType.CLASSIC;
            this.gameSerialize.name = name.orElse(new Random().nextLong() + "");
            this.gameSerialize.spawns = new ArrayList<>();
            showProgress(src);
            return CommandResult.success();
        }
        if (adminType.equals(GameKow.AdminBuildTypes.STOP)) {
            this.gameSerialize = null;
            src.sendMessage(Text.of(TextColors.GREEN, "Setup stopped"));
            return CommandResult.success();
        }
        if (adminType.equals(GameKow.AdminBuildTypes.TYPE)) {
            Optional<String> name = args.getOne(Text.of("name"));
            this.gameSerialize.gameType = GameKow.GameType.valueOf(name.orElse("CLASSIC"));
        }
        Player player = (Player)src;
        switch (adminType) {
            case LOBBY:
                gameSerialize.lobby = player.getTransform();
                break;
            case SPAWN:
                gameSerialize.spawns.add(player.getTransform());
                break;
            case CORNER_PLAY_1:
                gameSerialize.corner_play_1 = player.getLocation();
                break;
            case CORNER_PLAY_2:
                gameSerialize.corner_play_2 = player.getLocation();
                break;
            case CORNER_AREA_1:
                gameSerialize.corner_area_1 = player.getLocation();
                break;
            case CORNER_AREA_2:
                gameSerialize.corner_area_2 = player.getLocation();
                break;
            default:
        }
        showProgress(src);
        return CommandResult.empty();
    }

    private void showProgress(CommandSource src) {
        List<Text> textArray = new ArrayList<>();
        if (gameSerialize == null) {
            textArray.add(Text.builder("No new builder start -click me- to start").onClick(TextActions.suggestCommand("/kow admin build NAME <name>")).build());
        } else {
            for (GameKow.AdminBuildTypes type : GameKow.AdminBuildTypes.values()) {
                Object o = null;
                switch (type) {
                    case NAME: o = gameSerialize.name; break;
                    case LOBBY: o = gameSerialize.lobby; break;
                    case SPAWN: o = gameSerialize.spawns; break;
                    case CORNER_PLAY_1: o = gameSerialize.corner_play_1; break;
                    case CORNER_PLAY_2: o = gameSerialize.corner_play_2; break;
                    case CORNER_AREA_1: o = gameSerialize.corner_area_1; break;
                    case CORNER_AREA_2: o = gameSerialize.corner_area_2; break;
                    default: break;
                }
                if (type.equals(GameKow.AdminBuildTypes.TYPE)) {
                    textArray.add(
                            Text.builder("Type (current: " + gameSerialize.gameType + "): ").color(TextColors.GREEN)
                                    .append(Text.builder("CLASSIC ").onClick(TextActions.runCommand("/kow admin build TYPE CLASSIC")).build())
                                    .append(Text.builder("FREEJOIN ").onClick(TextActions.runCommand("/kow admin build TYPE FREEJOIN")).build())
                                    .build());
                    continue;
                }
                if (type.equals(GameKow.AdminBuildTypes.STOP)) continue;;
                if (o == null) {
                    textArray.add(Text.builder(type.name() + ": --").color(TextColors.RED).onClick(TextActions.runCommand("/kow admin build " + type.name())).build());
                } else {
                    if (o instanceof List) {
                        List list = (List) o;
                        if (list.size() >= 2) {
                            textArray.add(Text.builder(type.name() + ": Okay (" + ((List) o).size() + ")").color(TextColors.GREEN).onClick(TextActions.runCommand("/kow admin build " + type.name())).build());
                        } else {
                            textArray.add(Text.builder(type.name() + ": -- (" + ((List) o).size() + ")").color(TextColors.RED).onClick(TextActions.runCommand("/kow admin build " + type.name())).build());
                        }
                    } else {
                        textArray.add(Text.builder(type.name() + ": Okay").color(TextColors.GREEN).onClick(TextActions.runCommand("/kow admin build " + type.name())).build());
                    }
                }
            }
        }
        PaginationList.builder()
                .title(Text.of("New build arena"))
                .contents(textArray)
                .build()
        .sendTo(src);
    }
}
