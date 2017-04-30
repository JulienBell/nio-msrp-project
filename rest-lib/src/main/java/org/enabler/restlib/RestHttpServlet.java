package org.enabler.restlib;

import org.enabler.restlib.wrapper.RestStubFullHttpRequest;


public interface RestHttpServlet {

	abstract public void handleRcvRequest(RestStubFullHttpRequest req, RestHttpConnector cnxToRespond);

}
