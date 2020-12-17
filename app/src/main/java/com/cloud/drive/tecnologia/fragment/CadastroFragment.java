package com.cloud.drive.tecnologia.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.activity.BoasVindasActivity;
import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class CadastroFragment extends Fragment {

    private EditText campoNome, campoEmail, campoSenha, campoConfirmar;
    private Button botaoCadastrar;
    private ProgressBar progressBar;
    private RadioButton passageiro, motorista;
    private FirebaseAuth auth;

    public CadastroFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_cadastro, container, false);

        iniComponentes(v);

        auth = ConfigFirebase.getAuth();

        botaoCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                validar();

            }
        });

        return v;
    }

    public void validar() {

        String nome = campoNome.getText().toString();
        String email = campoEmail.getText().toString();
        String senha = campoSenha.getText().toString();
        String confirmar = campoConfirmar.getText().toString();

        if (!nome.isEmpty() && nome.length() > 15) {
            if (!email.isEmpty()) {
                if(email.contains("gmail") || email.contains("hotmail") || email.contains("outlook")){
                    if (!senha.isEmpty()) {
                        if (confirmar.equals(senha)) {

                            progressBar.setVisibility(View.VISIBLE);
                            botaoCadastrar.setClickable(false);
                            botaoCadastrar.setText("");

                            Usuario u = new Usuario();
                            u.setNome(nome);
                            u.setEmail(email);
                            u.setSenha(senha);
                            u.setTipoCadastro(tipoCadastro());

                            cadastrar(u);

                        } else {
                            snackBar("Ops! Você digitou algo errado. Confirme a senha novamente");
                        }
                    } else {
                        snackBar("Qual vai ser sua senha de acesso?");
                    }
                }else{
                    snackBar("Utilize um email real");
                }
            } else {
                snackBar("Qual seu endereço de e-mail?");
            }
        } else {
            snackBar("Qual seu nome completo?");
        }

    }

    public String tipoCadastro() {

        String tipo = "PASSAGEIRO";

        if (passageiro.isChecked()) {
            tipo = "PASSAGEIRO";
        } else if (motorista.isChecked()) {
            tipo = "MOTORISTA";
        }

        return tipo;
    }

    public void cadastrar(final Usuario u) {

        auth.createUserWithEmailAndPassword(
                u.getEmail(),
                u.getSenha()).addOnCompleteListener(getActivity(),
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        try {

                            String id = task.getResult().getUser().getUid();
                            u.setId(id);
                            u.salvarCadastro();

                            UsuarioFirebase.profileFirebase(u.getNome()); // -> Salva nome no profile do firebase

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (task.isSuccessful()) {

                            Intent i = new Intent(getContext(), BoasVindasActivity.class);
                            startActivity(i);

                        } else {

                            progressBar.setVisibility(View.GONE);
                            botaoCadastrar.setClickable(true);
                            botaoCadastrar.setText("Continuar Cadastro");


                            String mensagem = "Mensagem";

                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                mensagem = "Ops! Por favor defina uma senha mais forte.";
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                mensagem = "Ops! Por favor defina um e-mail válido.";
                            } catch (FirebaseAuthUserCollisionException e) {
                                mensagem = "Ops! Este endereço de e-mail já está cadastrado.";
                            } catch (Exception e) {
                                mensagem = "Ops! Algo saiu errado. Tente novamente ou volte outra hora.";
                                e.printStackTrace();
                            }
                            snackBar(mensagem);

                        }

                    }
                });

    }

    public void snackBar(String mensagem){

        Snackbar.make(getView(),mensagem, BaseTransientBottomBar.LENGTH_SHORT).show();

    }

    public void iniComponentes(View v) {

        campoNome = v.findViewById(R.id.editNomeCadastro);
        campoEmail = v.findViewById(R.id.editEmailCadastro);
        campoSenha = v.findViewById(R.id.editSenhaCadastro);
        campoConfirmar = v.findViewById(R.id.editConfSenhaCadastro);
        botaoCadastrar = v.findViewById(R.id.btnCadastrar);
        progressBar = v.findViewById(R.id.progressBarCadastro);
        passageiro = v.findViewById(R.id.radioBtnPassageiro);
        motorista = v.findViewById(R.id.radioBtnMotorista);

    }

}