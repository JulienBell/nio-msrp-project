package org.msrpenabler.server.cnx;

import org.msrpenabler.server.api.DefaultMsrpSession;
import org.msrpenabler.server.exception.TransactionException;
import org.msrpenabler.server.net.TransactionHandler;

public class MsrpConnexion extends TransactionHandler {


//	private static final Logger logger =
//        LoggerFactory.getLogger(MsrpConnexion.class);	
	
	
//    public MsrpConnexion(MsrpSessionHdImpl msrpSessionHdImpl) {
//		super(msrpSessionHdImpl);
//	}


	public void closeSess(DefaultMsrpSession msrpSess) throws TransactionException {

    	closeSess(msrpSess.getLocalSessId(), msrpSess.getRemoteSessId() );
    }

}
