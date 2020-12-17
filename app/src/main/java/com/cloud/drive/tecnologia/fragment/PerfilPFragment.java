package com.cloud.drive.tecnologia.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.helper.Permissao;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class PerfilPFragment extends Fragment {

    private static final int SELECAO_GALERIA = 200;
    private String[] permissoes = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private StorageReference storage;
    private ImageView fotoPerfil;
    private Button botaoGaleria;
    private String idUsuario;

    public PerfilPFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_perfil_passageiro, container, false);

        iniComponetes(v);
        configInterface();
        downloadImagem();

        return v;
    }

    public void configInterface(){

        botaoGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                if(i.resolveActivity(getActivity().getPackageManager()) != null){
                    startActivityForResult(i,SELECAO_GALERIA);
                }

            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK){

            Bitmap imagem = null;

            try{

                switch (requestCode){
                    case SELECAO_GALERIA:
                        Uri localImagem = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap(getActivity()
                                .getContentResolver(),localImagem);
                        break;
                }

                if(imagem != null){

                    fotoPerfil.setImageBitmap(imagem);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG,100,baos);
                    byte[] imagemBytes = baos.toByteArray();

                    StorageReference imagemRef = storage
                            .child("Imagens")
                            .child("Perfil")
                            .child(idUsuario + ".JPEG");

                    UploadTask upload = imagemRef.putBytes(imagemBytes);
                    upload.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if(task.isSuccessful()){

                                Toast.makeText(getContext(), "Foto atualizada com sucesso!", Toast.LENGTH_SHORT).show();

                            }else{

                                Toast.makeText(getContext(), "Ops! Algo saiu errado.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int permissao : grantResults){
            if(permissao == PackageManager.PERMISSION_DENIED){

            }
        }

    }

    public void downloadImagem(){

        final StorageReference imagem = storage
                .child("Imagens")
                .child("Perfil")
                .child(idUsuario + ".JPEG");

        long MAXIMOBYTES = 1080 * 1080;

        imagem.getBytes(MAXIMOBYTES).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {

                Bitmap b = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                fotoPerfil.setImageBitmap(b);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ERRO",e.getMessage());
            }
        });

    }

    public void iniComponetes(View v){

        Permissao.validarPermissoes(permissoes,getActivity(),1);

        botaoGaleria = v.findViewById(R.id.btnGaleriaPerfil);
        fotoPerfil = v.findViewById(R.id.imageViewFotoPerfil);
        idUsuario = UsuarioFirebase.getIdUser();
        storage = ConfigFirebase.getStorage();

    }

}