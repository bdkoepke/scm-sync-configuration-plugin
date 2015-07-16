package hudson.plugins.test.utils;

import hudson.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PluginUtil {

    public static void loadPlugin(Plugin plugin)
            throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        Method loadMethod = Plugin.class.getDeclaredMethod("load");
        boolean loadMethodAccessibility = loadMethod.isAccessible();
        loadMethod.setAccessible(true);
        loadMethod.invoke(plugin);
        loadMethod.setAccessible(loadMethodAccessibility);
    }
}
