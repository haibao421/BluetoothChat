package com.example.android.BluetoothChat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
	// Debugging
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// ��BluetoothChatService Handler���͵Ķ�������
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// ��BluetoothChatService Handler�ӹܶ���ʱӦ�õļ���(��-ֵģ��)
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent������루�������ӣ�����ɼ���
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Layout Views
	private TextView mTitle;
	private ListView mConversationView;
	private EditText mOutEditText;
	private Button mSendButton;

	// ���ӵ��豸������
	private String mConnectedDeviceName = null;
	private ArrayAdapter<String> mConversationArrayAdapter;
	// ��Ҫ���ͳ�ȥ���ַ���
	private StringBuffer mOutStringBuffer;
	// ��������������
	private BluetoothAdapter mBluetoothAdapter = null;
	// �������Ķ���
	// �Լ������һ���������������Ķ˿ڼ��������ӣ���������ĳ��򣬺������ǻ����ݡ�
	// ��������Ҫ˵��һ�㣬��Щ���붼����google��Ա��֮��
	private BluetoothChatService mChatService = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (D)
			Log.e(TAG, "+++ ON CREATE +++");

		// ���ô���
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		// �����ڷ������Ϊ�Զ����񣬲���ָ�����Զ���title����Ϊcustom_title
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);

		// �����Զ���title����
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);

		// �õ�һ����������������
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// ����������Ϊnull����֧������
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// ��������û�д򿪣������
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// ������������Ự
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	@Override
	// ������ĳ���豸֮ǰ��������Ҫ����һ���˿ڼ���
	public synchronized void onResume() {
		super.onResume();
		if (D)
			Log.e(TAG, "+ ON RESUME +");

		if (mChatService != null) {
			// ���統ǰ״̬ΪSTATE_NONE������Ҫ���������������
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				// ��ʼһ�������������
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");

		// ��ʼ���Ի�����
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		// ��ʼ���Ի���ʾ�б�
		mConversationView = (ListView) findViewById(R.id.in);
		// ���û���ʾ�б�Դ
		mConversationView.setAdapter(mConversationArrayAdapter);

		// ��ʼ������򣬲�����һ�����������ڴ����س������Ͷ���
		mOutEditText = (EditText) findViewById(R.id.edit_text_out);
		mOutEditText.setOnEditorActionListener(mWriteListener);

		// ��ʼ�����Ͱ�ť���������¼�����
		mSendButton = (Button) findViewById(R.id.button_send);
		mSendButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// ȡ��TextView�е����������Ͷ���
				/**
				 * ����ط���TextView���EditText�ؼ����ҵõ�EditText��ֵ ����ͦ����� ԭ��û��ô�ù�
				 */
				TextView view = (TextView) findViewById(R.id.edit_text_out);
				String message = view.getText().toString();
				sendMessage(message);
			}
		});

		// ��ʼ��BluetoothChatService��������������
		mChatService = new BluetoothChatService(this, mHandler);

		// ��ʼ����Ҫ�����Ķ������ַ���
		mOutStringBuffer = new StringBuffer("");
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// ֹͣ��������
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		// �ж�ɨ��ģʽ�Ƿ�Ϊ�ȿɱ������ֿ��Ա����� ʹ��getScanMode����ȡ�øø�������ɨ��ģʽ
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			// ����ɼ�״̬
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			// ��Ӹ������ԣ��ɼ�״̬��ʱ��
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            A string of text to send.
	 */
	private void sendMessage(String message) {
		// ��ʡ�Ƿ�������״̬
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// ��������Ķ�����Ϊ�ղŷ��ͣ����򲻷���
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			mChatService.write(send);

			// Reset out string buffer to zero and clear the edit text field
			mOutStringBuffer.setLength(0);
			mOutEditText.setText(mOutStringBuffer);
		}
	}

	// ���������������ݵ�����򣬶���������һ���¼�����mWriteListener
	private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView view, int actionId,
				KeyEvent event) {
			// ���»س��������ǰ���������¼�ʱ���Ͷ���
			if (actionId == EditorInfo.IME_NULL
					&& event.getAction() == KeyEvent.ACTION_UP) {
				String message = view.getText().toString();
				sendMessage(message);
			}
			if (D)
				Log.i(TAG, "END onEditorAction");
			return true;
		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					// ����״̬Ϊ�Ѿ�����
					mTitle.setText(R.string.title_connected_to);
					// ����豸����
					mTitle.append(mConnectedDeviceName);
					// �����������
					mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					// ������������
					mTitle.setText(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					// ���ڼ���״̬����û��Ԥ��״̬������ʾû������
					mTitle.setText(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				// ���Լ�д��Ķ���Ҳ��ʾ���Ự�б���
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// ȡ�����ݲ���ӵ�����Ի��б���
				String readMessage = new String(readBuf, 0, msg.arg1);
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// �������ӵ��豸���ƣ�����ʾһ��toast��ʾ
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				// ��������(����)ʧ�ܵĶ���
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// ��DeviceListActivity�����豸����
			if (resultCode == Activity.RESULT_OK) {
				// ��Intent�еõ��豸��MAC��ַ
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// �õ������豸���� ͨ��getRemoteDevice���������ҵ��õ�ַ���豸BluetoothDevice
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// ������������豸
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// �ڰ��������ʱ���صĴ���
			if (resultCode == Activity.RESULT_OK) {
				// �����Ѿ��򿪣���������һ������Ự
				setupChat();
			} else {
				// �������������
				Log.d(TAG, "BT not enabled");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			// ����DeviceListActivity�鿴�豸��ɨ��
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			return true;
		case R.id.discoverable:
			// ȷ���豸���ڿɼ�״̬
			ensureDiscoverable();
			return true;
		}
		return false;
	}

}