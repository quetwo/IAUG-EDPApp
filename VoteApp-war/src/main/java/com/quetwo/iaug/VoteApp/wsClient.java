/*
 * 
 *   Copyright 2015 Michigan State University Board of Trustees
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * 
 */

package com.quetwo.iaug.VoteApp;

import java.io.IOException;
import java.net.URI;

import javax.websocket.*;

@ClientEndpoint
public class wsClient 
{
	
	private Session userSession = null;
	private MessageHandler messageHandler = null;
	private URI endpoint = null;
	
	public wsClient(final URI endpoint)
	{
		this.endpoint = endpoint;
		try
		{
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this,  endpoint);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@OnOpen
	public void onOpen(final Session userSession)
	{
		this.userSession = userSession;
	}
	
	@OnClose
	public void onClose(final Session userSession, final CloseReason reason)
	{
		this.userSession = null;
	}
	
	@OnMessage
	public void onMessage(final String message)
	{
		if (messageHandler != null)
		{
			messageHandler.handleMessage(message);
		}
	}
	
	public boolean isConnected()
	{
		return (this.userSession != null);
	}
	
	public void reconnect() throws Exception
	{
		if (!isConnected())
		{
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, this.endpoint);
		}
	}
	
	public void closeConnection()
	{
		try
		{
			userSession.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void addMessageHandler(final MessageHandler msgHandler)
	{
		messageHandler = msgHandler;
	}
	
	public void sendMessage(final String message)
	{
		userSession.getAsyncRemote().sendText(message);
	}
	
	public static interface MessageHandler
	{
		public void handleMessage(String message);
	}
}
