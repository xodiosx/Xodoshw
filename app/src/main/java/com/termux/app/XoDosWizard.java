package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import com.termux.R;
import java.io.File;
import java.io.FileWriter;

public class XoDosWizard {

    private final Activity mActivity;
    private static final String USR_PREFIX = "/data/data/com.termux/files/usr";
    private static final String HOME_PREFIX = "/data/data/com.termux/files/home";

    private String androidChoice;
    private String cpuChoice;
    private String wineChoice;
    private String dxvkChoice;
    private String driverChoice;
    private String primaryCores = null;
    private String secondaryCores = null;

    public XoDosWizard(Activity activity) {
        this.mActivity = activity;
    }

    public void start() {
        if (!hasShownPhantomKillerWarning()) {
            showPhantomKillerWarning();
        } else {
            detectAndroidVersion();
        }
    }

    private boolean hasShownPhantomKillerWarning() {
        SharedPreferences prefs = mActivity.getSharedPreferences("xodos_prefs", Activity.MODE_PRIVATE);
        return prefs.getBoolean("phantom_killer_warning_shown", false);
    }

    private void showPhantomKillerWarning() {
        new AlertDialog.Builder(mActivity)
            .setTitle("⚠️ Disable Phantom Process Killer")
            .setMessage("To ensure background processes work correctly, please disable Phantom Process Killer for Termux. You only need to do this once.\n\nDetails: https://github.com/xodiosx/XoDos/wiki/PhantomKiller")
            .setPositiveButton("OK", (dialog, which) -> {
                SharedPreferences.Editor editor = mActivity.getSharedPreferences("xodos_prefs", Activity.MODE_PRIVATE).edit();
                editor.putBoolean("phantom_killer_warning_shown", true);
                editor.apply();
                detectAndroidVersion();
            })
            .setCancelable(false)
            .show();
    }

    private void detectAndroidVersion() {
        int sdkInt = android.os.Build.VERSION.SDK_INT;
        androidChoice = (sdkInt >= 30) ? "above11" : "under11";

        if (sdkInt >= 30) {
            new AlertDialog.Builder(mActivity)
                .setTitle(mActivity.getString(R.string.modern_android_title))
                .setMessage(String.format(mActivity.getString(R.string.modern_android_message), 
                    "https://github.com/xodiosx/XoDos"))
                .setPositiveButton(mActivity.getString(R.string.ok), (dialog, which) -> askCpuType())
                .setCancelable(false)
                .show();
        } else {
            askCpuType();
        }
    }

