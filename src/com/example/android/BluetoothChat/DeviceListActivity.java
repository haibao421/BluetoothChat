package com.example.android.BluetoothChat;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


public class DeviceListActivity extends Activity {
	// Debugging
	private static final String TAG = "DeviceListActivity";
	private static final boolean D = true;

	// Intent��������ʱ������Ϣ���豸��ַ
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	// ����������
	private BluetoothAdapter mBtAdapter;
	// �Ѿ���Ե������豸
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	// �µ������豸
	private ArrayAdapter<String> mNewDevicesArrayAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// ���ô���
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);

		// Set result CANCELED incase the user backs out
		setResult(Activity.RESULT_CANCELED);

		// ��ʼ��ɨ�谴ť
		Button scanButton = (Button) findViewById(R.id.button_scan);
		scanButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// ����ɨ�����
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});

		// ��ʼ��ArrayAdapter��һ�����Ѿ���Ե��豸��һ�����·��ֵ��豸
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.device_name);

		// ��Ⲣ��������Ե��豸ListView
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		// �����ѷ��ֵ������豸ListView
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// ��һ���豸������ʱ����Ҫע��һ���㲥
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// ����ʾ��ʡ��ϵ�ʱ����Ҫע��һ���㲥
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// �õ����ص�����������
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// �õ�һ���Ѿ�ƥ�䵽������������BluetoothDevice��Ķ����£
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// ����ͨ��������������getBondedDevices����ȡ���Ѿ���Ե������豸��
		// ��������ӵ�mPairedDevicesArrayAdapter����Դ�У�����ʾ��pairedListView�б���ͼ�У�
		// ����û���Ѿ���Ե������豸������ʾһ��R.string.none_paired�ַ�����ʾĿǰû����Գɹ����豸
		if (pairedDevices.size() > 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		} else {
			String noDevices = getResources().getText(R.string.none_paired)
					.toString();
			mPairedDevicesArrayAdapter.add(noDevices);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// ȷ������û�з��֣�����豸
		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}

		// ж����ע��Ĺ㲥
		this.unregisterReceiver(mReceiver);
	}

	/**
	 * �����ܱ����ֵ��豸
	 */
	private void doDiscovery() {
		if (D)
			Log.d(TAG, "doDiscovery()");

		// ������ʾ������
		setProgressBarIndeterminateVisibility(true);
		// ����titleΪɨ��״̬
		setTitle(R.string.scanning);

		// ��ʾ���豸���ӱ���
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

		// �����Ѿ��ڰ���ʵ�F�ˣ���ô������ֹ
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}

		// ����������������õ��ܹ������ֵ��豸
		mBtAdapter.startDiscovery();
	}

	// ListViews�������豸�ĵ���¼�����
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			// ȡ�޼��ɨ�跢���豸������,�����ڷǳ�������Դ
			mBtAdapter.cancelDiscovery();

			// �õ�mac��ַ
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);

			// ����һ������Mac��ַ��Intent����
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

			// ����result���ճ�Activity
			finish();
		}
	};

	// ����ɨ�������豸
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// ������һ���豸ʱ
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// ��Intent�õ������豸����
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// �����Ѿ���ԣ����������������Ѿ����豸�б�����
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					// ������ӵ��豸�б�
					mNewDevicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
				// ��ɨ�����֮��ı�Activity��title
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				// ���ý���������ʾ
				setProgressBarIndeterminateVisibility(false);
				// ����title
				setTitle(R.string.select_device);
				// �������Ϊ0�����ʾû�з�������
				if (mNewDevicesArrayAdapter.getCount() == 0) {
					String noDevices = getResources().getText(
							R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(noDevices);
				}
			}
		}
	};

}
