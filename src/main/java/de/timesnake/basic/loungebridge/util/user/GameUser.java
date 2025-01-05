/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.user;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.chat.Chat;
import de.timesnake.basic.bukkit.util.user.UserDamage;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroup;
import de.timesnake.basic.bukkit.util.user.scoreboard.TablistGroupType;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.game.util.game.Team;
import de.timesnake.basic.game.util.server.GameServer;
import de.timesnake.basic.game.util.user.StatUser;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.tool.listener.GameUserQuitListener;
import de.timesnake.basic.loungebridge.util.tool.listener.SpectatorUserJoinListener;
import de.timesnake.library.basic.util.Status;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

public abstract class GameUser extends StatUser {

  protected Kit kit;

  protected int kills = 0;
  protected int killStreak = 0;
  protected int highestKillStreak = 0;
  protected int deaths = 0;
  protected int respawns = 0;

  protected int bowShots = 0;
  protected int bowHitTarget = 0;
  protected int bowHits = 0;

  protected int longestShot = 0;

  protected boolean kitLoaded = false;

  private boolean playedGame = false;

  private float gameCoins = 0;

  private BukkitTask respawnTask;

  public GameUser(Player player) {
    super(player);
    if (LoungeBridgeServer.areKitsEnabled()) {
      Integer kitId = this.getDatabase().getKit();

      if (kitId != null) {
        this.kit = LoungeBridgeServer.getGame().getKitManager().getKit(kitId).orElse(null);
      }
    }
  }

  public void joinGame() {
    this.getInventory().clear();
    this.heal();
    this.setInvulnerable(false);
    this.setAllowFlight(false);
    this.setFlying(false);
    this.setGravity(true);
    this.setFlySpeed((float) 0.2);
    this.setWalkSpeed((float) 0.2);
    this.setGameMode(GameMode.ADVENTURE);
    this.setFireTicks(0);
    this.removePotionEffects();
    this.setStatus(Status.User.PRE_GAME);

    if (this.getTeam() != null && this.getTeam().hasPrivateChat() && LoungeBridgeServer.getServerTeamAmount() > 0) {
      Chat teamChat = Server.getChat(this.getTeam().getName());
      if (teamChat != null) {
        teamChat.addWriter(this);
        teamChat.addListener(this);
        Server.getGlobalChat().removeWriter(this);
      }
    }

    this.setSideboard(GameServer.getGameSideboard());

    if (LoungeBridgeServer.getGame().hasTexturePack()) {
      this.setResourcePack(LoungeBridgeServer.getGame().getTexturePackLink(),
          LoungeBridgeServer.getGame().getTexturePackHash(), true);
    }

    this.onGameJoin();
    this.applyKit();
  }

  public final void stopGame() {
    if (this.respawnTask != null) {
      this.respawnTask.cancel();
    }
    this.getEnderChest().clear();
    this.onGameStop();
  }

  @Override
  public TablistGroup getTablistGroup(TablistGroupType type) {
    if (de.timesnake.basic.game.util.game.TablistGroupType.GAME_TEAM.equals(type)) {
      if (this.hasStatus(Status.User.SPECTATOR, Status.User.OUT_GAME)) {
        return null;
      }
      if (LoungeBridgeServer.getServerTeamAmount() == 0 || LoungeBridgeServer.getGame().hideTeams()) {
        return LoungeBridgeServer.getTablistGameTeam();
      }
    }
    return super.getTablistGroup(type);
  }

  @Override
  public void joinSpectator() {
    super.joinSpectator();

    LoungeBridgeServer.getToolManager().applyOnTools(GameUserQuitListener.class, t -> t.onGameUserQuit(this));
    LoungeBridgeServer.getToolManager().applyOnTools(SpectatorUserJoinListener.class, t -> t.onSpectatorUserJoin(this));
  }

  public void playedGame() {
    this.playedGame = true;
  }

  public boolean hasPlayedGame() {
    return playedGame;
  }

  public Kit getKit() {
    return kit;
  }

  public void setKit(Kit kit) {
    this.kit = kit;
  }

  /**
   * Applies kit to user.
   * <p>
   * This method can only be invoked once. To reuse it, set {@link #kitLoaded} to false.
   *
   * @return true if kit was applied, else false.
   */
  public boolean applyKit() {
    if (this.kitLoaded) {
      return false;
    }

    if (this.kit == null) {
      return false;
    }

    this.kitLoaded = true;

    this.kit.getApplier().forEach(a -> a.accept(this));
    return true;
  }

  public void changeKitTo(Kit kit) {
    this.clearInventory();
    this.removePotionEffects();

    this.kit = kit;
    kit.getApplier().forEach(a -> a.accept(this));
  }

  public int getKills() {
    return kills;
  }

  public void setKills(Integer kills) {
    this.kills = kills;
  }

  public int getKillStreak() {
    return this.killStreak;
  }

  public int getHighestKillStreak() {
    return this.highestKillStreak;
  }

  public void addKill() {
    this.kills++;
    this.killStreak++;
    if ((this.killStreak % 5 == 0 && this.killStreak != 0) || this.killStreak == 3) {
      this.broadcastKillStreak();
    }
  }

  public void broadcastKillStreak() {
    LoungeBridgeServer.broadcastGameTDMessage(this.getTDChatName() + "§p has a kill-streak of §v" + this.killStreak);
  }

