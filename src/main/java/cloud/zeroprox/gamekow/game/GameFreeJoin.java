package cloud.zeroprox.gamekow.game;

import cloud.zeroprox.gamekow.GameKow;
import cloud.zeroprox.gamekow.stats.LastHit;
import cloud.zeroprox.gamekow.stats.PlayerStats;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.AABB;
import org.spongepowered.api.world.World;

import java.util.*;

public class GameFreeJoin extends GameClassic {

    private Map<UUID, PlayerStats> allPlayers = new HashMap<>();

    public GameFreeJoin(String name, Transform<World> lobby, AABB area, AABB playground, Map<TextColor, Transform<World>> spawns) {
        super(name, lobby, area, playground, spawns);
        this.limit = 25;
    }

    @Override
    public void manageLoss(Player player, GameKow.LossType lossType) {
        PlayerStats playerStats = this.getPlayerStats(player).get();
        this.showStats(player);
        player.setItemInHand(HandTypes.MAIN_HAND, ItemStack.builder().itemType(ItemTypes.STICK).quantity(1).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.KNOCKBACK, 1))).build());
        ((PlayerInventory) player.getInventory()).getEquipment().set(EquipmentTypes.HEADWEAR, ItemStack.builder().itemType(ItemTypes.LEATHER_HELMET).add(Keys.COLOR, playerStats.getColor().getColor()).add(Keys.ITEM_ENCHANTMENTS, Arrays.asList(Enchantment.of(EnchantmentTypes.AQUA_AFFINITY, 1), Enchantment.of(EnchantmentTypes.UNBREAKING, 10), Enchantment.of(EnchantmentTypes.RESPIRATION, 10))).add(Keys.UNBREAKABLE, true).build());

        if (this.getAllPlayers().size() <= 1) {
            offerToLeave(player);
            return;
        }

        this.showStats(player);
        playerStats.addDeaths(1);
        playerStats.addPoints(-4);
        player.offer(Keys.FALL_DISTANCE, 0f);
        player.offer(Keys.FALL_TIME, 0);
        LastHit lastHit = playerStats.getLastHit();

        if (!lastHit.getLastHit().isPresent()) {
            if (lossType.equals(GameKow.LossType.FALL_OUTSIDE))
                offerToLeave(player);
            return;
        }

        Optional<PlayerStats> playerStatsLastHitOptional = this.getPlayerStats(lastHit.getLastHit().get());
        if (!playerStatsLastHitOptional.isPresent()) {
            return;
        }

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

    private void offerToLeave(Player player) {
        player.sendMessage(Text.builder(Text.of(TextColors.RED), "Want to leave? ").append(Text.of(TextColors.DARK_RED, "yes")).onClick(TextActions.runCommand("/gamekow leave")).build());
    }

    @Override
    public void addPlayer(Player player) {
        if (this.allPlayers.containsKey(player.getUniqueId())) {
            this.activePlayers.put(player.getUniqueId(), this.allPlayers.get(player.getUniqueId()));
        } else {
            PlayerStats playerStats = new PlayerStats(player.getUniqueId(), TextColors.AQUA);
            this.activePlayers.put(player.getUniqueId(), playerStats);
            this.allPlayers.put(player.getUniqueId(), playerStats);
        }
        super.addPlayer(player);
    }

    @Override
    public void leavePlayer(Player player, boolean resetStats) {
        super.leavePlayer(player, resetStats);
        if (this.allPlayers.containsKey(player.getUniqueId())) {
            PlayerStats stats = this.allPlayers.get(player.getUniqueId());
            stats.setLeft(System.currentTimeMillis());
        }
    }
}
