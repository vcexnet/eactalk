package com.eacpay.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eacpay.R;

public class FragmentPhraseWord extends Fragment {
    private static final String TAG = FragmentPhraseWord.class.getName();

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_word_item, container, false);
        TextView b = (TextView) rootView.findViewById(R.id.word_button);
        b.setText(getArguments().getString("text"));

        return rootView;
    }

    public static FragmentPhraseWord newInstance(String text) {

        FragmentPhraseWord f = new FragmentPhraseWord();
        Bundle b = new Bundle();
        b.putString("text", text);

        f.setArguments(b);

        return f;
    }

}