package com.cloud.drive.tecnologia.config;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cloud.drive.tecnologia.activity.MenuActivity;
import com.cloud.drive.tecnologia.activity.MotoristaActivity;
import com.cloud.drive.tecnologia.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UsuarioFirebase {

    /**
     * Salvando nome do usuario no profile do firebase
     */
    public static void profileFirebase(String nome){

        try{

            FirebaseUser user = getUsuario();
            UserProfileChangeRequest p = new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome).build();
            user.updateProfile(p).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Recuperando id do usuario logado
     * @return
     */
    public static String getIdUser() {
        FirebaseAuth auth = ConfigFirebase.getAuth();
        return auth.getCurrentUser().getUid();
    }

    /**
     * Recuperando usuaruo logado
     * @return
     */
    public static FirebaseUser getUsuario() {

        FirebaseAuth auth = ConfigFirebase.getAuth();
        return auth.getCurrentUser();

    }

    /**
     * Salvando dados do usuario no profile do firebase
     * @return
     */
    public static Usuario getDadosUsuario() {

        FirebaseUser firebase = getUsuario();

        Usuario usuario = new Usuario();
        usuario.setId(firebase.getUid());
        usuario.setEmail(firebase.getEmail());
        usuario.setNome(firebase.getDisplayName());

        return usuario;
    }

    /**
     * Validando o tipo de cadastro
     * Caso seja MOTORISTA abre a activity motorista, caso seja PASSAGEIRO abre MapaActivity
     * @param a
     */
    public static void validarTipoCadastro(final Activity a) { // -> Recebendo activity

        if (getUsuario() != null) { // -> Verificando se o usuario está logado

            DatabaseReference database = ConfigFirebase.getDatabase()
                    .child("Usuarios")
                    .child("Perfil")
                    .child(getIdUser());

            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    Usuario u = snapshot.getValue(Usuario.class);
                    String tipoCadastro = u.getTipoCadastro();

                    if (tipoCadastro.equals("PASSAGEIRO")) {

                        Intent i = new Intent(a, MenuActivity.class);
                        a.startActivity(i);

                    } else if (tipoCadastro.equals("MOTORISTA")) {
                        Intent i = new Intent(a, MotoristaActivity.class);
                        a.startActivity(i);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                    Log.i("ERRO", error.getMessage());

                }
            });

        }

    }

    /**
     * Salvando localizacao do PASSAGEIRO no GeoFire que recebe a LATITUDE e LONGITUDE do metodo
     * @param latitude
     * @param longitude
     */
    public static void salvarGeoFire(double latitude, double longitude){

        Usuario u = UsuarioFirebase.getDadosUsuario();
        DatabaseReference database = ConfigFirebase.getDatabase()
                .child("LocalGeofire");

        GeoFire gF = new GeoFire(database);
        gF.setLocation(
                u.getId(), // -> Salvando no id do passageiro
                new GeoLocation(latitude, longitude),
                new GeoFire.CompletionListener() { // -> testando se foi possivel salvar o local do passageiro
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                        if(error != null ){
                            Log.d("ERRO","ERRO AO SALVAR DADOS NO GEOFIRE");
                        }else{
                            Log.d("KEYGEOFIRE",key);
                        }

                    }
                }
        );
    }

}
