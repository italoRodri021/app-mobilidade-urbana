package com.cloud.drive.tecnologia.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.config.CalculoDistancia;
import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.model.Corrida;
import com.cloud.drive.tecnologia.model.Usuario;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdapterCorridas extends RecyclerView.Adapter<AdapterCorridas.MyViewHolder> {

    private List<Corrida> lista;
    private Context context;
    private Usuario motorista;

    public AdapterCorridas(List<Corrida> lista, Context context, Usuario motorista) {

        this.lista = lista;
        this.context = context;
        this.motorista = motorista;

    }


    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_corridas, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        Corrida corrida = lista.get(position);
        Usuario passageiro = corrida.getPassageiro();

        holder.nome.setText(passageiro.getNome());

        if (motorista != null) {

            LatLng localPassageiro = new LatLng(
                    Double.parseDouble(passageiro.getLatitude()),
                    Double.parseDouble(passageiro.getLongitude())
            );

            LatLng localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            float distancia = CalculoDistancia
                    .calcularDistancia(localPassageiro, localMotorista);
            String distanciaFormatada = CalculoDistancia.formatoDistancia(distancia);
            holder.distancia.setText("~" + distanciaFormatada + " de distancia");

        }



    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView nome, distancia;
        ImageView foto;
        StorageReference storage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nome = itemView.findViewById(R.id.textNomeCorridaAdapter);
            distancia = itemView.findViewById(R.id.textDistanciaCorridaAdapter);
            foto = itemView.findViewById(R.id.imageFotoPassageiroAdapter);
            storage = ConfigFirebase.getStorage().child("Imagens").child("Perfil");


        }
    }
}
