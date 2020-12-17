package com.cloud.drive.tecnologia.config;

import android.location.Location;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class CalculoDistancia {

    public static float calcularDistancia(LatLng pontoA, LatLng pontoB){

        Location A = new Location("PONTO A");
        A.setLatitude(pontoA.latitude);
        A.setLongitude(pontoA.longitude);

        Location B = new Location("PONTO B");
        B.setLatitude(pontoB.latitude);
        B.setLongitude(pontoB.longitude);

        float distancia = A.distanceTo(B) / 1000; // -> Calculando distancia do PONTO A ao PONTO B divindindo por 1000 para retornar em KM

        return distancia;
    }

    public static String formatoDistancia(float distancia){

        String formato= "";
        if(distancia < 1){
            distancia = distancia * 1000; // -> Pegando a distancia e multriplicando por 1000 para converter em metros
            formato = Math.round(distancia) + " Metros ";
        }else if(distancia > 1){
            DecimalFormat d = new DecimalFormat("00.00");
            formato = d.format(distancia) + " Km ";
        }
        return  formato;
    }

}
