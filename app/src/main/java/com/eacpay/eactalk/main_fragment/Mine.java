package com.eacpay.eactalk.main_fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.eacpay.R;
import com.eacpay.databinding.FragmentMainMineBinding;
import com.eacpay.presenter.activities.settings.AboutActivity;
import com.eacpay.presenter.activities.settings.DisplayCurrencyActivity;
import com.eacpay.presenter.activities.settings.ImportActivity;
import com.eacpay.presenter.activities.settings.NodesActivity;
import com.eacpay.presenter.activities.settings.SecurityCenterActivity;
import com.eacpay.presenter.activities.settings.WipeActivity;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.manager.TxManager;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRCurrency;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.wallet.BRWalletManager;

import java.math.BigDecimal;

public class Mine extends Fragment implements BRWalletManager.OnBalanceChanged {
    private static final String TAG = "oldfeel";
    FragmentMainMineBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainMineBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        binding.mineResetAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(WipeActivity.class);
            }
        });

        binding.mineImportKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(ImportActivity.class);
            }
        });

        binding.mineSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showSendFragment(getActivity(), null);
            }
        });

        binding.mineReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showReceiveFragment(getActivity(), true);
            }
        });

        binding.mineFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        binding.mineCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(DisplayCurrencyActivity.class);
            }
        });

        binding.mineNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(NodesActivity.class);
            }
        });

        binding.mineInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://www.eactalk.com/");
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        binding.mineSafe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(SecurityCenterActivity.class);
            }
        });

        binding.mineHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        binding.mineCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://github.com/vcexnet/eactalk");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        binding.mineAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(AboutActivity.class);
            }
        });

        binding.mineSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://www.eactalk.com/");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });

        BRWalletManager.getInstance().addBalanceChangedListener(this);
        updateUI();
    }

    @Override
    public void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        }, 1000);
    }

    public void updateUI() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) {
                    return;
                }
                Thread.currentThread().setName(Thread.currentThread().getName() + ":updateUI");
                //sleep a little in order to make sure all the commits are finished (like SharePreferences commits)
                String iso = BRSharedPrefs.getIso(getActivity());

                //current amount in satoshis
                final BigDecimal amount = new BigDecimal(BRSharedPrefs.getCatchedBalance(getActivity()));

                //amount in BTC units
                BigDecimal btcAmount = BRExchange.getBitcoinForSatoshis(getActivity(), amount);
                final String formattedBTCAmount = BRCurrency.getFormattedCurrencyString(getActivity(), "EAC", btcAmount);

                //amount in currency units
                BigDecimal curAmount = BRExchange.getAmountFromSatoshis(getActivity(), iso, amount);
                final String formattedCurAmount = BRCurrency.getFormattedCurrencyString(getActivity(), iso, curAmount);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.mineBalance.setText(formattedBTCAmount);
                        binding.mineBalanceLocal.setText(String.format("%s", formattedCurAmount));
                    }
                });
                Log.e(TAG, "run: updateTxList by mine updateUI");
                TxManager.getInstance().updateTxList(getActivity());
            }
        });
    }

    private void openActivity(Class<?> c) {
        Intent intent = new Intent(getActivity(), c);
        startActivity(intent);

        getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onBalanceChanged(long balance) {
        updateUI();
    }
}
