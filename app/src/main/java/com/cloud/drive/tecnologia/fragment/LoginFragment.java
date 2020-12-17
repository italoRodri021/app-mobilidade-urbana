package com.cloud.drive.tecnologia.fragment;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.activity.SplashActivity;
import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.helper.Permissao;
import com.cloud.drive.tecnologia.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginFragment extends Fragment {

    private String[] permissoes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private EditText campoEmail, campoSenha;
    private Button botaoEntrar;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private AlertDialog.Builder alertDialog;

    public LoginFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        iniComponentes(v);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        UsuarioFirebase.validarTipoCadastro(getActivity());

    }

    public void validar() {

        String email = campoEmail.getText().toString();
        String senha = campoSenha.getText().toString();

        if (!email.isEmpty()) {
            if (!senha.isEmpty()) {

                botaoEntrar.setClickable(false);
                botaoEntrar.setText("");
                progressBar.setVisibility(View.VISIBLE);

                Usuario u = new Usuario();
                u.setEmail(email);
                u.setSenha(senha);

                logar(u);

            } else {
                snackBar("Qual sua senha?");
            }
        } else {
            snackBar("Qual seu endereço de e-mail?");
        }

    }

    public void logar(final Usuario u) {

        auth.signInWithEmailAndPassword(
                u.getEmail(),
                u.getSenha()).addOnCompleteListener(getActivity(),
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            UsuarioFirebase.validarTipoCadastro(getActivity());

                        } else {

                            botaoEntrar.setClickable(true);
                            botaoEntrar.setText("Continuar");
                            progressBar.setVisibility(View.GONE);

                            String mensagem = "Mensagem";

                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                mensagem = "Ops! Você ainda não está cadastrado.";
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                mensagem = "Ops! Endereço de e-mail ou senha estão incorretos.";
                            } catch (Exception e) {
                                mensagem = "Ops! Algo saiu errado. Verifique a sua conexão e tente novamente.";
                                e.printStackTrace();
                            }
                            snackBar(mensagem);

                        }

                    }
                });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int permissao : grantResults) {
            if (permissao == PackageManager.PERMISSION_DENIED) {
                alerta();
            }
        }

    }

    public void alerta() {

        alertDialog.setTitle("Permissões Negadas!")
                .setMessage("Para utlizar este o Cloud Driver você precisa aceitar as permissões!")
                .setCancelable(false)
                .setPositiveButton("Entendo",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                Intent reload = new Intent(getContext(), SplashActivity.class);
                                startActivity(reload);

                            }
                        }).create().show();

    }

    public void snackBar(String mensagem){

        Snackbar.make(getView(),mensagem, BaseTransientBottomBar.LENGTH_SHORT).show();

    }

    public void iniComponentes(View v) {

        campoEmail = v.findViewById(R.id.editEmailLogin);
        campoSenha = v.findViewById(R.id.editSenhaLogin);
        botaoEntrar = v.findViewById(R.id.btnEntrarLogin);
        progressBar = v.findViewById(R.id.progressBarLogin);
        alertDialog = new AlertDialog.Builder(getContext());

        auth = ConfigFirebase.getAuth();

        botaoEntrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                validar();

            }
        });

        Permissao.validarPermissoes(permissoes, getActivity(), 1);

    }

}