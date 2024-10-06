/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool;

import de.timesnake.basic.loungebridge.util.tool.scheduler.PreStopableTool;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;

public class TaskManager implements PreStopableTool {

  private final Collection<BukkitTask> tasks = new ArrayList<>();

  public BukkitTask addTask(BukkitTask task) {
    tasks.add(task);
    return task;
  }

  @Override
  public void preStop() {
    for (BukkitTask task : tasks) {
      task.cancel();
    }
    this.tasks.clear();
  }
}
