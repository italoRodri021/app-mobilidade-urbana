package com.cloud.drive.tecnologia.fragment;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.activity.StatusActivity;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.helper.Status;
import com.cloud.drive.tecnologia.model.Corrida;
import com.cloud.drive.tecnologia.model.Destino;
import com.cloud.drive.tecnologia.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;

public class PassageiroFragment extends Fragment implements OnMapReadyCallback {

    private EditText campoLocal, campoDestino;
    private Button botoaBuscar;
    private LocationManager locManager;
    private LocationListener locListener;
    private GoogleMap mMap;
    private static LatLng localPassageiro;
    private  AlertDialog.Builder alertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_passageiro, container, false);

        iniComponentes(v);
        configInterface();

        return v;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        localPassageiro();
    }

    public void configInterface() {


        botoaBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(!localPassageiro.equals("")){

                buscarMotorista();

            }

            }
        });

    }

    public void localPassageiro() {

        locManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                double LATITUDE = location.getLatitude();
                double LONGITUDE = location.getLongitude();

                localPassageiro = new LatLng(LATITUDE, LONGITUDE);

                UsuarioFirebase.salvarGeoFire(LATITUDE, LONGITUDE);

                mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position(localPassageiro)
                                .title("Meu Local")
                                .icon(BitmapDescriptorFactory
                                        .fromResource(R.drawable.ic_passageiro_marker))

                );

                mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(localPassageiro, 20));

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000, 10, locListener);
        }

    }

    public void buscarMotorista() {

        final String destino = campoDestino.getText().toString();

        if (!destino.isEmpty()) {

            Address endereco = recuperarEndereco(destino);

            if (endereco != null) { // -> Testando se o endereco é diferente de null

                final Destino d = new Destino();
                d.setCidade(endereco.getAdminArea());
                d.setCep(endereco.getPostalCode());
                d.setBairro(endereco.getSubLocality());
                d.setRua(endereco.getThoroughfare());
                d.setNumero(endereco.getFeatureName());
                d.setLatitude(String.valueOf(endereco.getLatitude()));
                d.setLongitude(String.valueOf(endereco.getLongitude()));

                final StringBuilder s = new StringBuilder(); // -> Criando String com todas as linhas do endereco
                s.append("\nRua: " + d.getRua());
                s.append("\nNumero: " + d.getNumero());
                s.append("\nBairro: " + d.getBairro());
                s.append("\nCEP: " + d.getCep());

                alertDialog.setTitle("CORFIRME O DESTINO")
                        .setMessage(s)
                        .setCancelable(false)
                        .setPositiveButton("CHAMAR MOTORISTA",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, final int i) {

                                        salvarCorrida(d);

                                        Intent continuar = new Intent(getContext(), StatusActivity.class);
                                        continuar.putExtra("ENDERECO_DESTINO", s.toString());
                                        startActivity(continuar);

                                    }
                                }).setNegativeButton("CANCELAR",null).create().show();

            } else {
                toast("Ops! Endereço não encontrado.");
            }

        } else {
            toast("Para onde vamos?");
        }
    }

    public void toast(String mensagem){
        Toast.makeText(getActivity(), mensagem,
                Toast.LENGTH_SHORT).show();
    }

    public static void salvarCorrida(Destino destino) {

        Usuario u = UsuarioFirebase.getDadosUsuario();

        u.setLatitude(String.valueOf(localPassageiro.latitude));
        u.setLongitude(String.valueOf(localPassageiro.longitude));

        Corrida c = new Corrida();
        c.setDestino(destino);
        c.setPassageiro(u);
        c.setStatus(Status.BUSCANDO_MOTORISTA);
        c.salvarCorrida();


    }

    public Address recuperarEndereco(String endereco) {

        Geocoder g = new Geocoder(getActivity(), Locale.getDefault());

        try {

            List<Address> lista = g.getFromLocationName(endereco, 1);
            if (lista != null && lista.size() > 0) {

                Address local = lista.get(0);

                return local;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void iniComponentes(View v) {

        campoDestino = v.findViewById(R.id.editLocalDestino);
        campoLocal = v.findViewById(R.id.editLocalPassageiro);
        botoaBuscar = v.findViewById(R.id.btnBuscarMotorista);
        alertDialog = new AlertDialog.Builder(getActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
}