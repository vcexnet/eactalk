package com.eacpay.eactalk;

import static com.eacpay.eactalk.ipfs.IpfsDataFetcher.ENCRYPT_PREFIX;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import com.blankj.utilcode.util.LanguageUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.eacpay.R;
import com.eacpay.databinding.ActivityHomeDetailBinding;
import com.eacpay.eactalk.fragment.dialog.ShowImage;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactCreate;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.eactalk.fragment.main.HomeFragment.HomeItem;
import com.eacpay.eactalk.ipfs.IpfsLs;
import com.eacpay.eactalk.ipfs.IpfsManager;
import com.eacpay.eactalk.service.MyService;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.util.BRActivity;
import com.eacpay.presenter.entities.TxItem;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.eacpay.wallet.BRWalletManager;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

public class HomeDetail extends BRActivity {
    private static final String TAG = "oldfeel";
    ActivityHomeDetailBinding binding;
    HomeItem homeItem;
    private boolean isLike;

    SimpleExoPlayer player;
    Uri fileUri;
    private boolean isStop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(getString(R.string.message_info));

        Intent intent = getIntent();
        String itemString = intent.getStringExtra("item");
        homeItem = new Gson().fromJson(itemString, HomeItem.class);

        if (homeItem.confirmations < 0) { // 从我的消息中点进来的
            binding.homeDetailStatus.setVisibility(View.GONE);
        }
        binding.homeDetailStatus.setText(Html.fromHtml(getString(R.string.status_submit, homeItem.confirmations), HtmlCompat.FROM_HTML_MODE_LEGACY));
        if (homeItem.type != null && homeItem.type.equals("sent")) {
            binding.homeDetailFrom.setText(getString(R.string.send_) + homeItem.getTarget());
        } else {
            binding.homeDetailFrom.setText(getString(R.string.from_) + homeItem.sender);
        }
        binding.homeDetailTime.setText(getString(R.string.date_) + MyUtils.formatTimeLA(homeItem.time));

        boolean received = homeItem.sent == 0;
        if (homeItem.type != null && !homeItem.type.equals("")) {
            received = homeItem.type.equals("received");
        }
        binding.homeDetailReceive.setText((received ? getString(R.string.received_) : getString(R.string.sended_)) + "Є" + homeItem.value);
        binding.homeDetailId.setText(Html.fromHtml("ID: <font color='#3d86f8'>" + homeItem.txid + "</font>", HtmlCompat.FROM_HTML_MODE_LEGACY));

//        if (homeItem.txcomment.startsWith(ENCRYPT_PREFIX)) {
//            homeItem.txcomment = MyUtils.decrypt(homeItem.txcomment, homeItem.address);
//        }
        homeItem.txcomment = homeItem.parseComment(this);
        binding.homeDetailContent.setText(homeItem.txcomment);

        updateName();

