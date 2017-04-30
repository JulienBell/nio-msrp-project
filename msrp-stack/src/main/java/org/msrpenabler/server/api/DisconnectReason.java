package org.msrpenabler.server.api;

public enum DisconnectReason {

		CNX_FAILURE("cnx_failure"),
		REMOTE_CLOSE("remote_close"),
		LOCAL_CLOSE("local_close"),
		INACTIVITY_RW("inactivity_timeout"), // Read and write inactivity on Cnx
		;
		
		private String value;

		private DisconnectReason(String value) {
			this.value = value;
		}

		public static DisconnectReason getValueOf(String val) {
			if (val == null) return null;
			for (DisconnectReason e : DisconnectReason.values()) {
				if (e.value.equals(val)) return e; 
			}
			return null;
		}

		public String toString() {
			return value;
		}
		
}
