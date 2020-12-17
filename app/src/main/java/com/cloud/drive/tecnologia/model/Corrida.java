package com.cloud.drive.tecnologia.model;

import android.provider.ContactsContract;

import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class Corrida {

    private String id;
    private String status;
    private Usuario passageiro;
    private Usuario motorista;
    private Destino destino;

    public Corrida() {

    }

    /**
     * Salvando pedido de corrida do passageiro
     */
    public void salvarCorrida() {

        String idUsuario = UsuarioFirebase.getIdUser();
        DatabaseReference db = ConfigFirebase.getDatabase().child("Corridas");

        String idCorrida = db.push().getKey();
        setId(idCorrida);
        db.child(getId()).setValue(this);

        DatabaseReference db2 = ConfigFirebase.getDatabase().child("CorridaAtual").child(idUsuario);
        db2.child("id").setValue(id);

    }

    /**
     * Atualizando dados quando a corrida for aceita
     */
    public void corridaAceita() {

        DatabaseReference database = ConfigFirebase.getDatabase()
                .child("Corridas")
                .child(getId());

        Map<String,Object> map = new HashMap<>();
        map.put("motorista", getMotorista());
        map.put("status", getStatus());


        database.updateChildren(map);
    }

    /**
     * Atualizando status da corrida
     * @return
     */
    public void atualizarStatus(){

        DatabaseReference database = ConfigFirebase.getDatabase()
                .child("Corridas")
                .child(getId());

        Map<String,Object> map = new HashMap<>();
        map.put("motorista",getMotorista());
        map.put("status",getStatus());

        database.updateChildren(map);
    }

    /**
     * Atualizando status da corrida
     * @return
     */
    public void atualizarLocalMotorista(){

        DatabaseReference database = ConfigFirebase.getDatabase()
                .child("Corridas")
                .child(getId())
                .child("motorista"); // -> Acessando apenas o nó motorista

        Map<String,Object> map = new HashMap<>();
        map.put("latitude",getMotorista().getLatitude());
        map.put("longitude",getMotorista().getLongitude());

        database.updateChildren(map);
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Usuario getPassageiro() {
        return passageiro;
    }

    public void setPassageiro(Usuario passageiro) {
        this.passageiro = passageiro;
    }

    public Usuario getMotorista() {
        return motorista;
    }

    public void setMotorista(Usuario motorista) {
        this.motorista = motorista;
    }

    public Destino getDestino() {
        return destino;
    }

    public void setDestino(Destino destino) {
        this.destino = destino;
    }
}
