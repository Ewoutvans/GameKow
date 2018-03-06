package cloud.zeroprox.gamekow;

import cloud.zeroprox.gamekow.game.IGame;
import cloud.zeroprox.gamekow.stats.PlayerStats;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.extent.EntityUniverse;

import java.util.*;

public class Listeners {

    @Listener
    public void onMoveEntityEvent(MoveEntityEvent event) {
        if (event.getTargetEntity() instanceof Player) {
            Player player = (Player) event.getTargetEntity();
            Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
            if (gameOptional.isPresent()) {
                IGame iGame = gameOptional.get();
                if (!iGame.isInsideArea(player.getLocation())) {
                    player.offer(Keys.FALL_DISTANCE, 0f);
                    player.offer(Keys.FALL_TIME, 0);
                    player.offer(Keys.FOOD_LEVEL, 20);
                    event.setToTransform(iGame.getRandomSpawn());
                    iGame.manageLoss(player, GameKow.LossType.FALL_OUTSIDE);
                }
            }
        }
    }

    @Listener
    public void onItemChangeEvent(ChangeInventoryEvent.Held event, @Root Player player) {
        Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            gameOptional.get().showStats(player);
            player.offer(Keys.FOOD_LEVEL, 20);
            event.setCancelled(true);

            // Show stats when scrolling of the player who you are looking at
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
        Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            player.offer(Keys.FOOD_LEVEL, 20);
            event.setCancelled(true);
        }
    }

    @Listener
    public void onItemDrop(DropItemEvent.Pre event) {
        if (event.getSource() instanceof Player) {
            Player player = (Player) event.getSource();
            Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
            if (gameOptional.isPresent()) {
                //event.setCancelled(true);
                player.offer(Keys.FOOD_LEVEL, 20);
            }
        }
    }

    @Listener
    public void onItemSwap(ChangeInventoryEvent.SwapHand event, @Root Player player) {
        Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            player.offer(Keys.FOOD_LEVEL, 20);
            event.setCancelled(true);
        }
    }

    @Listener
    public void onDisconnect(ClientConnectionEvent.Disconnect event, @Root Player player) {
        Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        gameOptional.ifPresent(game -> game.leavePlayer(player, true));
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
        Optional<IGame> optionalGame = GameKow.getGameManager().getPlayerGame(player);
        if (optionalGame.isPresent()) {
            IGame iGame = optionalGame.get();
            if (!iGame.isInsidePlayGround(player) || !iGame.isInsidePlayGround(attacker)) {
                event.setCancelled(true);
                attacker.sendMessage(Text.of(TextColors.RED, "Forbidden to hit if not in playground."));
                return;
            }
            player.offer(Keys.HEALTH, 20D);

            PlayerStats playerStats = iGame.getPlayerStats(player).get();
            playerStats.addHitFast(attacker);

            if (iGame.getPlayerStats(attacker).isPresent()) {
                PlayerStats playerStatsAttacker = iGame.getPlayerStats(attacker).get();
                playerStatsAttacker.addHitsGiven(1);
            }
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
        Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(player);
        if (gameOptional.isPresent()) {
            IGame iGame = gameOptional.get();
            if (iGame.isInsideArea(player.getLocation())) {
                player.offer(Keys.FOOD_LEVEL, 20);
                iGame.manageLoss(player, GameKow.LossType.FALL_DAMAGE);
            }
        }
    }

    @Listener
    public void onClick(ClickInventoryEvent event) {
        List<Player> optionalPlayer = event.getCause().allOf(Player.class);
        if (optionalPlayer.size() >= 1) {
            Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(optionalPlayer.get(0));
            gameOptional.ifPresent(game -> event.setCancelled(true));
        }

    }

    @Listener
    public void onDrop(ClickInventoryEvent.Drop event) {
        List<Player> optionalPlayer = event.getCause().allOf(Player.class);
        if (optionalPlayer.size() >= 1) {
            Optional<IGame> gameOptional = GameKow.getGameManager().getPlayerGame(optionalPlayer.get(0));
            gameOptional.ifPresent(game -> event.setCancelled(true));
        }
    }

}
