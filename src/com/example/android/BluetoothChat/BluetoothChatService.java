package com.example.android.BluetoothChat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothChatService {
	// Debugging
	private static final String TAG = "BluetoothChatService";
	private static final boolean D = true;

	// ������socket����ʱ��SDP����
	private static final String NAME = "BluetoothChat";

	// ʹ�ó���Ķ�һUUID
	private static final UUID MY_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// ��������������
	private final BluetoothAdapter mAdapter;
	// Handler
	private final Handler mHandler;
	// �������ӵļ����߳�
	private AcceptThread mAcceptThread;
	// ����һ���豸���߳�
	private ConnectThread mConnectThread;
	// �Ѿ�����֮��Ĺ����߳�
	private ConnectedThread mConnectedThread;
	// ��ǰ��״̬
	private int mState;

	// ����״̬
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
	public static final int STATE_CONNECTED = 3; // now connected to a remote

	/**
	 * ���췽��
	 * 
	 * @param context
	 *            The UI Activity Context
	 * @param handler
	 *            A Handler to send messages back to the UI Activity
	 */
	public BluetoothChatService(Context context, Handler handler) {
		// �õ���������������
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		// ����״̬
		mState = STATE_NONE;
		// ����Handler
		mHandler = handler;
	}

	/**
	 * ���õ�ǰ״̬
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		if (D)
			Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		// ״̬����֮��UI ActivityҲ��Ҫ����
		mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	/**
	 * ���ص�ǰ״̬
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * 
	 * �{��start����������һ�����������
	 */
	public synchronized void start() {
		if (D)
			Log.d(TAG, "start");

		// ȡ���κ��߳���ͼ����һ������
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// ȡ���κ��������е�����
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// ����AcceptThread�߳�������BluetoothServerSocket
		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		// ����״̬Ϊ�������ȴ�����
		setState(STATE_LISTEN);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device.
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 */
	public synchronized void connect(BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connect to: " + device);

		// ȡ���κ������̣߳���ͼ����һ������
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// ȡ���κ��������е��߳�
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// ����һ�������߳�����ָ�����豸
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		if (D)
			Log.d(TAG, "connected");

		// ȡ��ConnectThread�����߳�
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// ȡ�������������ӵ��߳�
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// ȡ�����еļ����̣߳����������Ѿ�������һ���豸
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// ����ConnectedThread�߳����������Ӻ����з���
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// �������ӵ��豸���Ƶ�UI Activity����
		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		// ״̬��Ϊ�Ѿ����ӣ�������������
		setState(STATE_CONNECTED);
	}

	/**
	 * ��ֹ���е��߳�
	 */
	public synchronized void stop() {
		if (D)
			Log.d(TAG, "stop");
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		// ״̬����ΪԤ��״̬
		setState(STATE_NONE);
	}

	/**
	 * д���Լ�Ҫ���ͳ����Ķ���
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(byte[] out) {
		// Create temporary object
		ConnectedThread r;
		// Synchronize a copy of the ConnectedThread
		synchronized (this) {
			// �ж��Ƿ����Ѿ�����״̬
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		// ����д
		r.write(out);
	}

	/**
	 * ��������ʧ�ܵĶ�����UI����
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);

		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * ����ʧ�ܶ�����UI����
	 */
	private void connectionLost() {
		setState(STATE_LISTEN);

		Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * 
	 * ����Intent����ļ����߳�
	 * 
	 */
	private class AcceptThread extends Thread {
		// ����socket����
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// ͨ��listenUsingRfcommWithServiceRecord����һ���µ�socket����
			try {
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "listen() failed", e);
			}
			mmServerSocket = tmp;
		}

		@Override
		public void run() {
			if (D)
				Log.d(TAG, "BEGIN mAcceptThread" + this);
			setName("AcceptThread");
			BluetoothSocket socket = null;

			// ���統ǰû����������������socket����
			while (mState != STATE_CONNECTED) {
				try {
					// �����а������ӣ������ ������һ��������ã���֮�������ӳɹ���һ���쳣
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
					break;
				}

				// ���������һ������
				if (socket != null) {
					synchronized (BluetoothChatService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// ����״̬Ϊ�����������������У��������connected������
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// new socket.
							// ����Ϊû��Ԥ�������Ѿ����ӣ�����ֹ��socket
							try {
								socket.close();
							} catch (IOException e) {
								Log
										.e(
												TAG,
												"Could not close unwanted socket",
												e);
							}
							break;
						}
					}
				}
			}
			if (D)
				Log.i(TAG, "END mAcceptThread");
		}

		// ���BluetoothServerSocket
		public void cancel() {
			if (D)
				Log.d(TAG, "cancel " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of server failed", e);
			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a
	 * device. It runs straight through; the connection either succeeds or
	 * fails.
	 */
	private class ConnectThread extends Thread {
		// ����Socket
		private final BluetoothSocket mmSocket;
		// �����豸
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// �õ�һ�������������豸��BluetoothSocket
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		@Override
		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// ȡ�޿ɼ�״̬�������������
			mAdapter.cancelDiscovery();

			// ����һ��BluetoothSocket����
			try {
				// һ��������ã����سɹ����쳣
				mmSocket.connect();
			} catch (IOException e) {
				// ����ʧ��
				connectionFailed();
				// �����쳣����socket
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,"unable to close() socket during connection failure",e2);
				}
				// ����������������״̬
				BluetoothChatService.this.start();
				return;
			}

			// ���������ConnectThread
			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			// ����ConnectedThread������������...���߳�
			connected(mmSocket, mmDevice);
		}

		// ȡ�������߳�ConnectThread
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all
	 * incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		// BluetoothSocket
		private final BluetoothSocket mmSocket;
		// ���������
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// �õ�BluetoothSocket�����������
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		@Override
		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			// ����������
			while (true) {
				try {
					// ���������ж�ȡ����
					bytes = mmInStream.read(buffer);

					// ����һ��������UI�߳̽��и���
					mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes,
							-1, buffer).sendToTarget();
				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					// �����쳣��������ɥʧ
					connectionLost();
					break;
				}
			}
		}

		/**
		 * д��Ҫ���͵Ķ���
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// ��д�Ķ���ͬʱ���ݸ�UI����
				mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1,
						buffer).sendToTarget();
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		// ȡ��ConnectedThread���������߳�
		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}
}
