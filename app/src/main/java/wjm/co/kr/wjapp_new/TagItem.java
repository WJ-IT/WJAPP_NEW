package wjm.co.kr.wjapp_new;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class TagItem {
    public String epcID;
    public String epcID_Ascii;
    private long count = 0;
    private static final String TAG = "WjmAPP";

    public TagItem(String _tagID){
        this.epcID = getEpc(_tagID);
        //origin
        this.epcID_Ascii = convertEpc(epcID);
        //posco rule
        //this.epcID_Ascii = convertTagValue(epcID);
        this.count = 1;
    }
    public void setEpcID_Ascii(String temp) {
        epcID_Ascii = temp;
    }
    public String getEpcID() {
        return epcID;
    }

    public String getEpcID_Ascii() {
        return epcID_Ascii;
    }

    public int getCount() {
        return (int)count;
    }

    public void incrCount() {
        count ++;
    }

    private String getEpc(String id) {
        String retVal = id.substring(4);
        return retVal;
    }

    private String convertEpc(String id) {
        String retVal = "encoding-error";

        byte[] temp = getEpcByte(id);
        retVal = new String(temp, StandardCharsets.UTF_8);

        return retVal;
    }

    private String convertTagValue(String id) {
        StringBuilder sb = new StringBuilder();

        for( int i=0; i<id.length()-1; i+=2 ){

            //	grab the hex in pairs
            String output = id.substring(i, (i + 2));
            //	convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

        }

        return sb.toString();
    }

    private byte[] getEpcByte(String epc) {
        int length = epc.length() / 2;
        byte[] retval = new byte[length];

        String two_chars;
        int offset = 0;
        for (int i = 0; i < length; i++) {
            two_chars = epc.substring(offset, offset + 2);
            retval[i] = convertStringToByte(two_chars);
            offset += 2;
        }

        return retval;
    }

    private byte convertStringToByte(String ch_two) {
        byte[] temp = ch_two.getBytes();

        if (temp[0] < 0x40) temp[0] -= 0x30;
        else temp[0] -= 0x37;

        if (temp[1] < 0x40) temp[1] -= 0x30;
        else temp[1] -= 0x37;

        byte retval = (byte)(temp[0]*16 + temp[1]);
        if(retval < 48) retval = 32;
        return retval;
    }
}
