package com.tanuch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FrendCommand implements CommandExecutor {
    private final FrendPlus plugin;
    private final Map<String, Set<String>> friendRequests = new HashMap<>();
    private final Map<String, Set<String>> friends = new HashMap<>();

    public FrendCommand(FrendPlus plugin) {
        this.plugin = plugin;
        loadFriendsFromFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        if (args.length < 1) {
            player.sendMessage(getMessage("usage_message"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!player.hasPermission("frend.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }

                // config.yml の再読み込み
                plugin.reloadConfig();

                // message.yml の再読み込み
                plugin.reloadMessageConfig();

                player.sendMessage(getMessage("reload_success"));
                break;

            case "add":
                if (args.length < 2) {
                    player.sendMessage(getMessage("specify_player"));
                    return true;
                }

                String targetName = args[1];
                Player targetPlayer = Bukkit.getPlayer(targetName);

                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    player.sendMessage(getMessage("player_offline").replace("%player%", targetName));
                    return true;
                }

                if (friends.getOrDefault(playerName, new HashSet<>()).contains(targetName)) {
                    player.sendMessage(getMessage("already_friend").replace("%player%", targetName));
                    return true;
                }

                if (friendRequests.getOrDefault(targetName, new HashSet<>()).contains(playerName)) {
                    player.sendMessage(getMessage("already_requested").replace("%player%", targetName));
                    return true;
                }

                friendRequests.computeIfAbsent(targetName, k -> new HashSet<>()).add(playerName);
                player.sendMessage(getMessage("request_sent").replace("%player%", targetName));
                targetPlayer.sendMessage(getMessage("received_request").replace("%player%", playerName));

                // 1分後にフレンド申請を自動削除
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Set<String> requests = friendRequests.get(targetName);
                    if (requests != null) {
                        requests.remove(playerName);
                        if (requests.isEmpty()) {
                            friendRequests.remove(targetName);
                        }
                    }
                }, 1200L); // 1分 = 1200 ticks
                break;

            case "accept":
                if (args.length < 2) {
                    player.sendMessage(getMessage("specify_player"));
                    return true;
                }

                String acceptPlayerName = args[1];
                if (!friendRequests.getOrDefault(playerName, new HashSet<>()).contains(acceptPlayerName)) {
                    player.sendMessage(getMessage("no_request_found").replace("%player%", acceptPlayerName));
                    return true;
                }

                friendRequests.get(playerName).remove(acceptPlayerName);
                friends.computeIfAbsent(playerName, k -> new HashSet<>()).add(acceptPlayerName);
                friends.computeIfAbsent(acceptPlayerName, k -> new HashSet<>()).add(playerName);

                player.sendMessage(getMessage("request_accepted").replace("%player%", acceptPlayerName));
                Player acceptPlayer = Bukkit.getPlayer(acceptPlayerName);
                if (acceptPlayer != null) {
                    acceptPlayer.sendMessage(getMessage("request_approved_by").replace("%player%", playerName));
                }
                saveFriendsToFile();
                break;

            case "deny":
                if (args.length < 2) {
                    player.sendMessage(getMessage("specify_player"));
                    return true;
                }

                String denyPlayerName = args[1];
                if (!friendRequests.getOrDefault(playerName, new HashSet<>()).contains(denyPlayerName)) {
                    player.sendMessage(getMessage("no_request_found").replace("%player%", denyPlayerName));
                    return true;
                }

                friendRequests.get(playerName).remove(denyPlayerName);
                player.sendMessage(getMessage("request_denied").replace("%player%", denyPlayerName));
                Player denyPlayer = Bukkit.getPlayer(denyPlayerName);
                if (denyPlayer != null) {
                    denyPlayer.sendMessage(getMessage("request_denied_by").replace("%player%", playerName));
                }
                break;

            case "list":
                Set<String> playerFriends = friends.getOrDefault(playerName, new HashSet<>());
                if (playerFriends.isEmpty()) {
                    player.sendMessage(getMessage("no_friends"));
                    return true;
                }

                StringBuilder friendList = new StringBuilder(getMessage("friend_list"));
                for (String friend : playerFriends) {
                    boolean isOnline = Bukkit.getPlayer(friend) != null && Bukkit.getPlayer(friend).isOnline();
                    friendList.append("\n").append(friend).append("&7 - ")
                            .append(isOnline ? getMessage("online") : getMessage("offline"));
                }
                player.sendMessage(friendList.toString());
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(getMessage("specify_player"));
                    return true;
                }

                String removeName = args[1];
                if (!friends.getOrDefault(playerName, new HashSet<>()).contains(removeName)) {
                    player.sendMessage(getMessage("not_friend").replace("%player%", removeName));
                    return true;
                }

                friends.get(playerName).remove(removeName);
                friends.get(removeName).remove(playerName);

                player.sendMessage(getMessage("friend_removed").replace("%player%", removeName));
                Player removePlayer = Bukkit.getPlayer(removeName);
                if (removePlayer != null) {
                    removePlayer.sendMessage(getMessage("friend_removed").replace("%player%", playerName));
                }
                saveFriendsToFile();
                break;

            default:
                player.sendMessage(getMessage("usage_message"));
                break;
        }

        return true;
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getMessageConfig().getString(key, "&cMissing message: " + key));
    }

    private void saveFriendsToFile() {
        try {
            File file = new File(plugin.getDataFolder(), "friends.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("friends", friends);
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not save friends file: " + e.getMessage());
        }
    }

    private void loadFriendsFromFile() {
        File file = new File(plugin.getDataFolder(), "friends.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Object> loadedFriends = config.getConfigurationSection("friends").getValues(false);
        for (Map.Entry<String, Object> entry : loadedFriends.entrySet()) {
            friends.put(entry.getKey(), new HashSet<>((List<String>) entry.getValue()));
        }
    }
}