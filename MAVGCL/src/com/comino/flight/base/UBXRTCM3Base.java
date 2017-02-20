/****************************************************************************
 *
 *   Copyright (c) 2017 Eike Mansfeld ecm@gmx.de. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 ****************************************************************************/

package com.comino.flight.base;


import org.mavlink.messages.MAV_SEVERITY;
import org.mavlink.messages.lquac.msg_gps_rtcm_data;

import com.comino.mav.control.IMAVController;
import com.comino.mavbase.ublox.reader.StreamEventListener;
import com.comino.mavbase.ublox.reader.UBXSerialConnection;
import com.comino.msp.log.MSPLogger;
import com.comino.msp.model.segment.GPS;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class UBXRTCM3Base {

	private static UBXRTCM3Base instance = null;
	private final UBXSerialConnection ubx;

	private BooleanProperty svin  = new SimpleBooleanProperty();
	private BooleanProperty valid = new SimpleBooleanProperty();

	private float mean_acc = 0;

	public static UBXRTCM3Base getInstance(IMAVController control, String port) {
		if(instance == null) {
			instance = new UBXRTCM3Base(control, port);
		}
		return instance;
	}

	public static UBXRTCM3Base getInstance() {
		return instance;
	}

	public UBXRTCM3Base(IMAVController control, String port) {
		System.out.println("StartUp RTCM3 base...");
		this.ubx = new UBXSerialConnection(port, 9600);
		this.ubx.setMeasurementRate(1);

		try {
			this.ubx.init();
		} catch (Exception e) {
			return;
		}

		svin.addListener((p,o,n) -> {
			if(n.booleanValue())
				MSPLogger.getInstance().writeLocalMsg("[mgc] Survey-In started", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
		});

		valid.addListener((p,o,n) -> {
			if(n.booleanValue())
				MSPLogger.getInstance().writeLocalMsg("[mgc] SVIN finalized. RTCM3 active", MAV_SEVERITY.MAV_SEVERITY_NOTICE);
		});

		ubx.addStreamEventListener( new StreamEventListener() {

			@Override
			public void streamClosed() {
				MSPLogger.getInstance().writeLocalMsg("[mgc] RTCM3 base lost", MAV_SEVERITY.MAV_SEVERITY_WARNING);
				try {
					Thread.sleep(10000);
					ubx.init();
				} catch (Exception e) {
					return;
				}
			}

			@Override
			public void getSurveyIn(float time_svin, boolean is_svin, boolean is_valid, float meanacc) {
				svin.set(is_svin); valid.set(is_valid);
				mean_acc = meanacc;
			}


			@Override
			public void getPosition(double lat, double lon, double altitude, int fix, int sats) {
                 GPS base = control.getCurrentModel().base;
                 base.latitude   = (float)lat;
                 base.longitude  = (float)lon;
                 base.altitude   = (short)altitude;
                 base.numsat     = sats;
                 System.out.println("Base position: Lat: "+lat+" Lon: "+lon+ " Alt: "+altitude+" Sat: "+sats);
			}

			@Override
			public void getRTCM3(byte[] buffer, int len) {

				if(!control.isConnected())
					return;

				msg_gps_rtcm_data msg = new msg_gps_rtcm_data(2,1);
				if(len < msg.data.length) {
					msg.flags = 0;
					msg.len   = len;
					for(int i = 0;i<len;i++)
						msg.data[i] = buffer[i];
					control.sendMAVLinkMessage(msg);
		//			System.out.println(msg);
				} else {
					int start = 0;
					while (start < len) {
						int length = Math.min(len - start, msg.data.length);
						msg.flags = 1;
						msg.len   = length;
						for(int i = start;i<length;i++)
							msg.data[i] = buffer[i];
						control.sendMAVLinkMessage(msg);
						start += length;
					}
				}

			}

		});
	}

	public BooleanProperty getSVINStatus() {
		return svin;
	}

	public BooleanProperty getValidStatus() {
		return valid;
	}

	public float getBaseAccuracy() {
		return mean_acc;
	}

}