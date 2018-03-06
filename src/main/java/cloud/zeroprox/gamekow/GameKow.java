package cloud.zeroprox.gamekow;

import cloud.zeroprox.gamekow.commands.*;
import cloud.zeroprox.gamekow.commands.admin.BuildCmd;
import cloud.zeroprox.gamekow.commands.admin.ClearStatsCmd;
import cloud.zeroprox.gamekow.commands.admin.DisableCmd;
import cloud.zeroprox.gamekow.commands.admin.RemoveCmd;
import cloud.zeroprox.gamekow.game.GameClassic;
import cloud.zeroprox.gamekow.game.GameFreeJoin;
import cloud.zeroprox.gamekow.game.GameManager;
import cloud.zeroprox.gamekow.game.IGame;
import cloud.zeroprox.gamekow.utils.GameSerialize;
import cloud.zeroprox.gamekow.utils.TransformWorldSerializer;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.TabCompleteEvent;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Plugin(id = "gamekow", name = "GameKow", description = "Game Kill of The Water", url = "https://zeroprox.cloud", authors = {"ewoutvs_", "Alagild"})
public class GameKow {

    @Inject
    private Logger logger;
    private static GameManager gameManager;
    private static GameKow instance;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    private ConfigurationNode rootNode;


    CommandSpec joinCmd = CommandSpec.builder()
            .description(Text.of("Join a game"))
            .arguments(GenericArguments.optional(GenericArguments.string(Text.of("game"))))
            .permission("gamekow.join")
            .executor(new JoinCmd())
            .build();

    CommandSpec leaveCmd = CommandSpec.builder()
            .description(Text.of("Leave game"))
            .arguments(GenericArguments.flags().flag("f").buildWith(GenericArguments.none()))
            .permission("gamekow.leave")
            .executor(new LeaveCmd())
            .build();

    CommandSpec adminBuildCmd = CommandSpec.builder()
            .description(Text.of("Build"))
            .arguments(
                    GenericArguments.optional(GenericArguments.enumValue(Text.of("type"), AdminBuildTypes.class)),
                    GenericArguments.optional(GenericArguments.string(Text.of("name")))
            )
            .executor(new BuildCmd())
            .build();

    CommandSpec adminToggleCmd = CommandSpec.builder()
            .description(Text.of("Toggle arena"))
            .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("game"))))
            .executor(new DisableCmd())
            .build();

    CommandSpec adminRemoveCmd = CommandSpec.builder()
            .description(Text.of("Remove arena"))
            .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("game"))))
            .executor(new RemoveCmd())
            .build();

    CommandSpec adminClearStatsCmd = CommandSpec.builder()
            .description(Text.of("Clear stats"))
            .arguments(GenericArguments.onlyOne(GenericArguments.string(Text.of("game"))))
            .executor(new ClearStatsCmd())
            .build();

    CommandSpec adminCmd = CommandSpec.builder()
            .description(Text.of("Area management"))
            .permission("gamekow.admin")
            .executor(new AdminCmd())
            .child(adminBuildCmd, "build")
            .child(adminToggleCmd, "toggle")
            .child(adminRemoveCmd, "remove")
            .child(adminClearStatsCmd, "clearstats")
            .build();

    CommandSpec listCmd = CommandSpec.builder()
            .description(Text.of("Show game list"))
            .executor(new ListCmd())
            .permission("gamekow.join")
            .build();

    CommandSpec kowCmd = CommandSpec.builder()
            .description(Text.of("Main command"))
            .child(joinCmd, "join")
            .child(leaveCmd, "leave")
            .child(listCmd, "list")
            .child(adminCmd, "admin")
            .executor(new HelpCmd())
            .build();

    public static GameKow getInstance() {
        return instance;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        Sponge.getCommandManager().register(this, kowCmd, "gamekow", "kow", "kingofthewater");
        Sponge.getEventManager().registerListeners(this, new Listeners());
        TypeToken<Transform<World>> transformTypeToken = new TypeToken<Transform<World>>() {};
        TypeSerializers.getDefaultSerializers().registerType(transformTypeToken, new TransformWorldSerializer());

        this.gameManager = new GameManager();
        this.instance = this;
        configManager = HoconConfigurationLoader.builder().setPath(defaultConfig).build();
        try {
            rootNode = configManager.load();
            loadConfig();
        } catch(IOException e) {
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        }
    }


    @Listener
    public void onGameReload(GameReloadEvent event) {
        try {
            loadConfig();
        } catch (IOException e) {
        } catch (ObjectMappingException e) {
        }
    }

    private void loadConfig() throws IOException, ObjectMappingException {
        if (rootNode.getNode("areas").isVirtual()) {
            logger.info("Creating configuration");

            rootNode.getNode("areas").setValue(new TypeToken<List<GameSerialize>>(){}, Arrays.asList());
            configManager.save(rootNode);
            loadConfig();
        } else {
            getGameManager().iGames.clear();
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            for (GameSerialize gameSerialize : gameSerializeList) {
                TextColor[] colors = Sponge.getRegistry().getAllOf(TextColor.class).toArray(new TextColor[]{});
                Map<TextColor, Transform<World>> spawns = new HashMap<>();
                for (Transform<World> spawn : gameSerialize.spawns) {
                    spawns.put(colors[spawns.size()], spawn);
                }
                IGame iGame = null;
                if (gameSerialize.gameType == GameType.CLASSIC) {
                     iGame = new GameClassic(gameSerialize.name,
                            gameSerialize.lobby,
                            gameSerialize.area.toAABB(),
                            gameSerialize.playground.toAABB(),
                            spawns
                    );
                } else if (gameSerialize.gameType == GameType.FREEJOIN) {
                    iGame = new GameFreeJoin(gameSerialize.name,
                            gameSerialize.lobby,
                            gameSerialize.area.toAABB(),
                            gameSerialize.playground.toAABB(),
                            spawns
                    );
                }
                getGameManager().iGames.add(iGame);
            }
            logger.info("Loaded: " + getGameManager().iGames.size() + " games");
        }
    }

    public static GameManager getGameManager() {
        return gameManager;
    }

    public void addArena(GameSerialize gameSerialize) {
        try {
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            List<GameSerialize> gameList = new ArrayList<>();
            gameList.addAll(gameSerializeList);
            gameList.add(gameSerialize);
            rootNode.getNode("areas").setValue(new TypeToken<List<GameSerialize>>(){}, gameList);
            configManager.save(rootNode);
            loadConfig();
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeArena(IGame iGame) {
        try {
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            List<GameSerialize> gameList = new ArrayList<>();
            gameList.addAll(gameSerializeList);
            gameList.removeIf(gameSerialize -> gameSerialize.name.equalsIgnoreCase(iGame.getName()));
            rootNode.getNode("areas").setValue(new TypeToken<List<GameSerialize>>(){}, gameList);
            configManager.save(rootNode);
            loadConfig();
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum Mode {
        DISABLED, READY
    }

    public enum LossType {
        FALL_OUTSIDE, FALL_DAMAGE
    }

    public enum AdminBuildTypes {
        NAME, LOBBY, SPAWN, CORNER_PLAY_1, CORNER_PLAY_2, CORNER_AREA_1, CORNER_AREA_2, SAVE, STOP, TYPE
    }

    public enum GameType {
        CLASSIC, FREEJOIN
    }
}
