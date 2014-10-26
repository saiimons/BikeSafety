package saiimons.com.intelmakerhackaton;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;


public class SensorActivity extends Activity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private Sensor mAccelSensor;

    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int FROM_RADS_TO_DEGS = -57;
    private static final int SHAKE_THRESHOLD = 7000;
    private boolean can_send = false;
    private WebSocketConnection conn;
    private long lastUpdate;
    private float last_x;
    private float last_y;
    private float last_z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
        ((Button) findViewById(R.id.brake)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (can_send) {
                    conn.sendTextMessage("brake");
                }
            }
        });
        try {
            mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
            mSensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_GAME);
        } catch (Exception e) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                conn = new WebSocketConnection();
                try {
                    WebSocketOptions options = new WebSocketOptions();
                    conn.connect("ws://10.10.25.99:8042/", new String[]{"echo-protocol"}, new WebSocket.ConnectionHandler() {
                        @Override
                        public void onOpen() {
                            can_send = true;
                            Log.d("WS", "open");
                        }

                        @Override
                        public void onClose(int code, String reason) {
                            can_send = false;
                            Log.d("WS", "close " + code + " " + reason);
                        }

                        @Override
                        public void onTextMessage(String payload) {
                            Log.d("WS", "text " + payload);
                        }

                        @Override
                        public void onRawTextMessage(byte[] payload) {

                        }

                        @Override
                        public void onBinaryMessage(byte[] payload) {

                        }
                    }, new WebSocketOptions(), new ArrayList<BasicNameValuePair>());
                } catch (WebSocketException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                update(truncatedRotationVector);
            } else {
                update(event.values);
            }
        } else if (event.sensor == mAccelSensor) {
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float x = event.values[SensorManager.DATA_X];
                float y = event.values[SensorManager.DATA_Y];
                float z = event.values[SensorManager.DATA_Z];
                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                if (speed > SHAKE_THRESHOLD) {
                    Log.d("sensor", "shake detected w/ speed: " + speed);
                    if (can_send) {
                        conn.sendTextMessage("beep");
                    }
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    private void update(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] * FROM_RADS_TO_DEGS;
        float roll = orientation[2] * FROM_RADS_TO_DEGS;
        ((TextView) findViewById(R.id.pitch)).setText("Pitch: " + pitch);
        ((TextView) findViewById(R.id.roll)).setText("Roll: " + roll);
        String status = "none";
        if (pitch > -15 && pitch < 15 && roll < 15 && roll > -15) {
            status = "turn_left";
        }
        if (roll > -110 && roll < -60 && pitch > -40 && pitch < 20) {
            status = "turn_right";
        }
        ((TextView) findViewById(R.id.status)).setText("Status: " + status);
        if (can_send) {
            conn.sendTextMessage(status);
        }
    }

}
