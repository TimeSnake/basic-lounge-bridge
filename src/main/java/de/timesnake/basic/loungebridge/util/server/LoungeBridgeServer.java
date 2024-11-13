/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.server;

import de.timesnake.basic.bukkit.core.user.scoreboard.tablist.Tablist2;
import de.timesnake.basic.bukkit.util.chat.Chat;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.ExWorld;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.game.util.game.Team;
import de.timesnake.basic.game.util.server.GameServer;
import de.timesnake.basic.game.util.user.SpectatorManager;
import de.timesnake.basic.loungebridge.core.DiscordManager;
import de.timesnake.basic.loungebridge.util.game.TmpGame;
import de.timesnake.basic.loungebridge.util.tool.ToolManager;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.basic.loungebridge.util.user.GameUserManager;
import de.timesnake.basic.loungebridge.util.user.OfflineUser;
import de.timesnake.basic.loungebridge.util.user.TablistTeam;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.library.basic.util.TimeCoins;
import de.timesnake.library.basic.util.statistics.StatType;
import de.timesnake.library.chat.Plugin;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class LoungeBridgeServer extends GameServer {

  public static final float WIN_COINS = 10 * TimeCoins.MULTIPLIER;

  public static TmpGame getGame() {
    return server.getGame();
  }

  public static void closeGame() {
    server.closeGame();
  }

  public static void closeGame6() {
    server.closeGame6();
  }

  public static void closeGame10() {
    server.closeGame10();
  }

  public static boolean checkGameEnd() {
    return server.checkGameEnd();
  }

  public static DbLoungeServer getTwinServer() {
    return server.getTwinServer();
  }

  public static int getGameCountdown() {
    return server.getGameCountdown();
  }

  public static void broadcastLoungeBridgeTDMessage(String msg) {
    server.broadcastLoungeBridgeTDMessage(msg);
  }

  public static void broadcastLoungeBridgeMessage(Component msg) {
    server.broadcastLoungeBridgeMessage(msg);
  }

  public static State getState() {
    return server.getState();
  }

  public static void setState(State state) {
    server.setState(state);
  }

  public static Tablist2 getGameTablist() {
    return server.getGameTablist();
  }

  public static SpectatorManager getSpectatorManager() {
    return server.getSpectatorManager();
  }

  public static Chat getSpectatorChat() {
    return server.getSpectatorChat();
  }

  public static void updateSpectatorTools() {
    server.updateSpectatorTools();
  }

  public static void loadMap() {
    server.loadMap();
  }

  public static void loadWorld() {
    server.loadWorld();
  }

  public static void onGamePlayerNumber(int number) {
    server.onEstimatedGamePlayerNumber(number);
  }

  public static ExWorld getGameWorld() {
    return server.getGameWorld();
  }

  public static ExLocation getSpectatorSpawn() {
    return server.getSpectatorSpawn();
  }

  public static boolean isGameRunning() {
    return server.isGameRunning();
  }

  public static Sideboard getSpectatorSideboard() {
    return server.getSpectatorSideboard();
  }

  public static void broadcastGameMessage(Component message) {
    server.broadcastGameMessage(message);
  }

  public static void broadcastGameTDMessage(String message) {
    server.broadcastGameTDMessage(message);
  }

  public static Plugin getGamePlugin() {
    return server.getGamePlugin();
  }

  public static boolean allowTeamMateDamage() {
    return server.allowTeamMateDamage();
  }

  public static void setTeamMateDamage(boolean teamMateDamage) {
    server.setTeamMateDamage(teamMateDamage);
  }

  public static TablistTeam getTablistGameTeam() {
    return server.getTablistGameTeam();
  }

  public static Integer getServerTeamAmount() {
    return server.getServerTeamAmount();
  }

  public static boolean areKitsEnabled() {
    return server.areKitsEnabled();
  }

  public static boolean areMapsEnabled() {
    return server.areMapsEnabled();
  }

  public static <M extends Map> M getMap() {
    return (M) server.getMap();
  }

  public static <U extends GameUser> Set<U> getMostKills(Collection<U> users, int number) {
    return server.getMostKills(users, number);
  }

  public static <U extends GameUser> Set<U> getHighestKillstreak(Collection<U> users, int number) {
    return server.getHighestKillStreak(users, number);
  }

  public static <U extends GameUser> Set<U> getMostDeaths(Collection<U> users, int number) {
    return server.getMostDeaths(users, number);
  }

  public static <U extends GameUser> Set<U> getHighestKD(Collection<U> users, int number) {
    return server.getHighestKD(users, number);
  }

  public static <U extends GameUser> Set<U> getLongestShot(Collection<U> users, int number) {
    return server.getLongestShot(users, number);
  }

  public static <U extends GameUser> Set<U> getHighscore(Collection<U> user, int number, Comparator<U> comparator) {
    return server.getHighScore(user, number, comparator);
  }

  public static String getHighscoreMessage(String name, Collection<? extends GameUser> users,
                                           int number, Predicate<GameUser> predicateToBroadcast,
                                           Function<GameUser, ? extends Comparable<?>> keyExtractor) {
    return server.getHighscoreMessage(name, users, number, predicateToBroadcast, keyExtractor);
  }

  public static String getHighscoreMessage(String name, Collection<? extends GameUser> users,
                                           int number, Function<GameUser, ? extends Comparable<?>> keyExtractor) {
    return server.getHighscoreMessage(name, users, number, keyExtractor);
  }

  public static Collection<Team> getNotEmptyGameTeams() {
    return server.getNotEmptyInGameTeams();
  }

  public static Integer getEstimatedPlayers() {
    return server.getEstimatedPlayers();
  }

  public static void setEstimatedPlayers(Integer amount) {
    server.setEstimatedPlayers(amount);
  }

  public static void checkGameStart() {
    server.checkGameStart();
  }

  public static boolean isRejoiningAllowed() {
    return server.isRejoiningAllowed();
  }

  public static boolean isOutGameRejoiningAllowed() {
    return server.isOutGameRejoiningAllowed();
  }

  public static void onGameUserRejoin(GameUser user) {
    server.onGameUserRejoin(user);
  }

  public static OfflineUser loadOfflineUser(GameUser user) {
    return server.loadOfflineUser(user);
  }

  public static void startGame() {
    server.startGame();
  }

  public static void resetGame() {
    server.resetGame();
  }

  public static void stopGame() {
    server.stopGame();
  }

  public static Set<StatType<?>> getStats() {
    return server.getStats();
  }

  public static void saveGameUserStats(GameUser user) {
    server.saveGameUserStats(user);
  }

  public static Integer getMaxPlayersPerTeam() {
    return server.getMaxPlayersPerTeam();
  }

  public static ToolManager getToolManager() {
    return server.getToolManager();
  }

  public static DiscordManager getDiscordManager() {
    return server.getDiscordManager();
  }

  public static Sideboard getGameSideboard() {
    return server.getGameSideboard();
  }

  public static BukkitTask addGameTask(BukkitTask task) {
    return server.addGameTask(task);
  }

  public static GameUserManager getGameUserManager() {
    return server.getGameUserManager();
  }

  private static final LoungeBridgeServerManager<?> server = LoungeBridgeServerManager.getInstance();

  public enum State {
    STARTING, RUNNING, STOPPED, CLOSING, RESETTING, WAITING
  }
}
