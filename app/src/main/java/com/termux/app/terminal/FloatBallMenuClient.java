package com.termux.app.terminal;

//////////
import android.os.Handler;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.app.AlertDialog;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import com.termux.R;
import android.app.Dialog;
import android.widget.TextView;
import android.widget.Button;
import android.graphics.Color;
import com.termux.floatball.menu.MenuLayout;
import android.view.ViewGroup;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.floatball.FloatBallManager;
import com.termux.floatball.menu.FloatMenuCfg;
import com.termux.floatball.menu.MenuItem;
import com.termux.floatball.permission.FloatPermissionManager;
import com.termux.floatball.utils.DensityUtil;
import com.termux.floatball.widget.FloatBallCfg;

public class FloatBallMenuClient {
///////
private SharedPreferences prefs;
private static final String PREFS_NAME = "FloatBallPrefs";
private static final String PREF_TUTORIAL_SHOWN = "tutorialShown";

    private FloatBallManager mFloatballManager;
    private FloatPermissionManager mFloatPermissionManager;
    private ActivityLifeCycleListener mActivityLifeCycleListener = new ActivityLifeCycleListener();
    private int resumed;
    private TermuxActivity mTermuxActivity;
    private boolean mAppNotOnFront = false;
    private boolean mShowKeyboard = false;
    private boolean mLockSlider = false;
    private boolean mShowTerminal = false;
    private boolean mShowPreference = false;

private boolean isActivityValid() {
    return mTermuxActivity != null && !mTermuxActivity.isFinishing() && !mTermuxActivity.isDestroyed();
}

    private FloatBallMenuClient() {
    }

    public FloatBallMenuClient(TermuxActivity termuxActivity) {
        mTermuxActivity = termuxActivity;
    }

    public void onCreate() {
        // Add these lines FIRST
    SharedPreferences prefs = mTermuxActivity.getSharedPreferences("FloatBallPrefs", Context.MODE_PRIVATE);
boolean tutorialShown = prefs.getBoolean("tutorialShown", false);

        init();
        mFloatballManager.show();
    // Add this after initializing mFloatballManager
    if (!tutorialShown) {
    new Handler().postDelayed(() -> {
    if (isActivityValid()) {
        startFloatMenuTutorial();
    }
}, 1000);
}
        //5 set float ball click handler
        if (mFloatballManager.getMenuItemSize() == 0) {
            toast(mTermuxActivity.getString(R.string.add_menu_item));
        } else {
            mFloatballManager.setOnFloatBallClickListener(() -> {
                if (mAppNotOnFront) {
                    PackageManager packageManager = mTermuxActivity.getPackageManager();
                    Intent intent = packageManager.getLaunchIntentForPackage("com.termux");
                    if (intent != null) {
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        mTermuxActivity.startActivity(intent);
                        toast(mTermuxActivity.getString(R.string.raise_termux_app));
                    }
                }
            });
        }
        //     6 if only float ball within app, register it to Application(out data, actually, it is enough within activity )
        mTermuxActivity.getApplication().registerActivityLifecycleCallbacks(mActivityLifeCycleListener);
    }

    public void onAttachedToWindow() {
        try {
            mFloatballManager.show();
            mFloatballManager.onFloatBallClick();
        } catch (RuntimeException e) {
            e.printStackTrace();
            toast(mTermuxActivity.getString(R.string.apply_display_over_other_app_permission));
        }

    }

    public void onDetachedFromWindow() {
        mFloatballManager.hide();
    }

