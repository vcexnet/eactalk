package com.eacpay.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.eacpay.R;
import com.eacpay.presenter.activities.settings.SecurityCenterActivity;
import com.eacpay.presenter.activities.settings.SettingsActivity;
import com.eacpay.presenter.activities.settings.SupportActivity;
import com.eacpay.presenter.activities.settings.WebViewActivity;
import com.eacpay.presenter.entities.BRMenuItem;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.animation.SlideDetector;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.util.ArrayList;
import java.util.List;

public class FragmentMenu extends Fragment {

    public TextView mTitle;
    public ListView mListView;
    public RelativeLayout background;
    public List<BRMenuItem> itemList;
    public ConstraintLayout signalLayout;
    private ImageButton close;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_menu, container, false);
        background = rootView.findViewById(R.id.layout);
        signalLayout = rootView.findViewById(R.id.signal_layout);
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        close = rootView.findViewById(R.id.close_button);

        itemList = new ArrayList<>();
        boolean buyBitcoinEnabled = APIClient.getInstance(getActivity()).isFeatureEnabled(APIClient.FeatureFlags.BUY_BITCOIN.toString());
        if (buyBitcoinEnabled)
            itemList.add(new BRMenuItem(getString(R.string.MenuButton_buy), R.drawable.buy_bitcoin, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), WebViewActivity.class);
                    intent.putExtra(WebViewActivity.URL_EXTRA, HTTPServer.URL_BUY);
                    Activity app = getActivity();
                    app.startActivity(intent);
                    app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);

                }
            }));
        itemList.add(new BRMenuItem(getString(R.string.MenuButton_security), R.drawable.ic_shield, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                Intent intent = new Intent(app, SecurityCenterActivity.class);
                app.startActivity(intent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
            }
        }));
        //TODO: Refactor with new FAQ / Support design using on-board FAQ data
        itemList.add(new BRMenuItem(getString(R.string.MenuButton_supportCenter),
                R.drawable.faq_question_black,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), SupportActivity.class);
                        Activity app = getActivity();
                        app.startActivity(intent);
                        app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                    }
                }));
        itemList.add(new BRMenuItem(getString(R.string.MenuButton_settings), R.drawable.ic_settings, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                Activity app = getActivity();
                app.startActivity(intent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
            }
        }));

        itemList.add(new BRMenuItem(getString(R.string.MenuButton_lock),
                R.drawable.ic_lock, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity from = getActivity();
                from.getFragmentManager().popBackStack();
                BRAnimator.startMainActivity(from, true);
            }
        }));

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                app.getFragmentManager().popBackStack();
            }
        });
        mTitle = rootView.findViewById(R.id.title);
        mListView = rootView.findViewById(R.id.menu_listview);
        mListView.setAdapter(new MenuListAdapter(getContext(), R.layout.menu_list_item, itemList));
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(this);
                }
                BRAnimator.animateBackgroundDim(background, false);
                BRAnimator.animateSignalSlide(signalLayout, false, null);
            }
        });
    }

    public class MenuListAdapter extends ArrayAdapter<BRMenuItem> {

        private final LayoutInflater mInflater;

        public MenuListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRMenuItem> items) {
            super(context, resource, items);
            mInflater = ((Activity) context).getLayoutInflater();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.menu_list_item, parent, false);
            }
            TextView text = convertView.findViewById(R.id.item_text);
            ImageView icon = convertView.findViewById(R.id.item_icon);

            final BRMenuItem item = getItem(position);
            text.setText(item.text);
            icon.setImageResource(item.resId);
            convertView.setOnClickListener(item.listener);
            return convertView;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(background, true);
        BRAnimator.animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
//                if (getActivity() != null)
//                    getActivity().getFragmentManager().popBackStack();
            }
        });

    }
}