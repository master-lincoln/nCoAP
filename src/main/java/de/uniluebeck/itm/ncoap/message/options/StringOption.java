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
package de.uniluebeck.itm.ncoap.message.options;

import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionName;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.OptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;

/**
 * This class contains all specific functionality for {@link Option} instances of {@link OptionType#STRING}. If there is
 * any need to access {@link Option} instances directly, e.g. to retrieve its value, one could either cast the option
 * to {@link StringOption} and call {@link #getDecodedValue()} or one could all {@link Option#getDecodedValue()} and
 * cast the return value to {@link String}.
 *
 * @author Oliver Kleine
 */
public class StringOption extends Option{

    private static Logger log = LoggerFactory.getLogger(StringOption.class.getName());

    //Constructor with encoded value should only be used for incoming messages
    StringOption(OptionName opt_name, byte[] value) throws InvalidOptionException{
        super(opt_name);
        setValue(opt_name, value);

        try {
            log.debug("New Option (" + opt_name + ") created (value: " + new String(this.value, CHARSET) +
                    ", encoded length: " + this.value.length + ")");
        } catch (UnsupportedEncodingException e) {
            log.debug("This should never happen:\n" + e);
        }
    }

    //Constructor with decoded value to be used for outgoing messages
    StringOption(OptionName optionName, String value) throws InvalidOptionException{
        super(optionName);
        //URI Host option must not contain upper case letters
        if(optionName == OptionName.URI_HOST){
            value =  value.toLowerCase(Locale.ENGLISH);
        }
        setValue(optionName,
                 convertToByteArrayWithoutPercentEncoding(optionName, value.toLowerCase(Locale.ENGLISH)));
    }

    //Sets the options value after checking option specific constraints
    private void setValue(OptionName optionName, byte[] bytes) throws InvalidOptionException{

        int min_length = OptionRegistry.getMinLength(optionName);
        int max_length = OptionRegistry.getMaxLength(optionName);


        //Check whether length constraints are fulfilled
        if(bytes.length < min_length || bytes.length > max_length){
            String msg = "[StringOption] Value length for " + optionName + " option must be between " +
                    min_length + " and " +  max_length + " but is " + bytes.length;
            throw new InvalidOptionException(optionNumber, msg);
        }

        //Set value if there was no Exception thrown so far
        this.value = bytes;
    }
    
    //Replaces percent-encoding from ASCII Strings with UTF-8 encoding
    private static byte[] convertToByteArrayWithoutPercentEncoding(OptionName optionName, String s)
            throws InvalidOptionException {

        ByteArrayInputStream in = null;
        try {
            in = new ByteArrayInputStream(s.getBytes(CHARSET));
        } catch (UnsupportedEncodingException e) {
            log.debug("This should never happen: \n", e);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int i;
        do{
            i = in.read();
            //-1 indicates end of stream
            if(i == -1){
                break;
            }
            //0x25 = '%'
            if(i == 0x25){
                //Character.digit returns the integer value encoded as in.read(). Since we know that percent encoding
                //uses bytes from 0x0 to 0xF (i.e. 0 to 15) the radix must be 16.
                int d1 = Character.digit(in.read(), 16);
                int d2 = Character.digit(in.read(), 16);

                if(d1 == -1 || d2 == -1){
                    //Unexpected end of stream (at least one byte missing after '%')
                    throw new InvalidOptionException(optionName.getNumber(), "Invalid percent encoding in: " + s);
                }

                //Write decoded value to Outputstream (e.g. sequence [0x02, 0x00] results into byte 0x20
                out.write((d1 << 4) | d2);
            }
            else{
                out.write(i);
            }
        } while(i != -1);

        return out.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof StringOption)){
            return false;
        }
        StringOption opt = (StringOption) o;
        if((this.optionNumber == opt.optionNumber) && Arrays.equals(this.value, opt.value)){
            return true;
        }
        return false;
    }

    /**
     * Returns the options value as decoded String assuming the value to be UTF-8 encoded
     * @return the options value as decoded String assuming the value to be UTF-8 encoded
     */
    public String getDecodedValue() {
        String result = null;
        try {
            result = new String(value, CHARSET);
        } catch (UnsupportedEncodingException e) {
           log.debug("This should never happen:\n", e);
        }
        return result;
    }
}
