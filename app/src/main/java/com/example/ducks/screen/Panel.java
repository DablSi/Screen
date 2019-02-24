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
    public void select(ImageButton button, TextView textView){
        float scale = getResources().getDisplayMetrics().density;
        button.setPadding((int) (48 * scale + 0.5f),(int) (8 * scale + 0.5f),(int) (48 * scale + 0.5f),(int) (16 * scale + 0.5f));
        textView.setVisibility(View.VISIBLE);
    }

    public void unselect(ImageButton button, TextView textView){
        float scale = getResources().getDisplayMetrics().density;
        button.setPadding((int) (48 * scale + 0.5f),(int) (16 * scale + 0.5f),(int) (48 * scale + 0.5f),(int) (16 * scale + 0.5f));
        textView.setVisibility(View.INVISIBLE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // менеджер компоновки, который позволяет получать доступ к layout с наших ресурсов
        View view = inflater.inflate(R.layout.fragment_panel, container, false);

        // теперь можем получить наши элементы, расположенные во фрагменте
        TextView textView = view.findViewById(R.id.add);
        ImageButton button = view.findViewById(R.id.first);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                select(button, textView);
                if (!getActivity().getClass().getSimpleName().equals("MainActivity")) {
                    Intent intent1 = new Intent(getContext(), MainActivity.class);
                    startActivity(intent1);
                }
            }
        });
        ImageButton button1 = view.findViewById(R.id.second);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView1 = view.findViewById(R.id.set);
                select(button1, textView1);
                if (getActivity().getClass().getSimpleName().equals("MainActivity")) {
                    Intent intent1 = new Intent(getContext(), Settings.class);
                    //startActivity(intent1);
                    unselect(button, textView);
                }
            }
        });
        return view;
    }
}
