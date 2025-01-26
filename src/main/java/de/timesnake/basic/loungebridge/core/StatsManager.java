/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.core;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.chat.cmd.Argument;
import de.timesnake.basic.bukkit.util.chat.cmd.CommandListener;
import de.timesnake.basic.bukkit.util.chat.cmd.Completion;
import de.timesnake.basic.bukkit.util.chat.cmd.Sender;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.game.util.user.Plugin;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.server.TmpGameServerManager;
import de.timesnake.basic.loungebridge.util.tool.GameTool;
import de.timesnake.basic.loungebridge.util.tool.scheduler.PreCloseableTool;
import de.timesnake.basic.loungebridge.util.tool.scheduler.ResetableTool;
import de.timesnake.basic.loungebridge.util.tool.scheduler.StopableTool;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.database.util.game.DbGame;
import de.timesnake.library.basic.util.statistics.StatType;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.ExTextColor;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class StatsManager implements GameTool, ResetableTool, PreCloseableTool, StopableTool {

  public static final Set<StatType<?>> BASE_STATS = Set.of(TmpGameServerManager.GAMES_PLAYED);

  private final Logger logger = LogManager.getLogger("lounge-bridge.stats");

  private boolean saveStats;

  public StatsManager() {
    this.reset();

    Server.getCommandManager().addCommand(BasicLoungeBridge.getPlugin(), "stats_discard",
        new StatsDiscardCmd(), Plugin.GAME);
  }

  @Override
  public void reset() {
    this.saveStats = true;
  }

  @Override
  public void preClose() {
    this.saveGameStats();
  }

  @Override
  public void stop() {
    this.sendStatSaveRequest();
  }

  @Override
  public int getPriority() {
    return 5;
  }

  public boolean isSaveStats() {
    return saveStats;
  }

  public void setSaveStats(boolean saveStats) {
    this.saveStats = saveStats;
  }

  public void loadStatTypesIntoDatabase() {
    if (LoungeBridgeServer.getStats() == null) {
      return;
    }

    DbGame game = LoungeBridgeServer.getGame().getDatabase();

    for (StatType<?> stat : BASE_STATS) {
      game.removeStat(stat);
      game.addStat(stat);
    }

    for (StatType<?> stat : LoungeBridgeServer.getStats()) {
      game.removeStat(stat);
      game.addStat(stat);
    }
  }

  public void sendStatSaveRequest() {
    for (User user : Server.getUsers()) {
      if (user.hasPermission("game.stats.discard")) {
        user.sendClickablePluginMessage(Plugin.GAME,
            Component.text("Discard game ", ExTextColor.WARNING)
                .append(Component.text("stats?", ExTextColor.BLUE)),
            "/stats_discard",
            Component.text("Click to discard all stats"),
            ClickEvent.Action.RUN_COMMAND);
      }
    }
  }

  public void saveGameStats() {
    if (this.isSaveStats()) {
      this.logger.info("Saved game stats");
      for (User user : LoungeBridgeServer.getGameUsers()) {
        if (((GameUser) user).hasPlayedGame()) {
          LoungeBridgeServer.saveGameUserStats(((GameUser) user));
        }

        if (user.hasPermission("game.stats.info")) {
          user.sendPluginTDMessage(Plugin.GAME, "§wSaved all stats");
        }
      }

      Server.getChannel().sendMessage(
          new ChannelServerMessage<>(Server.getName(), MessageType.Server.USER_STATS,
              LoungeBridgeServer.getGame().getName()));
    } else {
      for (User user : LoungeBridgeServer.getGameUsers()) {
        if (user.hasPermission("game.stats.info")) {
          user.sendPluginTDMessage(Plugin.GAME, "§wDiscarded all stats");
        }
      }

      this.logger.info("Discarded game stats");
    }

  }

  public class StatsDiscardCmd implements CommandListener {

    private final Code perm = Plugin.GAME.createPermssionCode("game.stats.discard");

    @Override
    public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
      sender.hasPermissionElseExit(this.perm);
      StatsManager.this.saveStats = false;
      sender.sendPluginTDMessage("§sAll stats will be discarded");
    }

    @Override
    public Completion getTabCompletion() {
      return new Completion(this.perm);
    }

    @Override
    public String getPermission() {
      return this.perm.getPermission();
    }
  }
}
