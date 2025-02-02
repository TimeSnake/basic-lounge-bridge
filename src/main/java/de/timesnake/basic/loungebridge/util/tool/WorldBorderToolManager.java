/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool;

import de.timesnake.basic.bukkit.util.chat.cmd.Argument;
import de.timesnake.basic.bukkit.util.chat.cmd.CommandListener;
import de.timesnake.basic.bukkit.util.chat.cmd.Completion;
import de.timesnake.basic.bukkit.util.chat.cmd.Sender;
import de.timesnake.basic.bukkit.util.server.ExTime;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.tool.advanced.WorldBorderTool;
import de.timesnake.library.chat.Code;
import de.timesnake.library.chat.Plugin;
import de.timesnake.library.commands.PluginCommand;
import de.timesnake.library.commands.simple.Arguments;

public class WorldBorderToolManager implements CommandListener {

  private final Code perm = Plugin.GAME.createPermssionCode("loungebridge.worldborder");

  @Override
  public void onCommand(Sender sender, PluginCommand cmd, Arguments<Argument> args) {
    sender.hasPermissionElseExit(this.perm);
    args.isLengthEqualsElseExit(2, true);
    double size = args.get(0).toBoundedDoubleOrExit(0, 256, true);
    int timeSec = args.get(1).toBoundedIntOrExit(0, 600, true);
    LoungeBridgeServer.getToolManager().applyOnTools(WorldBorderTool.class, t -> t.shrinkBorder(size,
        ExTime.ofSeconds(timeSec)));
    sender.sendPluginTDMessage("§sShrinking border to §v" + size + "§s within §v" + timeSec + "§ss");
  }

  @Override
  public Completion getTabCompletion() {
    return new Completion(this.perm)
        .addArgument(new Completion("<size>", "16", "32")
            .addArgument(new Completion("<time>", "30", "60", "120")));
  }

  @Override
  public String getPermission() {
    return this.perm.getPermission();
  }
}
