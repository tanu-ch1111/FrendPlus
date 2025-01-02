package com.tanuch;

import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class FrendManager {

    private List<String> friends = new ArrayList<>();

    // フレンド追加
    public boolean addFriend(String playerName) {
        if (!friends.contains(playerName)) {
            friends.add(playerName);
            return true;
        }
        return false;
    }

    // フレンド削除
    public boolean removeFriend(String playerName) {
        return friends.remove(playerName);
    }

    // フレンド一覧表示
    public List<String> getFriends() {
        return friends;
    }
    // プレイヤーがフレンドかどうか
    public boolean isFriend(String playerName) {
        return friends.contains(playerName);
    }
}