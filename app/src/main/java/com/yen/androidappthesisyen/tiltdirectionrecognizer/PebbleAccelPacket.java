package com.yen.androidappthesisyen.tiltdirectionrecognizer;

/**
 * SOURCE: Partially based on https://gist.github.com/C-D-Lewis/ba1349bb0ebdee76b0cf#file-pebbleaccelpacket-java
 */






// IS OUD EN MAG WEG









public class PebbleAccelPacket {

    // TODO private maken? maar bij Android performance guide stond dat getters/setters overbodig was dus wrsl zo laten?
    // (zet mss wel verklaring bij)
    //Data members
    public int x, y, z;

    /**
     * Default constructor
     */
    public PebbleAccelPacket() {
        x = 0;
        y = 0;
        y = 0;
    }

    /**
     * Create with data
     *
     * @param inX X value
     * @param inY Y value
     * @param inZ Z value
     */
    public PebbleAccelPacket(int inX, int inY, int inZ) {
        x = inX;
        y = inY;
        z = inZ;
    }


}
