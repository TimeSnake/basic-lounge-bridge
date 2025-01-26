/*
 * Copyright (C) 2023 timesnake
 */

package de.timesnake.basic.loungebridge.core;

import de.timesnake.basic.bukkit.util.Server;
import de.timesnake.basic.bukkit.util.user.User;
import de.timesnake.basic.bukkit.util.user.event.AsyncUserMoveEvent;
import de.timesnake.basic.bukkit.util.user.event.UserTeleportEvent;
import de.timesnake.basic.game.util.user.SpectatorUser;
import de.timesnake.basic.loungebridge.core.main.BasicLoungeBridge;
import de.timesnake.basic.loungebridge.util.server.LoungeBridgeServer;
import de.timesnake.basic.loungebridge.util.tool.listener.UserJoinQuitListener;
import de.timesnake.basic.loungebridge.util.tool.scheduler.PreStopableTool;
import de.timesnake.basic.loungebridge.util.tool.scheduler.StartableTool;
import de.timesnake.basic.loungebridge.util.user.GameUser;
import de.timesnake.channel.util.listener.ChannelHandler;
import de.timesnake.channel.util.listener.ChannelListener;
import de.timesnake.channel.util.listener.ListenerType;
import de.timesnake.channel.util.message.ChannelDiscordMessage;
import de.timesnake.channel.util.message.ChannelServerMessage;
import de.timesnake.channel.util.message.MessageType;
import de.timesnake.library.basic.util.DiscordChannelType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DiscordManager implements Listener, PreStopableTool, StartableTool, UserJoinQuitListener, ChannelListener {

  public static final String DISCORD_SPECTATOR = "Spectator";
  public static final String DISCORD_LOUNGE = "Lounge";

  public static final double HORIZONTAL_DISTANCE = 25;
  public static final double VERTICAL_DISTANCE = 10;

  public static final int DELAY = 10;

  private final Logger logger = LogManager.getLogger("lounge-bridge.discord");

  private final Map<String, DistanceChannel> channelByName = new ConcurrentHashMap<>();
  private final Map<UUID, DistanceChannel> channelByUuid = new ConcurrentHashMap<>();
  private final Map<UUID, Action> actionsByUuid = new ConcurrentHashMap<>();
  private boolean lock = false;
  private boolean enabled;
  private BukkitTask updateTask;

  private boolean isLoaded = false;

  public DiscordManager() {
    Server.getChannel().addListener(this, Set.of(LoungeBridgeServer.getTwinServer().getName()));
  }

  @ChannelHandler(type = ListenerType.SERVER_DISCORD, filtered = true)
  public void onServerMessage(ChannelServerMessage<Boolean> msg) {
    this.setEnabled(msg.getValue());
  }

  public void update() {
    if (this.isEnabled()) {
      if (LoungeBridgeServer.getGame().getDiscordType() == null
          || LoungeBridgeServer.getGame().getDiscordType().equals(DiscordChannelType.FORBIDDEN)) {
        return;
      }

      if (LoungeBridgeServer.getGame().getDiscordType().equals(DiscordChannelType.DISTANCE)) {
        if (!this.isLoaded) {
          Server.registerListener(this, BasicLoungeBridge.getPlugin());
        }

        this.logger.info("Loaded discord manager with distance channels");
      } else if (LoungeBridgeServer.getGame().getDiscordType().equals(DiscordChannelType.TEAMS)) {
        this.logger.info("Loaded discord manager with team channels");
      }
      this.isLoaded = true;
    }
  }

  @Override
  public void start() {
    if (this.isEnabled() && LoungeBridgeServer.getGame().getDiscordType().equals(DiscordChannelType.DISTANCE)) {
      Server.getChannel().sendMessage(new ChannelDiscordMessage<>(Server.getName(), MessageType.Discord.HIDE_CHANNELS
          , true));

      for (User user : Server.getInGameUsers()) {
        // create channel for each user
        DistanceChannel channel = new DistanceChannel(UUID.randomUUID().toString());
        this.channelByName.put(channel.getName(), channel);

        // add user to channel
        this.actionsByUuid.put(user.getUniqueId(), Action.ADD);
      }
      this.updateTask = Server.runTaskTimerAsynchrony(this::updateDistanceChannels, 0, DELAY,
          BasicLoungeBridge.getPlugin());
      this.logger.info("Started discord distance channel updater");
    }
  }

  @Override
  public void preStop() {
    if (this.isEnabled()) {
      Server.runTaskLaterSynchrony(() -> {
        // move all users to lounge channel
        LinkedHashMap<String, List<UUID>> uuidsByTeam = new LinkedHashMap<>();

        uuidsByTeam.put(DISCORD_LOUNGE, Server.getUsers().stream().map(User::getUniqueId).collect(Collectors.toList()));

        Server.getChannel().sendMessage(new ChannelDiscordMessage<>(Server.getName(),
            MessageType.Discord.MOVE_MEMBERS, new ChannelDiscordMessage.Allocation(uuidsByTeam)));

        // clean up distance channels
        if (LoungeBridgeServer.getGame().getDiscordType().equals(DiscordChannelType.DISTANCE)) {
          if (this.updateTask != null) {
            this.updateTask.cancel();
          }

          Server.runTaskLaterSynchrony(() -> {
            Server.getChannel().sendMessage(new ChannelDiscordMessage<>(Server.getName(),
                MessageType.Discord.DESTROY_CHANNELS, new LinkedList<>(this.channelByName.keySet())));
            this.channelByName.values().forEach(DistanceChannel::clear);
            this.channelByName.clear();
            this.channelByUuid.clear();
          }, 20 * 4, BasicLoungeBridge.getPlugin());
        }

      }, 20 * 2, BasicLoungeBridge.getPlugin());
    }

  }

  @EventHandler
  public void onUserMove(AsyncUserMoveEvent e) {
    if (e.getUser().isInGame()) {
      this.actionsByUuid.put(e.getUser().getUniqueId(), Action.MOVE);
    }
  }

  @EventHandler
  public void onUserTeleport(UserTeleportEvent e) {
    if (e.getUser().isInGame()) {
      this.actionsByUuid.put(e.getUser().getUniqueId(), Action.MOVE);
    }
  }

  public void updateDistanceChannels() {
    if (this.lock) {
      return;
    }
    this.lock = true;

    try {
      if (this.actionsByUuid.isEmpty()) {
        this.lock = false;
        return;
      }

      Map<UUID, Action> actionsByUuid = Map.copyOf(this.actionsByUuid);
      this.actionsByUuid.clear();

      for (Map.Entry<UUID, Action> entry : actionsByUuid.entrySet()) {
        UUID uuid = entry.getKey();
        Action action = entry.getValue();

        if (action == Action.REMOVE) {
          this.removeUuid(uuid);
        } else if (action == Action.ADD || action == Action.MOVE) {
          DistanceChannel channelForUuid = null;
          for (DistanceChannel channel : this.channelByName.values()) {
            UUID joinToUuid = channel.canJoin(uuid);
            if (joinToUuid != null) {
              if (channelForUuid == null) {
                // first usable channel
                channelForUuid = channel;
              } else {
                // merge channels into larger one
                if (channel.size() >= channelForUuid.size()) {
                  DistanceChannel tmp = channelForUuid;
                  channelForUuid = channel;
                  channel = tmp;
                }

                channelForUuid.addAll(channel);
                channel.clear();
              }

            }
          }

          // move to default channel or extract new from default
          if (channelForUuid == null) {
            if (!this.channelByUuid.containsKey(uuid)
                || this.channelByUuid.get(uuid).size() > 1) {
              this.moveUuidToEmptyChannel(uuid);
            }
          } else {
            channelForUuid.add(uuid);
          }
        }
      }

      if (!this.channelByName.isEmpty()) {
        Server.getChannel().sendMessage(new ChannelDiscordMessage<>(Server.getName(),
            MessageType.Discord.MOVE_MEMBERS,
            new ChannelDiscordMessage.Allocation(this.channelByName)));
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      this.lock = false;
    }
  }

  private void moveUuidToEmptyChannel(UUID uuid) {
    this.channelByName.values().stream().filter(DistanceChannel::isEmpty).findAny().get()
        .add(uuid);
  }

  private void removeUuid(UUID uuid) {
    Server.getChannel().sendMessage(new ChannelDiscordMessage<>(Server.getName(),
        MessageType.Discord.DISCONNECT_MEMBER, uuid));
  }

  private UUID canJoinTo(UUID uuid, Collection<UUID> uuids) {
    User user = Server.getUser(uuid);

    if (user == null) {
      return null;
    }

    for (UUID memberUuid : uuids) {
      if (this.canJoinTo(user, memberUuid)) {
        return memberUuid;
      }
    }
    return null;
  }

  private boolean canJoinTo(User user, UUID toUuid) {
    User member = Server.getUser(toUuid);
    if (member == null || member.equals(user)) {
      return false;
    }

    Location userLocation = user.getLocation();
    Location memberLocation = member.getLocation();

    if (!userLocation.getWorld().equals(memberLocation.getWorld())) {
      return false;
    }

    double xDiff = userLocation.getX() - memberLocation.getX();
    double yDiff = userLocation.getY() - memberLocation.getY();
    double zDiff = userLocation.getZ() - memberLocation.getZ();

    if (xDiff * xDiff + zDiff * zDiff <= HORIZONTAL_DISTANCE * HORIZONTAL_DISTANCE) {
      return Math.abs(yDiff) <= VERTICAL_DISTANCE;
    }

    return false;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    this.logger.info((enabled ? "Enabled" : "Disabled") + " discord voice channels");
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void onGameUserJoin(GameUser user) {
    if (user.isInGame()) {
      this.actionsByUuid.put(user.getUniqueId(), Action.ADD);
    }
  }

  @Override
  public void onGameUserQuit(GameUser user) {
    if (user.isInGame()) {
      this.actionsByUuid.put(user.getUniqueId(), Action.REMOVE);
    }
  }

  @Override
  public void onSpectatorUserJoin(SpectatorUser user) {
    if (this.isEnabled()) {
      LinkedHashMap<String, List<UUID>> uuidsByTeam = new LinkedHashMap<>();
      uuidsByTeam.put(DISCORD_SPECTATOR, List.of(user.getUniqueId()));
      Server.getChannel().sendMessage(new ChannelDiscordMessage<>(Server.getName(),
          MessageType.Discord.MOVE_MEMBERS, new ChannelDiscordMessage.Allocation(uuidsByTeam)));
    }
  }

  @Override
  public void onSpectatorUserQuit(SpectatorUser user) {

  }

  @Override
  public int getPriority() {
    return 5;
  }

  enum Action {
    ADD,
    REMOVE,
    MOVE
  }

  private class DistanceChannel extends HashSet<UUID> {

    private final String name;
    private boolean closed = false;

    public DistanceChannel(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public UUID canJoin(UUID uuid) {
      if (this.isClosed()) {
        return null;
      }

      return DiscordManager.this.canJoinTo(uuid, this);
    }

    @Override
    public void clear() {
      super.clear();
      this.closed = true;
    }

    @Override
    public boolean add(UUID uuid) {
      DistanceChannel previous = DiscordManager.this.channelByUuid.get(uuid);
      if (previous != null && !previous.equals(this)) {
        previous.remove(uuid);
      }
      DiscordManager.this.channelByUuid.put(uuid, this);
      return super.add(uuid);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends UUID> c) {
      boolean modified = false;
      for (UUID uuid : c) {
        if (add(uuid)) {
          modified = true;
        }
      }
      return modified;
    }

    @Override
    public boolean remove(Object o) {
      boolean res = super.remove(o);
      if (this.isEmpty()) {
        this.closed = true;
      }
      return res;
    }

    public boolean isClosed() {
      return closed;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof DistanceChannel channel)) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      return Objects.equals(name, channel.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

}
