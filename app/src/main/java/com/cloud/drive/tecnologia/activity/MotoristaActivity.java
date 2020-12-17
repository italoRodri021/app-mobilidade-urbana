package com.cloud.drive.tecnologia.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cloud.drive.tecnologia.R;
import com.cloud.drive.tecnologia.adapter.AdapterCorridas;
import com.cloud.drive.tecnologia.config.ConfigFirebase;
import com.cloud.drive.tecnologia.config.UsuarioFirebase;
import com.cloud.drive.tecnologia.fragment.CorridaFragment;
import com.cloud.drive.tecnologia.helper.GerenciadorClicks;
import com.cloud.drive.tecnologia.helper.Status;
import com.cloud.drive.tecnologia.model.Corrida;
import com.cloud.drive.tecnologia.model.Usuario;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MotoristaActivity extends AppCompatActivity {

    private CorridaFragment corridaFrag;
    private DatabaseReference database;
    private RecyclerView recyclerCorridas;
    private TextView textResultado;
    private AdapterCorridas adapter;
    private Usuario motorista;
    private String idMotorista;
    private List<Corrida> corridaLista = new ArrayList<>();
    private LocationManager locManager;
    private LocationListener locListener;
    private Corrida corrida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motorista);

        iniComponentes();
        localMotorista();
        recuperandoCorridas();
        //corridaAndamento();

    }

    /**
     * Verificando se o motorista ja possui uma corrida em curso
     */
    public void corridaAndamento() {

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                DatabaseReference db = database.child("CorridasAceita").child(idMotorista);
                Query pesquisa = db.orderByChild("id").equalTo(corrida.getId());

                pesquisa.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Corrida c = snapshot.getValue(Corrida.class);
                        String id = c.getId();


                        DatabaseReference corrida = database.child("Corridas").child(id);
                        corrida.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Corrida c = snapshot.getValue(Corrida.class);
                                String status = c.getStatus();


                                if (status.equals(Status.MOTORISTA_A_CAMINHO) || status.equals(Status.CORRIDA_EM_ANDAMENTO)) {

                                    Log.i("STATUS", status);
                                    Log.i("MOTORISTA", motorista.getNome());

                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                                Log.i("ERRO", error.getMessage());

                            }
                        });


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Log.i("ERRO", error.getMessage());

                    }
                });

            }
        }, 5000);


    }

    /**
     * Recuperando Localização do motorista
     */
    public void localMotorista() {

        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                String LATITUDE = String.valueOf(location.getLatitude());
                String LONGITUDE = String.valueOf(location.getLongitude());

                motorista.setLatitude(LATITUDE); // -> Salvando codernadas do motorista
                motorista.setLongitude(LONGITUDE);

                UsuarioFirebase.salvarGeoFire( // -> Salvando localizacao no GeoFire
                        location.getLatitude(),
                        location.getLongitude());

                clickItemRecycler(); // -> Configurando click apenas quando já tiver uma latitude e longitude recuperadas
                locManager.removeUpdates(locListener);   // -> Removendo processo de atualizacao de localizacao do motorista
                adapter.notifyDataSetChanged(); // -> Notificando adapter sobre as mudancas

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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            locManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0, 0, locListener);

        }

    }

    /**
     * Configurando eventos de click
     */
    public void clickItemRecycler() {

        recyclerCorridas.addOnItemTouchListener(
                new GerenciadorClicks(this, recyclerCorridas,
                        new GerenciadorClicks.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {

                                Corrida c = corridaLista.get(position);

                                abrirCorrida(c.getId(), motorista); // -> Chamando metodo

                            }

                            @Override
                            public void onLongItemClick(View view, int position) {

                            }

                            @Override
                            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                            }
                        }));

    }

    /**
     * Metodo para abrir a tela de corrida quando for clicado no
     * item do recycler ou quando for confirmado que existe uma corrida em andamento
     *
     * @param idCorrida
     */
    public void abrirCorrida(String idCorrida, Usuario motorista) {

        corridaFrag = new CorridaFragment();
        FragmentTransaction retomarCorrida = getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.containerCorrida, corridaFrag);

        Bundle b = new Bundle();
        b.putString("ID_CORRIDA", idCorrida);
        b.putSerializable("MOTORISTA", motorista);
        corridaFrag.setArguments(b);

        retomarCorrida.commit();
    }

    /**
     * Recuperando corridas sem motorista
     */
    public void recuperandoCorridas() {

        /**
         * Utilizando Controladora para resolver o bug que quando
         * iniciava a activity Motorista não recuperava a lita de corridas a tempo
         */

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                DatabaseReference corrida = database.child("Corridas");
                Query pesquisa = corrida.orderByChild("status")
                        .equalTo(Status.BUSCANDO_MOTORISTA);

                pesquisa.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.getChildrenCount() > 0) { // -> Contando quantas corridas tem no snapshot

                            textResultado.setVisibility(View.GONE);
                            recyclerCorridas.setVisibility(View.VISIBLE);
                        } else {

                            textResultado.setVisibility(View.VISIBLE);
                            recyclerCorridas.setVisibility(View.GONE);
                        }

                        corridaLista.clear(); // -> Limpando lista

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Corrida corridas = ds.getValue(Corrida.class);

                            corridaLista.add(corridas);
                        }
                        adapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                        Log.d("ERRO", error.getMessage());

                    }
                });

            }
        }, 10000);

    }

    public void iniComponentes() {

        textResultado = findViewById(R.id.textResultado);
        recyclerCorridas = findViewById(R.id.recyclerCorridas);

        idMotorista = UsuarioFirebase.getIdUser();
        database = ConfigFirebase.getDatabase();
        motorista = UsuarioFirebase.getDadosUsuario();
        corrida = new Corrida();

        adapter = new AdapterCorridas(corridaLista, getApplicationContext(), motorista);
        recyclerCorridas.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerCorridas.setAdapter(adapter);
        recyclerCorridas.setHasFixedSize(true);

    }

}