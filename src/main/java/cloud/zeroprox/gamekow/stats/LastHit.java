package cloud.zeroprox.gamekow.stats;

import java.util.Optional;
import java.util.UUID;

public class LastHit {

    private UUID lastHit;
    private long lastHitTime, fastHitTime;

    public LastHit() {
        this.lastHitTime = System.currentTimeMillis();
        this.fastHitTime = System.currentTimeMillis();
    }

    public boolean setLastHit(UUID hit) {
        this.lastHit = hit;
        if ((System.currentTimeMillis() - fastHitTime) < 60) {
            return false;
        } else {
            this.lastHitTime = System.currentTimeMillis();
            this.fastHitTime = System.currentTimeMillis();
            return true;
        }
    }

    public Optional<UUID> getLastHit() {
        if (this.lastHitTime == -1) {
            return Optional.empty();
        }
        if ((System.currentTimeMillis() - lastHitTime) > 8000) {
            this.lastHit = null;
            this.lastHitTime = -1;
            return Optional.empty();
        }
        if (lastHit == null) return Optional.empty();
        return Optional.of(lastHit);
    }
}
