/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.data;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlauncher.App;
import com.atlauncher.FileSystem;
import com.atlauncher.Update;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.SettingsManager;
import com.atlauncher.utils.Hashing;

public enum OS {
    LINUX, WINDOWS, OSX;

    public static OS getOS() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("mac")) {
            return OS.OSX;
        } else {
            return OS.LINUX;
        }
    }

    public static String getName() {
        return System.getProperty("os.name");
    }

    public static String getVersion() {
        return System.getProperty("os.version");
    }

    public static boolean isWindows() {
        return getOS() == WINDOWS;
    }

    public static boolean isMac() {
        return getOS() == OSX;
    }

    public static boolean isLinux() {
        return getOS() == LINUX;
    }

    public static Path storagePath() {
        switch (getOS()) {
            case WINDOWS:
                return Paths.get(System.getenv("APPDATA")).resolve("." + Constants.LAUNCHER_NAME.toLowerCase());
            case OSX:
                return Paths.get(System.getProperty("user.home")).resolve("Library").resolve("Application Support")
                        .resolve("." + Constants.LAUNCHER_NAME.toLowerCase());
            default:
                return Paths.get(System.getProperty("user.home")).resolve("." + Constants.LAUNCHER_NAME.toLowerCase());
        }
    }

    public static boolean isUsingMacApp() {
        return OS.isMac() && Files.exists(FileSystem.BASE_DIR.getParent().resolve("MacOS"));
    }

    public static void openWebBrowser(String url) {
        try {
            OS.openWebBrowser(new URI(url));
        } catch (Exception e) {
            LogManager.logStackTrace("Error opening web browser!", e);
        }
    }

    public static void openWebBrowser(URL url) {
        try {
            OS.openWebBrowser(url.toURI());
        } catch (URISyntaxException e) {
            LogManager.logStackTrace("Error opening web browser!", e);
        }
    }

    public static void openWebBrowser(URI uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else if (getOS() == LINUX && (Files.exists(Paths.get("/usr/bin/xdg-open")) || Files.exists(Paths.get
                    ("/usr/local/bin/xdg-open")))) {
                Runtime.getRuntime().exec("xdg-open " + uri);
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error opening web browser!", e);
        }
    }

    public static void openFileExplorer(Path path) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
            } else if (getOS() == LINUX && (Files.exists(Paths.get("/usr/bin/xdg-open")) || Files.exists(Paths.get
                    ("/usr/local/bin/xdg-open")))) {
                Runtime.getRuntime().exec("xdg-open " + path.toString());
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error opening file explorer!", e);
        }
    }

    /**
     * Gets the java home.
     *
     * @return the java home
     */
    public static String getJavaHome() {
        return System.getProperty("java.home");
    }

    /**
     * Checks if is 64 bit.
     *
     * @return true, if is 64 bit
     */
    public static boolean is64Bit() {
        return System.getProperty("sun.arch.data.model").contains("64");
    }

    /**
     * Checks if Windows is 64 bit
     *
     * @return true, if it is 64 bit
     */
    public static boolean isWindows64Bit(){
    	return System.getenv("ProgramFiles(x86)") != null;
    }

    /**
     * Gets the arch.
     *
     * @return the arch
     */
    public static String getArch() {
        if (is64Bit()) {
            return "64";
        } else {
            return "32";
        }
    }

    /**
     * Gets the memory options.
     *
     * @return the memory options
     */
    public static String[] getMemoryOptions() {
        int options = getMaximumRam() / 512;
        int ramLeft = 0;
        int count = 0;
        String[] ramOptions = new String[options];
        while ((ramLeft + 512) <= getMaximumRam()) {
            ramLeft = ramLeft + 512;
            ramOptions[count] = ramLeft + " MB";
            count++;
        }
        return ramOptions;
    }

    /**
     * Returns the amount of RAM in the users system.
     *
     * @return The amount of RAM in the system
     */
    public static int getSystemRam() {
        long ramm = 0;
        int ram = 0;
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        try {
            Method m = operatingSystemMXBean.getClass().getDeclaredMethod("getTotalPhysicalMemorySize");
            m.setAccessible(true);
            Object value = m.invoke(operatingSystemMXBean);
            if (value != null) {
                ramm = Long.parseLong(value.toString());
                ram = (int) (ramm / 1048576);
            } else {
                ram = 1024;
            }
        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException |
                IllegalAccessException e) {
            LogManager.logStackTrace(e);
        }
        return ram;
    }

    /**
     * Returns the maximum RAM available to Java. If on 64 Bit system then its all of the System RAM otherwise its
     * limited to 1GB or less due to allocations of PermGen
     *
     * @return The maximum RAM available to Java
     */
    public static int getMaximumRam() {
        int maxRam = getSystemRam();
        if (!is64Bit()) {
            if (maxRam < 1024) {
                return maxRam;
            } else {
                return 1024;
            }
        } else {
            return maxRam;
        }
    }

    public static int getMaximumSafeRam() {
        if (!is64Bit()) {
            return 1024;
        }

        int halfRam = (getMaximumRam() / 1000) * 512;

        return (halfRam >= 4096 ? 4096 : halfRam);
    }

    /**
     * Returns the safe amount of maximum ram available to Java. This is set to half of the total maximum ram available
     * to Java in order to not allocate too much and leave enough RAM for the OS and other application
     *
     * @return Half the maximum RAM available to Java
     */
    public static int getSafeMaximumRam() {
        int maxRam = getSystemRam();
        if (!is64Bit()) {
            if (maxRam < 1024) {
                return maxRam / 2;
            } else {
                return 512;
            }
        } else {
            return maxRam / 2;
        }
    }

    /**
     * Gets the maximum window width.
     *
     * @return the maximum window width
     */
    public static int getMaximumWindowWidth() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dim = toolkit.getScreenSize();
        return dim.width;
    }

    /**
     * Gets the maximum window height.
     *
     * @return the maximum window height
     */
    public static int getMaximumWindowHeight() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dim = toolkit.getScreenSize();
        return dim.height;
    }

    /**
     * Get the Java version that the launcher runs on.
     *
     * @return the Java version that the launcher runs on
     */
    public static String getLauncherJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * Get the Java version used to run Minecraft.
     *
     * @return the Java version used to run Minecraft
     */
    public static String getMinecraftJavaVersion() {
        if (SettingsManager.isUsingCustomJavaPath()) {
            File folder = new File(SettingsManager.getJavaPath(), "bin/");
            String javaCommand = folder + File.separator + "java" + (isWindows() ? ".exe" : "");

            ProcessBuilder processBuilder = new ProcessBuilder(javaCommand, "-version");
            processBuilder.directory(folder);
            processBuilder.redirectErrorStream(true);

            String version = "Unknown";

            try {
                Process process = processBuilder.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = null;
                    Pattern p = Pattern.compile("java version \"([^\"]*)\"");

                    while ((line = br.readLine()) != null) {
                        // Extract version information
                        Matcher m = p.matcher(line);

                        if (m.find()) {
                            version = m.group(1);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                LogManager.logStackTrace(e);
            }

            LogManager.warn("Cannot get Java version from the ouput of \"" + javaCommand + "\" -version");

            return version;
        } else {
            return getLauncherJavaVersion();
        }
    }

    /**
     * Parse a Java version string and get the major version number. For example "1.8.0_91" is parsed to 8.
     *
     * @param version the version string to parse
     * @return the parsed major version number
     */
    public static int parseJavaVersionNumber(String version) {
        Matcher m = Pattern.compile("(?:1\\.)?([0-9]+).*").matcher(version);

        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /**
     * Get the major Java version that the launcher runs on.
     *
     * @return the major Java version that the launcher runs on
     */
    public static int getLauncherJavaVersionNumber() {
        return parseJavaVersionNumber(getLauncherJavaVersion());
    }

    /**
     * Get the major Java version used to run Minecraft.
     *
     * @return the major Java version used to run Minecraft
     */
    public static int getMinecraftJavaVersionNumber() {
        return parseJavaVersionNumber(getMinecraftJavaVersion());
    }

    /**
     * Get the Java versions used by the Launcher and Minecraft as a string.
     *
     * @return the Java versions used by the Launcher and Minecraft as a string
     */
    public static String getActualJavaVersion() {
        return String.format("Launcher: Java %d (%s), Minecraft: Java %d (%s)",
            getLauncherJavaVersionNumber(), getLauncherJavaVersion(),
            getMinecraftJavaVersionNumber(), getMinecraftJavaVersion()
        );
    }

    public static boolean isValidJavaPath(String path) {
        return Files.exists(Paths.get(path).resolve("bin").resolve("java" + (isWindows() ? ".exe" : "")));
    }

    /**
     * Checks if the user is using Java 7 or above.
     *
     * @return true if the user is using Java 7 or above else false
     */
    public static boolean isJava7OrAbove(boolean checkCustomPath) {
        int version = checkCustomPath ? getMinecraftJavaVersionNumber() : getLauncherJavaVersionNumber();
        return version >= 7 || version == -1;
    }

    /**
     * Checks if the user is using exactly Java 8.
     *
     * @return true if the user is using exactly Java 8
     */
    public static boolean isJava8() {
        return getMinecraftJavaVersionNumber() == 8;
    }

    /**
     * Checks if the user is using exactly Java 9.
     *
     * @return true if the user is using exactly Java 9
     */
    public static boolean isJava9() {
        return getMinecraftJavaVersionNumber() == 9;
    }

	/**
     * Checks whether Metaspace should be used instead of PermGen. This is the case for Java 8 and above.
     *
     * @return whether Metaspace should be used instead of PermGen
     */
    public static boolean useMetaspace() {
        return getMinecraftJavaVersionNumber() >= 8;
    }

    public static String getMACAdressHash() {
        String returnStr = null;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            // If network is null, user may be using Linux or something it doesn't support so try alternative way
            if (network == null) {
                Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();

                while (e.hasMoreElements()) {
                    NetworkInterface n = e.nextElement();
                    Enumeration<InetAddress> ee = n.getInetAddresses();
                    while (ee.hasMoreElements()) {
                        InetAddress i = ee.nextElement();
                        if (!i.isLoopbackAddress() && !i.isLinkLocalAddress() && i.isSiteLocalAddress()) {
                            ip = i;
                        }
                    }
                }

                network = NetworkInterface.getByInetAddress(ip);
            }

            // If network is still null, well you're SOL
            if (network != null) {
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    returnStr = sb.toString();
                }
            }
        } catch (Exception e) {
            LogManager.logStackTrace(e);
        } finally {
            returnStr = (returnStr == null ? "NotARandomKeyYes" : returnStr);
        }

        return Hashing.md5(returnStr).toString();
    }

    /**
     * Credit to https://github.com/Slowpoke101/FTBLaunch/blob/master/src/main/java/net/ftb/workers/AuthlibDLWorker.java
     */
    public static boolean addToClasspath(Path path) {
        LogManager.info("Loading external library " + path.getFileName() + " to classpath!");

        try {
            if (Files.exists(path)) {
                addURL(path.toUri().toURL());
            } else {
                LogManager.error("Error loading " + path + " to classpath as it doesn't exist!");
                return false;
            }
        } catch (Throwable t) {
            if (t.getMessage() != null) {
                LogManager.error(t.getMessage());
            }

            return false;
        }

        return true;
    }

    public static boolean checkAuthLibLoaded() {
        try {
            Class.forName("com.mojang.authlib.exceptions.AuthenticationException");
            Class.forName("com.mojang.authlib.Agent");
            Class.forName("com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService");
            Class.forName("com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication");
        } catch (ClassNotFoundException e) {
            LogManager.logStackTrace(e);
            return false;
        }

        return true;
    }

    /**
     * Credit to https://github.com/Slowpoke101/FTBLaunch/blob/master/src/main/java/net/ftb/workers/AuthlibDLWorker.java
     */
    public static void addURL(URL u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) App.class.getClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(sysloader, u);
        } catch (Throwable t) {
            if (t.getMessage() != null) {
                LogManager.error(t.getMessage());
            }
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    public static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    public static void restartLauncher() {
        File thisFile = new File(Update.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String path = null;
        try {
            path = thisFile.getCanonicalPath();
            path = URLDecoder.decode(path, "UTF-8");
        } catch (IOException e) {
            LogManager.logStackTrace(e);
        }

        List<String> arguments = new ArrayList<>();

        if (isUsingMacApp()) {
            arguments.add("open");
            arguments.add("-n");
            arguments.add(FileSystem.BASE_DIR.getParent().getParent().toString());
        } else {
            String jpath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            if (isWindows()) {
                jpath += "w";
            }
            arguments.add(jpath);
            arguments.add("-jar");
            arguments.add(path);
        }

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(arguments);

        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void copyToClipboard(String data) {
        StringSelection text = new StringSelection(data);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(text, null);
    }
}
