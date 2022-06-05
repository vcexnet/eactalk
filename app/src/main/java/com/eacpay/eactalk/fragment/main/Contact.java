package com.eacpay.eactalk.fragment.main;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.eacpay.R;
import com.eacpay.databinding.FragmentMainContactBinding;
import com.eacpay.databinding.ItemContactBinding;
import com.eacpay.eactalk.ContactDetail;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactCreate;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactDelete;
import com.eacpay.eactalk.fragment.main.ContactFragment.ContactItem;
import com.eacpay.eactalk.utils.MyUtils;
import com.eacpay.presenter.activities.camera.ScanQRActivity;
import com.eacpay.tools.manager.BRSharedPrefs;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class Contact extends Fragment {
    private static final String TAG = "oldfeel";
    String cidDir = "QmUNLLsPACCz1vLxQVkXqqLX5R1X345qqfHbsf67hvA3Nn";
    FragmentMainContactBinding binding;
    String firstAddress;
    MyAdapter myAdapter;
    boolean isSelect;

    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        String address = intent.getStringExtra("result");
                        create(MyUtils.parseAddress(address));
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainContactBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();

        Toolbar toolbar = binding.getRoot().findViewById(R.id.my_toolbar_center);
        toolbar.inflateMenu(R.menu.main_contact);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_main_contact_import:
                        contactImport();
                        return true;
                    case R.id.menu_main_contact_export:
                        MyUtils.exportContact(getActivity());
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getData();
    }

    ActivityResultLauncher<String> fileSelect = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri uri) {
            if (uri == null) {
                return;
            }
            String fileName = MyUtils.getFileName(getActivity(), uri);
            byte[] fileData = MyUtils.getFileBytes(getActivity(), uri);
            List<ContactItem> list = new ArrayList<>();
            if (fileData != null) {
                list = new Gson().fromJson(new String(fileData), new TypeToken<List<ContactItem>>() {
                }.getType());
                myAdapter.importList(list);
            }
            Toast.makeText(getActivity(), getString(R.string.import_success), Toast.LENGTH_SHORT).show();
        }
    });

    private void contactImport() {
        fileSelect.launch("application/json");
    }

    private void init() {
        TextView title = binding.getRoot().findViewById(R.id.my_toolbar_center_title);
        title.setText(R.string.contact);

        firstAddress = BRSharedPrefs.getFirstAddress(getActivity());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        myAdapter = new MyAdapter();
        binding.recyclerView.setAdapter(myAdapter);

        binding.mainContactSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    getData();
                    return true;
                }
                return false;
            }
        });

        binding.mainContactSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                getData();
            }
        });

        binding.mainContactCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                create("");
            }
        });

        binding.mainContactScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStartForResult.launch(new Intent(getActivity(), ScanQRActivity.class));
            }
        });

        binding.mainContactCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String address = "";
                for (int i = 0; i < myAdapter.list.size(); i++) {
                    if (myAdapter.list.get(i).select) {
                        if (!address.equals("")) {
                            address += "\n";
                        }
                        address += myAdapter.list.get(i).address;
                    }
                }
                ClipData clip = ClipData.newPlainText("simple text", address);
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(clip);
                if (!address.equals("")) {
                    Toast.makeText(getActivity(), getString(R.string.copy_success) + address, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.mainContactDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContactDelete contactDelete = new ContactDelete();
                contactDelete.setList(myAdapter.list);
                contactDelete.setOnOKListener(new ContactDelete.OnOKListener() {
                    @Override
                    public void onOk() {
                        myAdapter.notifyDataSetChanged();
                    }
                });
                contactDelete.show(getActivity().getSupportFragmentManager(), "contact_delete");
            }
        });

        binding.mainContactSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSelect = !isSelect;
                myAdapter.notifyDataSetChanged();
            }
        });
    }

    private void create(String address) {
        ContactCreate contactCreate = new ContactCreate();
        contactCreate.setAddress(address);
        contactCreate.setOnOKListener(new ContactCreate.OnOKListener() {
            @Override
            public void onOk() {
                getData();
            }
        });
        contactCreate.show(getActivity().getSupportFragmentManager(), "contact_create");
    }

    private void getData() {
        if (getActivity() == null) {
            return;
        }

        List<ContactItem> list = BRSharedPrefs.getContactList(getActivity());
        List<ContactItem> newList = new ArrayList<>();
        String key = binding.mainContactSearch.getText().toString();
        if (MyUtils.isEmpty(key)) {
            newList = list;
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).name.contains(key) || list.get(i).address.contains(key)) {
                    newList.add(list.get(i));
                }
            }
        }
        myAdapter.setList(newList);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (IpfsManager.getInstance().getIpfs() == null) {
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                try {
//                    byte[] data = IpfsManager.getInstance().getIpfs().newRequest("cat")
//                            .withArgument(cidDir + "/" + firstAddress)
//                            .send();
//                    final List<ContactItem> list = new Gson().fromJson(new String(data), new TypeToken<List<ContactItem>>() {
//                    }.getType());
//                    if (getActivity() != null) {
//                        getActivity().runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                myAdapter.setList(list);
//                            }
//                        });
//                    }
//                } catch (RequestBuilder.RequestBuilderException e) {
//                    e.printStackTrace();
//                } catch (IPFS.ShellRequestException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        List<ContactItem> list = new ArrayList<>();

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemContactBinding binding = ItemContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            MyViewHolder holder = new MyViewHolder(binding.getRoot());
            holder.setBinding(binding);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            final ContactItem item = list.get(position);
            holder.binding.itemContactName.setText(item.name);
            holder.binding.itemContactAddress.setText(item.address);
            holder.binding.itemContactSelect.setChecked(item.select);
            holder.binding.itemContactSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    item.select = b;
                }
            });
            holder.binding.itemContactSelect.setVisibility(isSelect ? View.VISIBLE : View.GONE);

            holder.binding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), ContactDetail.class);
                    intent.putExtra("item", new Gson().toJson(item));
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public void setList(List<ContactItem> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        public void importList(List<ContactItem> list) {
            for (int i = 0; i < list.size(); i++) {
                boolean isHave = false;
                for (int j = 0; j < this.list.size(); j++) {
                    if (this.list.get(j).address.equals(list.get(i).address)) {
                        isHave = true;
                        break;
                    }
                }
                if (!isHave) {
                    this.list.add(list.get(i));
                }
            }
            notifyDataSetChanged();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        ItemContactBinding binding;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setBinding(ItemContactBinding binding) {
            this.binding = binding;
        }
    }

}
