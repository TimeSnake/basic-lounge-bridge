/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool;

import com.google.common.collect.TreeMultiset;
import de.timesnake.basic.game.util.game.Map;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

public class ToolManager {

  private final Logger logger = LogManager.getLogger("lounge-bridge.tool.manager");

  protected TreeMultiset<GameTool> gameTools = TreeMultiset.create();
  protected HashMap<Map, TreeMultiset<GameTool>> gameToolsByMap = new HashMap<>();

  public ToolManager() {

  }

  public void add(GameTool gameTool) {
    this.gameTools.add(gameTool);
  }

  public void add(Map map, GameTool gameTool) {
    this.gameToolsByMap.computeIfAbsent(map, m -> TreeMultiset.create()).add(gameTool);
  }

  public void runTools(Class<? extends GameTool> type) {
    this.runTools(type, this.gameTools);
    this.runTools(type,
        this.gameToolsByMap.getOrDefault(LoungeBridgeServer.getMap(), TreeMultiset.create()));
  }

  public <T extends GameTool> void applyOnTools(Class<T> type, Consumer<T> consumer) {
    this.applyOnTools(type, this.gameTools, consumer);
    this.applyOnTools(type,
        this.gameToolsByMap.getOrDefault(LoungeBridgeServer.getMap(), TreeMultiset.create()),
        consumer);
  }

  private void runTools(Class<? extends GameTool> type, Collection<GameTool> tools) {
    this.applyOnTools(type, tools, t -> {
      try {
        Method method = type.getDeclaredMethods()[0];
        method.invoke(t);
      } catch (IllegalAccessException e) {
        this.logger.warn("Failed to invoke/access method of class {}: {}", type.getSimpleName(), e);
      } catch (InvocationTargetException e) {
        this.logger.warn("Exception while invoking method of class {}: {}", type.getSimpleName(),
            e.getTargetException().getMessage());
      }
    });
  }

  private <T extends GameTool> void applyOnTools(Class<T> type, Collection<GameTool> tools,
      Consumer<T> consumer) {
    tools.stream().filter(t -> type.isAssignableFrom(t.getClass()))
        .forEach(gameTool -> consumer.accept((T) gameTool));
  }
}
