package cloud.zeroprox.gamekow;

import cloud.zeroprox.gamekow.commands.*;
import cloud.zeroprox.gamekow.commands.admin.BuildCmd;
import cloud.zeroprox.gamekow.commands.admin.DisableCmd;
import cloud.zeroprox.gamekow.commands.admin.RemoveCmd;
import cloud.zeroprox.gamekow.game.Game;
import cloud.zeroprox.gamekow.game.GameManager;
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
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
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

    CommandSpec adminCmd = CommandSpec.builder()
            .description(Text.of("Area management"))
            .permission("gamekow.admin")
            .executor(new AdminCmd())
            .child(adminBuildCmd, "build")
            .child(adminToggleCmd, "toggle")
            .child(adminRemoveCmd, "remove")
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
            getGameManager().games.clear();
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            for (GameSerialize gameSerialize : gameSerializeList) {
                Map<TextColor, Transform<World>> spawns = new HashMap<>();
                spawns.put(TextColors.RED, gameSerialize.red);
                spawns.put(TextColors.GREEN, gameSerialize.green);
                spawns.put(TextColors.GOLD, gameSerialize.orange);
                spawns.put(TextColors.YELLOW, gameSerialize.yellow);
                spawns.put(TextColors.BLUE, gameSerialize.blue);
                spawns.put(TextColors.LIGHT_PURPLE, gameSerialize.purple);
                Game game = new Game(gameSerialize.name,
                        gameSerialize.lobby,
                        gameSerialize.area.toAABB(),
                        gameSerialize.playground.toAABB(),
                        spawns
                );
                getGameManager().games.add(game);
            }
            logger.info("Loaded: " + getGameManager().games.size() + " games");
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

    public void removeArena(Game game) {
        try {
            List<GameSerialize> gameSerializeList = rootNode.getNode("areas").getList(TypeToken.of(GameSerialize.class));
            List<GameSerialize> gameList = new ArrayList<>();
            gameList.addAll(gameSerializeList);
            gameList.removeIf(gameSerialize -> gameSerialize.name.equalsIgnoreCase(game.getName()));
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
        DISABLED, READY;
    }

    public enum AdminBuildTypes {
        NAME, LOBBY, RED, GREEN, ORANGE, YELLOW, PURPLE, BLUE, CORNER_PLAY_1, CORNER_PLAY_2, CORNER_AREA_1, CORNER_AREA_2, SAVE, STOP
    }
}
