package cloud.zeroprox.gamekow.stats;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.format.TextColor;

import java.util.Optional;
import java.util.UUID;

public class PlayerStats {

    private int kills, deaths, hitsGiven, hitsTaken, points;
    private TextColor color;
    private long started;
    private long left;
    private LastHit lastHit;
    private UUID owner;

    public PlayerStats(UUID owner, TextColor color) {
        this.owner = owner;
        this.kills = 0;
        this.deaths = 0;
        this.hitsGiven = 0;
        this.hitsTaken = 0;
        this.points = 0;
        this.color = color;
        this.started = System.currentTimeMillis();
        this.lastHit = new LastHit();
    }

    public int getKills() {
        return kills;
    }

    public void addKills(int kills) {
        this.kills += kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeaths(int deaths) {
        this.deaths += deaths;
    }

    public int getHitsGiven() {
        return hitsGiven;
    }

    public void addHitsGiven(int hitsGiven) {
        this.hitsGiven += hitsGiven;
    }

    public int getHitsTaken() {
        return hitsTaken;
    }

    public void addHitsTaken(int hitsTaken) {
        this.hitsTaken += hitsTaken;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int points) {
        this.points += points;
        if (this.points < 0) this.points = 0;
    }

    public TextColor getColor() {
        return color;
    }

    public LastHit getLastHit() {
        return lastHit;
    }

    public void addHitFast(Player attacker) {
        if (lastHit.setLastHit(attacker.getUniqueId())) {
            addHitsTaken(1);
        }
    }

    public void setLastNull() {
        this.lastHit.setLastHit(null);
    }

    public String getName() {
        return Sponge.getServer().getPlayer(this.owner).map(Player::getName).orElse("unknown");
    }

    public long getLeft() {
        return left;
    }

    public void setLeft(long left) {
        this.left = left;
    }

    public void reset() {
        this.deaths = 0;
        this.hitsGiven = 0;
        this.hitsTaken = 0;
        this.kills = 0;
        this.points = 0;
        this.lastHit.setLastHit(null);
    }
}
