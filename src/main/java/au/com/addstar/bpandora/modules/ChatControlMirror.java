package au.com.addstar.bpandora.modules;

import au.com.addstar.bpandora.MasterPlugin;
import au.com.addstar.bpandora.Module;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.YamlConfig;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ChatControlMirror implements Module, Listener
{
	private static Plugin plugin;
	private static ProxyServer proxy;
	private static Logger log;
	private static ChatControlMirrorConfig config;
	private static List<String> validServers;

	@Override
	public void onEnable()
	{
		proxy = ProxyServer.getInstance();
		config = new ChatControlMirrorConfig();
		try {
			File conffile = new File(plugin.getDataFolder(), "chatcontrolmirror.yml");
			config.init(conffile);
		} catch (Exception e) {
			log.warning("[ChatControlMirror] Unable to load config: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		validServers = Arrays.asList(config.mirrorServers.split(","));
		plugin.getProxy().registerChannel(config.inChannel);
	}

	@Override
	public void onDisable()
	{
		plugin.getProxy().unregisterChannel(config.inChannel);
	}

	@Override
	public void setPandoraInstance( MasterPlugin plugin )
	{
		this.plugin = plugin;
		log = plugin.getLogger();
	}

	@EventHandler
	public void onPluginMessage(PluginMessageEvent event) {
		if (config.inChannel.equals(event.getTag())) {
			try {
				ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
				String type = in.readUTF();
				String channel = in.readUTF();
				String msg = in.readUTF();
				doChatControlMirror(type, channel, msg);
			} catch (Exception e) {
				log.warning("[ChatControlMirror] Error reading message: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
	}

	public static boolean doChatControlMirror(String type, String channel, String message) {
		Server server = null;

		// Search for a valid server with a player online
		for (ProxiedPlayer p : MasterPlugin.getInstance().getProxy().getPlayers()) {
			if (validServers.contains(p.getServer().getInfo().getName())) {
				// Found one!
				server = p.getServer();
				break;
			}
		}

		if (server != null) {
			try {
				ByteArrayDataOutput out = ByteStreams.newDataOutput();
				out.writeUTF(type);
				out.writeUTF(channel);
				out.writeUTF(message);
				server.sendData(config.outChannel, out.toByteArray());
			} catch (Exception e) {
				log.warning("[ChatControlMirror] Error sending message: " + e.getLocalizedMessage());
				e.printStackTrace();
			}
			return true;
		} else {
			log.warning("[ChatControlMirror] No valid server found to relay mirror message");
			return false;
		}
	}

	public static class ChatControlMirrorConfig extends YamlConfig {
		@Comment("Plugin message channel to listen on for mirror events")
		public String inChannel = "bpandora:chatcontrolmirror";

		@Comment("Relay messages to a server on this channel")
		public String outChannel = "pandora:chatcontrolmirror";

		@Comment("Relay messages to a server on this channel")
		public String mirrorServers = "hub,survival,hardcore,skyblock2,limbo";
	}
}
