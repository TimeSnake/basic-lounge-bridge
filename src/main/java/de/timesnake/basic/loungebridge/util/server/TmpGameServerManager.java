/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.server;

import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.loungebridge.util.tool.GameTool;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.basic.loungebridge.util.user.OfflineUser;
import de.timesnake.library.basic.util.statistics.IntegerStat;
import de.timesnake.library.basic.util.statistics.StatType;
import de.timesnake.library.chat.Plugin;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public interface TmpGameServerManager {

  StatType<Integer> GAMES_PLAYED = new IntegerStat("games_played", "Games Played", 0, 10,
      1, false, 0, 1);

  GameUser loadUser(Player player);

  default Plugin getGamePlugin() {
    return Plugin.GAME;
  }

  /**
   * Called by channel map load message from lounge (map-voting).
   */
  default void onMapLoad() {

  }

  /**
   * Called by channel world load message from lounge
   */
  default void onWorldLoad() {

  }

  /**
   * Called by channel players message from lounge
   */
  default void onEstimatedGamePlayerNumber(int number) {

  }

  /**
   * Called on game start.
   * <p>
   * But before all {@link GameTool}s, especially the {@link GameUser#onGameStart()} method.
   */
  default void onBeforeGameStart() {

  }

  /**
   * Called on game start.
   * <p>
   * But after all {@link GameTool}s, especially the {@link GameUser#onGameStart()} method.
   */
  void onGameStart();

  void onGameStop();

  /**
   * Called after all users quit
   */
  void onGameReset();

  /**
   * Ingame user quits the server
   *
   * @param user The {@link User} who left
   */
  default void onGameUserQuit(GameUser user) {
    if (this.checkGameEnd()) {
      LoungeBridgeServer.stopGame();
    }
  }

  /**
   * Allows users to rejoin the game
   *
   * @return true if it is allowed
   */
  boolean isRejoiningAllowed();

  default boolean checkGameEnd() {
    return false;
  }

  /**
   * Allows users with status outgame to rejoin with status outgame
   *
   * @return true if it is allowed
   */
  default boolean isOutGameRejoiningAllowed() {
    return false;
  }

  /**
   * Game user rejoins game
   *
   * @param user The user who is rejoining
   */
  default void onGameUserRejoin(GameUser user) {

  }

  default Sideboard getGameSideboard() {
    return null;
  }

  default Sideboard getSpectatorSideboard() {
    return null;
  }

  default ExWorld getGameWorld() {
    return null;
  }

  ExLocation getSpectatorSpawn();

  default OfflineUser loadOfflineUser(GameUser user) {
    return new OfflineUser(user);
  }

  default Set<StatType<?>> getStats() {
    return new HashSet<>();
  }

  default void saveGameUserStats(GameUser user) {
    user.getStat(GAMES_PLAYED).increaseAllBy(1);
  }

}
