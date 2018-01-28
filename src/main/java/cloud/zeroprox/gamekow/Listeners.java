package cloud.zeroprox.gamekow;

import cloud.zeroprox.gamekow.game.Game;
import cloud.zeroprox.gamekow.stats.LastHit;
import cloud.zeroprox.gamekow.stats.PlayerStats;
import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.DurabilityData;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.data.ChangeDataHolderEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.EntityUniverse;

import javax.swing.text.html.Option;
import java.util.*;

public class Listeners {

    @Listener
    public void onMoveEntityEvent(MoveEntityEvent event) {
        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();
            Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(player);
            if (gameOptional.isPresent()) {
                Game game = gameOptional.get();
                if (!game.isInside(player.getLocation())) {
                    event.setToTransform(game.getSpawn(player));
                    player.offer(Keys.FOOD_LEVEL, 20);
                    manageLoss(player, game);
                }
            }
        }
    }

    @Listener
    public void onItemChangeEvent(ChangeInventoryEvent.Held event, @Root Player player) {
        Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            gameOptional.get().showStats(ChatTypes.ACTION_BAR, player);
            player.offer(Keys.FOOD_LEVEL, 20);
            event.setCancelled(true);

            Set<EntityUniverse.EntityHit> entityHitSet = player.getWorld().getIntersectingEntities(player, 30, entityHit -> entityHit.getEntity().getType().equals(EntityTypes.PLAYER) && entityHit.getEntity() != player);
            Optional<EntityUniverse.EntityHit> entityOptional = entityHitSet.stream().findFirst();
            if (entityOptional.isPresent()) {
                if (entityOptional.get().getEntity() instanceof Player) {
                    gameOptional.get().showStatsOfPlayer(player, (Player) entityOptional.get().getEntity());
                }
            }
        }
    }

    @Listener
    public void onItemChangeEvent(ClickInventoryEvent event, @Root Player player) {
        Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            player.offer(Keys.FOOD_LEVEL, 20);
            event.setCancelled(true);
        }
    }

    @Listener
    public void onItemDrop(DropItemEvent.Pre event) {
        if (event.getSource() instanceof Player) {
            Player player = (Player) event.getSource();
            Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(player);
            if (gameOptional.isPresent()) {
                //event.setCancelled(true);
                player.offer(Keys.FOOD_LEVEL, 20);
            }
        }
    }

    @Listener
    public void onDisconnect(ClientConnectionEvent.Disconnect event, @Root Player player) {
        Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        gameOptional.ifPresent(game -> game.leavePlayer(player));
    }

    @Listener
    public void onDamagePlayerHit(DamageEntityEvent event) {
        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }
        if (!(event.getSource() instanceof EntityDamageSource)) {
            return;
        }
        EntityDamageSource source = (EntityDamageSource) event.getSource();
        if (!(source.getSource() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getTargetEntity();
        Player attacker = (Player) source.getSource();
        Optional<Game> optionalGame = GameKow.getGameManager().getPlayerGame(player);
        if (optionalGame.isPresent()) {
            Game game = optionalGame.get();
            boolean b = game.isInsidePlayGround(player);
            boolean b2= game.isInsidePlayGround(attacker);
            if (!game.isInsidePlayGround(player) || !game.isInsidePlayGround(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage(Text.of(TextColors.RED, "Forbidden to hit if not in playground."));
                return;
            }
            player.offer(Keys.HEALTH, 20D);

            PlayerStats playerStats = game.getPlayerStats(player);
            playerStats.addHitFast(attacker);

            PlayerStats playerStatsAttacker = game.getPlayerStats(attacker);
            playerStatsAttacker.addHitsGiven(1);
        }
    }

    @Listener
    public void onDamageFall(DamageEntityEvent event) {
        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }
        if (!(event.getSource() instanceof DamageSource)) {
            return;
        }
        DamageSource damageSource = (DamageSource) event.getSource();
        if (damageSource.getType() != DamageTypes.FALL) {
            return;
        }
        Player player = (Player) event.getTargetEntity();
        Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();
            if (game.isInside(player.getLocation())) {
                player.offer(Keys.FOOD_LEVEL, 20);
                manageLoss(player, game);
            }
        }
    }

    @Listener
    public void onClick(ClickInventoryEvent event) {
        List<Player> optionalPlayer = event.getCause().allOf(Player.class);
        if (optionalPlayer.size() >= 1) {
            Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(optionalPlayer.get(0));
            gameOptional.ifPresent(game -> event.setCancelled(true));
        }

    }

    @Listener
    public void onDrop(ClickInventoryEvent.Drop event) {
        List<Player> optionalPlayer = event.getCause().allOf(Player.class);
        if (optionalPlayer.size() >= 1) {
            Optional<Game> gameOptional = GameKow.getGameManager().getPlayerGame(optionalPlayer.get(0));
            gameOptional.ifPresent(game -> event.setCancelled(true));
        }
    }

    private void manageLoss(Player player, Game game) {
        PlayerStats playerStats = game.getPlayerStats(player);

        game.showStats(ChatTypes.SYSTEM, player);
        player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.builder().itemType(ItemTypes.STICK).quantity(1).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.KNOCKBACK, 1))).build());
        ((PlayerInventory) player.getInventory()).getEquipment().set(EquipmentTypes.HEADWEAR, ItemStack.builder().itemType(ItemTypes.LEATHER_HELMET).add(Keys.COLOR, playerStats.getColor().getColor()).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.AQUA_AFFINITY, 1), Enchantment.of(EnchantmentTypes.UNBREAKING, 10), Enchantment.of(EnchantmentTypes.RESPIRATION, 10))).add(Keys.UNBREAKABLE, true).build());

        if (game.getAllPlayers().size() > 1) {
            game.showStats(ChatTypes.CHAT, player);
            playerStats.addDeaths(1);
            playerStats.addPoints(-4);
            player.offer(Keys.FALL_DISTANCE, 0f);
            player.offer(Keys.FALL_TIME, 0);
            game.scoreboardUpdate(player);
            LastHit lastHit = playerStats.getLastHit();
            if (lastHit.getLastHit().isPresent()) {
                PlayerStats playerStatsLastHit = game.getPlayerStats(lastHit.getLastHit().get());
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
