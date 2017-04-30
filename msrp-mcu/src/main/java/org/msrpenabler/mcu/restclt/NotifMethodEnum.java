package org.msrpenabler.mcu.restclt;

public enum NotifMethodEnum {
	
	SESSION_CONNECT("sessconnect"),
	SESSION_DISCONNECT("sessdisconnect"),	

	RCV_MSG("msgrcv"),
	RCV_CHUNK("chunckrcv"),
	RCV_REPORT("reportrcv"),

	SEND_MSG_FAILURE("cannotsendmsg"),
	SEND_CHUNCK_FAILURE("cannotsendchunck"),
	;
	
	private String value;

	private NotifMethodEnum(String value) {
		this.value = value;
	}

	public static NotifMethodEnum getValueOf(String val) {
		if (val == null) return null;
		for (NotifMethodEnum e : NotifMethodEnum.values()) {
			if (e.value.equals(val)) return e; 
		}
		return null;
	}

	public String toString() {
		return value;
	}
	
}
