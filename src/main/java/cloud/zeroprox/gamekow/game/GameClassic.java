package cloud.zeroprox.gamekow.game;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.stats.LastHit;
import cloud.zeroprox.gamekow.stats.PlayerStats;
import com.google.common.collect.Iterables;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.*;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.displayslot.DisplaySlots;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;

public class GameClassic implements IGame {

    private String name;
    private GameKow.Mode mode;
    private Transform<World> lobby;
    private UUID world;
    private Random random = new Random();
    private AABB area;
    private AABB playground;
    Map<UUID, PlayerStats> activePlayers = new HashMap<>();
    int limit;

    private ServerBossBar serverBossBar = ServerBossBar.builder()
            .color(BossBarColors.YELLOW)
            .createFog(false)
            .darkenSky(false)
            .name(Text.of("-"))
            .overlay(BossBarOverlays.PROGRESS)
            .percent(1)
            .visible(true)
            .build();
    private Map<TextColor, Transform<World>> spawns;

    public GameClassic(String name, Transform<World> lobby, AABB area, AABB playground, Map<TextColor, Transform<World>> spawns) {
        this.name = name;
        this.lobby = lobby;
        this.area = area;
        this.playground = playground;
        this.limit = spawns.size();
        this.world = lobby.getExtent().getUniqueId();
        this.mode = GameKow.Mode.READY;
        this.spawns = spawns;
    }

