package tevonial.digitocr;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class InputActivity extends AppCompatActivity {
    private DrawingView draw;
    private int[] buffer;
    private ArrayList<Integer> data;
    private TextView targetView, of;
    private int head, size;
    private int digit;

    private File dir;
    private File[] files = new File[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        draw = new DrawingView(this) {
            @Override
            void onTouchUp(int[] pixels) {
                buffer = pixels;
            }
        };
        ((RelativeLayout) findViewById(R.id.draw)).addView(draw);

        targetView = (TextView) findViewById(R.id.target);
        of = (TextView) findViewById(R.id.of);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int pixel : buffer) {
                    data.add(pixel);
                }
                draw.reset();

                head++;
                update();
            }
        });

        File sdCard = Environment.getExternalStorageDirectory();
        dir = new File (sdCard.getAbsolutePath() + "/aOCR");
        dir.mkdirs();

        for (int i=0; i<10; i++) {
            files[i] = new File(dir, "data" + i);
        }

        setTarget(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_input, menu);
        for (int i=0; i<10; i++)
            menu.add(Menu.NONE, i, 0, String.valueOf(i));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save) {

            if (size != head) {
                try {
                    FileOutputStream fos = new FileOutputStream(files[digit], true);
                    byte[] output = new byte[data.size()];
                    for (int i = 0; i < data.size(); i++) {
                        output[i] = data.get(i).byteValue();
                    }
                    fos.write(output);
                    fos.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

                size = (int) (files[digit].length() / 784);
                update();
            }

        } else {
            if (id >= 0 && id <= 9) {
                setTarget(id);
            }
        }

        return true;
    }

    private void setTarget(final int target) {
        digit = target;
        data = new ArrayList<>();
        targetView.post(new Runnable() {
            @Override
            public void run() {
                targetView.setText(String.valueOf(target));
            }
        });

        size = head = (int) (files[digit].length() / 784);

        update();
    }

    private void update() {
        of.post(new Runnable() {
            @Override
            public void run() {
                of.setText("(" + (head + 1) + "/" + size + ")");
            }
        });
    }
}
