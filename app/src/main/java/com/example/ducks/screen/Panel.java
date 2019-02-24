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
import android.widget.ImageButton;
import android.widget.Toast;

public class Panel extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // менеджер компоновки, который позволяет получать доступ к layout с наших ресурсов
        View view = inflater.inflate(R.layout.fragment_panel, container, false);

        // теперь можем получить наши элементы, расположенные во фрагменте
        ImageButton button = view.findViewById(R.id.first);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!getActivity().getClass().getSimpleName().equals("MainActivity")){
                    Intent intent1 = new Intent(getContext(), MainActivity.class);
                    startActivity(intent1);
                }
            }
        });
        ImageButton button1 = view.findViewById(R.id.second);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getActivity().getClass().getSimpleName().equals("MainActivity")){
                    Intent intent1 = new Intent(getContext(), Settings.class);
                    startActivity(intent1);
                }
            }
        });
        return view;
    }
}
