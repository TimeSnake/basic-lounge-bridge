/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.core;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.*;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.basic.game.util.user.TeamUser;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServerManager;
import de.timesnake.basic.loungebridge.util.tool.listener.*;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.basic.loungebridge.util.user.OfflineUser;
import de.timesnake.library.basic.util.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class UserManager implements Listener {

  private static final int REJOIN_TIME_SEC = 180;

  private final Logger logger = LogManager.getLogger("lounge-bridge.user.manager");

  private final HashMap<UUID, OfflineUser> offlineUsersByUniqueId = new HashMap<>();
  private final HashMap<UUID, BukkitTask> offlineUserRemoveTaskByUniqueId = new HashMap<>();

  public UserManager() {
    Server.registerListener(this, BasicLoungeBridge.getPlugin());
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onUserJoin(UserJoinEvent e) {
    GameUser user = (GameUser) e.getUser();
    String task = user.getTask();

    if (this.offlineUsersByUniqueId.containsKey(user.getUniqueId())) {
      OfflineUser offlineUser = this.offlineUsersByUniqueId.remove(user.getUniqueId());
      this.offlineUserRemoveTaskByUniqueId.remove(user.getUniqueId()).cancel();
      offlineUser.loadInto(user);
      LoungeBridgeServer.onGameUserRejoin(user);
      LoungeBridgeServer.getToolManager().applyOnTools(GameUserJoinListener.class, t -> t.onGameUserJoin(user));
    } else if (task != null && task.equalsIgnoreCase(LoungeBridgeServer.getGame().getName())
               && user.hasStatus(Status.User.PRE_GAME)) {

      user.joinGame();
      LoungeBridgeServer.getToolManager().applyOnTools(GameUserJoinListener.class, t -> t.onGameUserJoin(user));
      LoungeBridgeServer.checkGameStart();
      LoungeBridgeServer.updateSpectatorTools();
    } else {
      user.setStatus(Status.User.SPECTATOR);
      user.joinSpectator();
    }
  }

  public void clearRejoinUsers() {
    this.offlineUsersByUniqueId.clear();
  }

  @EventHandler
  public void onUserQuit(UserQuitEvent e) {
    User user = e.getUser();

    if (LoungeBridgeServer.isRejoiningAllowed()
        && (user.hasStatus(Status.User.IN_GAME)
        || (LoungeBridgeServer.isOutGameRejoiningAllowed() && user.hasStatus(Status.User.OUT_GAME)))
        && LoungeBridgeServer.isGameRunning()) {
      OfflineUser offlineUser = LoungeBridgeServer.loadOfflineUser(((GameUser) user));
      this.offlineUsersByUniqueId.put(user.getUniqueId(), offlineUser);
      this.logger.info("Saved user '{}'", user.getName());
      this.offlineUserRemoveTaskByUniqueId.put(user.getUniqueId(),
          Server.runTaskLaterSynchrony(
              () -> this.offlineUsersByUniqueId.remove(user.getUniqueId()),
              20 * REJOIN_TIME_SEC, BasicLoungeBridge.getPlugin()));
    }

    if (user.hasStatus(Status.User.IN_GAME, Status.User.PRE_GAME)) {
      if (LoungeBridgeServer.isGameRunning()) {
        LoungeBridgeServerManager.getInstance().onGameUserQuit((GameUser) user);
      }

      LoungeBridgeServer.updateSpectatorTools();

      LoungeBridgeServer.getToolManager().applyOnTools(GameUserQuitListener.class,
          t -> t.onGameUserQuit(((GameUser) user)));
    } else {
      LoungeBridgeServer.getToolManager().applyOnTools(SpectatorUserQuitListener.class,
          t -> t.onSpectatorUserQuit(((SpectatorUser) user)));
    }

    ((TeamUser) user).setTeam(null);


  }

  @EventHandler
  public void onPlayerDeath(UserDeathEvent e) {
    User user = e.getUser();

    if (user.isInGame()) {
      ((GameUser) user).addDeath();
    }

    LoungeBridgeServer.getToolManager().applyOnTools(GameUserDeathListener.class, t -> t.onGameUserDeath(e,
        ((GameUser) user)));
    List<ItemStack> drops = ((GameUser) user).onGameDeath();

    if (drops != null) {
      e.setDrops(drops);
    }

    e.setAutoRespawn(true);
  }


  @EventHandler
  public void onPlayerRespawn(UserRespawnEvent e) {
    GameUser user = (GameUser) e.getUser();

    AtomicReference<Location> respawnLoc = new AtomicReference<>();

    LoungeBridgeServer.getToolManager().applyOnTools(GameUserRespawnListener.class,
        t -> {
          Location loc = t.onGameUserRespawn(user);
          if (loc != null) {
            respawnLoc.set(loc);
          }
        });

    ExLocation loc = user.getRespawnLocation();
    if (loc != null) {
      respawnLoc.set(loc);
    }

    if (respawnLoc.get() != null) {
      e.setRespawnLocation(respawnLoc.get());
    }

    user.respawn();
  }


  @EventHandler
  public void onBowShoot(EntityShootBowEvent e) {
    if (e.getEntity() instanceof Player) {
      User user = Server.getUser(((Player) e.getEntity()));

      if (user == null) {
        return;
      }

      if (user.isInGame()) {
        ((GameUser) user).addBowShot();
      }
    }
  }

  @EventHandler
  public void onHit(ProjectileHitEvent e) {
    if (e.getEntity().getShooter() instanceof Player) {
      if (e.getHitEntity() instanceof Player) {
        User shooter = Server.getUser(((Player) e.getEntity().getShooter()));
        User hitUser = Server.getUser(((Player) e.getHitEntity()));

        if (shooter == null || hitUser == null) {
          return;
        }

        if (shooter.isInGame()) {
          ((GameUser) shooter).addBowHitTarget();
        }
        if (hitUser.isInGame()) {
          ((GameUser) hitUser).addBowHit();
        }
      }
    }
  }

  @EventHandler
  public void onUserDropItem(UserDropItemEvent e) {
    User user = e.getUser();

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onUserDamage(UserDamageEvent e) {
    User user = e.getUser();

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      e.setCancelDamage(true);
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityDamage(VehicleDamageEvent e) {
    if (e.getAttacker() instanceof Player player) {
      User user = Server.getUser(player);
      if (user == null) {
        return;
      }

      if (user.isService()) {
        return;
      }

      if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
        e.setCancelled(true);
        user.getPlayer().setFireTicks(0);
      }
    }
  }

  @EventHandler
  public void onEntity(EntityDamageByUserEvent e) {
    User user = e.getUser();

    if (user.isService()) {
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
      user.getPlayer().setFireTicks(0);
    }
  }

  @EventHandler
  public void onUserDamageByUser(UserDamageByUserEvent e) {


    GameUser clickedUser = (GameUser) e.getUser();
    GameUser user = (GameUser) e.getUserDamager();

    if (!LoungeBridgeServer.allowTeamMateDamage()) {
      if (clickedUser.isTeamMate(user)) {
        user.sendPluginTDMessage(LoungeBridgeServer.getGamePlugin(), "§sYou can't damage your teammate");
        e.setCancelled(true);
        return;
      }
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      if (!user.isService()) {
        e.setCancelled(true);
        e.setCancelDamage(true);
      }

      if (clickedUser.isInGame()) {
        user.setGameMode(GameMode.SPECTATOR);
        user.setSpectatorTarget(clickedUser.getPlayer());
      }
    }
  }

  @EventHandler
  public void onEntityInteract(PlayerInteractEntityEvent e) {
    SpectatorUser user = (SpectatorUser) Server.getUser(e.getPlayer());

    if (!(e.getRightClicked() instanceof Player)) {
      return;
    }

    GameUser clickedUser = (GameUser) Server.getUser(((Player) e.getRightClicked()));

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      if (!user.isService()) {
        e.setCancelled(true);
      }
      if (clickedUser.isInGame()) {
        user.openInventory(clickedUser.getInventory());
      }
    }
  }

  @EventHandler
  public void onPlayerStopSpectatingEntity(PlayerStopSpectatingEntityEvent e) {
    SpectatorUser user = (SpectatorUser) Server.getUser(e.getPlayer());

    user.setGameMode(GameMode.ADVENTURE);
    if (user.hasFlyEnabled()) {
      user.setAllowFlight(true);
      user.setFlying(true);
    }
  }

  @EventHandler
  public void onPlayerPickUpItem(PlayerAttemptPickupItemEvent e) {
    User user = Server.getUser(e.getPlayer());

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerPickUpArrow(PlayerPickupArrowEvent e) {
    User user = Server.getUser(e.getPlayer());

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    User user = Server.getUser(e.getPlayer());

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {
    User user = Server.getUser(e.getWhoClicked().getUniqueId());

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      user.closeInventory();
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onSwapHandItem(PlayerSwapHandItemsEvent e) {
    User user = Server.getUser(e.getPlayer());

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onFoodLevelChange(FoodLevelChangeEvent e) {
    if (!(e.getEntity() instanceof Player)) {
      return;
    }

    User user = Server.getUser(((Player) e.getEntity()));

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning() || !user.isInGame()) {
      e.setCancelled(true);
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryOpen(InventoryOpenEvent e) {
    if (!(e.getPlayer() instanceof Player)) {
      return;
    }

    User user = Server.getUser(((Player) e.getPlayer()));

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setCancelled(true);
      user.closeInventory();
      return;
    }

    if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof ShulkerBox) {
      if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
        e.setCancelled(true);
        Inventory inv = e.getInventory();
        user.openInventory(inv);
      }
    }
  }

  @EventHandler
  public void onRide(VehicleEnterEvent e) {
    if (!(e.getEntered() instanceof Player)) {
      return;
    }

    User user = Server.getUser(((Player) e.getEntered()));

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    User user = Server.getUser(e.getPlayer());

    if (user == null) {
      return;
    }

    if (user.isService()) {
      return;
    }

    if (!LoungeBridgeServer.isGameRunning()) {
      e.setUseInteractedBlock(Event.Result.DENY);
      e.setCancelled(true);
    }

    if (!user.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
      return;
    }

    e.setUseInteractedBlock(Event.Result.DENY);
    e.setCancelled(true);
  }
}
