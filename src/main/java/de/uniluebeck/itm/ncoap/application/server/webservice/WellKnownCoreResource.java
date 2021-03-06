/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uniluebeck.itm.ncoap.application.server.webservice;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import de.uniluebeck.itm.ncoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * The .well-known/core resource is a standard webservice to be provided by every CoAP webserver as defined in
 * the CoAP protocol draft. It provides a list of all available services on the server in CoRE Link Format.
 *
 * @author Oliver Kleine
 */
public final class WellKnownCoreResource extends ObservableWebService<Map<String, WebService>> {

    private static Logger log = LoggerFactory.getLogger(WellKnownCoreResource.class.getName());

    /**
     * Creates the well-known/core resource at path /.well-known/core as defined in the CoAP draft
     * @param initialStatus the Map containing all available path
     */
    public WellKnownCoreResource(Map<String, WebService> initialStatus) {
        super("/.well-known/core", initialStatus);
    }

    /**
     * The .well-known/core resource only allows requests with {@link Code#GET}. Any other out of {@link Code#POST},
     * {@link Code#PUT} or {@link Code#DELETE} returns a {@link CoapResponse} with {@link Code#METHOD_NOT_ALLOWED_405}.
     *
     * In case of a request with {@link Code#GET} it returns a {@link CoapResponse} with {@link Code#CONTENT_205} and
     * with a payload listing all paths to the available resources (i.e. {@link WebService} instances}). The
     * payload is always formatted in {@link MediaType#APP_LINK_FORMAT}. If the request contains an
     * {@link OptionName#ACCEPT} option requesting another payload format, this option is ignored.
     *
     * @param request The {@link CoapRequest} to be processed by the {@link WebService} instance
     * @param remoteAddress The address of the sender of the request
     * @return the list of all paths of services registered at the same {@link CoapServerApplication} instance as
     * this service is registered at.
     */
    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest request,
                                   InetSocketAddress remoteAddress) {

        if(request.getCode() != Code.GET){
            responseFuture.set(new CoapResponse(Code.METHOD_NOT_ALLOWED_405));
            return;
        }

        CoapResponse response = new CoapResponse(Code.CONTENT_205);

        try {
            byte[] payload = getSerializedResourceStatus(MediaType.APP_LINK_FORMAT);
            response.setPayload(ChannelBuffers.wrappedBuffer(payload));
            response.setContentType(OptionRegistry.MediaType.APP_LINK_FORMAT);

        } catch (Exception e) {
            log.error("This should never happen.", e);
        }

        responseFuture.set(response);
    }

    @Override
    public byte[] getSerializedResourceStatus(MediaType mediaType) throws MediaTypeNotSupportedException {
        StringBuffer buffer = new StringBuffer();

        //TODO make this real CoRE link format
        for(String path : getResourceStatus().keySet()){
            buffer.append("<" + path + ">,\n");
        }
        buffer.deleteCharAt(buffer.length()-2);

        return buffer.toString().getBytes(Charset.forName("UTF-8"));
    }

    @Override
    public void shutdown() {
        //nothing to do here...
    }
}
