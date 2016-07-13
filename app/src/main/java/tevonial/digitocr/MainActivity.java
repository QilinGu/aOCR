package tevonial.digitocr;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import tevonial.neural.Layer;
import tevonial.neural.Network;
import tevonial.neural.Neuron;

public class MainActivity extends AppCompatActivity {
    private DrawingView draw;
    private ListView list;
    private ResultAdapter resultAdapter;
    private Network net;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        if (net == null) {
            new NetworkLoader().execute();
        }

        draw = new DrawingView(this) {
            @Override
            void onTouchUp(int[] pixels) {
                guessDigit(pixels);
            }
        };
        ((RelativeLayout) findViewById(R.id.draw)).addView(draw);

        ArrayList<Integer> outputs = new ArrayList<>();
        ArrayList<Double> values = new ArrayList<>();
        for (int i=0; i<10; i++) {
            outputs.add(i);
            values.add(0.0);
        }

        list = (ListView) findViewById(R.id.list);
        resultAdapter = new ResultAdapter(this, outputs);
        list.setAdapter(resultAdapter);
        resultAdapter.setValues(values, -1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_input:
                Intent i = new Intent(getApplicationContext(), InputActivity.class);
                startActivity(i);
                break;
            case R.id.action_select_network:
                new FileChooser(this).setFileListener(new FileChooser.FileSelectedListener() {
                    @Override
                    public void fileSelected(final File file) {
                        new NetworkLoader().execute(file);
                    }}).showDialog();
                break;
            case R.id.action_default_network:
                new NetworkLoader().execute();
                break;
        }
        return true;
    }

    public void guessDigit(int[] data) {
        ArrayList<Double> values = new ArrayList<>();

        double[] input = new double[data.length];
        for (int j = 0; j < data.length; j++) {
            input[j] = data[j] & 0xff;
        }

        double[] output = net.process(input, null, false, null);
        double max = 0.0; int guess = 0;
        for (int j=0; j<10; j++) {
            values.add(output[j] * 100.0);
            if (output[j] > max) {
                max = output[j];
                guess = j;
            }
        }

        resultAdapter.setValues(values, guess);
    }

    private class ResultAdapter extends ArrayAdapter<Integer> {
        private Context context;
        private ArrayList<Integer> outputs;
        private ArrayList<Double> values;
        private String f = "%3.4f";
        private int guess = 0;

        public ResultAdapter(Context context, ArrayList<Integer> outputs) {
            super(context, R.layout.result_row, outputs);
            this.context = context; this.outputs = outputs;
        }

        public void setValues(ArrayList<Double> values, int guess) {
            this.values = values;
            this.guess = guess;
            notifyDataSetChanged();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.result_row, parent, false);

            ((TextView) rowView.findViewById(R.id.label)).setText(String.valueOf(outputs.get(position)));
            ((TextView) rowView.findViewById(R.id.value)).setText(String.format(f, values.get(position)));

            if (position == guess) {
                rowView.setBackgroundColor(getColor(R.color.colorPrimary));
                ((TextView) rowView.findViewById(R.id.label)).setTextColor(Color.WHITE);
                ((TextView) rowView.findViewById(R.id.value)).setTextColor(Color.WHITE);
            }

            return rowView;
        }
    }

    private class NetworkLoader extends AsyncTask<File, Void, Void> {
        ProgressDialog ringProgressDialog;
        Network temp = null;
        String filename = "";

        @Override
        protected Void doInBackground(File... f) {
            InputStream fis;

            try {
                fis = new FileInputStream(f[0]);
                filename = f[0].getName();
            } catch (FileNotFoundException | ArrayIndexOutOfBoundsException e) {
                fis = getResources().openRawResource(R.raw.net_default);
                filename = "net_default";
            }

            try {
                Kryo kryo = new Kryo();

                kryo.register(Network.class, 0);
                kryo.register(Layer.class, 1);
                kryo.register(Neuron.class, 2);

                Input input = new Input(fis);
                temp = (Network) kryo.readClassAndObject(input);
                input.close();

                fis.close();

            } catch (Exception e) {

                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ringProgressDialog.dismiss();

            if (temp != null) {
                net = temp;
                ((TextView) findViewById(R.id.filename)).setText("[" + filename + "]");
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("\"" + filename + "\" is not valid network")
                        .setNeutralButton("Continue", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ringProgressDialog = ProgressDialog.show(MainActivity.this, null, "Loading...", true, false);
        }
    }

}
