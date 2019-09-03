/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.ContextProgressView;
import org.telegram.ui.Components.HintEditText;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SlideView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@SuppressLint("HardwareIds")
public class LoginActivity extends BaseFragment {

    private int currentViewNum;
    private SlideView[] views = new SlideView[1];
    private boolean newAccount;
    private boolean syncContacts = true;

    private int scrollHeight;

    private ActionBarMenuItem doneItem;
    private AnimatorSet doneItemAnimation;
    private ContextProgressView doneProgressView;
    private int progressRequestId;

    private final static int done_button = 1;

    LoginActivity() {
        super();
    }

    LoginActivity(int account) {
        super();
        currentAccount = account;
        newAccount = true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        for (SlideView view : views) {
            if (view != null) {
                view.onDestroyActivity();
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == done_button) {
                    if (doneProgressView.getTag() != null) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("StopLoading", R.string.StopLoading));
                        builder.setPositiveButton(LocaleController.getString("WaitMore", R.string.WaitMore), null);
                        builder.setNegativeButton(LocaleController.getString("Stop", R.string.Stop), (dialogInterface, i) -> {
                            views[currentViewNum].onCancelPressed();
                            needHideProgress(true);
                        });
                        showDialog(builder.create());
                    } else {
                        views[currentViewNum].onNextPressed();
                    }
                } else if (id == -1) {
                    onBackPressed();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        actionBar.setAllowOverlayTitle(true);
        doneItem = menu.addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));
        doneProgressView = new ContextProgressView(context, 1);
        doneProgressView.setAlpha(0.0f);
        doneProgressView.setScaleX(0.1f);
        doneProgressView.setScaleY(0.1f);
        doneProgressView.setVisibility(View.INVISIBLE);
        doneItem.addView(doneProgressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        ScrollView scrollView = new ScrollView(context) {
            @Override
            public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
                if (currentViewNum == 1 || currentViewNum == 2 || currentViewNum == 4) {
                    rectangle.bottom += AndroidUtilities.dp(40);
                }
                return super.requestChildRectangleOnScreen(child, rectangle, immediate);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                scrollHeight = MeasureSpec.getSize(heightMeasureSpec) - AndroidUtilities.dp(30);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        scrollView.setFillViewport(true);
        fragmentView = scrollView;

        FrameLayout frameLayout = new FrameLayout(context);
        scrollView.addView(frameLayout, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        views[0] = new PhoneView(context);

        for (int a = 0; a < views.length; a++) {
            views[a].setVisibility(a == 0 ? View.VISIBLE : View.GONE);
            frameLayout.addView(views[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, AndroidUtilities.isTablet() ? 26 : 18, 30, AndroidUtilities.isTablet() ? 26 : 18, 0));
        }

        Bundle savedInstanceState = loadCurrentState();
        if (savedInstanceState != null) {
            currentViewNum = savedInstanceState.getInt("currentViewNum", 0);
            syncContacts = savedInstanceState.getInt("syncContacts", 1) == 1;
            if (currentViewNum >= 1 && currentViewNum <= 4) {
                int time = savedInstanceState.getInt("open");
                if (time != 0 && Math.abs(System.currentTimeMillis() / 1000 - time) >= 24 * 60 * 60) {
                    currentViewNum = 0;
                    savedInstanceState = null;
                    clearCurrentState();
                }
            }
        }
        for (int a = 0; a < views.length; a++) {
            if (savedInstanceState != null) {
                if (a >= 1 && a <= 4) {
                    if (a == currentViewNum) {
                        views[a].restoreStateParams(savedInstanceState);
                    }
                } else {
                    views[a].restoreStateParams(savedInstanceState);
                }
            }
            if (currentViewNum == a) {
                actionBar.setBackButtonImage(views[a].needBackButton() || newAccount ? R.drawable.ic_ab_back : 0);
                views[a].setVisibility(View.VISIBLE);
                views[a].onShow();
                if (a == 3 || a == 8) {
                    doneItem.setVisibility(View.GONE);
                }
            } else {
                views[a].setVisibility(View.GONE);
            }
        }
        actionBar.setTitle(views[currentViewNum].getHeaderName());

        return fragmentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);
        if (newAccount) {
            ConnectionsManager.getInstance(currentAccount).setAppPaused(true, false);
        }
    }

    private Bundle loadCurrentState() {
        if (newAccount) {
            return null;
        }
        try {
            Bundle bundle = new Bundle();
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("logininfo2", Context.MODE_PRIVATE);
            Map<String, ?> params = preferences.getAll();
            for (Map.Entry<String, ?> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String[] args = key.split("_\\|_");
                if (args.length == 1) {
                    if (value instanceof String) {
                        bundle.putString(key, (String) value);
                    } else if (value instanceof Integer) {
                        bundle.putInt(key, (Integer) value);
                    }
                } else if (args.length == 2) {
                    Bundle inner = bundle.getBundle(args[0]);
                    if (inner == null) {
                        inner = new Bundle();
                        bundle.putBundle(args[0], inner);
                    }
                    if (value instanceof String) {
                        inner.putString(args[1], (String) value);
                    } else if (value instanceof Integer) {
                        inner.putInt(args[1], (Integer) value);
                    }
                }
            }
            return bundle;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    private void clearCurrentState() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("logininfo2", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    private void putBundleToEditor(Bundle bundle, SharedPreferences.Editor editor, String prefix) {
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Object obj = bundle.get(key);
            if (obj instanceof String) {
                if (prefix != null) {
                    editor.putString(prefix + "_|_" + key, (String) obj);
                } else {
                    editor.putString(key, (String) obj);
                }
            } else if (obj instanceof Integer) {
                if (prefix != null) {
                    editor.putInt(prefix + "_|_" + key, (Integer) obj);
                } else {
                    editor.putInt(key, (Integer) obj);
                }
            } else if (obj instanceof Bundle) {
                putBundleToEditor((Bundle) obj, editor, key);
            }
        }
    }


    private void needShowAlert(String title, String text) {
        if (text == null || getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setMessage(text);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        showDialog(builder.create());
    }

    private void showEditDoneProgress(final boolean show) {
        if (doneItemAnimation != null) {
            doneItemAnimation.cancel();
        }
        doneItemAnimation = new AnimatorSet();
        if (show) {
            doneProgressView.setTag(1);
            doneProgressView.setVisibility(View.VISIBLE);
            doneItemAnimation.playTogether(
                    ObjectAnimator.ofFloat(doneItem.getContentView(), "scaleX", 0.1f),
                    ObjectAnimator.ofFloat(doneItem.getContentView(), "scaleY", 0.1f),
                    ObjectAnimator.ofFloat(doneItem.getContentView(), "alpha", 0.0f),
                    ObjectAnimator.ofFloat(doneProgressView, "scaleX", 1.0f),
                    ObjectAnimator.ofFloat(doneProgressView, "scaleY", 1.0f),
                    ObjectAnimator.ofFloat(doneProgressView, "alpha", 1.0f));
        } else {
            doneProgressView.setTag(null);
            doneItem.getContentView().setVisibility(View.VISIBLE);
            doneItemAnimation.playTogether(
                    ObjectAnimator.ofFloat(doneProgressView, "scaleX", 0.1f),
                    ObjectAnimator.ofFloat(doneProgressView, "scaleY", 0.1f),
                    ObjectAnimator.ofFloat(doneProgressView, "alpha", 0.0f),
                    ObjectAnimator.ofFloat(doneItem.getContentView(), "scaleX", 1.0f),
                    ObjectAnimator.ofFloat(doneItem.getContentView(), "scaleY", 1.0f),
                    ObjectAnimator.ofFloat(doneItem.getContentView(), "alpha", 1.0f));
        }
        doneItemAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (doneItemAnimation != null && doneItemAnimation.equals(animation)) {
                    if (!show) {
                        doneProgressView.setVisibility(View.INVISIBLE);
                    } else {
                        doneItem.getContentView().setVisibility(View.INVISIBLE);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (doneItemAnimation != null && doneItemAnimation.equals(animation)) {
                    doneItemAnimation = null;
                }
            }
        });
        doneItemAnimation.setDuration(150);
        doneItemAnimation.start();
    }


    private void needShowProgress(final int reqiestId) {
        progressRequestId = reqiestId;
        showEditDoneProgress(true);
    }

    public void needHideProgress(boolean cancel) {
        if (progressRequestId != 0) {
            if (cancel) {
                ConnectionsManager.getInstance(currentAccount).cancelRequest(progressRequestId, true);
            }
            progressRequestId = 0;
        }
        showEditDoneProgress(false);
    }

    public void setPage(int page, boolean animated, Bundle params, boolean back) {
        doneItem.setVisibility(View.VISIBLE);

        if (animated) {
            final SlideView outView = views[currentViewNum];
            final SlideView newView = views[page];
            currentViewNum = page;
            actionBar.setBackButtonImage(newView.needBackButton() || newAccount ? R.drawable.ic_ab_back : 0);

            newView.setParams(params, false);
            actionBar.setTitle(newView.getHeaderName());
            newView.onShow();
            newView.setX(back ? -AndroidUtilities.displaySize.x : AndroidUtilities.displaySize.x);
            newView.setVisibility(View.VISIBLE);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    outView.setVisibility(View.GONE);
                    outView.setX(0);
                }
            });
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(outView, View.TRANSLATION_X, back ? AndroidUtilities.displaySize.x : -AndroidUtilities.displaySize.x),
                    ObjectAnimator.ofFloat(newView, View.TRANSLATION_X, 0));
            animatorSet.setDuration(300);
            animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
            animatorSet.start();
        } else {
            actionBar.setBackButtonImage(views[page].needBackButton() || newAccount ? R.drawable.ic_ab_back : 0);
            views[currentViewNum].setVisibility(View.GONE);
            currentViewNum = page;
            views[page].setParams(params, false);
            views[page].setVisibility(View.VISIBLE);
            actionBar.setTitle(views[page].getHeaderName());
            views[page].onShow();
        }
    }

    @Override
    public void saveSelfArgs(Bundle outState) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("currentViewNum", currentViewNum);
            bundle.putInt("syncContacts", syncContacts ? 1 : 0);
            for (int a = 0; a <= currentViewNum; a++) {
                SlideView v = views[a];
                if (v != null) {
                    v.saveStateParams(bundle);
                }
            }
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("logininfo2", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            putBundleToEditor(bundle, editor, null);
            editor.apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void needFinishActivity() {
        clearCurrentState();
        if (getParentActivity() instanceof LaunchActivity) {
            if (newAccount) {
                newAccount = false;
                ((LaunchActivity) getParentActivity()).switchToAccount(currentAccount, false);
                finishFragment();
            } else {
                presentFragment(new DialogsActivity(null), true);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.mainUserInfoChanged);
            }
        } else if (getParentActivity() instanceof ExternalActionActivity) {
            ((ExternalActionActivity) getParentActivity()).onFinishLogin();
        }
    }

    private void onAuthSuccess(TLRPC.TL_auth_authorization res, boolean diff) {
        ConnectionsManager.getInstance(currentAccount).setUserId(res.user.id);
        UserConfig.getInstance(currentAccount).clearConfig();
        MessagesController.getInstance(currentAccount).cleanup();
        UserConfig.getInstance(currentAccount).syncContacts = syncContacts;
        UserConfig.getInstance(currentAccount).setCurrentUser(res.user);
        UserConfig.getInstance(currentAccount).saveConfig(true);
        MessagesStorage.getInstance(currentAccount).cleanup(true);
        ArrayList<TLRPC.User> users = new ArrayList<>();
        users.add(res.user);
        MessagesStorage.getInstance(currentAccount).putUsersAndChats(users, null, true, true);
        MessagesController.getInstance(currentAccount).putUser(res.user, false);
        ContactsController.getInstance(currentAccount).checkAppAccount();
        MessagesController.getInstance(currentAccount).getBlockedUsers(true);
        MessagesController.getInstance(currentAccount).checkProxyInfo(true);
        ConnectionsManager.getInstance(currentAccount).updateDcSettings();

        if (diff) {
            MessagesStorage.getInstance(currentAccount).setLastPtsValue(1);
            MessagesStorage.getInstance(currentAccount).setLastDateValue(1);
            MessagesStorage.getInstance(currentAccount).setLastQtsValue(0);
        }

        needFinishActivity();
    }

    public class PhoneView extends SlideView {

        private HintEditText phoneField;
        private View view;
        private boolean nextPressed = false;

        @SuppressLint({"SetTextI18n", "RtlHardcoded"})
        public PhoneView(Context context) {
            super(context);

            setOrientation(VERTICAL);

            view = new View(context);
            view.setPadding(AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12), 0);
            view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayLine));
            addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 4, -17.5f, 4, 0));

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(HORIZONTAL);
            addView(linearLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 20, 0, 0));

            phoneField = new HintEditText(context) {
                @Override
                public boolean onTouchEvent(MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!AndroidUtilities.showKeyboard(this)) {
                            clearFocus();
                            requestFocus();
                        }
                    }
                    return super.onTouchEvent(event);
                }
            };
            phoneField.setInputType(InputType.TYPE_CLASS_TEXT);
            phoneField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            phoneField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
            phoneField.setBackgroundDrawable(Theme.createEditTextDrawable(context, false));
            phoneField.setPadding(0, 0, 0, 0);
            phoneField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            phoneField.setMaxLines(1);
            phoneField.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            phoneField.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            phoneField.setHint("TOKEN");

            linearLayout.addView(phoneField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36));

            phoneField.setOnEditorActionListener((textView, i, keyEvent) -> {
                if (i == EditorInfo.IME_ACTION_NEXT) {
                    onNextPressed();
                    return true;
                }
                return false;
            });
        }

        @Override
        public void onNextPressed() {
            if (getParentActivity() == null || nextPressed) {
                return;
            }

            ConnectionsManager.getInstance(currentAccount).cleanup(false);
            final TLRPC.TL_auth_importBotAuthorization req = new TLRPC.TL_auth_importBotAuthorization();

            String phone = phoneField.getText().toString();

            boolean diff = phone.endsWith(":f");

            if (diff) {
                phone = phone.substring(0, phone.length() - 2);
            }

            req.api_hash = BuildVars.APP_HASH;
            req.api_id = BuildVars.APP_ID;
            req.bot_auth_token = phone;
            req.flags = 0;

            final Bundle params = new Bundle();
            params.putString("phone", phone);

            try {
                params.putString("ephone", phone);
            } catch (Exception e) {
                FileLog.e(e);
                params.putString("ephone", "+" + phone);
            }
            params.putString("phoneFormated", phone);
            nextPressed = true;
            int reqId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                nextPressed = false;
                if (error == null) {
                    onAuthSuccess((TLRPC.TL_auth_authorization) response, !diff);
                } else {
                    if (error.text != null) {
                        needShowAlert("ERROR", error.text);
                    }
                }
                needHideProgress(false);
            }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagTryDifferentDc | ConnectionsManager.RequestFlagEnableUnauthorized);
            needShowProgress(reqId);
        }

        @Override
        public String getHeaderName() {
            return "PWRTelegram";
        }

        @Override
        public void saveStateParams(Bundle bundle) {
            String code = "";
            if (code.length() != 0) {
                bundle.putString("phoneview_code", code);
            }
            String phone = phoneField.getText().toString();
            if (phone.length() != 0) {
                bundle.putString("phoneview_phone", phone);
            }
        }
    }


    @Override
    public ThemeDescription[] getThemeDescriptions() {
        PhoneView phoneView = (PhoneView) views[0];

        ArrayList<ThemeDescription> arrayList = new ArrayList<>();

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        arrayList.add(new ThemeDescription(phoneView.view, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhiteGrayLine));
        arrayList.add(new ThemeDescription(phoneView.phoneField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(phoneView.phoneField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        arrayList.add(new ThemeDescription(phoneView.phoneField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        arrayList.add(new ThemeDescription(phoneView.phoneField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));


        return arrayList.toArray(new ThemeDescription[arrayList.size()]);
    }
}
