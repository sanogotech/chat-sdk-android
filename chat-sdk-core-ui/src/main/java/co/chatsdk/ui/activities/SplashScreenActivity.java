package co.chatsdk.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import butterknife.BindView;
import sdk.chat.core.hook.Hook;
import sdk.chat.core.hook.HookEvent;
import sdk.chat.core.session.ChatSDK;
import co.chatsdk.ui.R;
import co.chatsdk.ui.R2;
import sdk.guru.common.RX;

public class SplashScreenActivity extends BaseActivity {

    public static int AUTH = 1;

    @BindView(R2.id.imageView) protected ImageView imageView;
    @BindView(R2.id.progressBar) protected ProgressBar progressBar;
    @BindView(R2.id.root) protected ConstraintLayout root;

    @Override
    protected @LayoutRes int getLayout() {
        return R.layout.activity_splash_screen;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageView.setImageResource(ChatSDK.config().logoDrawableResourceID);

        stopProgressBar();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        startNextActivity();
    }

    protected void startNextActivity() {
        if (ChatSDK.auth().isAuthenticatedThisSession()) {
            startMainActivity();
        } else if (ChatSDK.auth().isAuthenticated() || ChatSDK.auth().isAuthenticating()) {
            startProgressBar();
            dm.add(ChatSDK.auth().authenticate()
                    .observeOn(RX.main())
                    .doFinally(this::stopProgressBar)
                    .subscribe(this::startMainActivity, throwable -> startLoginActivity()));
        } else {
            startLoginActivity();
        }
    }

    protected void startMainActivity() {
        ChatSDK.ui().startMainActivity(this, extras);
    }

    protected void startLoginActivity() {
        startActivityForResult(ChatSDK.ui().getLoginIntent(this, extras), AUTH);
    }

    protected void startProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        progressBar.animate();
    }

    protected void stopProgressBar() {
//        progressBar.setVisibility(View.GONE);
        progressBar.setProgress(0);
        progressBar.setIndeterminate(false);
    }

}