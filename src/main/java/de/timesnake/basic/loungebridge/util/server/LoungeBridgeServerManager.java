/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.server;

import de.timesnake.basic.bukkit.core.user.scoreboard.tablist.Tablist2;
import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.ServerManager;
import de.timesnake.basic.bukkit.util.chat.Chat;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.scoreboard.Tablist;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.game.util.game.Team;
import de.timesnake.basic.game.util.server.GameServerManager;
import de.timesnake.basic.game.util.user.SpectatorManager;
import de.timesnake.basic.loungebridge.core.*;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.basic.loungebridge.util.game.ResetableMap;
import de.timesnake.basic.loungebridge.util.game.TmpGame;
import de.timesnake.basic.loungebridge.util.tool.TaskManager;
import de.timesnake.basic.loungebridge.util.tool.ToolManager;
import de.timesnake.basic.loungebridge.util.tool.scheduler.*;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.basic.loungebridge.util.user.GameUserManager;
import de.timesnake.basic.loungebridge.util.user.Kit;
import de.timesnake.basic.loungebridge.util.user.TablistTeam;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.database.util.object.UnsupportedStringException;
import de.timesnake.database.util.server.DbLoungeServer;
import de.timesnake.database.util.server.DbTmpGameServer;
import de.timesnake.library.basic.util.Status;
import de.timesnake.library.chat.ExTextColor;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

