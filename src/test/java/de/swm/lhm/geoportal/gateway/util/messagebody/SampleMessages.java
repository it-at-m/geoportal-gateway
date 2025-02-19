package de.swm.lhm.geoportal.gateway.util.messagebody;

import java.util.Base64;

/*
https://stackoverflow.com/questions/1089662/python-inflate-and-deflate-implementations

Python 3.12.3 (main, Apr  9 2024, 08:09:14) [Clang 15.0.0 (clang-1500.3.9.4)] on darwin
Type "help", "copyright", "credits" or "license" for more information.
>>> import zlib
>>> import base64
>>> base64.b64encode(zlib.compress(b"payload"))
b'eJwrSKzMyU9MAQAL3QLr'
>>> base64.b64encode(zlib.compress(b"payload")[2:-4])
b'K0iszMlPTAEA'
 */
public class SampleMessages {
    static final String MESSAGE1_CONTENT = "payload";
    static final byte[] MESSAGE1_DEFLATED_BYTES = Base64.getDecoder().decode("K0iszMlPTAEA");
}
