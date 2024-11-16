/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool;

import de.timesnake.basic.bukkit.util.server.ExTime;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExPolygon;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.tool.scheduler.PreStopableTool;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class WorldDestroyer implements PreStopableTool {

  private ExPolygon polygon;
  private int blocksToDestroyPerPeriod;
  private BukkitTask task;

  public WorldDestroyer() {
  }

  public void start(ExTime destroyPeriod, int blocksToDestroyPerPeriod) {
    this.blocksToDestroyPerPeriod = blocksToDestroyPerPeriod;

    if (this.task != null) {
      this.task.cancel();
    }

    this.polygon = ((BoundedMap) LoungeBridgeServer.getMap()).getBounds();
    this.task = LoungeBridgeServer.runTaskTimerSynchrony(this::destroyBlocks, 0, destroyPeriod.toTicks(),
        BasicLoungeBridge.getPlugin());
  }

  private void destroyBlocks() {
    int i = 0;
    List<ExBlock> blocksToDestroy =
        new ArrayList<>(this.polygon.getBlocksInsideSortedByDistanceToCenter(b -> !b.getType().isEmpty())).reversed();
    for (ExBlock block : blocksToDestroy) {
      if (i >= this.blocksToDestroyPerPeriod) {
        break;
      }
      block.getBlock().setType(Material.AIR);
      i++;
    }
  }

  @Override
  public void preStop() {
    if (task != null) {
      this.task.cancel();
    }
  }
}