    @Override
    public Collection<UUID> getAllPlayers() {
        return this.activePlayers.keySet();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isInsideArea(Location<World> location) {
        return area.contains(location.getX(), location.getY(), location.getZ()) && world.equals(location.getExtent().getUniqueId());
    }

    @Override
    public Transform<World> getLobby() {
        return lobby;
    }

    @Override
    public void addPlayer(Player player) {
        /*if (GameKow.getGameManager().isPlayerActive(player)) {
            player.sendMessage(Text.of(TextColors.RED, "You are already in a game"));
            return;
        }*/
        if (this.activePlayers.size() >= this.limit) {
            player.sendMessage(Text.of(TextColors.RED, "You can't join, the game is at a limit"));
            return;
        }
        if (this.mode == GameKow.Mode.DISABLED) {
            player.sendMessage(Text.of(TextColors.RED, "You can't join the game is not ready"));
            return;
        }
        player.health().set(20D);
        player.maxHealth().set(20D);
        player.foodLevel().set(20);
        player.offer(Keys.GAME_MODE, GameModes.ADVENTURE);
        player.offer(Keys.CAN_FLY, false);
        player.offer(Keys.FIRE_TICKS, 0);
        player.offer(Keys.POTION_EFFECTS, Arrays.asList(
                PotionEffect.builder().amplifier(5).duration(20 * 60 * 60 * 60).particles(false).potionType(PotionEffectTypes.RESISTANCE).build(),
                PotionEffect.builder().amplifier(1).duration(20 * 60 * 60 * 60).particles(false).potionType(PotionEffectTypes.WATER_BREATHING).build(),
                PotionEffect.builder().amplifier(1).duration(20 * 60 * 60 * 60).particles(false).potionType(PotionEffectTypes.NIGHT_VISION).build()));
        player.sendMessage(Text.of(TextColors.GREEN, "You have joined the game"));
        ((PlayerInventory) player.getInventory()).getHotbar().setSelectedSlotIndex(0);

        PlayerStats playerStats;
        if (this.activePlayers.containsKey(player.getUniqueId())) {
            playerStats = this.activePlayers.get(player.getUniqueId());
        } else {
            playerStats = new PlayerStats(player.getUniqueId(), getFreeColor());
            this.activePlayers.put(player.getUniqueId(), playerStats);
        }

        player.setTransform(this.getRandomSpawn());
        player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.builder().itemType(ItemTypes.STICK).quantity(1).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.KNOCKBACK, 1))).build());
        ((PlayerInventory) player.getInventory()).getEquipment().set(EquipmentTypes.HEADWEAR, ItemStack.builder().itemType(ItemTypes.LEATHER_HELMET).add(Keys.COLOR, playerStats.getColor().getColor()).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.AQUA_AFFINITY, 1), Enchantment.of(EnchantmentTypes.UNBREAKING, 10), Enchantment.of(EnchantmentTypes.RESPIRATION, 10))).add(Keys.UNBREAKABLE, true).build());

        this.serverBossBar.addPlayer(player);
        this.showStats(player);
    }

    @Override
    public void leavePlayer(Player player, boolean resetStats) {
        this.getPlayerStats(player).ifPresent(PlayerStats::reset);
        player.getInventory().clear();
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        player.offer(Keys.POTION_EFFECTS, new ArrayList<>());
        player.setScoreboard(null);
        player.setTransform(this.getLobby());
        this.activePlayers.remove(player.getUniqueId());
        this.serverBossBar.removePlayer(player);

    }

    @Override
    public void clearScores() {
        for (PlayerStats stats : this.activePlayers.values()) {
            stats.reset();
        }
    }


    @Override
    public Optional<PlayerStats> getPlayerStats(Player player) {
        return this.getPlayerStats(player.getUniqueId());
    }

    @Override
    public Optional<PlayerStats> getPlayerStats(UUID uuid) {
        if (this.activePlayers.containsKey(uuid)) {
            return Optional.of(this.activePlayers.get(uuid));
        }
        return Optional.empty();
    }

    @Override
    public void showStats(Player player) {
        PlayerStats playerStats = getPlayerStats(player).get();

        String name = player.getUniqueId().toString();

        Objective obj = Objective.builder()
                .name(name.substring(0, 16))
                .displayName(Text.of("Score"))
                .criterion(Criteria.DUMMY)
                .displayName(Text.of(TextColors.RED, "Stats"))
                .build();

        obj.getOrCreateScore(Text.of("Kills")).setScore(playerStats.getKills());
        obj.getOrCreateScore(Text.of("Deaths")).setScore(playerStats.getDeaths());
        obj.getOrCreateScore(Text.of("HitsGiven")).setScore(playerStats.getHitsGiven());
        obj.getOrCreateScore(Text.of("HitsTaken")).setScore(playerStats.getHitsTaken());
        obj.getOrCreateScore(Text.of("Points")).setScore(playerStats.getPoints());

        Scoreboard scoreboard = Scoreboard.builder().build();
        scoreboard.addObjective(obj);
        scoreboard.updateDisplaySlot(obj, DisplaySlots.SIDEBAR);
        player.setScoreboard(scoreboard);
        PlayerStats topPoints = this.activePlayers.values().stream().sorted(Comparator.comparingLong(PlayerStats::getPoints).reversed()).findFirst().get();
        PlayerStats topKills = this.activePlayers.values().stream() .sorted(Comparator.comparingLong(PlayerStats::getKills) .reversed()).findFirst().get();
        PlayerStats topDeaths = this.activePlayers.values().stream().sorted(Comparator.comparingLong(PlayerStats::getDeaths).reversed()).findFirst().get();

        serverBossBar.setName(Text.builder()
                .append(Text.of(TextColors.GRAY, "[", TextColors.RED, "KOW-TOP", TextColors.GRAY, "]"))
                .append(Text.of(TextColors.RED, " points: ", TextColors.GRAY, topPoints.getName(), TextColors.GRAY, " (", topPoints.getPoints(), "),"))
                .append(Text.of(TextColors.RED, " kills: ", TextColors.GRAY, topKills.getName(), TextColors.GRAY, " (", topKills.getKills(), "),"))
                .append(Text.of(TextColors.RED, " deaths: ", TextColors.GRAY, topDeaths.getName(), TextColors.GRAY, " (", topDeaths.getDeaths(), ")"))
                .build());
    }

    @Override
    public void showStatsOfPlayer(Player player, Player target) {
        if (this.activePlayers.containsKey(target.getUniqueId())) {
            PlayerStats playerStats = getPlayerStats(target).get();

            player.sendMessage(ChatTypes.ACTION_BAR,
                    Text.builder()
                            .append(Text.of(TextColors.GRAY, "[", TextColors.RED, target.getName(), TextColors.GRAY, "]:"))
                            .append(Text.of(TextColors.GRAY, " Deaths: ", TextColors.YELLOW, playerStats.getDeaths()))
                            .append(Text.of(TextColors.GRAY, " Kills: ", TextColors.YELLOW, playerStats.getKills()))
                            .append(Text.of(TextColors.GRAY, " Points: ", TextColors.YELLOW, playerStats.getPoints()))
                            .append(Text.of(TextColors.GRAY, " HitsGiven: ", TextColors.YELLOW, playerStats.getHitsGiven()))
                            .append(Text.of(TextColors.GRAY, " HitsTaken: ", TextColors.YELLOW, playerStats.getHitsTaken()))
                            .build()
            );
        }
    }

    @Override
    public boolean isInsidePlayGround(Player player) {
        return this.playground.contains(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());
    }


    @Override
    public TextColor getFreeColor() {
        return Iterables.filter(this.spawns.keySet(), this::isColorNotInUse).iterator().next();
    }

    private boolean isColorNotInUse(TextColor color) {
        return !this.activePlayers.values().stream().anyMatch(playerStats -> playerStats.getColor() == color);

    }

    @Override
    public Transform<World> getRandomSpawn() {
        return this.spawns.values().toArray(new Transform[]{})[this.random.nextInt(this.spawns.size())];
    }

    @Override
    public GameKow.Mode getMode() {
        return this.mode;
    }

    @Override
    public void toggleStatus() {
        this.mode = this.mode.equals(GameKow.Mode.READY) ? GameKow.Mode.DISABLED : GameKow.Mode.READY;
    }

    @Override
    public void manageLoss(Player player, GameKow.LossType lossType) {
        PlayerStats playerStats = this.getPlayerStats(player).get();

        this.showStats(player);
        player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.builder().itemType(ItemTypes.STICK).quantity(1).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.KNOCKBACK, 1))).build());
        ((PlayerInventory) player.getInventory()).getEquipment().set(EquipmentTypes.HEADWEAR, ItemStack.builder().itemType(ItemTypes.LEATHER_HELMET).add(Keys.COLOR, playerStats.getColor().getColor()).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.AQUA_AFFINITY, 1), Enchantment.of(EnchantmentTypes.UNBREAKING, 10), Enchantment.of(EnchantmentTypes.RESPIRATION, 10))).add(Keys.UNBREAKABLE, true).build());

        if (this.getAllPlayers().size() > 1) {
            this.showStats(player);
            playerStats.addDeaths(1);
            playerStats.addPoints(-4);
            player.offer(Keys.FALL_DISTANCE, 0f);
            player.offer(Keys.FALL_TIME, 0);
            LastHit lastHit = playerStats.getLastHit();
            if (lastHit.getLastHit().isPresent()) {
                Optional<PlayerStats> playerStatsLastHitOptional = this.getPlayerStats(lastHit.getLastHit().get());
                if (playerStatsLastHitOptional.isPresent()) {
                    PlayerStats playerStatsLastHit = playerStatsLastHitOptional.get();
                    playerStatsLastHit.addKills(1);
                    playerStatsLastHit.addPoints(10);
                    Optional<Player> hitter = Sponge.getServer().getPlayer(lastHit.getLastHit().get());
                    if (hitter.isPresent()) {
                        hitter.get().sendMessage(ChatTypes.ACTION_BAR, Text.of(TextColors.YELLOW, "+1 kill ", TextColors.GRAY, "[killed ", player.getName(), "]"));
                        player.sendMessage(ChatTypes.ACTION_BAR, Text.of(TextColors.DARK_RED, "+1 death ", TextColors.GRAY, "[by ", hitter.get().getName(), "]"));
                    }
                    playerStats.setLastNull();
                }
            }
        }
    }
}
