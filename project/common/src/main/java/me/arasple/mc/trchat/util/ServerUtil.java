package me.arasple.mc.trchat.util;

public class ServerUtil {

    public static boolean isModdedServer = false;

    static {
        try {
            Class.forName("catserver.server.CatServerLaunch");
            isModdedServer = true;
        } catch (Throwable ignored) {
        }
        try {
            Class.forName("com.mohistmc.MohistMC");
            isModdedServer = true;
        } catch (Throwable ignored) {
        }
    }
}
