package com.eacpay.eactalk.fragment.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.eacpay.R;
import com.eacpay.databinding.FragmentMainMineBinding;
import com.eacpay.eactalk.About;
import com.eacpay.eactalk.ApiSettings;
import com.eacpay.eactalk.Code;
import com.eacpay.eactalk.Help;
import com.eacpay.eactalk.Listen;
import com.eacpay.eactalk.MainActivity;
import com.eacpay.eactalk.ResetAccount;
import com.eacpay.eactalk.SendMessage;
import com.eacpay.eactalk.Share;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.presenter.activities.settings.DisplayCurrencyActivity;
import com.eacpay.presenter.activities.settings.ImportActivity;
import com.eacpay.presenter.activities.settings.SecurityCenterActivity;
import com.eacpay.presenter.customviews.BRDialogView;
import com.eacpay.presenter.fragments.FragmentSend;
import com.eacpay.tools.animation.BRAnimator;
import com.eacpay.tools.animation.BRDialog;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.tools.manager.TxManager;
import com.eacpay.tools.threads.BRExecutor;
import com.eacpay.tools.util.BRCurrency;
import com.eacpay.tools.util.BRExchange;
import com.eacpay.wallet.BRPeerManager;
import com.eacpay.wallet.BRWalletManager;

import java.math.BigDecimal;

public class Mine extends Fragment implements BRWalletManager.OnBalanceChanged {
    private static final String TAG = "oldfeel";
    FragmentMainMineBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainMineBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.mineResetAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(ResetAccount.class);
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
//                BRAnimator.showSendFragment(getActivity(), null);
                MyService eacService = ((MainActivity) getActivity()).getEacService();
                if (!eacService.isEacConnect || !eacService.isEacSyncFinish) {
                    showCannotSend();
                    return;
                }
                FragmentSend fragmentSend = new FragmentSend();
                fragmentSend.show(getActivity().getSupportFragmentManager(), "fragment_send");
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
                new AlertDialog.Builder(getActivity(), R.style.my_dialog)
                        .setTitle(getString(R.string.tips))
                        .setMessage(R.string.tip_mine_flash)
                        .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .show();
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
//                openActivity(NodesActivity.class);
                openActivity(ApiSettings.class);
            }
        });

        binding.mineInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(Share.class);
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
                openActivity(Help.class);
            }
        });

        binding.mineCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(Code.class);
            }
        });

        binding.mineContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SendMessage.class);
                intent.putExtra("address", "eiXsF2MuvuZZJQ1exxjXW1i1pwKrKypFB3");
                intent.putExtra("name", getActivity().getString(R.string.eactalk_team));
                intent.putExtra("amount", "1");
                intent.putExtra("title", getString(R.string.contact_us));
                startActivity(intent);
            }
        });

        binding.mineAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(About.class);
            }
        });

        binding.mineListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(Listen.class);
//                BRAnimator.showMenuFragment(getActivity());
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

        binding.mineRescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRDialog.showCustomDialog(getActivity(), getString(R.string.ReScan_alertTitle),
                        getString(R.string.ReScan_footer), getString(R.string.ReScan_alertAction), getString(R.string.Button_cancel),
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRSharedPrefs.putStartHeight(getActivity(), 0);
                                        BRSharedPrefs.putAllowSpend(getActivity(), false);
                                        BRPeerManager.getInstance().rescan();
                                        BRAnimator.startMainActivity(getActivity(), false);

                                    }
                                });
                            }
                        }, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, 0);
            }
        });

        boolean autoTranslate = BRSharedPrefs.getBoolean(getActivity(), "auto_translate", false);
        binding.mineTranslate.setChecked(autoTranslate);
        binding.mineTranslate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                BRSharedPrefs.putBoolean(getActivity(), "auto_translate", b);
            }
        });

        BRWalletManager.getInstance().addBalanceChangedListener(this);
        updateUI();
    }

    private void showCannotSend() {
        Dialog dialog = new AlertDialog.Builder(getActivity(), R.style.my_dialog)
                .setTitle(getString(R.string.tips))
                .setMessage(R.string.tip_eac_syncing)
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (binding != null && binding.mineBalance != null) {
                                binding.mineBalance.setText(formattedBTCAmount);
                                binding.mineBalanceLocal.setText(String.format("%s", formattedCurAmount));
                            }
                        }
                    });
                    TxManager.getInstance().updateTxList(getActivity());
                }
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
