package com.cloud.drive.tecnologia.helper;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Permissao {

    public static  boolean validarPermissoes(String[] permissoes, Activity activity, int requestCode){
        //VERIFICANDO SE O DISPOSITIVO DO USUÁRIO ESTÁ UTILIZANDO A VERSÃO DE API:29 OU SUPERIOR
        if (Build.VERSION.SDK_INT >= 23){

            //LISTA DE PERMISSÕES

            List<String> listaPermissoes = new ArrayList<>();

            /*       O QUE EU FIZ!
            *
            *
            * PERCORRI AS PERMISSÕES PASSADAS
            * VERIFIQUEI UMA A UMA
            * TESTEI SE AS PERMISSÕES JÁ ESTAVAM LIBERADAS
            *
            * */

            for (String permissao : permissoes  ){


                //VALIDANDO SE A PERMISSÃO JÁ FOI CONCEDIDA PELO USUÁRIO DO DISPOSITIVO

                //VERIFICANDO SE JÁ FOI CONCEDIDA
                Boolean temPermissao = ContextCompat.checkSelfPermission(activity, permissao) == PackageManager.PERMISSION_GRANTED;

               //SE NÃO TIVER PERMISSÃO
                if ( !temPermissao ) listaPermissoes.add( permissao ) ;
            }

            //VERIFICANDO SE A LISTA DE PERMISSÕES ESTÁ VAZIA => CASO ESTEJA NÃO VAI PRECISAR PEDIR MAIS PERMISSÃO
            if ( listaPermissoes.isEmpty() ) return true;

            //CONVERTENDO NOVAS PERMISSÕES
            String[] novasPermissoes = new String[ listaPermissoes.size() ];
            listaPermissoes.toArray( novasPermissoes );

            //CASO A LISTA NÃO ESTEJA VAZIA VAI CAIR AQUI
            ActivityCompat.requestPermissions(activity, novasPermissoes, requestCode  );
        }

        return true;
    }



}
