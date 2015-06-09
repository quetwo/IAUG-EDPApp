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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import com.avaya.collaboration.call.Call;
import com.avaya.collaboration.call.CallListenerAbstract;
import com.avaya.collaboration.call.TheCallListener;
import com.avaya.collaboration.call.media.DigitCollectorOperationCause;
import com.avaya.collaboration.call.media.DigitOptions;
import com.avaya.collaboration.call.media.MediaFactory;
import com.avaya.collaboration.call.media.MediaListener;
import com.avaya.collaboration.call.media.MediaService;
import com.avaya.collaboration.call.media.PlayItem;
import com.avaya.collaboration.call.media.PlayOperationCause;
import com.avaya.collaboration.util.logger.Logger;

// The decorator below tells the Engagement Development Platform that this is the class
// that will be called when an incoming call comes in.

@TheCallListener
public class MyCallListener extends CallListenerAbstract implements MediaListener
{
    private static Logger logger = Logger.getLogger(MyCallListener.class);
    private static Call thisCaller;
    private static wsClient wsEndpoint;
    
	public MyCallListener() throws URISyntaxException 
	{
		// This is run when the EDP app is loaded.  We want to connect to the WebSocket
		// endpoint immediately to avoid a delay with the announcements.  The endpoint below
		// may not be available forever, but it is a basic WebSocket echo service.
		
		wsEndpoint = new wsClient(new URI("ws://vote.suroot.com:8080/wsEndpoint/5178849999"));
	}
	
	@Override
	public final void callIntercepted(final Call call) 
	{
        logger.fine("Entered Voting Application from phone number " + call.getCallingParty().getAddress());
        
        thisCaller = call;
        
        call.enableMediaBeforeAnswer();    // enable the media server to be able to serve media to call
        PlayItem announcement = MediaFactory.createPlayItem();  // create the announcement to play
        announcement.setInterruptible(false);
        announcement.setIterateCount(1);  // we don't want to repeat the announcement.
        try
        {
        	// setting the source to the wav file located within the web folder of this WAR. 
        	announcement.setSource("http://172.24.2.119/services/VoteApp/VoteAppAnn.wav");	
        }
        catch (final Exception e)
        {
        	logger.fatal("Unable to play announcement -- 'main' with error " + e);
        }
        
        final MediaService mediaService = MediaFactory.createMediaService();  
		mediaService.play(call.getCallingParty(), announcement, this);   // tell AMS to play the announcement
        
        logger.fine("Exited Voting Application from phone number " + call.getCallingParty().getAddress());
	}

	
	// this is called when digits are collected from the dispatched event from the .collect() method. 
	@Override
	public void digitsCollected(UUID requestId, String digits,
			DigitCollectorOperationCause cause) 
	{
		logger.fine("Collected digits : " + digits);
		
		// check if the web socket is still connected.  If not, we will try to reconnect.
		if (!wsEndpoint.isConnected())
		{
			try 
			{
				wsEndpoint.reconnect();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
		// we are sending the digits and the called party to the websocket as a string.
		wsEndpoint.sendMessage(digits + " " + thisCaller.getCallingParty().getAddress());

		// if the caller presses the # key, drop them.
		if (digits == "#")
		{
			logger.fine("Caller wished to terminate session " + thisCaller.getCallingParty().getAddress());
			thisCaller.drop();
			return;
		}
		
		// collect some more digits from the user.
		collectDigitsFromCaller();
	}

	// this method is called when the announcement is finished playing.  We want to start collecting
	// digits right away.
	@Override
	public void playCompleted(UUID requestId, PlayOperationCause cause) 
	{
		logger.fine("Finished playing announcement.  Ended with cause: " + cause.toString());
		collectDigitsFromCaller();
	}
	
	// collect digits from the caller.  We utilize the Avaya Media Server for this function.
	private void collectDigitsFromCaller()
	{
		final MediaService mediaService = MediaFactory.createMediaService();
		
		DigitOptions collectDigits = MediaFactory.createDigitOptions();
		collectDigits.setNumberOfDigits(1);
		collectDigits.setTimeout(120000);    // timeout is 2 minutes.  Will work for this demo.
		collectDigits.setFlushBuffer(true);  // throw away any digits collected before this call. 
		
		mediaService.collect(thisCaller.getCallingParty(), collectDigits, this);		
		
	}
	
}