public abstract class LoungeBridgeServerManager<Game extends TmpGame> extends
    GameServerManager<Game> implements TmpGameServerManager, HighScoreCalculator {

  public static final String SPECTATOR_NAME = "spectator";
  public static final String SPECTATOR_CHAT_DISPLAY_NAME = "Spec";
  public static final ExTextColor SPECTATOR_CHAT_COLOR = ExTextColor.GRAY;
  public static final ExTextColor SPECTATOR_TABLIST_CHAT_COLOR = ExTextColor.GRAY;

  public static final Integer MAX_START_DELAY = 5 * 20; // max start delay after first join

  public static LoungeBridgeServerManager<?> getInstance() {
    return (LoungeBridgeServerManager<?>) ServerManager.getInstance();
  }

  private Logger logger = LogManager.getLogger("lounge-bridge.server");

  protected DbLoungeServer twinServer;

  protected LoungeBridgeServer.State state;
  protected ChannelListener channelListener;
  protected boolean kitsEnabled;
  protected boolean mapsEnabled;
  protected Integer serverTeamAmount;
  protected Integer maxPlayersPerTeam;
  protected boolean allowTeamMateDamage = true;
  protected Integer estimatedPlayers;
  protected Integer startPlayers;
  protected ToolManager toolManager;
  protected Boolean running = false;
  private Map map;
  private UserManager userManager;
  private GameScheduler gameScheduler;
  private TablistManager tablistManager;
  private DiscordManager discordManager;
  private TaskManager taskManager;
  private GameUserManager gameUserManager;

  public final void onLoungeBridgeEnable() {
    this.toolManager = this.initToolManager();
    if (this.toolManager == null) {
      this.toolManager = new ToolManager();
    }

    super.onGameEnable();

    // update kits into database, if enabled
    this.kitsEnabled = ((DbTmpGameServer) this.getDatabase()).areKitsEnabled();
    if (this.kitsEnabled) {
      this.loadKitsIntoDatabase();
    }

    // search twin server
    DbTmpGameServer database = ((DbTmpGameServer) Server.getDatabase());
    this.twinServer = database.getTwinServer();
    if (twinServer == null) {
      this.logger.error("No twin server found in database");
      Bukkit.shutdown();
      return;
    }

    this.userManager = new UserManager();
    this.gameScheduler = new GameScheduler();

    StatsManager statsManager = this.initStatsManager();
    if (statsManager != null) {
      statsManager.loadStatTypesIntoDatabase();
      this.toolManager.add(statsManager);
    }

    CoinsManager coinsManager = new CoinsManager();
    this.toolManager.add(coinsManager);

    this.mapsEnabled = ((DbTmpGameServer) this.getDatabase()).areMapsEnabled();
    Integer serverTeamAmount = ((DbTmpGameServer) this.getDatabase()).getTeamAmount();
    this.serverTeamAmount = serverTeamAmount != null ? serverTeamAmount : 0;
    this.maxPlayersPerTeam = ((DbTmpGameServer) this.getDatabase()).getMaxPlayersPerTeam();

    this.channelListener = new ChannelListener();

    this.discordManager = this.initDiscordManager();
    if (discordManager == null) {
      this.discordManager = new DiscordManager();
    }
    this.toolManager.add(this.discordManager);
    this.discordManager.update();

    this.tablistManager = this.initTablistManager();
    if (this.tablistManager == null) {
      this.tablistManager = new TablistManager();
    }
    this.tablistManager.loadTablist(Tablist.Type.BLANK);
    Server.getScoreboardManager().setActiveTablist(this.tablistManager.getGameTablist());

    this.taskManager = new TaskManager();
    this.toolManager.add(this.taskManager);

    this.toolManager.add((StartableTool) () -> Server.getInGameUsers()
        .forEach(u -> ((GameUser) u).onGameStart()));

    this.toolManager.add((StopableTool) () -> Server.getInGameUsers()
        .forEach(u -> ((GameUser) u).stopGame()));

    this.gameUserManager = new GameUserManager();

    // load maps, if enabled
    if (this.mapsEnabled) {
      for (Map map : this.getGame().getMaps()) {
        map.getWorld();
      }
    }

    // set map from database
    this.map = this.getGame().getMap(database.getMapName());

    // silent join quit
    Server.getChat().setBroadcastJoinQuit(false);

    this.loadChats();

    // mark as ready
    this.logger.info("Server loaded");
    this.setState(LoungeBridgeServer.State.WAITING);

    this.loadTools();
  }

  public ToolManager initToolManager() {
    return new ToolManager();
  }

  public StatsManager initStatsManager() {
    return new StatsManager();
  }

  public CoinsManager initCoinsManager() {
    return new CoinsManager();
  }

  public TablistManager initTablistManager() {
    return new TablistManager();
  }

  public DiscordManager initDiscordManager() {
    return new DiscordManager();
  }

  @Override
  protected SpectatorManager initSpectatorManager() {
    return new de.timesnake.basic.loungebridge.SpectatorManager();
  }

  public void loadChats() {
    // create team chat
    for (Team team : this.getGame().getTeams()) {
      // chat
      if (team.hasPrivateChat()) {
        Server.getChatManager()
            .createChat(team.getName(), team.getDisplayName(), team.getTextColor(),
                new HashSet<>());
      }
    }

    // create spectator chat
    Server.getChatManager()
        .createChat(SPECTATOR_NAME, SPECTATOR_CHAT_DISPLAY_NAME, SPECTATOR_CHAT_COLOR,
            new HashSet<>());
  }

  @Override
  public abstract GameUser loadUser(Player player);

  public void checkGameStart() {
    int gameUsers = Server.getPreGameUsers().size();
    if (this.getEstimatedPlayers() != null && gameUsers >= this.getEstimatedPlayers()) {
      this.startGameCountdown();
    } else if (gameUsers == 1) {
      Server.runTaskLaterSynchrony(() -> {
        if (!this.state.equals(LoungeBridgeServer.State.STARTING)) {
          this.startGameCountdown();
        }
      }, LoungeBridgeServerManager.MAX_START_DELAY, BasicLoungeBridge.getPlugin());
    }
  }

  private void startGameCountdown() {
    this.gameScheduler.startGameCountdown();
  }

  public void loadKitsIntoDatabase() {
    if (this.getGame().getKitManager() == null) {
      return;
    }
    DbGame game = this.getGame().getDatabase();
    for (Kit kit : this.getGame().getKitManager().getKits()) {
      if (game.getKit(kit.getId()) != null) {
        game.removeKitSynchronized(kit.getId());
      }
      try {
        game.addKit(kit.getId(), kit.getName(), kit.getMaterial().toString(),
            kit.getDescription());
        this.logger.info("Loaded kit '{}' into the database", kit.getName());
      } catch (UnsupportedStringException e) {
        this.logger.warn("Can not load kit '{}' into database (UnsupportedStringException: {})", kit.getName(),
            e.getMessage());
      } catch (Exception e) {
        this.logger.warn("Can not load kit '{}' into database: {}", kit.getName(), e.getMessage());
      }
    }
  }

  public final void loadMap() {
    Map map = this.getGame().getMap(((DbTmpGameServer) Server.getDatabase()).getMapName());
    this.map = map;

    this.onMapLoad();
    this.toolManager.runTools(MapLoadableTool.class);

    this.logger.info("Loaded map '{}'", map.getName());
  }

  public final void loadWorld() {
    this.onWorldLoad();

    this.toolManager.runTools(WorldLoadableTool.class);

    this.logger.info("Loaded world");
  }

  public final void startGame() {
    LoungeBridgeServer.setState(LoungeBridgeServer.State.RUNNING);

    this.running = true;

    for (User user : Server.getPreGameUsers()) {
      user.setStatus(Status.User.IN_GAME);
    }

    if (this.checkGameEnd()) {
      this.logger.info("Stopped game, due to fulfilled end condition");
      this.stopGame();
      return;
    }

    Server.getInGameUsers().forEach(u -> ((GameUser) u).playedGame());

    this.startPlayers = Server.getInGameUsers().size();
    this.logger.info("Starting with {} players", this.startPlayers);

    this.toolManager.runTools(StartableTool.class);

    Server.getChat().setBroadcastJoinQuit(true);

    this.logger.info("Game started");
    this.onGameStart();
  }

  public final void stopGame() {
    if (!this.running) {
      return;
    }

    this.running = false;

    this.logger.info("Game stopped");

    this.toolManager.runTools(PreStopableTool.class);

    this.onGameStop();

    this.toolManager.runTools(StopableTool.class);

    this.closeGame();
  }

  public final void closeGame() {
    if (LoungeBridgeServer.getState() != LoungeBridgeServer.State.CLOSING) {
      LoungeBridgeServer.setState(LoungeBridgeServer.State.CLOSING);

      this.toolManager.runTools(CloseableTool.class);

      for (User user : Server.getInGameUsers()) {
        user.setDefault();
        user.getPlayer().setInvulnerable(true);
        user.lockLocation();
      }

      for (User user : Server.getSpectatorUsers()) {
        user.clearInventory();
      }

      this.getLoungeBridgeUserManager().clearRejoinUsers();

      Chat specChat = this.getSpectatorChat();
      for (User user : Server.getUsers()) {
        if (((GameUser) user).getTeam() != null) {
          Chat teamChat = Server.getChat(((GameUser) user).getTeam().getName());
          if (teamChat != null) {
            teamChat.removeWriter(user);
            teamChat.removeListener(user);
          }
        }

        specChat.removeWriter(user);
        specChat.removeListener(user);

        Server.getGlobalChat().addWriter(user);
        Server.getGlobalChat().addListener(user);
        user.clearInventory();
      }

      this.getSpectatorManager().clearTools();

      this.gameScheduler.closeGame();
    }
  }

  public final void closeGame6() {
    this.toolManager.runTools(PreCloseableTool.class);
  }

  public final void closeGame10() {
    Server.getChat().setBroadcastJoinQuit(false);
    for (User user : Server.getUsers()) {
      if (!user.isAirMode() && !user.isService()) {
        user.getDatabase().setKit(null);
        ((GameUser) user).setTeam(null);
      }

      if (LoungeBridgeServer.getGame().hasTexturePack()) {
        user.removeResourcePacks();
      }

      user.switchToServer(LoungeBridgeServer.getTwinServer());
    }
  }

  public final void resetGame() {
    LoungeBridgeServer.setState(LoungeBridgeServer.State.RESETTING);

    this.estimatedPlayers = null;
    this.startPlayers = null;

    this.logger.info("Starting game reset");

    this.toolManager.runTools(ResetableTool.class);

    if (this.getMap() != null && this.getMap() instanceof ResetableMap) {
      ((ResetableMap) this.getMap()).reset();
    }

    this.onGameReset();
    LoungeBridgeServer.setState(LoungeBridgeServer.State.WAITING);

    this.logger.info("Finished game reset");
  }

  public void loadTools() {
    this.getToolManager().add((ResetableTool) () -> {
      for (Team team : getGame().getTeams()) {
        team.setDeaths(0);
        team.setKills(0);
      }
    });
  }

  public de.timesnake.basic.loungebridge.SpectatorManager getSpectatorManager() {
    return (de.timesnake.basic.loungebridge.SpectatorManager) super.getSpectatorManager();
  }

  public Chat getSpectatorChat() {
    return Server.getChat(SPECTATOR_NAME);
  }

  public void updateSpectatorTools() {
    this.getSpectatorManager().updateSpectatorTools();
  }

  public boolean allowTeamMateDamage() {
    return allowTeamMateDamage;
  }

  public void setTeamMateDamage(boolean allowTeaMateDamage) {
    this.allowTeamMateDamage = allowTeaMateDamage;
  }

  public TablistTeam getTablistGameTeam() {
    return this.tablistManager.getTablistGameTeam();
  }

  public boolean areKitsEnabled() {
    return kitsEnabled;
  }

  public boolean areMapsEnabled() {
    return mapsEnabled;
  }

  public UserManager getLoungeBridgeUserManager() {
    return this.userManager;
  }

  public Integer getMaxPlayersPerTeam() {
    return maxPlayersPerTeam;
  }

  public ToolManager getToolManager() {
    return toolManager;
  }

  public boolean isGameRunning() {
    return this.running;
  }

  public Map getMap() {
    return map;
  }

  public Integer getServerTeamAmount() {
    return serverTeamAmount;
  }

  public DbLoungeServer getTwinServer() {
    return twinServer;
  }

  public int getGameCountdown() {
    return this.gameScheduler.getGameCountdown();
  }

  public void broadcastLoungeBridgeTDMessage(String msg) {
    Server.broadcastTDMessage(this.getGamePlugin(), msg);
  }

  public void broadcastLoungeBridgeMessage(Component msg) {
    Server.broadcastMessage(de.timesnake.library.chat.Chat.getSenderPlugin(this.getGamePlugin()).append(msg));
  }

  public void broadcastGameMessage(Component msg) {
    Server.broadcastMessage(this.getGamePlugin(), msg);
  }

  public void broadcastGameTDMessage(String msg) {
    Server.broadcastTDMessage(this.getGamePlugin(), msg);
  }

  public Tablist2 getGameTablist() {
    return this.tablistManager.getGameTablist();
  }

  public Collection<Team> getNotEmptyInGameTeams() {
    return this.getGame().getTeams().stream()
        .filter((team -> !team.getInGameUsers().isEmpty()))
        .collect(Collectors.toList());
  }

  public Integer getEstimatedPlayers() {
    return this.estimatedPlayers;
  }

  public void setEstimatedPlayers(Integer amount) {
    this.estimatedPlayers = amount;

    if (amount == null) {
      this.logger.warn("Estimated players value is null");
      return;
    }

    this.logger.info("Estimated players: {}", amount);
  }

  public LoungeBridgeServer.State getState() {
    return state;
  }

  public void setState(LoungeBridgeServer.State state) {
    this.state = state;
    switch (state) {
      case RUNNING, STOPPED -> Server.setStatus(Status.Server.IN_GAME);
      case STARTING -> Server.setStatus(Status.Server.PRE_GAME);
      case CLOSING, RESETTING -> Server.setStatus(Status.Server.POST_GAME);
      case WAITING -> {
        Server.setStatus(Status.Server.ONLINE);
        Server.getChannel().sendMessage(
            new ChannelServerMessage<>(Server.getName(), MessageType.Server.STATE
                , ChannelServerMessage.State.READY));
        this.logger.info("Send lounge ready state");
      }
    }
  }

  public DiscordManager getDiscordManager() {
    return discordManager;
  }

  public Integer getStartPlayers() {
    return startPlayers != null ? startPlayers : this.getEstimatedPlayers();
  }

  public BukkitTask addGameTask(BukkitTask task) {
    return this.taskManager.addTask(task);
  }

  public GameUserManager getGameUserManager() {
    return gameUserManager;
  }
}
