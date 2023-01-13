package test.beeloggerbluetooth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import test.beeloggerbluetooth.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private String currentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); //call before any findViewByIDs!

        setSupportActionBar(binding.toolbar);


        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.action_global_FirstFragment);
                return true;
            case R.id.action_currentData:
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.action_global_currentDataFragment);
                return true;
            case R.id.action_help:
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main).navigate(R.id.action_global_helpFragment);
                return true;
            case R.id.action_bt:
                Fragment host =  getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
                Fragment firstStackFragment = host.getChildFragmentManager().getPrimaryNavigationFragment();
                try {
                    SecondFragment f = (SecondFragment) firstStackFragment;
                    f.BluetoothButtonHandling(item); //TODO CAST MIT IF ÜBERPRÜFEN
                    } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }

    public void setCurrentData(String cd){
        this.currentData = cd;
    }

    public String getCurrentData(){
        if (currentData != null) {
            return currentData;
        } else {
            return "";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}