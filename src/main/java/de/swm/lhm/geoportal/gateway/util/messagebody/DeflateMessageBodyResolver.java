package de.swm.lhm.geoportal.gateway.util.messagebody;

import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyDecoder;
import org.springframework.cloud.gateway.filter.factory.rewrite.MessageBodyEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class DeflateMessageBodyResolver implements MessageBodyDecoder, MessageBodyEncoder {

    private final DeflateParameters deflateParameters = createDeflateParameters();

    private static DeflateParameters createDeflateParameters() {
        DeflateParameters parameters = new DeflateParameters();
        parameters.setWithZlibHeader(false);
        return parameters;
    }

    @Override
    public byte[] decode(byte[] encoded) {
        try (
                ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
                DeflateCompressorInputStream dis = new DeflateCompressorInputStream(bis, deflateParameters);
        ) {
            return FileCopyUtils.copyToByteArray(dis);
        } catch (IOException e) {
            throw new IllegalStateException("couldn't decode body from deflate", e);
        }
    }

    @Override
    public byte[] encode(DataBuffer original) {
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DeflateCompressorOutputStream dos = new DeflateCompressorOutputStream(bos, deflateParameters);
        ) {
            FileCopyUtils.copy(original.asInputStream(), dos);
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("couldn't decode body from deflate", e);
        }

    }

    @Override
    public String encodingType() {
        return "deflate";
    }
}
