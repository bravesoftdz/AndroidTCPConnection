/* This code is free. H. Farid from Iran */

package java.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author      Hadi Farid <hadifaridhadi@gmail.com>
 * @version     1.0
 * @since       1.0
 */
public class TCPServer_MultiThread {
	public static int DefaultMaxClientSize = 64;
	private int port;
	private ServerSocket ss;
	private OnTCPServerReceived_MT onReceive;
	private boolean isStopped = false;
	private boolean isPaused = false;
	private ArrayList<ClientRunnable> clients;
	private int maxCS = 64;
	private static long cid = 1;
	
	/**
	 * Initialize and start the Server.
	 * @param sPort The Port you want to start Server with.
	 */
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
	
	/**
	 * Sets a on received listener which has Client and Data params.
	 * Client: Client message recived from.
	 * Data: Data received from the client.
	 */
	public void setListener(OnTCPServerReceived_MT listener) {
		onReceive = listener;
	}
	
	/**
	 * Sets the max count of clients.
	 * @param value Maximum count of clients. If it's smaller than previous value, the Server will remove and disconnect from previous clients that their index was larger than value.
	 */
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
	
	/**
	 * All clients accepted by the server.
	 * @return A list of clients accepted by the server.
	 */
	public ArrayList<ClientRunnable> getClients() {
		return clients;
	}
	
	/**
	 * Count of all clients accepted by the server.
	 * @return Count of all clients accepted by the server.
	 */
	public int getClientCount() {
		return clients.size();
	}
	
	/**
	 * Sends a message to all clients connected to the server.
	 * @param data The message you want to send.
	 */
	public void sendDataToAllClients(String data) {
		for (int i = 0; i < clients.size(); i++) {
			try {
				if(clients.get(i).isConnected()) { clients.get(i).sendData(data); }
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sends a message to a specific client.
	 * @param data The message you want to send.
	 */
	public void sendData(ClientRunnable client, String data) {
		try {
			if(client.isConnected()) { client.sendData(data); }
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove and stop a specific client.
	 * @param client Client you want to remove
	 */
	public void removeClient(final ClientRunnable client) {
		new Thread(new Runnable() {
			public void run() {
				if (client != null) {
					for (int i = 0; i < clients.size(); i++) {
						ClientRunnable cc = clients.get(i);
						if (cc.uniqID == client.uniqID) {
							clients.remove(i);
							cc.stop();
						}
					}
				}
			}
		}).start();
	}
	
	/**
	 * Remove and stop all clients are already accepted by the server and now are not connected to the server.
	 */
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
	
	/**
	 * End and stop the server. (Destroy)
	 */
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
	
	/**
	 * Pauses the server temporarily.
	 */
	public void pause() {
		isPaused = true;
	}
	
	/**
	 * Resume the server after pause().
	 */
	public void resume() {
		isPaused = false;
	}
	
	public boolean isPaused() {
		return isPaused;
	}
	
	public interface OnTCPServerReceived_MT {
		/**
		 * This method will be called when the server receives a message from a client.
		 * @param client Client that message received from.
		 * @param data The message received from the client.
		 */
		public void onDataReceived(ClientRunnable client, String data);
	}
	
	private class ClientRunnable implements Runnable {
		private Socket clientSocket = null;
		private boolean isWorking = true;
		private BufferedReader inr;
		private BufferedWriter outw;
		private long uniqID = 0;

	    public ClientRunnable(Socket clientSocket, long ID) {
	        this.clientSocket = clientSocket;
	        this.uniqID = ID;
	    }
	    
	    /**
	     * Use Server removeClient method.
	     */
	    public void stop() {
	    	isWorking = false;
	    }
	    
	    /**
	     * Client Socket
	     * @return Client Socket of this ClientRunnable.
	     */
	    public Socket getClient() {
	    	return clientSocket;
	    }
	    
	    /**
	     * Is the client connected to the server?
	     * @return Returns true if this client is connected to the server.
	     */
	    public boolean isConnected() {
	    	try {
				return clientSocket.isConnected();
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}
	    }
	    
	    /**
	     * Sends your message to the client. Use Server sendData or sendDataToAllClients instead.
	     * @param data The message you want to send.
	     */
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
