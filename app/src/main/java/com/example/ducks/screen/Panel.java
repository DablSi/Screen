package com.example.ducks.screen;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;


public class Panel extends Fragment {
    private TextView textView, textView1, textView2;
    private ImageButton button, button1, button2;

    public void select(ImageButton button, TextView textView) {
        float scale = getResources().getDisplayMetrics().density;
        button.setPadding((int) (48 * scale + 0.5f), (int) (8 * scale + 0.5f), (int) (48 * scale + 0.5f), (int) (16 * scale + 0.5f));
        textView.setVisibility(View.VISIBLE);
    }

    public void unselect(ImageButton button, TextView textView) {
        float scale = getResources().getDisplayMetrics().density;
        button.setPadding((int) (48 * scale + 0.5f), (int) (16 * scale + 0.5f), (int) (48 * scale + 0.5f), (int) (16 * scale + 0.5f));
        textView.setVisibility(View.INVISIBLE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // менеджер компоновки, который позволяет получать доступ к layout с наших ресурсов
        View view = inflater.inflate(R.layout.fragment_panel, container, false);

        // теперь можем получить наши элементы, расположенные во фрагменте
        textView = view.findViewById(R.id.add);
        button = view.findViewById(R.id.first);
        button1 = view.findViewById(R.id.second);
        textView1 = view.findViewById(R.id.search);
        button2 = view.findViewById(R.id.third);
        textView2 = view.findViewById(R.id.set);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(button, textView);
                if (!getActivity().getClass().getSimpleName().equals("MainActivity")) {
                    Intent intent1 = new Intent(getContext(), MainActivity.class);
                    startActivity(intent1);
                    unselect(button1, textView1);
                    unselect(button2, textView2);
                }
            }
        });

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(button1, textView1);
                if (!getActivity().getClass().getSimpleName().equals("Search")) {
                    Intent intent1 = new Intent(getContext(), Search.class);
                    startActivity(intent1);
                    unselect(button, textView);
                    unselect(button2, textView2);
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(button2, textView2);
                if (!getActivity().getClass().getSimpleName().equals("Settings")) {
                    Intent intent1 = new Intent(getContext(), Settings.class);
                    startActivity(intent1);
                    unselect(button, textView);
                    unselect(button1, textView1);
                }
            }
        });

        switch (getActivity().getClass().getSimpleName()) {
            case "MainActivity":
                select(button, textView);
                break;
            case "Search":
                select(button1, textView1);
                break;
            case "Settings":
                select(button2, textView2);
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        switch (getActivity().getClass().getSimpleName()) {
            case "MainActivity":
                select(button, textView);
                unselect(button1, textView1);
                unselect(button2, textView2);
                break;
            case "Search":
                select(button1, textView1);
                unselect(button, textView);
                unselect(button2, textView2);
                break;
            case "Settings":
                select(button2, textView2);
                unselect(button1, textView1);
                unselect(button, textView);
        }
    }
}
