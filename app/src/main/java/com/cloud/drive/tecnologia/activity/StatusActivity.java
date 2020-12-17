package com.cloud.drive.tecnologia.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class StatusActivity extends AppCompatActivity implements OnMapReadyCallback {

    private TextView textEndereco, textStatus, textPreco;
    private Button botaoCancelar;
    private String idUsuario;
    private DatabaseReference database;
    private static String ID_CORRIDA_ATUAL;
    private LocationManager locManager;
    private LocationListener locListener;
    private LatLng localMotorista, localPassageiro;
    private String statusCorrida;
    private Usuario motorista, passageiro;
    private Corrida corrida;
    private Destino destino;
    private Marker markerM, markerP, markerD;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        iniComponentes();
        status();
        localPassageiro();
        corridaAtual();
        botoes();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        localPassageiro(); // -> Recuperando local do passageiro
    }

    public void botoes() {

        botaoCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // -> Só pode cancelar quando estiver buscando um motorista ou quando ele estiver a caminho
                if (statusCorrida.equals(Status.BUSCANDO_MOTORISTA) || statusCorrida.equals(Status.MOTORISTA_A_CAMINHO)) {

                    Snackbar.make(view, "Deseja cancelar o pedido?",
                            BaseTransientBottomBar.LENGTH_LONG)
                            .setBackgroundTint(getResources().getColor(R.color.backSnack))
                            .setAction("Cofirmar", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    corrida.setStatus(Status.CORRIDA_CANCELADA);
                                    corrida.atualizarStatus();

                                    Intent i = new Intent(getApplicationContext(),MenuActivity.class);
                                    startActivity(i);

                                }
                            }).show();

                } else {

                    Toast.makeText(getApplicationContext(),
                            "Você não pode mais cancelar a corrida!",
                            Toast.LENGTH_LONG).show();

                }

            }
        });

    }

    /**
     * Recuperando status atual da corrida
     */
    public void status() {

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                final DatabaseReference status = database.child("Corridas").child(ID_CORRIDA_ATUAL);

                status.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        Corrida corrida = snapshot.getValue(Corrida.class);
                        statusCorrida = corrida.getStatus();
                        textStatus.setText(corrida.getStatus());

                        if (corrida != null) {

                            if (!corrida.getStatus().equals(Status.CORRIDA_FINALIZADA)) { // -> Se o status estiver como finalizado

                                passageiro = corrida.getPassageiro();
                                localPassageiro = new LatLng( // -> Configurando local do passageiro
                                        Double.parseDouble(passageiro.getLatitude()),
                                        Double.parseDouble(passageiro.getLongitude())
                                );

                                statusCorrida = corrida.getStatus(); // -> Configurando status e destino
                                destino = corrida.getDestino();

                                if (corrida.getMotorista() != null) { // -> Se o motorista for diferente de null

                                    motorista = corrida.getMotorista(); // -> Configurando motorista
                                    localMotorista = new LatLng( // -> Recuperando local do motorista
                                            Double.parseDouble(motorista.getLatitude()),
                                            Double.parseDouble(motorista.getLongitude())
                                    );

                                }

                            }

                            configStatusCorrida(statusCorrida);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.i("STATUS", error.getMessage());
                    }
                });
            }
        }, 5000);
    }

    /**
     * Configurando status da carrida
     *
     * @param status
     */
    public void configStatusCorrida(String status) {

        switch (status) { // -> Configurando status requisicao
            case Status.BUSCANDO_MOTORISTA:
                statusBuscandoMotorista();
                break;
            case Status.MOTORISTA_A_CAMINHO:
                statusACaminho(); // -> Chamando metodo vom a configuracao da interface
                break;
            case Status.CORRIDA_EM_ANDAMENTO:
                statusAndamento(); // -> Chamando metodo
                break;
            case Status.CORRIDA_FINALIZADA:
                statusFinalizada(); // -> Chamando metodo
                break;
        }

    }

    /**
     * Recuperando localizacao do passageiro
     */
    public void localPassageiro() {

        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                double LATITUDE = location.getLatitude();
                double LONGITUDE = location.getLongitude();

                localPassageiro = new LatLng(LATITUDE, LONGITUDE); // -> Recuperando cordenadas

                if (statusCorrida != null && !statusCorrida.isEmpty()) {

                    if (statusCorrida.equals(Status.CORRIDA_EM_ANDAMENTO) || statusCorrida.equals(Status.CORRIDA_FINALIZADA)) {

                        locManager.removeUpdates(locListener);

                    } else {

                        // -> Solicitando atualizacao no GPS
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                            locManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000, 10, locListener);

                        }

                    }
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

        // -> Solicitando atualizacao no GPS
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000, 10, locListener);

        }

    }

    /**
     * Recuperando dados da corrida
     */
    public void corridaAtual() {

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                DatabaseReference corrida = database.child("CorridaAtual").child(idUsuario);

                corrida.addListenerForSingleValueEvent(new ValueEventListener() { // -> Recuperando id da corrida atual
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot != null) {
                            Corrida c = snapshot.getValue(Corrida.class);
                            ID_CORRIDA_ATUAL = c.getId();

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d("CORRIDA-ATUAL", error.getMessage());
                    }
                });
            }
        }, 1000);
    }

    /**
     * Configurando status "Buscando motorista"
     */
    public void statusBuscandoMotorista() {

        marcadorP(localPassageiro, "PASSAGEIRO"); // -> Add marcador do Passageiro

        mMap.moveCamera( // -> Configurando Zoom do marcador no centro
                CameraUpdateFactory.newLatLngZoom(localPassageiro, 20)
        );
    }

    /**
     * Configurando status "Motorista acaminho"
     */
    public void statusACaminho() {

        marcadorP(localPassageiro, "PASSAGEIRO"); // -> Add marcador do Passageiro

        marcadorM(localMotorista, "MOTORISTA"); // -> Add marcador do Motorista

        configurarCamera(markerM, markerP);

    }

    /**
     * Configurando status "Corrida em andamento"
     */
    public void statusAndamento() {

        botaoCancelar.setVisibility(View.GONE);

        marcadorM(localMotorista, "MOTORISTA"); // -> Exibindo marcador do motorista

        LatLng localDestino = new LatLng( // -> Add marcador de destino
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );

        marcadorD(localDestino, "DESTINO: " + destino.getRua());

        configurarCamera(markerM, markerD); // -> centralizando os marcadores

    }

    /**
     * Configurando status "Corrida Finalizada"
     */
    public void statusFinalizada() {

        if (markerM != null) // -> removendo marcador do motorista
            markerM.remove();

        if (markerD != null)
            markerD.remove();

        botaoCancelar.setVisibility(View.GONE);

        LatLng localDestino = new LatLng( // Add marcador do Destino
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );

        marcadorD(localDestino, "CHEGAMOS");

        mMap.moveCamera( // -> Configurando Zoom do marcador no centro
                CameraUpdateFactory.newLatLngZoom(localPassageiro, 20)
        );

        calcularCorrida(localDestino);
    }

    /**
     * Calculando distancia do local do passageiro até o destino
     * Obs: Calculando por quilometro percorrido
     * @param localDestino
     */
    public void calcularCorrida(LatLng localDestino){

        float distancia = CalculoDistancia
                .calcularDistancia(localPassageiro,localDestino);

        double valorBasico = distancia * 5.80; // -> Calculando distancia por R$ 5.80 KM
        DecimalFormat d = new DecimalFormat("00.00");
        String valorFinal = d.format(valorBasico);
        textPreco.setText("Fim da corrida - Total: " + valorFinal + " R$");

    }

    /**
     * Configurando marcador do Passageiro
     *
     * @param localP
     * @param titulo
     */
    public void marcadorP(LatLng localP, String titulo) {

        if (markerP != null)
            markerP.remove();

        markerP = mMap.addMarker(
                new MarkerOptions()
                        .title(titulo)
                        .position(localP)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.ic_passageiro_marker))
        );

    }

    /**
     * Configurando marcador do Motorista
     *
     * @param localM
     * @param titulo
     */
    public void marcadorM(LatLng localM, String titulo) {

        if (markerM != null)
            markerM.remove();

        markerM = mMap.addMarker(
                new MarkerOptions()
                        .title(titulo)
                        .position(localM).icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.ic_carro_marker))
        );

    }

    /**
     * Configurando marcador do Destino do Passageiro
     *
     * @param localD
     * @param titulo
     */
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
                                .fromResource(R.drawable.ic_bandeira_marker))
        );

    }

    /**
     * Configurando camera para centralizar os marcadores
     *
     * @param marker1
     * @param marker2
     */
    public void configurarCamera(Marker marker1, Marker marker2) {

        LatLngBounds.Builder b = new LatLngBounds.Builder(); // -> Definindo limite estre os marcadores Motorista e Passageiro
        b.include(marker1.getPosition());
        b.include(marker2.getPosition());

        LatLngBounds LIMITE = b.build();

        int WIDTH = getResources().getDisplayMetrics().widthPixels; // -> Recuperando largura da tela
        int HEIGTH = getResources().getDisplayMetrics().heightPixels; // -> Recuperando Altura da tela
        int PADDING = (int) (WIDTH * 0.20); // -> Calculando largura * 20%

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                        LIMITE, WIDTH, HEIGTH, PADDING
                )
        );

    }

    public void monitorar(final Usuario origem, LatLng destino, String status) {

        DatabaseReference database = ConfigFirebase.getDatabase();
        GeoFire gF = new GeoFire(database);

        final Circle raio = mMap.addCircle( // -> Add circulo no passageiro
                new CircleOptions()
                        .center(destino)
                        .radius(50) // -> Metros
                        .fillColor(Color.argb(30, 131, 111, 255))
                        .strokeColor(Color.argb(30, 0, 255, 255))

        );

        final GeoQuery geoQuery = gF.queryAtLocation(
                new GeoLocation(destino.latitude, destino.longitude), 05 // -> Em KM = 50 metros
        );
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                /**
                 * -> Utilizando o origem que sempre é o motorista
                 * -> Se o obj de origem esteja dentro da area de destino ai sera atualizado o status
                 */

                if (key.equals(origem)) {
                    textStatus.setText("Seu motorista chegou!"); // -> Configurando status da requisicao

                    geoQuery.removeAllListeners(); // -> Removendo pois o motorista ja chegou ao passageiro
                    raio.remove(); // -> Removendo o circulo de proximidade
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

    public void iniComponentes() {

        textEndereco = findViewById(R.id.textViewDadosDestino);
        textStatus = findViewById(R.id.textViewStatusCorrida);
        textPreco = findViewById(R.id.textPrecoCorridaPassageiro);
        botaoCancelar = findViewById(R.id.btnConfirmarConrrida);
        idUsuario = UsuarioFirebase.getIdUser();
        database = ConfigFirebase.getDatabase();
        corrida = new Corrida();

        Intent i = getIntent(); // -> Recebendo endereco e configurando no textView
        String endereco = i.getStringExtra("ENDERECO_DESTINO");
        textEndereco.setText(endereco);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onBackPressed() {

    }
}