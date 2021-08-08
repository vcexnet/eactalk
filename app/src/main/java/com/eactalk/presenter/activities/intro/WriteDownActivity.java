package com.eactalk.presenter.activities.intro;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.eactalk.R;
import com.eactalk.presenter.activities.util.BRActivity;
import com.eactalk.presenter.interfaces.BRAuthCompletion;
import com.eactalk.tools.animation.BRAnimator;
import com.eactalk.tools.security.AuthManager;
import com.eactalk.tools.security.PostAuth;

public class WriteDownActivity extends BRActivity {
    private Button writeButton;
    private ImageButton close;
    public static boolean appVisible = false;
    private static WriteDownActivity app;

    public static WriteDownActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_down);

        writeButton = (Button) findViewById(R.id.button_write_down);
        close = (ImageButton) findViewById(R.id.close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });

        //TODO: all views are using the layout of this button. Views should be refactored without it
        // Hiding until layouts are built.
        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                AuthManager.getInstance().authPrompt(WriteDownActivity.this, null, getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        PostAuth.getInstance().onPhraseCheckAuth(WriteDownActivity.this, false);
                    }

                    @Override
                    public void onCancel() {

                    }
                });

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            close();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private void close() {
        BRAnimator.startBreadActivity(this, false);
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
        if (!isDestroyed()) finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
