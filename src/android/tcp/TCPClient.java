/* This code is free. H. Farid from Iran */

package android.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import android.os.AsyncTask;
import android.os.Handler;

public class TCPClient {
    private String host;
    private int port;
    private BufferedWriter out;
	private BufferedReader in;
	private Socket s;
	private OnTCPClientReceived onReceive;
	private boolean alreadyStarted = false;
	
	public TCPClient() {
		host = "";
		port = 0;
	}
	
	public void start() {
		if (!alreadyStarted) {
			alreadyStarted = true;
			InitTCPClientTask task = new InitTCPClientTask();
			task.execute(new Void[0]);
		}
	}
	
	public void sendMessageToServer(final String data) {
		Runnable runnable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try
				{
			        String outMsg = data;
			        out.write(outMsg);
			        out.flush(); 
				}
				catch(Throwable e)
				{
					e.printStackTrace();
				}
			}
			
		};
		Thread thread = new Thread(runnable);
		thread.start();
	}
	
	public void setHost(String value) {
		this.host = value;
	}
	
	public String getHost() {
		return host;
	}
	
	public void setPort(int value) {
		this.port = value;
	}
	
	public int getPort() {
		return port;
	}
	
	public boolean isConnected() {
		return s.isConnected();
	}
	
	public void setOnReceived(OnTCPClientReceived value) {
		onReceive = value;
	}
	
	public interface OnTCPClientReceived {
		public void onDataReceived(String data);
	}
	
	private class InitTCPClientTask extends AsyncTask<Void, Void, Void>
	{
		public InitTCPClientTask()
		{
			
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub

			try
			{
				s = new Socket(host, port);
		        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		        out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		        while(true)
		        {
		        	try {
		        		String inMsg = in.readLine();
			        	if(inMsg!=null)
			        	{
					        if (onReceive != null) {
					        	onReceive.onDataReceived(inMsg);
					        }
			        	}
					} catch (Throwable e) {
						e.printStackTrace();
					}
		        }

		    } catch (Throwable e) {
		        e.printStackTrace();
		    } 
			
			return null;
			
		}
		
	}
}
