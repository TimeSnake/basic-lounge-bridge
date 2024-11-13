package de.timesnake.basic.loungebridge.util.user;

import de.timesnake.basic.bukkit.util.user.event.UserBlockBreakEvent;
import de.timesnake.basic.bukkit.util.user.event.UserBlockPlaceEvent;
import de.timesnake.basic.bukkit.util.user.event.UserDamageByUserEvent;
import de.timesnake.library.basic.util.Tuple;
import org.bukkit.block.Block;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;

public interface GameUserEventListener {

  default void onProjectileHitUser(GameUser user, Projectile projectile, ProjectileHitEvent event) {
  }

  default void onProjectileHitBlock(Block block, Projectile projectile, ProjectileHitEvent event) {
  }

  default void onUserDamageByUser(GameUser user, GameUser damager, UserDamageByUserEvent event) {
  }

  default void onUserBlockPlace(GameUser user, Block block, UserBlockPlaceEvent event) {
  }

  default void onUserBlockBreak(GameUser user, Block block, UserBlockBreakEvent event) {
  }

  default void onUserPickupArrow(GameUser user, PlayerPickupArrowEvent event) {
  }
}
