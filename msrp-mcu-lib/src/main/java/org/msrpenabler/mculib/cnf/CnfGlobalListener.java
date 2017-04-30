package org.msrpenabler.mculib.cnf;

import org.msrpenabler.server.api.MsrpChunkData;
import org.msrpenabler.server.api.MsrpMessageData;
import org.msrpenabler.server.api.MsrpSessionHd;

public abstract interface CnfGlobalListener {

	/**
	 * Here the content data is not retain in memory after the call
	 * user have to duplicate or retain the content during the call if necessary...
	 */
	public void evtRcvMessage(ConferenceUnit confHub, MsrpSessionHd fromSessMsrp, MsrpMessageData msrpContent, boolean wasChunked);	

	/**
	 * Here the content data is not retain in memory after the call
	 * user have to duplicate or retain the content during the call if necessary...
	 */
	public void evtRcvMsrpChunk(ConferenceUnit confHub, MsrpSessionHd fromSessMsrp, MsrpChunkData msrpChunk);

}
