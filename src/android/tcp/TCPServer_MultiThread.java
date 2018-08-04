/* This code is free. H. Farid from Iran */

package android.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.R;

public class TCPServer_MultiThread {
	public static int DefaultMaxClientSize = 64;
	private int port;
	private ServerSocket ss;
	private OnTCPServerReceived_MT onReceive;
	private boolean isStopped = false;
	private boolean isPaused = false;
	private ArrayList<ClientRunnable> clients;
	private int maxCS = 64;
	private long cid = 1;
	
	public TCPServer_MultiThread(int sPort) {
		port = sPort;
		clients = new ArrayList<ClientRunnable>(0);
		Runnable r = new Runnable() {			
			@Override
			public void run() {
				try {
					ss = new ServerSocket(port);
					while (!isStopped) {
						if (isPaused == false & getClientCount() <= getMaxClientSize()) {
							try {
								Socket clientSocket = ss.accept();
								cid = cid + 1;
								ClientRunnable cr = new ClientRunnable(clientSocket, cid);
								clients.add(cr);
								new Thread(cr).start();
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}				
			}
		};
		new Thread(r).start();
	}
	
	public void setListener(OnTCPServerReceived_MT listener) {
		onReceive = listener;
	}
	
	public void setMaxClientSize(final int value) {
		new Thread(new Runnable() {
			public void run() {
				if(value > 0 & value != maxCS) {
					if (value > maxCS) {
						maxCS = value;
					} else if (value < maxCS) {
						int min = clients.size() - (maxCS - value) - 1;
						for (int i = clients.size() - 1; i > min; i = i - 1) {
							try {
								clients.get(i).stop();
						    } catch (Throwable e) {
						    	e.printStackTrace();
						    }
							try {
								clients.remove(i);
							} catch (Throwable e) {
								e.printStackTrace();
							}			
						}
						maxCS = value;
					}
				}
			}
		}).start();
	}
	
	public int getMaxClientSize() {
		return maxCS;
	}
	
	public int getPort() {
		return port;
	}
	
	public ArrayList<ClientRunnable> getClients() {
		return clients;
	}
	
	public int getClientCount() {
		return clients.size();
	}
	
	public void sendDataToAllClients(String data) {
		for (int i = 0; i < clients.size(); i++) {
			try {
				clients.get(i).sendData(data);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public void sendData(ClientRunnable client, String data) {
		try {
			client.sendData(data);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public void removeDisconnectedClients() {
		new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < clients.size(); i++) {
					ClientRunnable clientRunnable = null;
					try {
						clientRunnable = clients.get(i);
					} catch (Throwable e) {
						e.printStackTrace();
					}
					if (clientRunnable != null) {
						if (!clientRunnable.isConnected()) {
							for (int i2 = 0; i2 < clients.size(); i2++) {
								if(clients.get(i2).uniqID == clientRunnable.uniqID) {
									clients.remove(i2);
								}
							}
							clientRunnable.stop();
						}
					}
				}
			}
		}).start();
	}
	
	public void stop() {
		new Thread(new Runnable() {
			public void run() {
				isStopped = true;
				for (ClientRunnable clientRunnable : clients) {
					try {
						clientRunnable.stop();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
		        try {
		            ss.close();
		        } catch (Throwable e) {
		            e.printStackTrace();
		        }
			}
		}).start();
	}
	
	public void pause() {
		isPaused = true;
	}
	
	public void resume() {
		isPaused = false;
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
	public interface OnTCPServerReceived_MT {
		public void onDataReceived(ClientRunnable client, String data);
	}
	
	private class ClientRunnable implements Runnable {
		protected Socket clientSocket = null;
		private boolean isWorking = true;
		private BufferedReader inr;
		private BufferedWriter outw;
		private long uniqID = 0;

	    public ClientRunnable(Socket clientSocket, long ID) {
	        this.clientSocket = clientSocket;
	        this.uniqID = ID;
	    }
	    
	    public void stop() {
	    	isWorking = false;
	    }
	    
	    public Socket getClient() {
	    	return clientSocket;
	    }
	    
	    public boolean isConnected() {
	    	try {
				return clientSocket.isConnected();
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}
	    }
	    
	    public void sendData(String data) {
	    	final String fData = data;
	    	try {
	    		new Thread(new Runnable() {
					public void run() {
						try {
							outw.write(fData);
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}).start();
			} catch (Throwable e) {
				e.printStackTrace();
			}
	    }
	    
	    public void run() {
            try {
                InputStream input  = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream();
                inr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                while(isWorking) {
                	try {
						String data = inr.readLine();
						if (data != null) {
							onReceive.onDataReceived(this, data);
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
                }
                try {
                	inr.close();
                    outw.close();
                    output.close();
                    input.close();
				} catch (Throwable e) {
					e.printStackTrace();
				}
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
	}
}