  public int getDeaths() {
    return deaths;
  }

  public void setDeaths(int deaths) {
    this.deaths = deaths;
    if (this.killStreak > this.highestKillStreak) {
      this.highestKillStreak = this.killStreak;
    }
    this.killStreak = 0;
  }

  public void addDeath() {
    this.deaths++;

    if (this.killStreak > this.highestKillStreak) {
      this.highestKillStreak = this.killStreak;
    }

    this.killStreak = 0;

    if (this.getLastDamager() != null && this.getLastDamager().getDamageType()
        .equals(UserDamage.DamageType.INSTANT)) {

      GameUser damager = ((GameUser) this.getLastDamager().getDamager());
      damager.addKill();

      if (this.getLastDamager().getDamageType().equals(UserDamage.DamageType.PLAYER_BOW)) {
        int distance =
            (int) this.getLastDamager().getUserLocation().distance(this.getLastDamager().getDamagerLocation());
        damager.checkLongestShot(distance);
      }

    } else {
      Player killer = this.getPlayer().getKiller();
      if (killer != null) {
        GameUser killerUser = (GameUser) Server.getUser(killer);
        killerUser.addKill();
        Team killerTeam = killerUser.getTeam();
        if (killerTeam != null) {
          killerTeam.addKill();
        }

        if (this.getLastDamager() != null) {
          GameUser damager = ((GameUser) this.getLastDamager().getDamager());

          if (!damager.equals(killerUser)) {
            return;
          }

          if (this.getLastDamager().getDamageType()
              .equals(UserDamage.DamageType.PLAYER_BOW)) {
            int distance =
                (int) this.getLastDamager().getUserLocation()
                    .distance(this.getLastDamager().getDamagerLocation());
            damager.checkLongestShot(distance);
          }
        }
      }
    }

    Team team = this.getTeam();
    if (team != null) {
      team.addDeath();
    }
  }

  public double getKillDeathRatio() {
    return this.kills / ((double) this.deaths);
  }

  public int getBowShots() {
    return bowShots;
  }

  public void setBowShots(int bowShots) {
    this.bowShots = bowShots;
  }

  public void addBowShot() {
    this.bowShots++;
  }

  public int getBowHitTarget() {
    return bowHitTarget;
  }

  public void setBowHitTarget(int bowHitTarget) {
    this.bowHitTarget = bowHitTarget;
  }

  public void addBowHitTarget() {
    this.bowHitTarget++;
  }

  public int getBowHits() {
    return this.bowHits;
  }

  public void setBowHits(int bowHits) {
    this.bowHits = bowHits;
  }

  public void addBowHit() {
    this.bowHits++;
  }

  public boolean checkLongestShot(int distance) {
    if (this.longestShot < distance) {
      this.longestShot = distance;
      return true;
    }
    return false;
  }

  public int getLongestShot() {
    return longestShot;
  }

  /**
   * Gets health with heart symbol
   *
   * @return
   */
  public String getHealthDisplay() {
    double health = this.getHealth() / 2;
    if (health - ((int) health) >= 0.25 && health - ((int) health) < 0.75) {
      health = ((int) health) + 0.5;
    } else {
      health = Math.round(health);
    }

    if (health == 0) {
      health = 0.5;
    }

    return health + "❤";
  }

  @Override
  public void addCoins(float coins, boolean sendMessage) {
    super.addCoins(coins, sendMessage);
    this.gameCoins += coins;
  }

  /**
   * Gets amount of gained coins during game
   *
   * @return the amount of coins
   */
  public float getGameCoins() {
    return gameCoins;
  }

  public void addRespawn() {
    this.respawns++;
  }

  public int getRespawns() {
    return respawns;
  }

  /**
   * Invoked after user respawns.
   */
  public void respawn() {
    this.onGameRespawn();
  }

  /**
   * Respawn user after given time.
   * It is recommended to call this method by overriding {@link #respawn()}.
   *
   * @param seconds to respawn
   */
  public final void respawnDelayed(int seconds) {
    this.lockLocation();
    this.lockInventory();
    this.lockBlockBreakPlace();

    this.respawnTask = Server.runTaskTimerSynchrony(time -> {
      this.showTDTitle("§w" + time, "", Duration.ofSeconds(1));

      if (time == 0) {
        this.unlockLocation();
        this.unlockInventory();
        this.unlockBlockBreakPlace();
        this.onGameRespawn();
      }
    }, seconds, true, 20, 0, BasicLoungeBridge.getPlugin());
  }

  /**
   * Invoked while user joins the game
   */
  public void onGameJoin() {

  }

  /**
   * Invoked at game start.
   */
  public void onGameStart() {

  }

  /**
   * Invoked at game stop.
   */
  public void onGameStop() {

  }

  /**
   * Invoked on user death.
   * <p>
   * Do not set items and other player stuff here. Use {@link #onGameRespawn()} instead.
   *
   * @return the list of items to drop and null to keep unchanged.
   */
  public List<ItemStack> onGameDeath() {
    return null;
  }

  /**
   * Invoked after user respawns.
   */
  public void onGameRespawn() {

  }

  /**
   * Determines respawn location of user
   *
   * @return the respawn location
   */
  @Nullable
  public ExLocation getRespawnLocation() {
    return null;
  }
}