    private void init() {
//      1 set position of float ball, set size, icon and drawable
        int ballSize = DensityUtil.dip2px(mTermuxActivity, 40);
        Drawable ballIcon = AppCompatResources.getDrawable(mTermuxActivity, R.drawable.icon_float_ball_shape);
//      different config below
//      FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon);
//      FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.LEFT_CENTER,false);
//      FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.LEFT_BOTTOM, -100);
//      FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.RIGHT_TOP, 100);
        FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.RIGHT_CENTER);
//      set float ball weather hide
        ballCfg.setHideHalfLater(true);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mTermuxActivity);
        boolean floatBallOverOtherApp = preferences.getBoolean("enableGlobalFloatBallMenu", false);
        Context ctx = mTermuxActivity;
        if (floatBallOverOtherApp) {
            ctx = mTermuxActivity.getApplicationContext();
        }
        //2 display float ball menu
        //2.1 init float ball menu config, every size of menu item and number of item
        int menuSize = DensityUtil.dip2px(mTermuxActivity, 110);
        int menuItemSize = DensityUtil.dip2px(mTermuxActivity, 20);
        FloatMenuCfg menuCfg = new FloatMenuCfg(menuSize, menuItemSize);
        //3 create float ball Manager
        mFloatballManager = new FloatBallManager(ctx, ballCfg, menuCfg);
        addFloatMenuItem();
        mFloatballManager.setFloatBallOverOtherApp(floatBallOverOtherApp);
        if (floatBallOverOtherApp) {
            setFloatPermission();
        }
    }

    private void setFloatPermission() {
        // set 'display over other app' permission of float bal menu
        //once permission, float ball never show
        mFloatPermissionManager = new FloatPermissionManager();
        mFloatballManager.setPermission(new FloatBallManager.IFloatBallPermission() {
            @Override
            public boolean onRequestFloatBallPermission() {
                requestFloatBallPermission(mTermuxActivity);
                return true;
            }

            @Override
            public boolean hasFloatBallPermission(Context context) {
                return mFloatPermissionManager.checkPermission(context);
            }

            @Override
            public void requestFloatBallPermission(Activity activity) {
                mFloatPermissionManager.applyPermission(activity);
            }

        });
    }

    public void setTerminalShow(boolean showTerminal) {
        mShowTerminal = showTerminal;
    }

    public void setShowPreference(boolean showPreference) {
        mShowPreference = showPreference;
    }

    public class ActivityLifeCycleListener implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            ++resumed;
            setFloatBallVisible(true);
        }

        @Override
        public void onActivityPaused(Activity activity) {
            --resumed;
            if (!isApplicationInForeground()) {
                setFloatBallVisible(false);
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    }

    private void toast(String msg) {
        Toast.makeText(mTermuxActivity, msg, Toast.LENGTH_SHORT).show();
    }

    private void addFloatMenuItem() {
        MenuItem terminalItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_start_terminal_shape)) {
            @Override
            public void action() {
            mTermuxActivity.runOnUiThread(() -> {
            
             }); 
                boolean preState = mShowTerminal;
                mShowTerminal = !mShowTerminal;
                if (!preState) {
                    mTermuxActivity.getMainContentView().setTerminalViewSwitchSlider(true);
                    toast(mTermuxActivity.getString(R.string.open_terminal));
                } else {
                    mTermuxActivity.getMainContentView().setTerminalViewSwitchSlider(false);
                    toast(mTermuxActivity.getString(R.string.hide_terminal));
                }
                mFloatballManager.closeMenu();
            }
        };
        MenuItem stopItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_kill_current_process_shape)) {
            @Override
            public void action() {
                mTermuxActivity.stopDesktop();
                toast(mTermuxActivity.getString(R.string.terminate_current_process));
                mFloatballManager.closeMenu();
            }
        };
        MenuItem gamePadItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_game_pad_shape)) {
            @Override
            public void action() {
                if (!mTermuxActivity.getTouchShow()) {
                    mTermuxActivity.showInputControlsDialog();
                    toast(mTermuxActivity.getString(com.termux.x11.R.string.open_controller));
                } else {
                    mTermuxActivity.hideInputControls();
                    toast(mTermuxActivity.getString(com.termux.x11.R.string.close_controller));
                }
                mFloatballManager.closeMenu();
            }
        };
        MenuItem unLockLayoutItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_unlock_layout_shape)) {
            @Override
            public void action() {
                if (mLockSlider) {
                    mDrawable = mTermuxActivity.getDrawable(R.drawable.icon_menu_unlock_layout_open_shape);
                } else {
                    mDrawable = mTermuxActivity.getDrawable(R.drawable.icon_menu_unlock_layout_shape);
                }
                mLockSlider = !mLockSlider;
                mTermuxActivity.getMainContentView().releaseSlider(true);
                toast(mTermuxActivity.getString(R.string.unlock_layout));
                mFloatballManager.closeMenu();
            }
        };
        MenuItem keyboardItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_show_keyboard_shape)) {
            @Override
            public void action() {
                if (mShowKeyboard) {
                    mDrawable = mTermuxActivity.getDrawable(R.drawable.icon_menu_show_keyboard_open_shape);
                } else {
                    mDrawable = mTermuxActivity.getDrawable(R.drawable.icon_menu_show_keyboard_shape);
                }
                mShowKeyboard = !mShowKeyboard;
                mTermuxActivity.openSoftKeyboardWithBackKeyPressed(mShowKeyboard);
                if(mShowKeyboard) {
                    toast(mTermuxActivity.getString(com.termux.x11.R.string.start_keyboard_x11));
                }else{
                    toast(mTermuxActivity.getString(com.termux.x11.R.string.stop_keyboard_x11));
                }
                mFloatballManager.closeMenu();
            }
        };
        MenuItem taskManagerItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_show_task_manager_shape)) {
            @Override
            public void action() {
                mTermuxActivity.showProcessManagerDialog();
                toast(mTermuxActivity.getString(com.termux.x11.R.string.task_manager));
                mFloatballManager.closeMenu();
            }
        };
        MenuItem settingItem = new MenuItem(mTermuxActivity.getDrawable(R.drawable.icon_menu_show_setting_shape)) {
            @Override
            public void action() {
                boolean preState = mShowPreference;
                mShowPreference = !mShowPreference;
                if (!preState) {
                    mTermuxActivity.getMainContentView().setX11PreferenceSwitchSlider(true);
                    toast(mTermuxActivity.getString(com.termux.x11.R.string.open_x11_settings));
                } else {
                    mTermuxActivity.getMainContentView().setX11PreferenceSwitchSlider(false);
                    toast(mTermuxActivity.getString(com.termux.x11.R.string.hide_x11_settings));
                }
                mFloatballManager.closeMenu();
            }
        };
        mFloatballManager.addMenuItem(terminalItem)
            .addMenuItem(stopItem)
            .addMenuItem(keyboardItem)
            .addMenuItem(gamePadItem)
            .addMenuItem(unLockLayoutItem)
            .addMenuItem(taskManagerItem)
            .addMenuItem(settingItem)
            .buildMenu();
    }

    private void setFloatBallVisible(boolean visible) {
        if (visible) {
//            mFloatballManager.show();
            mAppNotOnFront = false;
        } else {
//            mFloatballManager.hide();
            mAppNotOnFront = true;
        }
    }

    public boolean isApplicationInForeground() {
        return resumed > 0;
    }

    public void onDestroy() {
        onDetachedFromWindow();
        new Handler().removeCallbacksAndMessages(null);
        //unregister ActivityLifeCycle listener once register it, in case of memory leak
        mTermuxActivity.getApplication().unregisterActivityLifecycleCallbacks(mActivityLifeCycleListener);
    }

    public boolean isGlobalFloatBallMenu() {
        return mFloatballManager.isFloatBallOverOtherApp();
    }                    
 ////////
 private void startFloatMenuTutorial() {
 if (isActivityValid()) {
 mFloatballManager.getFloatMenu().setKeepOpen(true);
    mFloatballManager.keepMenuOpenDuringTutorial();
 mFloatballManager.onFloatBallClick();
}
 

 //   String[] steps = mTermuxActivity.getResources().getStringArray(R.array.float_tutorial_steps);
//   int[] itemPositions = {0, 1, 2, 3, 4, 5, 6, 7};
  // With:
   

//    final int[] step = {0};


  
        final Dialog dialog = new Dialog(mTermuxActivity);
dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
dialog.setCancelable(false);
dialog.setContentView(R.layout.tutorial_dialog);

// Access views inside the dialog
TextView text = dialog.findViewById(R.id.tutorial_text);
Button next = dialog.findViewById(R.id.tutorial_next);

String[] steps = mTermuxActivity.getResources().getStringArray(R.array.float_tutorial_steps);
int[] itemPositions = {0, 1, 2, 3, 4, 5, 6}; // Match your actual button count

final int[] step = {0};

// Prevent float menu from closing
mFloatballManager.getFloatMenu().setKeepOpen(true);
mFloatballManager.onFloatBallClick(); // Force open the menu

// Setup click handler
next.setOnClickListener(v -> {
    step[0]++;
    if (step[0] < steps.length) {
        text.setText(steps[step[0]]);
        highlightMenuItem(itemPositions[step[0]]);

        if (step[0] == steps.length - 1) {
            next.setText(R.string.got_it);
        }
    } else {
        mFloatballManager.getFloatMenu().setKeepOpen(false);
        mFloatballManager.closeMenu();
        dialog.dismiss();

        mTermuxActivity.getSharedPreferences("FloatBallPrefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("tutorialShown", true)
            .apply();
    }
});

// --- Set window properties to prevent stealing focus ---
Window window = dialog.getWindow();
if (window != null) {
    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

    WindowManager.LayoutParams params = window.getAttributes();
    params.gravity = Gravity.TOP;
    params.y = 150;
    window.setAttributes(params);
}

// --- Show dialog and highlight first button ---
new Handler().postDelayed(() -> {
    if (mTermuxActivity != null && !mTermuxActivity.isFinishing() && !mTermuxActivity.isDestroyed()) {
        dialog.show();
        highlightMenuItem(itemPositions[0]);
    }
}, 300); // slight delay to ensure float menu is ready
   
}
          private void highlightMenuItem(int position) {
    try {
        if (mFloatballManager == null || 
            mFloatballManager.getFloatMenu() == null ||
            mFloatballManager.getFloatMenu().getMenuLayout() == null) return;
            
        ViewGroup menuLayout = mFloatballManager.getFloatMenu().getMenuLayout();
        
        if (position >= 0 && position < menuLayout.getChildCount()) {
            View item = menuLayout.getChildAt(position);
            item.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(300)
                .withEndAction(() -> {
                    if (item != null) {
                        item.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(300)
                            .start();
                    }
                })
                .start();
        }
    } catch (Exception e) {
        Toast.makeText(mTermuxActivity, "Tutorial config pointer error", Toast.LENGTH_SHORT).show();
    }
}                             
}
