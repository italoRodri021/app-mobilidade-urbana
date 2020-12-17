package com.cloud.drive.tecnologia.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.config.CalculoDistancia;
import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.helper.Status;
import com.cloud.drive.tecnologia.model.Corrida;
import com.cloud.drive.tecnologia.model.Destino;
import com.cloud.drive.tecnologia.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.text.DecimalFormat;

public class CorridaFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Button botaoAceitar, botaoVerRota;
    private TextView textStatus;
    private TextView textValorCorrida;
    private TextView textPontoB;
    private LocationManager locManager;
    private LocationListener locListener;
    private DatabaseReference database;
    private LatLng localMotorista, localPassageiro;
    private Usuario motorista, passageiro;
    private Marker markerM, markerP, markerD;
    private String idMotorista;
    private Corrida corrida;
    private Destino destino;
    private String idCorrida;
    private String statusCorrida;

    public CorridaFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_corrida, container, false);

        iniComponentes(v);
        status();
        botoes();
        localMotorista();


        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        status();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        localMotorista();
    }

    public void botoes() {

        botaoAceitar.setOnClickListener(new View.OnClickListener() { // -> Botao aceitar corrida
            @Override
            public void onClick(View view) {

                Snackbar.make(view, "DESEJA FAZER ESTA CORRIDA?",
                        BaseTransientBottomBar.LENGTH_SHORT)
                        .setBackgroundTint(getResources().getColor(R.color.backSnack))
                        .setAction("CONFIRMAR", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                aceitarCorrida(); // -> Chamando metodo para aceitar corrida

                            }
                        }).show();

            }
        });

        botaoVerRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String status = statusCorrida;

                if (!status.isEmpty()) {

                    String latitude = "";
                    String longitude = "";

                    switch (status) {
                        case Status.MOTORISTA_A_CAMINHO:
                            latitude = String.valueOf(localPassageiro.latitude);
                            longitude = String.valueOf(localPassageiro.longitude);
                            break;
                        case Status.CORRIDA_EM_ANDAMENTO: // -> Quando clicar vai abrir a navegacao já com as cordenadas de destino
                            latitude = destino.getLatitude();
                            longitude = destino.getLongitude();
                            break;
                    }

                    Uri uri = Uri.parse("google.navigation:q=" + latitude + "," + longitude + "&mode=d"); // -> Chamando intent para abrir o maps com a rota
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    i.setPackage("com.google.android.apps.maps");
                    startActivity(i);


                }

            }
        });

    }

    public void status() {

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                DatabaseReference db = database.child("Corridas").child(idCorrida);

                db.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        corrida = snapshot.getValue(Corrida.class);
                        textStatus.setText(corrida.getStatus());

                        if (corrida != null) {

                            passageiro = corrida.getPassageiro(); // -> Recuperando local do passageiro

                            localPassageiro = new LatLng(
                                    Double.parseDouble(passageiro.getLatitude()),
                                    Double.parseDouble(passageiro.getLongitude())
                            );
                            statusCorrida = corrida.getStatus(); // -> Recuperando Status da corrida
                            destino = corrida.getDestino(); // -> Recuperando destino do passageiro

                            confStatusCorrida(statusCorrida);

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Log.d("ERRO", error.getMessage());

                    }
                });

            }
        }, 3000);

    }

    private void confStatusCorrida(String status){

        switch (status) {
            case Status.BUSCANDO_MOTORISTA:
                statusAguardando();
                break;
            case Status.MOTORISTA_A_CAMINHO:
                statusACaminho();
                break;
            case Status.CORRIDA_EM_ANDAMENTO:
                statusEmAndamento();
                break;
            case Status.CORRIDA_FINALIZADA:
                statusFinalizada();
                break;
        }

    }

    public void statusAguardando() {

        botaoAceitar.setClickable(true);
        botaoAceitar.setText("ACEITAR CORRIDA");
        textStatus.setText("Deseja fazer esta corrida?");
        botaoVerRota.setVisibility(View.GONE);

        marcadorM(localMotorista, "MEU LOCAL");

        String enderecoPontoB = "B - " + destino.getRua() + ", " + destino.getNumero() + " - " + destino.getBairro();
        textPontoB.setText(enderecoPontoB);
    }

    public void statusACaminho() {

        botaoAceitar.setClickable(false);
        botaoAceitar.setText("VOCÊ ACEITOU A CORRIDA!");
        textStatus.setText("Inicie a rota até o passageiro!");
        botaoVerRota.setVisibility(View.VISIBLE);

        marcadorM(localMotorista, motorista.getNome());
        marcadorP(localPassageiro, passageiro.getNome());
        moverCamera(markerM, markerP);
        monitorar(motorista, localPassageiro, Status.CORRIDA_EM_ANDAMENTO);
    }

    public void statusEmAndamento() {

        botaoVerRota.setVisibility(View.VISIBLE);
        botaoAceitar.setText("VOCÊ ACEITOU A CORRIDA!");
        textStatus.setText("A caminho do destino!");

        marcadorM(localMotorista, "MEU LOCAL");

        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );

        marcadorD(localDestino, "DESTINO: " + destino.getRua());

        moverCamera(markerM, markerD);

        monitorar(motorista, localDestino, Status.CORRIDA_FINALIZADA);
    }

    public void statusFinalizada() {

        botaoVerRota.setVisibility(View.GONE);

        if (markerM != null) // -> removendo marcador do motorista
            markerM.remove();

        if (markerD != null)
            markerD.remove();

        LatLng localDestino = new LatLng( // -> Recuperando cordenadas
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );

        marcadorD(localDestino, "CHEGAMOS"); // -> Add Marcador do destino
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(localDestino, 20)
        );

        calculoCorrida(localDestino);

    }

    public void calculoCorrida(LatLng localDestino) {

        float distancia = CalculoDistancia
                .calcularDistancia(localPassageiro, localDestino);

        double valorBasico = distancia * 5.80; // -> Calculando distancia por R$ 5.80 KM
        DecimalFormat d = new DecimalFormat("00.00");
        String valorFinal = d.format(valorBasico);
        textValorCorrida.setText("Fim da corrida - Total: " + valorFinal + " R$");

    }

    public void localMotorista() {

        locManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                double LATITUDE = location.getLatitude();
                double LONGITUDE = location.getLongitude();
                localMotorista = new LatLng(LATITUDE, LONGITUDE);

                UsuarioFirebase.salvarGeoFire(LATITUDE, LONGITUDE);

                if (motorista != null && corrida != null) {

                    motorista.setLatitude(String.valueOf(motorista.getLatitude()));
                    motorista.setLongitude(String.valueOf(motorista.getLongitude()));
                    corrida.setMotorista(motorista);
                    corrida.atualizarLocalMotorista();

                    confStatusCorrida(statusCorrida);

                }
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
                    2000, 5, locListener
            );
        }


    }

    public void aceitarCorrida() {

        corrida = new Corrida();
        corrida.setId(idCorrida);
        corrida.setMotorista(motorista);
        corrida.setStatus(Status.MOTORISTA_A_CAMINHO);
        corrida.corridaAceita();

        DatabaseReference db = database
                .child("CorridasAceita")
                .child(idMotorista);
        db.child("id").setValue(idCorrida);

    }

    public void monitorar(final Usuario origem, LatLng destino, final String status) {

        DatabaseReference database = ConfigFirebase.getDatabase().child("LocalGeofire");
        GeoFire gF = new GeoFire(database);

        final Circle raio = mMap.addCircle(
                new CircleOptions()
                        .center(destino)
                        .radius(50) // -> Metros
                        .fillColor(Color.argb(30, 131, 111, 255))
                        .strokeColor(Color.argb(30, 0, 255, 255))
        );

        final GeoQuery geoQuery = gF.queryAtLocation(

                new GeoLocation(destino.latitude, destino.longitude), 0.05
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                if (key.equals(origem.getId())) {
                    Log.i("onKeyEntered", "Motorista Dentro da area");

                    corrida.setStatus(status);
                    corrida.atualizarStatus();

                    geoQuery.removeAllListeners();
                    raio.remove();
                }

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    public void marcadorM(LatLng localM, String titulo) {

        if (markerM != null)
            markerM.remove();

        markerM = mMap.addMarker(
                new MarkerOptions()
                        .title(titulo)
                        .position(localM)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.ic_carro_marker))
        );

    }

    public void marcadorP(LatLng localP, String titulo) {

        if (markerP != null)
            markerP.remove();

        markerP = mMap.addMarker(
                new MarkerOptions()
                        .title(titulo)
                        .position(localP)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.ic_ponto_a_pequeno))
        );

    }

    public void marcadorD(LatLng localD, String titulo) {

        if (markerP != null)
            markerP.remove();

        if (markerD != null)
            markerD.remove();

        markerD = mMap.addMarker(
                new MarkerOptions()
                        .title(titulo)
                        .position(localD)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.ic_ponto_b_pequeno))
        );

    }

    public void moverCamera(Marker marker1, Marker marker2) {

        LatLngBounds.Builder b = new LatLngBounds.Builder();
        b.include(marker1.getPosition());
        b.include(marker2.getPosition());


        LatLngBounds LIMITE = b.build();

        int WIDTH = getResources().getDisplayMetrics().widthPixels;
        int HEIGTH = getResources().getDisplayMetrics().heightPixels;
        int PADDING = (int) (WIDTH * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                        LIMITE, WIDTH, HEIGTH, PADDING));

    }

    public void iniComponentes(View v) {

        botaoAceitar = v.findViewById(R.id.btnAceitarCorrida);
        botaoVerRota = v.findViewById(R.id.btnIniciarRota);
        textStatus = v.findViewById(R.id.textStatusCorridaMotorista);
        textValorCorrida = v.findViewById(R.id.textPrecoCorridaMotorista);
        textPontoB = v.findViewById(R.id.textLocalDestinoPontoB);
        idMotorista = UsuarioFirebase.getIdUser();
        database = ConfigFirebase.getDatabase();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle b = getArguments();
        idCorrida = b.getString("ID_CORRIDA");
        motorista = (Usuario) b.getSerializable("MOTORISTA");

    }

}