        binding.homeDetailLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLike) {
                    unLike();
                    return;
                }
                ContactCreate contactCreate = new ContactCreate();
                contactCreate.setAddress(homeItem.sender);
                contactCreate.setOnOKListener(new ContactCreate.OnOKListener() {
                    @Override
                    public void onOk() {
                        updateName();
                    }
                });
                contactCreate.show(getSupportFragmentManager(), "contact_create");
            }
        });

        binding.homeDetailReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent replyIntent = new Intent(HomeDetail.this, SendMessage.class);
                replyIntent.putExtra("address", homeItem.sender);
                replyIntent.putExtra("name", homeItem.contactName == null ? "" : homeItem.contactName);
                replyIntent.putExtra("isReply", true);
                startActivity(replyIntent);
            }
        });

        binding.homeDetailFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyUtils.copy(getApplicationContext(), homeItem.sender);
                Toast.makeText(HomeDetail.this, getString(R.string.copy_success_) + homeItem.sender, Toast.LENGTH_SHORT).show();
            }
        });

        binding.homeDetailId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String txUrl = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("earth_coin_url", MyService.defaultEarthCoinUrl) + "/transaction/" + homeItem.txid;
                Timber.d("txUrl = %s", txUrl);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(txUrl));
                startActivity(browserIntent);
            }
        });

        binding.homeDetailTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                identifyLanguage(homeItem.txcomment);
            }
        });

        binding.homeDetailShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.homeDetailShareView.setVisibility(View.VISIBLE);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startShare();
                    }
                }, 200);
            }
        });

        boolean autoTranslate = BRSharedPrefs.getBoolean(this, "auto_translate", false);
        if (autoTranslate) {
            identifyLanguage(homeItem.txcomment);
        }

        showIpfs();
        loadIpfs();

        new Thread(new Runnable() {
            @Override
            public void run() {
                TxItem[] arr = BRWalletManager.getInstance().getTransactions();
                if (arr == null) {
                    return;
                }
                for (int i = 0; i < arr.length; i++) {
                    TxItem item = arr[i];
                    if (item.getTxHashHexReversed().equals(homeItem.txid) && item.getSent() != 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.homeDetailReply.setVisibility(View.GONE);
                            }
                        });
                        break;
                    }
                }
            }
        }).start();
    }

    public static Bitmap getBitmapByView(ScrollView scrollView) {
        int h = 0;
        Bitmap bitmap = null;
        // 获取scrollview实际高度 
        for (int i = 0; i < scrollView.getChildCount(); i++) {
            h += scrollView.getChildAt(i).getHeight();
            scrollView.getChildAt(i).setBackgroundColor(
                    Color.parseColor("#ffffff"));
        }
        // 创建对应大小的bitmap 
        bitmap = Bitmap.createBitmap(scrollView.getWidth(), h,
                Bitmap.Config.RGB_565);
        final Canvas canvas = new Canvas(bitmap);
        scrollView.draw(canvas);
        return bitmap;
    }

    private void startShare() {
//        Bitmap bitmap = ScreenUtils.screenShot(this);
        Bitmap bitmap = getBitmapByView(binding.homeDetailScrollView);

        String bitmapPath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, getString(R.string.About_appName_android), null);
        Uri bitmapUri = Uri.parse(bitmapPath);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
        intent.setType("image/jpeg");
        startActivity(Intent.createChooser(intent, getResources().getText(R.string.About_appName_android)));
    }


    private void identifyLanguage(final String text) {
        if (text == null || text.equals("")) {
            return;
        }
        FirebaseLanguageIdentification languageIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
        languageIdentifier
                .identifyLanguage(text)
                .addOnSuccessListener(
                        new OnSuccessListener<String>() {
                            @Override
                            public void onSuccess(@Nullable String languageCode) {
                                MyUtils.log("identifyLanguage onSuccess " + languageCode);
                                if (languageCode.equals("und")) { // 无法识别
                                    identifyLanguageFail();
                                } else {
                                    showTranslate(text, languageCode);
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                MyUtils.log("identifyLanguage onFailure " + e.getMessage());
                                identifyLanguageFail();
                            }
                        });
    }

    private void showTranslate(String word, String sl) {
        String tl = LanguageUtils.getSystemLanguage().getLanguage();
        if (sl.equals(tl)) {
            Toast.makeText(HomeDetail.this, R.string.language_same, Toast.LENGTH_SHORT).show();
            return;
        }
        OkHttpClient client = new OkHttpClient();
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" + sl + "&tl=" + tl + "&dt=t&ie=UTF-8&oe=UTF-8&otf=1&ssel=0&tsel=0&kc=7&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&q=" + word;
        MyUtils.log("url is " + url);
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                translateFail();
                MyUtils.log("showTranslate onFailure " + e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String result = response.body().string();
                MyUtils.log("showTranslate " + result);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray jsonArray = new JSONArray(result).getJSONArray(0);
                            String target = "";
                            for (int i = 0; i < jsonArray.length(); i++) {
                                String line = jsonArray.getJSONArray(i).getString(0);
                                if (line != null && !line.equals("null")) {
                                    target += line;
                                }
                            }
                            binding.homeDetailContent2.setText(target);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            translateFail();
                        }
                    }
                });
            }
        });
    }

    private void translateFail() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.homeDetailContent2.setText(R.string.translate_fail);
            }
        });
    }

    private void identifyLanguageFail() {
        binding.homeDetailContent2.setText(R.string.identify_language_fail);
    }

    private void loadIpfs() {
        IpfsManager.getInstance().getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                while (IpfsManager.getInstance().getIpfs() == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    byte[] lsData = IpfsManager.getInstance().getIpfs().newRequest("ls")
                            .withArgument(homeItem.ipfs)
                            .send();

                    IpfsLs ipfsLs = new Gson().fromJson(new String(lsData), IpfsLs.class);
                    homeItem.ipfsLs = ipfsLs;

                    MyUtils.log("file download by success");
                    fileDownload();
                } catch (Exception e) {
                    e.printStackTrace();
                    MyUtils.log("file download by fail");
                    fileDownload();
                }
            }
        });
    }

    private void unLike() {
        new AlertDialog.Builder(HomeDetail.this, R.style.my_dialog)
                .setTitle(R.string.cancel_like)
                .setMessage(getString(R.string.confirm_cancel_like, homeItem.contactName))
                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int what) {
                        BRSharedPrefs.removeContact(HomeDetail.this, homeItem.contactName);
                        Toast.makeText(HomeDetail.this, getString(R.string.unlike_success), Toast.LENGTH_SHORT).show();
                        binding.homeDetailLike.setText(getString(R.string.like));
                        isLike = false;
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .show();

    }

    private void updateName() {
        if (homeItem.contactName != null && !homeItem.equals("")) {
            binding.homeDetailName.setText(getString(R.string.nickname_) + homeItem.contactName);
            binding.homeDetailLike.setText(R.string.liked);
            isLike = true;
            return;
        }
        List<ContactItem> list = BRSharedPrefs.getContactList(this);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).address.equals(homeItem.sender)) {
                homeItem.contactName = list.get(i).name;
                binding.homeDetailName.setText(getString(R.string.nickname_) + list.get(i).name);
                binding.homeDetailLike.setText(R.string.liked);
                isLike = true;
                break;
            }
        }
    }

    private void showIpfs() {
        if (isStop) {
            return;
        }
        if (homeItem.isIpfs()) {
            binding.homeDetailFileDownloading.setVisibility(View.VISIBLE);
        }
        binding.homeDetailImage.setVisibility(homeItem.isImage() ? View.VISIBLE : View.GONE);
        binding.homeDetailVideoLayout.setVisibility(homeItem.isVideo() ? View.VISIBLE : View.GONE);
        binding.homeDetailFile.setVisibility((!homeItem.isImage() && !homeItem.isVideo()) ? View.VISIBLE : View.GONE);
        if (homeItem.isImage()) {
            Glide.with(this)
                    .load(homeItem)
                    .placeholder(R.mipmap.image_default)
                    .apply(new RequestOptions().transform(new CenterCrop(), new RoundedCorners(16)))
                    .into(binding.homeDetailImage);

            binding.homeDetailImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (fileUri != null) {
                        ShowImage showImage = new ShowImage();
                        showImage.setImageUri(fileUri);
                        showImage.show(getSupportFragmentManager(), "show_image");
                    }
                }
            });
        } else if (homeItem.isVideo()) {
            if (player == null) {
                player = new SimpleExoPlayer.Builder(this)
                        .build();
                binding.homeDetailVideo.setPlayer(player);
            }

            binding.homeDetailFullScreen.setVisibility(View.VISIBLE);
            binding.homeDetailFullScreen.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    if (fileUri != null) {
                        Intent intent = new Intent(HomeDetail.this, VideoPlayer.class);
                        intent.putExtra("uri", fileUri);
                        intent.putExtra("position", player.getCurrentPosition());

                        player.stop();
                        mStartForResult.launch(intent);
                    }
                }
            });
        } else {
        }
    }

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && fileUri != null) {
                        long position = result.getData().getLongExtra("position", 0);
                        player = new SimpleExoPlayer.Builder(HomeDetail.this).build();
                        binding.homeDetailVideo.setPlayer(player);
                        MediaItem mediaItem = MediaItem.fromUri(fileUri);
                        player.setMediaItem(mediaItem);
                        player.seekTo(position);
                        Log.e(TAG, "onActivityResult: position " + position);
                        player.prepare();
                        player.play();
                    }
                }
            });

    private void fileDownload() {
        IpfsManager.getInstance().getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                String mimeType = null;
                while (IpfsManager.getInstance().getIpfs() == null || !homeItem.isParsed()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (homeItem.isDir()) {
                    try {
                        byte[] data = IpfsManager.getInstance().getIpfs().newRequest("cat")
                                .withArgument(homeItem.ipfsLs.getFirstIpfsCid())
                                .send();
                        MyUtils.log("homeItem.ipfsLs.getFirstName() " + homeItem.ipfsLs.getFirstName());
                        if (homeItem.ipfsLs.getFirstName().startsWith(ENCRYPT_PREFIX)) {
                            data = MyUtils.decryptFile(data, homeItem.address);
                        }

                        mimeType = MyUtils.guessMimeType(data);
                        File file = MyUtils.saveBytesToFile(HomeDetail.this, data, homeItem.ipfsLs.getFirstName());
                        fileUri = FileProvider.getUriForFile(HomeDetail.this, getApplicationContext().getPackageName() + ".provider", file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (homeItem.isFile()) {
                    try {
                        byte[] data = IpfsManager.getInstance().getIpfs().newRequest("cat")
                                .withArgument(homeItem.ipfs)
                                .send();

                        mimeType = MyUtils.guessMimeType(data);
                        File file = null;
                        if (mimeType.startsWith("image")) {
                            file = MyUtils.saveBytesToFile(HomeDetail.this, data, homeItem.ipfs + ".jpg");
                        }
                        if (mimeType.startsWith("video")) {
                            file = MyUtils.saveBytesToFile(HomeDetail.this, data, homeItem.ipfs + ".mp4");
                        }
                        if (file != null) {
                            fileUri = Uri.fromFile(file);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (fileUri != null && mimeType != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (homeItem.isImage()) {
                                binding.homeDetailImage.setImageURI(fileUri);
                            }

                            if (homeItem.isVideo()) {
                                if (player == null) {
                                    player = new SimpleExoPlayer.Builder(HomeDetail.this)
                                            .build();
                                    binding.homeDetailVideo.setPlayer(player);
                                }

                                MediaItem mediaItem = MediaItem.fromUri(fileUri);
                                player.setMediaItem(mediaItem);
                                player.prepare();
                                player.play();
                            }

                            if (!homeItem.isImage() && !homeItem.isVideo()) {
                                binding.homeDetailFile.setText(fileUri.getPath());

                                binding.homeDetailFile.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                                        String fileName = fileUri.getPath();
                                        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
                                        String type = mime.getMimeTypeFromExtension(ext);
                                        Log.e(TAG, "run: type is " + type);

                                        Intent openIntent = new Intent(Intent.ACTION_VIEW);
                                        openIntent.setDataAndType(fileUri, type);
                                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        try {
                                            startActivity(openIntent);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                            showIpfs();
                            binding.homeDetailFileDownloading.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isStop = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
        isStop = true;
    }
}
