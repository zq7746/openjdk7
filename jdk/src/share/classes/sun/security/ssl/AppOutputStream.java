/*
 * Copyright (c) 1996, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package sun.security.ssl;

import java.io.OutputStream;
import java.io.IOException;

/*
 * Output stream for application data. This is the kind of stream
 * that's handed out via SSLSocket.getOutputStream(). It's all the application
 * ever sees.
 *
 * Once the initial handshake has completed, application data may be
 * interleaved with handshake data. That is handled internally and remains
 * transparent to the application.
 *
 * @author  David Brownell
 */
class AppOutputStream extends OutputStream {

    private SSLSocketImpl c;
    OutputRecord r;

    // One element array used to implement the write(byte) method
    private final byte[] oneByte = new byte[1];

    AppOutputStream(SSLSocketImpl conn) {
        r = new OutputRecord(Record.ct_application_data);
        c = conn;
    }

    /**
     * Write the data out, NOW.
     */
    synchronized public void write(byte b[], int off, int len)
            throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        // check if the Socket is invalid (error or closed)
        c.checkWrite();

        // Always flush at the end of each application level record.
        // This lets application synchronize read and write streams
        // however they like; if we buffered here, they couldn't.
        try {
            do {
                int howmuch = Math.min(len, r.availableDataBytes());

                // NOTE: *must* call c.writeRecord() even for howmuch == 0
                if (howmuch > 0) {
                    r.write(b, off, howmuch);
                    off += howmuch;
                    len -= howmuch;
                }
                c.writeRecord(r);
                c.checkWrite();
            } while (len > 0);
        } catch (Exception e) {
            // shutdown and rethrow (wrapped) exception as appropriate
            c.handleException(e);
        }
    }

    /**
     * Write one byte now.
     */
    synchronized public void write(int i) throws IOException {
        oneByte[0] = (byte)i;
        write(oneByte, 0, 1);
    }

    /*
     * Socket close is already synchronized, no need to block here.
     */
    public void close() throws IOException {
        c.close();
    }

    // inherit no-op flush()
}
