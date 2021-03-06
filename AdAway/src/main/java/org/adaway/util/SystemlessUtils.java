package org.adaway.util;

import android.content.Context;

import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.Toolbox;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * This class provides methods to check, install and remove systemless script.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class SystemlessUtils {

    /**
     * Check if the systemless mode is supported.
     *
     * @param shell The shell used to check.
     * @return <code>true</code> if the systemless mode is supported, <code>false</code> otherwise.
     */
    public static boolean isSystemlessModeSupported(Shell shell) {
        try {
            // Check if SuperSU systemless root is installed
            Toolbox toolbox = new Toolbox(shell);
            return toolbox.fileExists("/su/bin/su");
        } catch (Exception exception) {
            Log.e(Constants.TAG, "Error while checking if systemless mode is supported.", exception);
            return false;
        }
    }

    /**
     * Check if systemless mode is enabled.
     *
     * @param shell The root shell used to check.
     * @return The systemless mode installation status (<code>true</code> if enabled,
     * <code>false</code> otherwise).
     */
    public static boolean isSystemlessModeEnabled(Shell shell) {
        try {
            // Look for mount point of system hosts file
            SimpleCommand command = new SimpleCommand("mount | grep " + Constants.ANDROID_SYSTEM_ETC_HOSTS);
            shell.add(command).waitForFinish();
            return command.getExitCode() == 0;
        } catch (Exception exception) {
            Log.e(Constants.TAG, "Error while checking if systemless mode is installed.", exception);
            // Consider systemless mode is not installed if script could not be checked
            return false;
        }
    }

    /**
     * Install systemless script.<br>
     * Create and execute<code>/su/su.d/0000adaway.script</code> file to mount hosts file to
     * <code>/su/etc/hosts</code> location and ensure mounted hosts file is present.<br>
     * Require SuperSU >= 2.56.
     *
     * @param context The application context (current activity).
     * @param shell   The current root shell to install script.
     * @return The script installation status (<code>true</code> if the systemless script is installed,
     * <code>false</code> otherwise).
     */
    public static boolean enableSystemlessMode(Context context, Shell shell) {
        try {
            Toolbox toolbox = new Toolbox(shell);
            // Ensure mounted hosts file exists
            if (!toolbox.fileExists(Constants.ANDROID_SU_ETC_HOSTS)) {
                // Copy current hosts file to mounted host file
                toolbox.copyFile(Constants.ANDROID_SYSTEM_ETC_HOSTS, Constants.ANDROID_SU_ETC_HOSTS, false, true);
            }
            // Check if systemless mode is already enabled
            if (!SystemlessUtils.isSystemlessModeEnabled(shell)) {
                // Create temp file
                File cacheDir = context.getCacheDir();
                File tempFile = File.createTempFile(Constants.TAG, ".script", cacheDir);
                // Write script content to temp file
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
                writer.write("mount -o bind " + Constants.ANDROID_SU_ETC_HOSTS + " " + Constants.ANDROID_SYSTEM_ETC_HOSTS + ";");
                writer.newLine();
                writer.close();
                // Copy temp file to /su partition
                toolbox.copyFile(tempFile.getAbsolutePath(), Constants.ANDROID_SYSTEMLESS_SCRIPT, false, false);
                // Apply script permissions
                toolbox.setFilePermissions(Constants.ANDROID_SYSTEMLESS_SCRIPT, "755");
                // Remove temp file
                if (!tempFile.delete()) {
                    Log.w(Constants.TAG, "Could not delete the temporary script file.");
                }
                // Execute script
                shell.add(new SimpleCommand(Constants.ANDROID_SYSTEMLESS_SCRIPT)).waitForFinish();
            }
            return true;
        } catch (Exception exception) {
            Log.e(Constants.TAG, "Error while enabling systemless mode.", exception);
            return false;
        }
    }

    /**
     * Remove systemless script.<br>
     * Remove <code>/su/su.d/0000adaway.script</code> and mounted hosts files.
     *
     * @param shell The current root shell to install script.
     * @return The script removal status (<code>true</code> if the systemless script is removed,
     * <code>false</code> otherwise).
     */
    public static boolean disableSystemlessMode(Shell shell) {
        try {
            // Check if systemless mode is enabled
            if (SystemlessUtils.isSystemlessModeEnabled(shell)) {
                // Remove systemless script
                SimpleCommand removeScriptCommand =
                        new SimpleCommand(Constants.COMMAND_RM + " " + Constants.ANDROID_SYSTEMLESS_SCRIPT);
                shell.add(removeScriptCommand).waitForFinish();
                // Try to umount hosts file (resource must be used: it requires reboot)
                SimpleCommand umountCommand =
                        new SimpleCommand("umount " + Constants.ANDROID_SYSTEM_ETC_HOSTS);
                shell.add(umountCommand).waitForFinish();
                // Remove mounted hosts file
                SimpleCommand removeMountedHostsCommand =
                        new SimpleCommand(Constants.COMMAND_RM + " " + Constants.ANDROID_SU_ETC_HOSTS);
                shell.add(removeMountedHostsCommand).waitForFinish();
            }
            return true;
        } catch (Exception exception) {
            Log.e(Constants.TAG, "Error while disabling systemless mode.", exception);
            return false;
        }
    }

    /**
     * This class provides systemless mode statuses (support and activation).
     *
     * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
     */
    public static class SystemlessModeStatus {
        /**
         * The systemless mode support status (<code>true</code> if supported, <code>false</code> otherwise).
         */
        private final boolean supported;
        /**
         * The systemless mode activation status (<code>true</code> if enabled, <code>false</code> otherwise).
         */
        private final boolean enabled;

        /**
         * Constructor.
         *
         * @param supported The systemless mode support status (<code>true</code> if supported, <code>false</code> otherwise).
         * @param enabled   The systemless mode activation status (<code>true</code> if enabled, <code>false</code> otherwise).
         */
        public SystemlessModeStatus(boolean supported, boolean enabled) {
            this.supported = supported;
            this.enabled = enabled;
        }

        /**
         * Get the systemless mode support status.
         *
         * @return The systemless mode support status (<code>true</code> if supported, <code>false</code> otherwise).
         */
        public boolean isSupported() {
            return this.supported;
        }

        /**
         * Get the systemless mode activation status.
         *
         * @return The systemless mode activation status (<code>true</code> if enabled, <code>false</code> otherwise).
         */
        public boolean isEnabled() {
            return this.enabled;
        }
    }
}
