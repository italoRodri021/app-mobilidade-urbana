package com.cloud.drive.tecnologia.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.fragment.PassageiroFragment;
import com.cloud.drive.tecnologia.fragment.PerfilPFragment;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;

public class MenuActivity extends AppCompatActivity {

    private SmartTabLayout smartTabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        iniComponentes();

        FragmentPagerAdapter adapter = new FragmentPagerItemAdapter(
                getSupportFragmentManager(), FragmentPagerItems.with(this)
                .add("Nova Corrida", PassageiroFragment.class)
                .add("Perfil", PerfilPFragment.class)
                .create()
        );

        smartTabLayout.setViewPager(viewPager);
        viewPager.setAdapter(adapter);

    }

    public void iniComponentes() {

        smartTabLayout = findViewById(R.id.smartTabMenu);
        viewPager = findViewById(R.id.viewPagerMenu);

    }

    @Override
    public void onBackPressed() {

    }
}