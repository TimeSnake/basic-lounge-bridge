package de.timesnake.basic.loungebridge.util.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.*;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.library.basic.util.Status;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class GameUserManager implements Listener {

    private final List<GameUserEventListener> listeners = new ArrayList<>();

    public GameUserManager() {
        Server.registerListener(this, BasicLoungeBridge.getPlugin());
    }

    public void addListener(GameUserEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameUserEventListener listener) {
        listeners.remove(listener);
    }

    private boolean isGameUser(User user) {
        return !user.isService() && !user.hasStatus(Status.User.OUT_GAME, Status.User.SPECTATOR);
    }

    private boolean isGameUser(Player player) {
        User user = Server.getUser(player);

        if (user == null) {
            return false;
        }

        return this.isGameUser(user);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        if (e.getHitBlock() != null) {
            this.listeners.forEach(l -> l.onProjectileHitBlock(e.getHitBlock(), e.getEntity(), e));
        }

        if (e.getHitEntity() == null || !(e.getHitEntity() instanceof Player hitPlayer)) {
            return;
        }

        User user = Server.getUser(hitPlayer);

        if (user != null && !user.hasStatus(Status.User.OUT_GAME, Status.User.SPECTATOR)) {
            this.listeners.forEach(l -> l.onProjectileHitUser((GameUser) user, e.getEntity(), e));
        }
    }

    @EventHandler
    public void onUserDamageUser(UserDamageByUserEvent e) {
        if (!this.isGameUser(e.getUser()) || !this.isGameUser(e.getUserDamager())) {
            return;
        }

        this.listeners.forEach(l -> l.onUserDamageByUser((GameUser) e.getUser(), (GameUser) e.getUserDamager(), e));
    }

    @EventHandler
    public void onBlockBreak(UserBlockBreakEvent e) {
        if (!this.isGameUser(e.getUser())) {
            return;
        }

        this.listeners.forEach(l -> l.onUserBlockBreak((GameUser) e.getUser(), e.getBlock(), e));
    }

    @EventHandler
    public void onPlayerPickUpArrow(PlayerPickupArrowEvent e) {
        if (!this.isGameUser(e.getPlayer())) {
            return;
        }

        this.listeners.forEach(l -> l.onUserPickupArrow((GameUser) Server.getUser(e.getPlayer()), e));
    }

    @EventHandler
    public void onUserBlockPlace(UserBlockPlaceEvent e) {
        if (this.isGameUser(e.getUser())) {
            return;
        }

        this.listeners.forEach(l -> l.onUserBlockPlace((GameUser) e.getUser(), e.getBlockPlaced(), e));
    }
}
