package org.kitteh.nametags;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.tag.PlayerReceiveNameTagEvent;
import org.kitteh.tag.TagAPI;

@SuppressWarnings("unused")
public class NameTags extends JavaPlugin implements Listener {
	private static final String CONFIG_REFRESH = "refreshAutomatically";
	private static final String CONFIG_SET_DISPLAYNAME = "setDisplayName";
	private static final String CONFIG_SET_TABNAME = "setTabName";
	private static final String METADATA_NAME = "nametags.displayname";
	private File configFile;
	private int refreshTaskID;
	private boolean setDisplayName;
	private boolean setTabName;

	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if ((args.length > 0) && (args[0].equalsIgnoreCase("reload"))) {
			load();
			sender.sendMessage("Reloaded!");
		}
		return true;
	}

	public void onDisable() {
		for (Player player : getServer().getOnlinePlayers())
			if ((player != null) && (player.isOnline()))
				player.removeMetadata("nametags.displayname", this);
	}

	public void onEnable() {
		if (!getServer().getPluginManager().isPluginEnabled("TagAPI")) {
			getLogger()
					.severe("TagAPI required. Get it at http://dev.bukkit.org/server-mods/tag/");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		this.configFile = new File(getDataFolder(), "config.yml");
		load();
		getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		calculate(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onNameTag(PlayerReceiveNameTagEvent event) {
		String tag = getDisplay(event.getNamedPlayer());
		if (tag != null)
			event.setTag(tag);
	}

	private void calculate(Player player) {
		StringBuilder name = new StringBuilder();
		List<Color> colors = Arrays.asList(Color.values());
		Collections.shuffle(colors);
		for (Color color : colors) {
			if (player.hasPermission(color.getNode())) {
				name.append(color.getColor());
				break;
			}
		}
		List<Format> formats = Arrays.asList(Format.values());
		Collections.shuffle(formats);
		for (Format format : formats) {
			if (player.hasPermission(format.getNode())) {
				name.append(format.getColor());
				break;
			}
		}
		name.append(player.getName());
		if (name.length() > 16) {
			name.setLength(16);
		}
		String newName = name.toString();
		player.setMetadata("nametags.displayname", new FixedMetadataValue(this,
				newName));
		if (this.setDisplayName) {
			player.setDisplayName(newName);
		}
		if (this.setTabName)
			player.setPlayerListName(newName);
	}

	private String getDisplay(Player player) {
		for (MetadataValue value : player.getMetadata("nametags.displayname")) {
			if (value.getOwningPlugin().equals(this)) {
				return value.asString();
			}
		}
		return null;
	}

	private void load() {
		if (this.refreshTaskID != -1) {
			getServer().getScheduler().cancelTask(this.refreshTaskID);
			this.refreshTaskID = -1;
		}
		if (!this.configFile.exists()) {
			saveDefaultConfig();
		}
		if (!getConfig().contains("refreshAutomatically")) {
			getConfig().set("refreshAutomatically", Boolean.valueOf(false));
		}
		if (getConfig().getBoolean("refreshAutomatically", false)) {
			this.refreshTaskID = getServer().getScheduler()
					.scheduleSyncRepeatingTask(this, new Runnable() {
						public void run() {
							NameTags.this.playerRefresh();
						}
					}, 1200L, 1200L);
		}

		boolean newSetDisplayName = getConfig().getBoolean("setDisplayName",
				false);
		boolean forceDisplayName = (this.setDisplayName)
				&& (!newSetDisplayName);
		boolean newSetTabName = getConfig().getBoolean("setTabName", false);
		boolean forceTabName = (this.setTabName) && (!newSetTabName);
		if ((forceDisplayName) || (forceTabName)) {
			for (Player player : getServer().getOnlinePlayers()) {
				if (forceDisplayName) {
					player.setDisplayName(player.getName());
				}
				if (forceTabName) {
					player.setPlayerListName(player.getName());
				}
			}
		}
		this.setDisplayName = newSetDisplayName;
		this.setTabName = newSetTabName;
		playerRefresh();
	}

	private void playerRefresh() {
		for (Player player : getServer().getOnlinePlayers())
			if ((player != null) && (player.isOnline())) {
				String oldTag = getDisplay(player);
				calculate(player);
				String newTag = getDisplay(player);
				boolean one = (oldTag == null) && (newTag != null);
				boolean two = (oldTag != null) && (newTag == null);
				boolean three = (oldTag != null) && (newTag != null)
						&& (!oldTag.equals(newTag));
				if ((one) || (two) || (three))
					TagAPI.refreshPlayer(player);
			}
	}

	private static enum Format {
		bold, italic, magic, strikethrough, underline;

		private final ChatColor color;
		private final String node;

		private Format() {
			this.color = ChatColor.valueOf(name().toUpperCase());
			this.node = ("nametags.format." + name());
		}

		public ChatColor getColor() {
			return this.color;
		}

		public String getNode() {
			return this.node;
		}
	}

	private static enum Color {
		aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green, dark_purple, dark_red, gold, gray, green, light_purple, red, yellow;

		private final ChatColor color;
		private final String node;

		private Color() {
			this.color = ChatColor.valueOf(name().toUpperCase());
			this.node = ("nametags.color." + name());
		}

		public ChatColor getColor() {
			return this.color;
		}

		public String getNode() {
			return this.node;
		}
	}
}