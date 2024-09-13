/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.util.tool.advanced;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.world.ExBlock;
import de.timesnake.basic.bukkit.util.world.ExLocation;
import de.timesnake.basic.bukkit.util.world.entity.PacketPlayer;
import de.timesnake.library.basic.util.Tuple;
import de.timesnake.library.entities.entity.PlayerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Objects;

public class DeadPlayer {

  protected final User user;
  protected final String name;
  protected final Tuple<String, String> textures;
  protected final ExLocation location;
  protected PacketPlayer bodyEntity;

  public DeadPlayer(User user, ExLocation location) {
    this.user = user;
    this.name = user.getName();
    this.textures = user.asPlayerBuilder().getTextures();
    this.location = this.calcEntityLocation(location);
  }

  public User getUser() {
    return user;
  }

  public String getName() {
    return name;
  }

  public Location getLocation() {
    return location;
  }

  public PacketPlayer getBodyEntity() {
    return bodyEntity;
  }

  public void spawn() {
    ServerPlayer deadBody = PlayerBuilder.ofName(this.name, this.textures.getA(), this.textures.getB())
        .applyOnEntity(e -> {
          e.setLevel(this.location.getExWorld().getHandle());
          e.setPos(this.location.getX(), this.location.getY() + 0.4, this.location.getZ());
          e.setRot(this.location.getYaw(), this.location.getPitch());
          e.setNoGravity(true);
          e.setCustomName(Component.literal(this.name + " (dead)"));
          e.setCustomNameVisible(true);
          e.setPose(Pose.SLEEPING);
        })
        .apply(this::onEntityBuild)
        .build();

    this.bodyEntity = new PacketPlayer(deadBody, this.location);
    Server.getEntityManager().registerEntity(this.bodyEntity);
  }

  protected ExLocation calcEntityLocation(ExLocation location) {
    for (Vector vector : ExBlock.NEAR_BLOCKS) {
      if (vector.getY() != 0) {
        continue;
      }

      if (location.clone().add(vector).getBlock().isEmpty()) {
        if (vector.getZ() == -1) {
          location.setYaw(180);
        } else if (vector.getX() == 1) {
          location.setYaw(90);
        } else if (vector.getX() == -1) {
          location.setYaw(-90);
        }
        break;
      }
    }

    return location;
  }

  protected void onEntityBuild(PlayerBuilder<?, ?> builder) {

  }

  public void despawn() {
    Server.getEntityManager().unregisterEntity(this.bodyEntity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeadPlayer deadBody = (DeadPlayer) o;
    return Objects.equals(name, deadBody.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}