    private void askCpuType() {
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.cpu_title))
            .setMessage(mActivity.getString(R.string.cpu_message))
            .setPositiveButton(mActivity.getString(R.string.snapdragon), (d, w) -> {
                cpuChoice = "snapdragon";
                askWineType();
            })
            .setNegativeButton(mActivity.getString(R.string.other), (d, w) -> {
                cpuChoice = "other";
                askWineType();
            })
            .setCancelable(false)
            .show();
    }

    private void askWineType() {
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.wine_title))
            .setMessage(mActivity.getString(R.string.wine_message))
            .setPositiveButton(mActivity.getString(R.string.glibc), (d, w) -> {
                wineChoice = "glibc";
                askDxvkVersion();
            })
            .setNegativeButton(mActivity.getString(R.string.bionic), (d, w) -> {
                wineChoice = "bionic";
                askDxvkVersion();
            })
            .setCancelable(false)
            .show();
    }

    private void askDxvkVersion() {
        File dxvkDir = new File(USR_PREFIX + "/glibc/opt/libs/d3d");
        String[] dxvkFiles = dxvkDir.list((dir, name) -> {
            boolean isD3DRequired = !"snapdragon".equalsIgnoreCase(cpuChoice) 
                                  && "glibc".equalsIgnoreCase(wineChoice);
            boolean is7z = name.toLowerCase().endsWith(".7z");
            boolean hasWineD3D = name.toLowerCase().contains("wined3d");
            return is7z && (isD3DRequired ? hasWineD3D : true);
        });

        if (dxvkFiles == null || dxvkFiles.length == 0) {
            showError(String.format(mActivity.getString(R.string.error_no_dxvk), dxvkDir.getAbsolutePath()));
            return;
        }

        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.dxvk_title))
            .setItems(dxvkFiles, (d, w) -> {
                dxvkChoice = dxvkFiles[w];
                askDriverVersion();
            })
            .setCancelable(false)
            .show();
    }

    private void askDriverVersion() {
        File driverDir = "glibc".equals(wineChoice)
            ? new File(USR_PREFIX + "/glibc/opt/libs/mesa")
            : new File(USR_PREFIX + "/drivers/25");

        String[] driverFiles = driverDir.list((d, name) -> {
            if (!name.endsWith(".7z")) return false;
            return !"other".equals(cpuChoice) || !name.toLowerCase().contains("turnip");
        });

        if (driverFiles == null || driverFiles.length == 0) {
            showError(String.format(mActivity.getString(R.string.error_no_driver), driverDir.getAbsolutePath()));
            return;
        }

        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.driver_title))
            .setItems(driverFiles, (d, w) -> {
                driverChoice = driverFiles[w];
                askCores();
            })
            .setCancelable(false)
            .show();
    }

    private void askCores() {
        String[] coreOptions = mActivity.getResources().getStringArray(R.array.core_options);
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.cores_title))
            .setItems(coreOptions, (dialog, which) -> {
                switch (which) {
                    case 0: primaryCores = null; secondaryCores = null; break;
                    case 1: primaryCores = "6-7"; secondaryCores = "0-5"; break;
                    case 2: primaryCores = "5-7"; secondaryCores = "0-4"; break;
                    case 3: primaryCores = "4-7"; secondaryCores = "0-3"; break;
                    case 4: primaryCores = "3-7"; secondaryCores = "0-2"; break;
                    case 5: primaryCores = "2-7"; secondaryCores = "0-1"; break;
                    case 6: primaryCores = "1-7"; secondaryCores = "0-1"; break;
                    case 7: primaryCores = "0-7"; secondaryCores = "0-1"; break;
                }
                saveConfig();
            })
            .setCancelable(false)
            .show();
    }

    private void saveConfig() {
        String dxvkSrc = USR_PREFIX + "/glibc/opt/libs/d3d/" + dxvkChoice;
        String driverSrc;
        String winePrefix;
        String dxvkExtractPath;
        String driverExtractPath;

        if ("glibc".equals(wineChoice)) {
            winePrefix = "$WINEPREFIX";
            dxvkExtractPath = winePrefix + "/drive_c/windows";
            driverSrc = USR_PREFIX + "/glibc/opt/libs/mesa/" + driverChoice;
            driverExtractPath = USR_PREFIX + "/glibc";
        } else {
            winePrefix = HOME_PREFIX + "/.wine";
            dxvkExtractPath = winePrefix + "/drive_c/windows";
            driverSrc = USR_PREFIX + "/drivers/25/" + driverChoice;
            driverExtractPath = USR_PREFIX + "/usr/drivers/mesa-242";
        }

        try {
            File cfg = new File(HOME_PREFIX + "/xodwine.cfg");
            File parent = cfg.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!cfg.exists()) cfg.createNewFile();

            FileWriter w = new FileWriter(cfg, false);

            w.write("ANDROID_VERSION=" + androidChoice + "\n");
            w.write("CPU=" + cpuChoice + "\n");
            w.write("WINE_TYPE=" + wineChoice + "\n");
            w.write("DXVK_FILE=" + dxvkChoice + "\n");
            w.write("DXVK_SRC=" + dxvkSrc + "\n");
            w.write("DXVK_EXTRACT_PATH=" + dxvkExtractPath + "\n");
            w.write("DRIVER_FILE=" + driverChoice + "\n");
            w.write("DRIVER_SRC=" + driverSrc + "\n");
            w.write("DRIVER_EXTRACT_PATH=" + driverExtractPath + "\n");
            w.write("WINEPREFIX=" + winePrefix + "\n");

            if (primaryCores == null || secondaryCores == null) {
                w.write("unset PRIMARY_CORES\n");
                w.write("unset SECONDARY_CORES\n");
            } else {
                w.write("PRIMARY_CORES=" + primaryCores + "\n");
                w.write("SECONDARY_CORES=" + secondaryCores + "\n");
            }

            if ("glibc".equals(wineChoice)) {
                w.write("SOURCE_CONFIG=1\n");
                w.write("CONFIG_SCRIPT=" + USR_PREFIX + "/glibc/opt/scripts/configs\n");
            } else {
                w.write("SOURCE_CONFIG=0\n");
            }

            w.close();

            new AlertDialog.Builder(mActivity)
                .setTitle(mActivity.getString(R.string.wizard_complete_title))
                .setMessage(String.format(mActivity.getString(R.string.wizard_complete_message), HOME_PREFIX))
                .setPositiveButton(mActivity.getString(R.string.ok), null)
                .show();

        } catch (Exception e) {
            showError(String.format(mActivity.getString(R.string.error_save_config), e.getMessage()));
        }
    }

    private void showError(String msg) {
        new AlertDialog.Builder(mActivity)
            .setTitle(mActivity.getString(R.string.error_title))
            .setMessage(msg)
            .setPositiveButton(mActivity.getString(R.string.ok), null)
            .show();
    }
            }
