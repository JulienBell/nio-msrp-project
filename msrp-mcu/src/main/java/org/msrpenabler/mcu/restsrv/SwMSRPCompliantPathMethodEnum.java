package org.msrpenabler.mcu.restsrv;

public enum SwMSRPCompliantPathMethodEnum {
	
	// Cmd
	CREATE_HUB("newhub"),
	DELETE_HUB("deletehub"),
	
	CREATE_SESSION("newsess"),
	DELETE_SESSION("deletesess"),
	UPDATE_SESSION("updatesess"),
	CLOSE_SESSION("closesess"),
	
	BIND_SESSION("bindsess"),
	
	SEND_MSG("sendmsg"),

	STATS("stats"),
	
	// Notif
	SESS_CONNECT("sessconnect"),
	SESS_DISCONNECT("sessdisconnect"),
	
	CANNOT_SEND("cannotsend"),
	DIRECT_MSG("directmsg"),
	FILTERED_MSG("filteredmsg"),
	LEGACY_CPIMDELIVERED("legacy_cpimdelivered"),
	
	TRANSFER_SUCCESS("transfer_success"),
	TRANSFER_FAILED("transfer_failed"),
	;
	
	private String value;

	private SwMSRPCompliantPathMethodEnum(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}

	public static SwMSRPCompliantPathMethodEnum getValueOf(String val) {
		if (val == null) return null;
		for (SwMSRPCompliantPathMethodEnum e : SwMSRPCompliantPathMethodEnum.values()) {
			if (e.value.equals(val)) return e; 
		}
		return null;
	}

}
