/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool.advanced;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.tool.ToolWatcher;
import de.timesnake.basic.loungebridge.util.tool.WatchableTool;
import de.timesnake.basic.loungebridge.util.tool.listener.UserJoinQuitListener;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.library.basic.util.UserSet;
import de.timesnake.library.chat.Chat;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

public abstract class BossBarMapTimerTool extends MapTimerTool implements WatchableTool,
    UserJoinQuitListener {

  private final UserSet<User> listeners = new UserSet<>();
  private final BossBar bar;
  private final boolean timedColor;

  private boolean finished = false;

  public BossBarMapTimerTool() {
    this(BarColor.WHITE, true);
  }

  public BossBarMapTimerTool(BarColor color, boolean timedColor) {
    super();
    this.bar = Server.createBossBar("", color, BarStyle.SOLID);
    this.timedColor = timedColor;
  }

  @Override
  public void prepareTimer() {
    super.prepareTimer();

    this.bar.setTitle(this.getTitle(Chat.getTimeString(this.time)));
    this.bar.setColor(BarColor.WHITE);
    this.bar.setProgress(1);

    this.finished = false;
  }

  @Override
  public void onTimerUpdate() {
    this.bar.setTitle(this.getTitle(Chat.getTimeString(this.time)));
    this.bar.setProgress(this.time / ((double) this.maxTime));

    if (this.timedColor) {
      if (this.time <= this.maxTime / 10) {
        this.bar.setColor(BarColor.RED);
      } else if (this.time <= this.maxTime / 4) {
        this.bar.setColor(BarColor.YELLOW);
      }
    }

    if (this.time > 0 && (this.time % 60 == 0 || this.time == 30 || this.time == 10 || this.time <= 5)) {
      String msg = this.getChatMessage(Chat.getTimeString(this.time));
      if (msg != null) {
        LoungeBridgeServer.broadcastGameTDMessage(msg);
      }
    }
  }

  @Override
  public void onTimerEnd() {
    this.listeners.forEach(u -> u.removeBossBar(this.bar));
    this.listeners.clear();
    this.finished = true;
  }

  @Override
  public void onGameUserJoin(GameUser user) {
    if (this.finished) {
      return;
    }

    if (this.getWatchers() == ToolWatcher.IN_GAME || this.getWatchers() == ToolWatcher.ALL) {
      user.addBossBar(this.bar);
      this.listeners.add(user);
    }
  }

  @Override
  public void onGameUserQuit(GameUser user) {
    if (this.getWatchers() == ToolWatcher.IN_GAME || this.getWatchers() == ToolWatcher.ALL) {
      user.removeBossBar(this.bar);
      this.listeners.remove(user);
    }
  }

  @Override
  public void onSpectatorUserJoin(SpectatorUser user) {
    if (this.finished) {
      return;
    }

    if (this.getWatchers() == ToolWatcher.SPECTATOR || this.getWatchers() == ToolWatcher.ALL) {
      user.addBossBar(this.bar);
      this.listeners.add(user);
    }
  }

  @Override
  public void onSpectatorUserQuit(SpectatorUser user) {
    if (this.getWatchers() == ToolWatcher.SPECTATOR || this.getWatchers() == ToolWatcher.ALL) {
      user.removeBossBar(this.bar);
      this.listeners.remove(user);
    }
  }

  public abstract String getTitle(String time);

  public String getChatMessage(String time) {
    return null;
  }

  public String getEndChatMessage() {
    return null;
  }

}