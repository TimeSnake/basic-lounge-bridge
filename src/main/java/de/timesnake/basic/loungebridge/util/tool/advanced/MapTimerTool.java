/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool.advanced;

import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.tool.Timeable;

public abstract class MapTimerTool extends TimerTool {

  public MapTimerTool() {
    super(() -> ((Timeable) LoungeBridgeServer.getMap()).getTime());
  }
}
