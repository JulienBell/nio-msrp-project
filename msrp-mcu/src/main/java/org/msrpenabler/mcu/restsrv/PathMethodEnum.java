package org.msrpenabler.mcu.restsrv;

public enum PathMethodEnum {
	
	CREATE_SESSION("create_sess"),
	DELETE_SESSION("delete_sess"),
	
	CREATE_CONF("create_conf"),
	DELETE_CONF("delete_conf"),
	
	ATTACH_SESSION("attach_sess"),
	DETACH_SESSION("detach_sess"),
	
	CONNECT_SESSION("connect_sess"),
	BIND_SESSION("bind_sess"),
	
	;
	
	private String value;

	private PathMethodEnum(String value) {
		this.value = value;
	}

	public static PathMethodEnum getValueOf(String val) {
		if (val == null) return null;
		for (PathMethodEnum e : PathMethodEnum.values()) {
			if (e.value.equals(val)) return e; 
		}
		return null;
	}

}
