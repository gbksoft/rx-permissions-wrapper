package com.gbksoft.rxpermissionswrapper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.jakewharton.rxbinding.view.RxView;
import com.tbruyelle.rxpermissions.RxPermissions;

import rx.Subscription;


/**
 * Class which can implement permission request on click event or activity/fragment appearance
 */
public class Permissions {

    private static void showPermissionDenidedSnackbar(View root, int denidedText) {
        Context context = root.getContext();
        Snackbar snack = Snackbar.make(root,
                denidedText,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.gbkrxwb_settings, v -> {
                    Intent myAppSettings = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.getPackageName()));
                    myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                    myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(myAppSettings);
                });
        TextView snackTextView = (TextView) snack.getView()
                .findViewById(android.support.design.R.id.snackbar_text);
        snackTextView.setMaxLines(10);
        snack.show();
    }

    /**
     * Permission wrapper builder
     */
    public static class RxWrapBuilder {

        private View view;
        private View rootView;
        private int denidedText;
        private int rationaleText;
        private IOnGrantedCallback callback;
        private IOnGrantedDeclinedCallback callbackActivity;
        private String permission;
        private RxPermissions rxPermissions;


        /**
         * Call this method to set instance of {@link RxPermissions} which handles permissions requests
         * @param rxPermissions {@link RxPermissions}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder withRxPermissions(RxPermissions rxPermissions) {
            this.rxPermissions = rxPermissions;
            return this;
        }

        /**
         * Sets view instance (Button etc), click event of which will be used to request permission
         * @param view {@link View}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder onView(View view) {
            this.view = view;
            return this;
        }

        /**
         * Set root view to show {@link Snackbar} with denided text
         * @param rootView {@link View}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder withRootView(View rootView) {
            this.rootView = rootView;
            return this;
        }

        /**
         * Set text resource to show if permission request was denided
         * @param denidedText {@link Integer}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder withDenidedText(int denidedText) {
            this.denidedText = denidedText;
            return this;
        }

        /**
         * Set text resource to show if permission rationale need to be shown
         * @param rationaleText {@link Integer}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder withRationaleText(int rationaleText) {
            this.rationaleText = rationaleText;
            return this;
        }

        /**
         * Set object which will receive callback when permission will be granted
         * @param callback {@link IOnGrantedCallback}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder withCallback(IOnGrantedCallback callback) {
            this.callback = callback;
            return this;
        }

        /**
         * Set Activity or Fragment which will receive callback when permission will be granted or declined
         * @param callback {@link IOnGrantedDeclinedCallback}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder withCallbackActivity(IOnGrantedDeclinedCallback callback) {
            this.callbackActivity = callback;
            return this;
        }

        /**
         * Set permission to request
         * @param permission {@link String}
         * @return {@link RxWrapBuilder}
         */
        public RxWrapBuilder requestPermission(String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Call this method if you need to request permission on activity or fractent creation
         * @return {@link Subscription}
         *
         * <pre>
         * {@code
         * //Example:
         * new Permissions.RxWrapBuilder()
         *     .requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
         *     .withRootView(layout.getRoot())
         *     .withCallbackActivity(this)
         *     .withDenidedText(R.string.permission_write_storage_preview_denided)
         *     .withRationaleText(R.string.permission_write_storage_preview_rationale)
         *     .withRxPermissions(rxPermissions)
         *     .buildForActivity();
         * }
         * </pre>
         */
        public Subscription buildForActivity() {
            if (permission == null) {
                throw new IllegalStateException("permission is empty, set permission with .requestPermission()");
            }
            if (rootView == null) {
                throw new IllegalStateException("rootView is empty, set rootView with .withRootView()");
            }
            if (rxPermissions == null) {
                throw new IllegalStateException("rxPermissions is empty, set rootView with .withRxPermissions()");
            }
            if (callbackActivity == null) {
                throw new IllegalStateException("callbackActivity is empty, set callback with .withCallbackActivity()");
            }
            if (denidedText == 0) {
                throw new IllegalStateException("denidedText is empty, set text with .withDenidedText()");
            }
            if (rationaleText == 0) {
                throw new IllegalStateException("rationaleText is empty, set text with .withRationaleText()");
            }
            rxPermissions
                    .requestEach(permission)
                    .subscribe(result -> {
                        if (result.granted) {
                            callbackActivity.onPermissionGranted();
                        } else if (result.shouldShowRequestPermissionRationale) {
                            AlertDialog.Builder bld = new AlertDialog.Builder(rootView.getContext());
                            bld.setTitle(R.string.gbkrxwb_permission_request);
                            bld.setMessage(rationaleText);
                            bld.setPositiveButton(R.string.gbkrxwb_ok, (dialog, which) -> {
                                callbackActivity.onPermissionDeclined();
                                dialog.dismiss();
                            });
                            bld.create().show();
                        } else {
                            showPermissionDenidedSnackbar(rootView, denidedText);
                            callbackActivity.onPermissionDeclined();
                        }
                    });
            return null;
        }

        /**
         * Call this method if you need to request permission on activity or fractent creation
         * @return {@link Subscription}
         *
         * <pre>
         * {@code
         * //Example:
         * new Permissions.RxWrapBuilder()
         *     .requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
         *     .withView(button)
         *     .withRootView(layout.getRoot())
         *     .withCallback(this)
         *     .withDenidedText(R.string.permission_write_storage_preview_denided)
         *     .withRationaleText(R.string.permission_write_storage_preview_rationale)
         *     .withRxPermissions(rxPermissions)
         *     .build();
         * }
         * </pre>
         */

        public Subscription build() {
            if (permission == null) {
                throw new IllegalStateException("permission is empty, set permission with .requestPermission()");
            }
            if (view == null) {
                throw new IllegalStateException("view is empty, set view with .withView()");
            }
            if (rootView == null) {
                throw new IllegalStateException("rootView is empty, set rootView with .withRootView()");
            }
            if (rxPermissions == null) {
                throw new IllegalStateException("rxPermissions is empty, set rootView with .withRxPermissions()");
            }
            if (callback == null) {
                throw new IllegalStateException("callback is empty, set callback with .withCallback()");
            }
            if (denidedText == 0) {
                throw new IllegalStateException("denidedText is empty, set text with .withDenidedText()");
            }
            if (rationaleText == 0) {
                throw new IllegalStateException("rationaleText is empty, set text with .withRationaleText()");
            }


            return RxView.clicks(view)
                    .compose(rxPermissions.ensureEach(permission))
                    .subscribe(result -> {
                        if (result.granted) {
                            callback.onPermissionGranted();
                        } else if (result.shouldShowRequestPermissionRationale) {
                            AlertDialog.Builder bld =
                                    new AlertDialog.Builder(rootView.getContext());
                            bld.setTitle(R.string.gbkrxwb_permission_request);
                            bld.setMessage(rationaleText);
                            bld.setNegativeButton(R.string.gbkrxwb_deny, (dialog, which) -> {
                                showPermissionDenidedSnackbar(rootView, denidedText);
                            });
                            bld.setPositiveButton(R.string.gbkrxwb_allow, (dialog, which) -> {
                                view.callOnClick();
                            });
                            bld.create().show();
                        } else {
                            showPermissionDenidedSnackbar(rootView, denidedText);
                        }
                    });
        }
    }

    /**
     * Need to implement this interface while using {@link RxWrapBuilder#build()} call,
     * to receive permission granted callback
     */
    public interface IOnGrantedCallback {
        void onPermissionGranted();
    }

    /**
     * Need to implement this interface while using {@link RxWrapBuilder#buildForActivity()} call,
     * to receive permission granted or declined callbacks
     */
    public interface IOnGrantedDeclinedCallback extends IOnGrantedCallback {
        void onPermissionDeclined();
    }
}