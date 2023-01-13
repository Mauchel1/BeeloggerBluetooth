package test.beeloggerbluetooth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import test.beeloggerbluetooth.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences pref = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        EditText editTextScraperCoreUrl = view.findViewById(R.id.editTextScraperCoreUrl);
        EditText editTextSketchID = view.findViewById(R.id.editTextSketchID);
        EditText editTextAux = view.findViewById(R.id.editTextAux);
        EditText editTextScraperSelect = view.findViewById(R.id.editTextScraperSelect);
        EditText editTextPfad = view.findViewById(R.id.editTextPfad);
        EditText editTextPassword = view.findViewById(R.id.editTextPassword);
        EditText editTextServerdatei = view.findViewById(R.id.editTextServerdatei);
        EditText editTextSystemkennung = view.findViewById(R.id.editTextSystemkennung);
        EditText editTextSystemtyp = view.findViewById(R.id.editTextSystemtyp);
        EditText editTextWebserver = view.findViewById(R.id.editTextWebserver);
        EditText editTextZeitsynchronisation = view.findViewById(R.id.editTextZeitsynchronisation);

        editTextScraperCoreUrl.setText(pref.getString("ScraperCoreUrl", getResources().getString(R.string.defaultScraperUrl)));
        editTextScraperSelect.setText(pref.getString("ScraperSelect", getResources().getString(R.string.ScraperSelect)));

        editTextWebserver.setText(pref.getString("Webserver", getResources().getString(R.string.Webserver)));
        editTextPfad.setText(pref.getString("Pfad", getResources().getString(R.string.Pfad)));
        editTextSystemtyp.setText(pref.getString("Systemtyp", getResources().getString(R.string.Systemtyp)));
        editTextServerdatei.setText(pref.getString("Serverdatei", getResources().getString(R.string.Serverdatei)));
        editTextPassword.setText(pref.getString("Password", getResources().getString(R.string.Password)));
        editTextZeitsynchronisation.setText(pref.getString("Zeitsynchronisation", getResources().getString(R.string.Zeitsynchronisation)));
        editTextAux.setText(pref.getString("Aux", getResources().getString(R.string.Aux)));
        editTextSketchID.setText(pref.getString("SketchID", getResources().getString(R.string.SketchID)));
        editTextSystemkennung.setText(pref.getString("Systemkennung", getResources().getString(R.string.Systemkennung)));


        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Save Setting Values
                SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("ScraperCoreUrl", String.valueOf(editTextScraperCoreUrl.getText()));
                editor.putString("ScraperSelect", String.valueOf(editTextScraperSelect.getText()));
                editor.putString("Webserver", String.valueOf(editTextWebserver.getText()));
                editor.putString("Pfad", String.valueOf(editTextPfad.getText()));
                editor.putString("Systemtyp", String.valueOf(editTextSystemtyp.getText()));
                editor.putString("Serverdatei", String.valueOf(editTextServerdatei.getText()));
                editor.putString("Password", String.valueOf(editTextPassword.getText()));
                editor.putString("Systemkennung", String.valueOf(editTextSystemkennung.getText()));
                editor.putString("Zeitsynchronisation", String.valueOf(editTextZeitsynchronisation.getText()));
                editor.putString("SketchID", String.valueOf(editTextSketchID.getText()));
                editor.putString("Aux", String.valueOf(editTextAux.getText()));

                editor.apply();

                NavHostFragment.findNavController(SettingsFragment.this)
                        .navigate(R.id.action_SettingsFragment_to_MainFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item=menu.findItem(R.id.action_bt);
        if(item!=null)
            item.setVisible(false);
    }

}