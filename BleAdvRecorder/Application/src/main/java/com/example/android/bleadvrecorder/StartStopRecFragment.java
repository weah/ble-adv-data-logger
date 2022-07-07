package com.example.android.bleadvrecorder;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.fragment.app.Fragment;

class StartStopRecFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.startstop_rec, container, false);

//        mSwitch = (Switch) view.findViewById(R.id.advertise_switch);
//        mSwitch.setOnClickListener(this);

        return view;
    }
}