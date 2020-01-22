package dev.brighten.pl.handlers;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.utils.MiscUtils;
import cc.funkemunky.api.utils.RunUtils;
import cc.funkemunky.api.utils.Tuple;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.data.UserData;
import dev.brighten.pl.listeners.impl.VPNCheckEvent;
import dev.brighten.pl.utils.Config;
import dev.brighten.pl.utils.StringUtils;
import dev.brighten.pl.vpn.VPNResponse;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class VPNHandler {
    private Deque<Tuple<UUID, String>> queue = new LinkedBlockingDeque<>();
    public Map<UUID, String> toKick = new HashMap<>();

    public void run() {
        AntiVPN.INSTANCE.vpnAPI.vpnThread.execute(() -> {
            Tuple<UUID, String> element;
            while(queue.size() > 0 && (element = queue.poll()) != null) {
                val response = AntiVPN.INSTANCE.vpnAPI.getResponse(element.two);
                if(response != null && response.isSuccess()) {
                    UserData data = UserData.getData(element.one);
                    data.response = response;
                    VPNCheckEvent event = new VPNCheckEvent(response);
                    if(Config.fireEvent)
                        RunUtils.task(() -> Bukkit.getPluginManager().callEvent(event), AntiVPN.INSTANCE);

                    if(response.isProxy()) {
                        if(Config.alertStaff) alert(response, element.one);
                        if(Config.kickPlayers) kick(response, element.one);
                    }
                } else MiscUtils.printToConsole((response != null) + "?");
            }
            run();
        });
    }
    private void alert(VPNResponse response, UUID uuid) {
        if(Config.alertBungee) {
            AntiVPN.INSTANCE.alertsHandler.sendBungeeAlert(uuid, response); //Empty method until Atlas v1.7 releases.
        } else {
            AntiVPN.INSTANCE.alertsHandler.sendAlert(uuid, response);
        }
    }

    private void kick(VPNResponse response, UUID uuid) {
        if(Config.kickBungee) {
            Atlas.getInstance().getBungeeManager().getBungeeAPI()
                    .kickPlayer(uuid, StringUtils.formatString(Config.kickMessage, response));
        } else {
            Player player = Bukkit.getPlayer(uuid);

            RunUtils.task(() -> {
                String message = StringUtils.formatString(Config.kickMessage, response);
                if(player != null)
                    player.kickPlayer(message);
                else toKick.put(uuid, message);
            });
        }
    }

    public void checkPlayer(Player player) {
        AntiVPN.INSTANCE.vpnAPI.vpnThread.execute(() -> {
            val element = new Tuple<>(player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
            val response = AntiVPN.INSTANCE.vpnAPI.getResponse(element.two);
            if(response != null && response.isSuccess()) {
                UserData data = UserData.getData(element.one);
                data.response = response;
                VPNCheckEvent event = new VPNCheckEvent(response);
                if(Config.fireEvent)
                    RunUtils.task(() -> Bukkit.getPluginManager().callEvent(event), AntiVPN.INSTANCE);

                if(response.isProxy()) {
                    if(Config.alertStaff) alert(response, element.one);
                    if(Config.kickPlayers) kick(response, element.one);
                }
            } else {
                if(response == null) {
                    if(Config.alertStaff) alert(response, element.one);
                    if(Config.kickPlayers) kick(response, element.one);
                }
            }
        });
    }
}
