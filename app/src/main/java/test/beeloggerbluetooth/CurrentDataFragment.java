package test.beeloggerbluetooth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CurrentDataFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CurrentDataFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String currentData;
    private String currentSystem;
    private String aux;

    private final ArrayList<TextView> textViews = new ArrayList<>();
    ;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public CurrentDataFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CurrentDataFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CurrentDataFragment newInstance(String param1, String param2) {
        CurrentDataFragment fragment = new CurrentDataFragment();
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

        currentData = "";
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViews.clear();
        textViews.add(requireActivity().findViewById(R.id.tv_Datum));
        textViews.add(requireActivity().findViewById(R.id.tv_Uhrzeit));
        textViews.add(requireActivity().findViewById(R.id.tv_Temperatur_wz));
        textViews.add(requireActivity().findViewById(R.id.tv_Temperatur_Innen_1));
        textViews.add(requireActivity().findViewById(R.id.tv_Temperatur_Aussen));
        textViews.add(requireActivity().findViewById(R.id.tv_Luftfeuchte_Innen_1));
        textViews.add(requireActivity().findViewById(R.id.tv_Luftfeuchte_Aussen));
        textViews.add(requireActivity().findViewById(R.id.tv_Lichtstaerke));
        textViews.add(requireActivity().findViewById(R.id.tv_Gewicht_1));
        textViews.add(requireActivity().findViewById(R.id.tv_Akkuspannung));
        textViews.add(requireActivity().findViewById(R.id.tv_Solarspannung));
        textViews.add(requireActivity().findViewById(R.id.tv_Service));
        textViews.add(requireActivity().findViewById(R.id.tv_Aux_1));
        textViews.add(requireActivity().findViewById(R.id.tv_Aux_2));
        textViews.add(requireActivity().findViewById(R.id.tv_Aux_3));

        //currentData = "2023/01/12 00:40:12,20.8,,,19.6,,,55.0,,-0.01,4.23,8.11,0.13,18.00,,,19.50,,"; //Teststring Duo //TODO uncomment
        //currentData = "2019/02/13_19:00,19.3,18.7,,,21.7,2.47,6.23,6.23,43,"; //Teststring beelogger
        //currentData = "2022/05/13 12:15:15,10.10,11.11,12.22,13.33,19.90,21.11,22.22,23.33,29.99,123456.78,31.11,32.22,33.33,4.11,1.55,44.1,101.1,102.2,103.3"; //Teststring Triple
        //currentData = "2019/05/13 12:14:15,10.0,,,,14.4,19.9,21.1,22.2,,,29.9,123456.7,,32.2,33.3,34.4,,1.5,44.0"; //Teststring Quad

        currentData = ((MainActivity) requireActivity()).getCurrentData(); // TODO insecure cast

        requireActivity().findViewById(R.id.tv_error).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tableRow_Aux).setVisibility(View.GONE);
        SharedPreferences pref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        currentSystem = pref.getString("Systemtyp", getResources().getString(R.string.Systemtyp));
        ((TextView) requireActivity().findViewById(R.id.tv_System)).setText(currentSystem);
        aux = pref.getString("Aux", getResources().getString(R.string.Aux));
        ((TextView) requireActivity().findViewById(R.id.tv_aux)).setText(aux);

        if (currentData.contains(",")) {
            displayData();
        } else {
            resetView();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        String value = null;
        if (getArguments() != null) {
            value = getArguments().getString("CurrentData");
        }
        value = "";
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_current_data, container, false);
    }

    private void resetView() {

        String defaultTitle = "Noch kein aktueller Datensatz übertragen";
        String defaultText = "-";

        ((TextView) requireActivity().findViewById(R.id.tv_currentData_Title)).setText(defaultTitle);
        ((TextView) requireActivity().findViewById(R.id.tv_raw_currentData)).setText("");

        for (TextView tv : textViews) {
            tv.setText(defaultText);
        }

        requireActivity().findViewById(R.id.tv_Temperatur_Innen_2).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Temperatur_Innen_3).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Temperatur_Innen_4).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Luftfeuchte_Innen_2).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Luftfeuchte_Innen_3).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Luftfeuchte_Innen_4).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Gewicht_2).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Gewicht_3).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tv_Gewicht_4).setVisibility(View.GONE);
        requireActivity().findViewById(R.id.tableRow_Aux).setVisibility(View.GONE);
    }

    public void setCurrentData(String currentData) {
        if (!currentData.isEmpty()) {
            this.currentData = currentData;
        }
    }

    private void displayData() {

        ((TextView) requireActivity().findViewById(R.id.tv_raw_currentData)).setText(currentData);


        if (currentSystem.toLowerCase().contains("beelogger")) {
            addTextviews(1);
            processData();
        } else if (currentSystem.toLowerCase().contains("duo")) {
            addTextviews(2);
            processData();
        } else if (currentSystem.toLowerCase().contains("triple")) {
            addTextviews(3);
            processData();
        } else if (currentSystem.toLowerCase().contains("quad")) {
            addTextviews(4);
            processData();
        } else {
            ((TextView) requireActivity().findViewById(R.id.tv_error)).setVisibility(View.VISIBLE);
        }

        //  2023/01/12 00:40:12,20.8 ,    ,    ,19.6,    ,    ,55.0,   ,-0.01,4.23,8.11,0.13,18.00  ,  ,  ,19.50, ,
        //  Date  Uhr          ,tmpwz,tmp1,tmp2,tmpa,hum1,hum2,huma,lux,g1   ,g2  ,VA  ,VS  ,Service,A1,A2,A3   ,-,-
    }

    private void processData() {
        String[] data = currentData.split(",", -1); //split limit -1 for not deleting trailing empty strings
        if (data.length >= textViews.size() - 1) {
            for (int i = 0; i < textViews.size() - 1; i++) {
                textViews.get(i + 1).setText(data[i]);
            }
            textViews.get(0).setText(data[0].split(" ")[0]);
            try {
                textViews.get(1).setText(data[0].split(" ")[1]);
            } catch (Exception ignored) {
                textViews.get(1).setText(data[0].split(" ")[0]);
            }

        } else {
            ((TextView) requireActivity().findViewById(R.id.tv_error)).setVisibility(View.VISIBLE);
        }
    }

    private void addTextviews(int datensatzzahl) {

        textViews.clear();
        FragmentActivity a = requireActivity();

        textViews.add(a.findViewById(R.id.tv_Datum));
        textViews.add(a.findViewById(R.id.tv_Uhrzeit));
        textViews.add(a.findViewById(R.id.tv_Temperatur_wz));
        textViews.add(a.findViewById(R.id.tv_Temperatur_Innen_1));
        if (datensatzzahl > 1) {
            textViews.add(a.findViewById(R.id.tv_Temperatur_Innen_2));
            a.findViewById(R.id.tv_Temperatur_Innen_2).setVisibility(View.VISIBLE);
        }
        if (datensatzzahl > 2) {
            textViews.add(a.findViewById(R.id.tv_Temperatur_Innen_3));
            a.findViewById(R.id.tv_Temperatur_Innen_3).setVisibility(View.VISIBLE);
        }
        if (datensatzzahl > 3) {
            textViews.add(a.findViewById(R.id.tv_Temperatur_Innen_4));
            a.findViewById(R.id.tv_Temperatur_Innen_4).setVisibility(View.VISIBLE);
        }
        textViews.add(a.findViewById(R.id.tv_Temperatur_Aussen));
        textViews.add(a.findViewById(R.id.tv_Luftfeuchte_Innen_1));
        if (datensatzzahl > 1) {
            textViews.add(a.findViewById(R.id.tv_Luftfeuchte_Innen_2));
            a.findViewById(R.id.tv_Luftfeuchte_Innen_2).setVisibility(View.VISIBLE);
        }
        if (datensatzzahl > 2) {
            textViews.add(a.findViewById(R.id.tv_Luftfeuchte_Innen_3));
            a.findViewById(R.id.tv_Luftfeuchte_Innen_3).setVisibility(View.VISIBLE);
        }
        if (datensatzzahl > 3) {
            textViews.add(a.findViewById(R.id.tv_Luftfeuchte_Innen_4));
            a.findViewById(R.id.tv_Luftfeuchte_Innen_4).setVisibility(View.VISIBLE);
        }
        textViews.add(a.findViewById(R.id.tv_Luftfeuchte_Aussen));
        textViews.add(a.findViewById(R.id.tv_Lichtstaerke));
        textViews.add(a.findViewById(R.id.tv_Gewicht_1));
        if (datensatzzahl > 1) {
            textViews.add(a.findViewById(R.id.tv_Gewicht_2));
            a.findViewById(R.id.tv_Gewicht_2).setVisibility(View.VISIBLE);
        }
        if (datensatzzahl > 2) {
            textViews.add(a.findViewById(R.id.tv_Gewicht_3));
            a.findViewById(R.id.tv_Gewicht_3).setVisibility(View.VISIBLE);
        }
        if (datensatzzahl > 3) {
            textViews.add(a.findViewById(R.id.tv_Gewicht_4));
            a.findViewById(R.id.tv_Gewicht_4).setVisibility(View.VISIBLE);
        }
        textViews.add(a.findViewById(R.id.tv_Akkuspannung));
        textViews.add(a.findViewById(R.id.tv_Solarspannung));
        textViews.add(a.findViewById(R.id.tv_Service));
        if (aux.equals("1")){
            textViews.add(a.findViewById(R.id.tv_Aux_1));
            textViews.add(a.findViewById(R.id.tv_Aux_2));
            textViews.add(a.findViewById(R.id.tv_Aux_3));
            a.findViewById(R.id.tableRow_Aux).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_bt);
        if (item != null)
            item.setVisible(false);
        item = menu.findItem(R.id.action_currentData);
        if (item != null)
            item.setVisible(false);
    }
}