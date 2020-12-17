package com.cloud.drive.tecnologia.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.helper.Permissao;

public class BoasVindasActivity extends AppCompatActivity {

    private CheckBox checkTermos;
    private Button botaoContinuar;
    private String[] permissoes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boas_vindas);

        iniComponentes();
        termosServico();

        botaoContinuar.setClickable(false);
        botaoContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                UsuarioFirebase.validarTipoCadastro(BoasVindasActivity.this);

            }
        });
    }

    public void termosServico(){

        if(checkTermos.isChecked()){

            botaoContinuar.setClickable(true);

        }else{

            botaoContinuar.setClickable(false);
            Toast.makeText(this, "Você precisa aceitar os termos!",
                    Toast.LENGTH_SHORT).show();

        }

    }

    public void iniComponentes(){

        botaoContinuar = findViewById(R.id.btnContunuarBoasVindas);
        checkTermos = findViewById(R.id.checkBoxTemosContrato);
    }

}