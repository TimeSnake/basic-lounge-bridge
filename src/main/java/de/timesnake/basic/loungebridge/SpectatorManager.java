/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge;

import de.timesnake.basic.bukkit.util.chat.Chat;
import de.timesnake.basic.bukkit.util.user.scoreboard.Sideboard;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorManager extends de.timesnake.basic.game.util.user.SpectatorManager {

  @Override
  public @Nullable Sideboard getSpectatorSideboard() {
    return LoungeBridgeServer.getSpectatorSideboard();
  }

  @Override
  public @Nullable Chat getSpectatorChat() {
    return LoungeBridgeServer.getSpectatorChat();
  }

  @Override
  public @NotNull ExLocation getSpectatorSpawn() {
    return LoungeBridgeServer.getSpectatorSpawn();
  }

  @Override
  public boolean loadSpectatorTools() {
    return !LoungeBridgeServer.getState().equals(LoungeBridgeServer.State.CLOSING)
        && !LoungeBridgeServer.getState().equals(LoungeBridgeServer.State.STOPPED);
  }
}
