package com.example.arduinogpstracker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final int REQUEST_ENABLE_BT = 1;
	private Button onBtn;
	private Button offBtn;
	private Button listBtn;
	private Button findBtn;
	private TextView text;
	private BluetoothAdapter myBluetoothAdapter;
	private Set<BluetoothDevice> pairedDevices;
	private ListView myListView;
	private ArrayAdapter<String> BTArrayAdapter;
	private String NAME = "HC-06";
	private UUID MY_UUID;
	private Handler mHandler, h;
	BluetoothSocket mmSocket;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// take an instance of BluetoothAdapter - Bluetooth radio
		myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (myBluetoothAdapter == null) {
			onBtn.setEnabled(false);
			offBtn.setEnabled(false);
			listBtn.setEnabled(false);
			findBtn.setEnabled(false);
			text.setText("Status: not supported");

			Toast.makeText(getApplicationContext(),
					"Your device does not support Bluetooth", Toast.LENGTH_LONG)
					.show();
		} else {
			text = (TextView) findViewById(R.id.text);					
			onBtn = (Button) findViewById(R.id.turnOn);
			onBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					on(v);
				}
			});

			offBtn = (Button) findViewById(R.id.turnOff);
			offBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					off(v);
				}
			});

			listBtn = (Button) findViewById(R.id.paired);
			listBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					list(v);
				}
			});

			findBtn = (Button) findViewById(R.id.search);
			findBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					find(v);
				}
			});

			myListView = (ListView) findViewById(R.id.listView1);

			// create the arrayAdapter that contains the BTDevices, and set it
			// to the ListView
			BTArrayAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1);
			myListView.setAdapter(BTArrayAdapter);
		}

		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				System.out.println("msg.what = " + msg.what);
			}
		};

		h = new Handler() {
			public void handleMessage(android.os.Message msg) {
				System.out.println("msg.what1 = " + msg.what);
				StringBuffer sb = new StringBuffer();
				switch (msg.what) {
				case 3: // if receive massage
					byte[] readBuf = (byte[]) msg.obj;
					String strIncom = new String(readBuf, 0, msg.arg1); // create
																		// string
																		// from
																		// bytes
																		// array
					sb.append(strIncom); // append string
					int endOfLineIndex = sb.indexOf("\r\n"); // determine the
																// end-of-line
					if (endOfLineIndex > 0) { // if end-of-line,
						String sbprint = sb.substring(0, endOfLineIndex); // extract
																			// string
						sb.delete(0, sb.length()); // and clear
						System.out.println("---------------" + sbprint);
						// incoming.setText("Data from Arduino: " + sbprint); //
						// update TextView
					}
					// Log.d(TAG, "...String:"+ sb.toString() + "Byte:" +
					// msg.arg1 + "...");
					break;
				}
			};
		};

	}

	public void on(View view) {
		if (!myBluetoothAdapter.isEnabled()) {
			Intent turnOnIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

			Toast.makeText(getApplicationContext(), "Bluetooth turned on",
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(getApplicationContext(), "Bluetooth is already on",
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (requestCode == REQUEST_ENABLE_BT) {
			if (myBluetoothAdapter.isEnabled()) {
				text.setText("Status: Enabled");
			} else {
				text.setText("Status: Disabled");
			}
		}
	}

	public void list(View view) {
		// get paired devices
		pairedDevices = myBluetoothAdapter.getBondedDevices();

	
		// put it's one to the adapter
		for (BluetoothDevice device : pairedDevices) {
			BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
			System.out.println("Device: " + device.getName() + "\n"
					+ device.getAddress());
			if (device.getName().compareTo("HC-06") == 0) {
				String addr = device.getAddress();
				device.fetchUuidsWithSdp();
				System.out.println(device.ACTION_UUID);
				MY_UUID = UUID
						.fromString("00001101-0000-1000-8000-00805F9B34FB");
				// MY_UUID = UUID
				// .fromString("00000003-0000-1000-8000-00805F9B34FB");
				System.out.println(MY_UUID.toString());

				AcceptThread t1 = new AcceptThread(device, addr);
				t1.start();
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		

		// MY_UUID = UUID.fromString("20:13:09:30:08:79");

		Toast.makeText(getApplicationContext(), "Show Paired Devices",
				Toast.LENGTH_SHORT).show();

	}

	final BroadcastReceiver bReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// add the name and the MAC address of the object to the
				// arrayAdapter
				BTArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
				BTArrayAdapter.notifyDataSetChanged();
			}
		}
	};

	public void find(View view) {
		if (myBluetoothAdapter.isDiscovering()) {
			// the button is pressed when it discovers, so cancel the discovery
			myBluetoothAdapter.cancelDiscovery();
		} else {
			BTArrayAdapter.clear();
			myBluetoothAdapter.startDiscovery();

			registerReceiver(bReceiver, new IntentFilter(
					BluetoothDevice.ACTION_FOUND));
		}
	}

	public void off(View view) {
		myBluetoothAdapter.disable();
		text.setText("Status: Disconnected");

		Toast.makeText(getApplicationContext(), "Bluetooth turned off",
				Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(bReceiver);
	}

	/*
	 * private final Handler mHandler = new Handler() {
	 * 
	 * @Override public void handleMessage(Message msg) {
	 * System.out.println("-----------------------------------");
	 * System.out.println(msg.toString());
	 * System.out.println("-----------------------------------"); } };
	 */

	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;
		//private final BluetoothSocket mmSocket;
		BluetoothDevice device;
		String addr;

		public AcceptThread(BluetoothDevice device1, String addr1) {
			this.device = device1;
			this.addr = addr1;
			System.out.println("-----------------------------------");
			System.out.println("Constrcutor of Accept Thread");
			System.out.println("-----------------------------------");
			// Use a temporary object that is later assigned to mmServerSocket,
			// because mmServerSocket is final
			
			myBluetoothAdapter.cancelDiscovery();
			
			BluetoothServerSocket tmp = null;
			try {
				// MY_UUID is the app's UUID string, also used by the client
				// code
				System.out.println("-----------------------------------");
				System.out.println(NAME);
				System.out.println(MY_UUID.toString());
				System.out.println("-----------------------------------");
				tmp = myBluetoothAdapter.listenUsingRfcommWithServiceRecord(
						NAME, MY_UUID);
			} catch (IOException e) {
			}
			mmServerSocket = tmp;

			BluetoothSocket tmp1 = null;
			ParcelUuid[] uuids = servicesFromDevice(device);

			try {
				tmp1 = device.createRfcommSocketToServiceRecord(uuids[0]
						.getUuid());
				System.out.println(uuids[0].getUuid().toString());
				System.out.println("TRying to connecct");
			} catch (IOException e) {
				e.printStackTrace();
			}

			mmSocket = tmp1;
			
			

		}

		public void run() {
			BluetoothSocket socket = null;
			// BluetoothDevice device =
			// myBluetoothAdapter.getRemoteDevice(addr);
			BluetoothServerSocket tempBluetoothServerSocket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {
				System.out.println("Opening Socket");
				try {
					// myBluetoothAdapter.cancelDiscovery();

					BluetoothSocket tmp1 = null;
					ParcelUuid[] uuids = servicesFromDevice(device);

					try {
						tmp1 = device
								.createRfcommSocketToServiceRecord(uuids[0]
										.getUuid());
						System.out.println("TRying to connecct"
								+ device.getName());
					} catch (IOException e) {
						e.printStackTrace();
					}

					mmSocket = tmp1;

					try {
						System.out.println("System connecting");						
						mmSocket.connect();
						System.out.println("System connected"
								+ device.getName());

						System.out.println(mmSocket.isConnected());
					} catch (IOException connectException) {
						System.out.println("System not connecting");
						connectException.printStackTrace();
					}
					// try {
					// mmSocket.close();
					// } catch (IOException closeException) { }
					//
					/*
					 * myBluetoothAdapter.cancelDiscovery(); //socket =
					 * mmServerSocket.accept();
					 * tempBluetoothServerSocket=myBluetoothAdapter
					 * .listenUsingRfcommWithServiceRecord("HC-06", MY_UUID);
					 * tempBluetoothServerSocket.accept();
					 * System.out.println("Accept");
					 * 
					 * 
					 * socket =
					 * device.createRfcommSocketToServiceRecord(MY_UUID);
					 * 
					 * //Method m =
					 * device.getClass().getMethod("createRfcommSocket", new
					 * Class[] {int.class}); //socket = (BluetoothSocket)
					 * m.invoke(device, 1);
					 * 
					 * Here is the part the connection is made, by asking the
					 * device to create a RfcommSocket (Unsecure socket I
					 * guess), It map a port for us or something like that
					 */
					// socket.connect();

				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Opened Socket");
				// If a connection was accepted
				if (mmSocket != null) {
					// Do work to manage the connection (in a separate thread)
					// manageConnectedSocket(socket);

					ConnectedThread ct = new ConnectedThread();
					ct.start();
					try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		}

		private ParcelUuid[] servicesFromDevice(BluetoothDevice device2) {
			try {
				Class<?> cl = Class
						.forName("android.bluetooth.BluetoothDevice");
				Class<?>[] par = {};
				Method method = cl.getMethod("getUuids", par);
				Object[] args = {};
				ParcelUuid[] retval = (ParcelUuid[]) method
						.invoke(device, args);
				return retval;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private class ConnectedThread extends Thread {
		//private final BluetoothSocket mmSocket;
		private InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread() {
			System.out.println("ConnectedThread Constructor");
			//mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				System.out.println(mmSocket.isConnected());
				mmInStream = mmSocket.getInputStream();
				System.out.println(mmSocket.isConnected());
				tmpOut = mmSocket.getOutputStream();
				System.out.println("Connection established");
				//
				// int readBytes;
				// byte[] buffer = new byte[256];
				//
				// while(true)
				// {
				// readBytes = tmpIn.read(buffer);
				// System.out.println(readBytes);
				// }
				//

			} catch (IOException e) {
				e.printStackTrace();
			}

			//mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			
			
			
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				
				 int treshHold = 0;
                 try {
                     try {
                         while (mmInStream.available() == 0 && treshHold < 10000) { // if there is something in the inputStream or after 3 seconds 
                        	
                        	 System.out.println(mmSocket.isConnected());
                        	 
                        	 System.out.println(treshHold);
                        	 Thread.sleep(1000);
                             treshHold = treshHold + 500;
                         }
                     } catch (IOException e1) {
                         e1.printStackTrace();
                     } catch (InterruptedException e1) {
                         e1.printStackTrace();
                     }
                     if (treshHold < 3000) { // if less than 3 seconds timeout 
                         System.out.println(treshHold);
                    	 treshHold = 0;
                         bytes = mmInStream.read(buffer);
                         h.obtainMessage(3, bytes, -1, buffer).sendToTarget();
                     } else {
                         break;
                     }
                 } catch (IOException e) {
                     e.printStackTrace();
                     break;
                 }
			}
				/*
				try {
					// Read from the InputStream
					if (mmInStream.available() > 0) {
						bytes = mmInStream.read(buffer);
						// Send the obtained bytes to the UI activity
						// mHandler.obtainMessage(1, bytes, -1,
						// buffer).sendToTarget();
						h.obtainMessage(3, bytes, -1, buffer).sendToTarget();
					}
				} catch (IOException e) {
					System.out.println("Handler error1");
					e.printStackTrace();
					break;
				}
			*/}
		

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) {
			}
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

}