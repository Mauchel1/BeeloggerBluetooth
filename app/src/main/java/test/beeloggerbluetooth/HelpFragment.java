package test.beeloggerbluetooth;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HelpFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HelpFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HelpFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HelpFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HelpFragment newInstance(String param1, String param2) {
        HelpFragment fragment = new HelpFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        String toolbarinfo = "In der oberen Toolbarleiste kann Bluetooth ein und ausgeschaltet werden. Au??erdem kann ??ber das Auswahlmen?? die Einstellungen angezeigt werden. Diese vor dem ersten Start bitte auf das eigene System anpassen. Mit dem CurrentData Men??eintrag k??nnen die aktuellen Werte, sofern ??bertragen, angezeigt werden. Hilfe ??ffnet Diese Seite.";
        String topInfo = "Der obere linke Button listet alle gepairten Bluetoothdevices auf. Diese k??nnen ausgew??hlt werden. Mit dem rechten Connectbutton kann ein ausgew??hltes Ger??t verbunden werden. ";

        String Websitetimeinfo = "Die Websitetime wird aus der Scraperwebsite des Beeloggers ermittelt. Die Zeit bestimmt des Zeitpunkt des zuletzt hochgeladenenen Datenpunkts. Beim Upload der Daten werden nur zeitlich danachliegende Datenpunkte hochgeladen um Mehrfachupload zu verhindern.";
        String FilenameInfo = "??bertr??gt den aktuellen Datensatz und den aktuellen Filename. Der Name wird zum abspeichern eines Datensatzes ben??tigt.";

        String Ablaufinfo1 = "Zun??chst Bluetooth einschalten. Dann BluetoothDevices auflisten, das gew??nschte Device ausw??hlen und ??ber den Connectbutton verbinden. Dies schaltet bei Erfolg die unteren Buttons frei.";
        String Ablaufinfo2 = "Um einen einzelnen (aktuellen) Datensatz anzuzeigen kann getFilename genutzt werden. Die Daten sind anschlie??end im Toolbareintrag CurrentData zu finden.";
        String Ablaufinfo3 = "Den Gesamtdatensatz von der Stockwaage wird ??ber Data abgerufen. Dies dauert je nach Datensatzanzahl einige Sekunden. (Anzahl der ??bertragenen Datens??tze unten rechts an der Progressbar).";
        String Ablaufinfo4 = "Der Datensatz kann mit dem Savebutton abgespeichert werden. Der Speicherort wird angezeigt. Vorher Filename abrufen. ";
        String Ablaufinfo5 = "Mittels Upload wird der empfangene Datensatz auf den in den Settings hinterlegten Webserver ??bertragen. Vorher WebsiteTime abrufen.";
        String Ablaufinfo6 = "Nach erfolgreichem Upload der Daten bitte ein neues File ??ber den Button anlegen, um k??nftigen Datentransfer gering zu halten.";
        String Ablaufinfo7 = "Zuletzt nicht das Ausschalten des Serviceschalters am Beelogger vergessen ;-)";

        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_help, container, false);

    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item=menu.findItem(R.id.action_bt);
        if(item!=null)
            item.setVisible(false);
    }

}