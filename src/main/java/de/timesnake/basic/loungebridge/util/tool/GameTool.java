/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool;

import org.jetbrains.annotations.NotNull;

public interface GameTool extends Comparable<GameTool> {

  default int getPriority() {
    return 10;
  }

  @Override
  default int compareTo(@NotNull GameTool o) {
    if (this.equals(o)) {
      return 0;
    } else if (this.getPriority() == o.getPriority()) {
      return -1;
    } else if (this.getPriority() > o.getPriority()) {
      return 1;
    } else {
      return 1;
    }
  }
}
