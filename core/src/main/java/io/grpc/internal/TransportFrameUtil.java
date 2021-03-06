/*
 * Copyright 2014, gRPC Authors All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.internal;

import static com.google.common.base.Charsets.US_ASCII;

import com.google.common.io.BaseEncoding;
import io.grpc.InternalMetadata;
import io.grpc.Metadata;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Utility functions for transport layer framing.
 *
 * <p>Within a given transport frame we reserve the first byte to indicate the type of compression
 * used for the contents of the transport frame.
 */
public final class TransportFrameUtil {

  private static final Logger logger = Logger.getLogger(TransportFrameUtil.class.getName());

  private static final byte[] binaryHeaderSuffixBytes =
      Metadata.BINARY_HEADER_SUFFIX.getBytes(US_ASCII);

  /**
   * Transform the given headers to a format where only spec-compliant ASCII characters are allowed.
   * Binary header values are encoded by Base64 in the result.  It is safe to modify the returned
   * array, but not to modify any of the underlying byte arrays.
   *
   * @return the interleaved keys and values.
   */
  public static byte[][] toHttp2Headers(Metadata headers) {
    byte[][] serializedHeaders = InternalMetadata.serialize(headers);
    // TODO(carl-mastrangelo): eventually remove this once all callers are updated.
    if (serializedHeaders == null) {
      return new byte[][]{};
    }
    int k = 0;
    for (int i = 0; i < serializedHeaders.length; i += 2) {
      byte[] key = serializedHeaders[i];
      byte[] value = serializedHeaders[i + 1];
      if (endsWith(key, binaryHeaderSuffixBytes)) {
        // Binary header.
        serializedHeaders[k] = key;
        serializedHeaders[k + 1] = BaseEncoding.base64().encode(value).getBytes(US_ASCII);
        k += 2;
      } else {
        // Non-binary header.
        // Filter out headers that contain non-spec-compliant ASCII characters.
        // TODO(zhangkun83): only do such check in development mode since it's expensive
        if (isSpecCompliantAscii(value)) {
          serializedHeaders[k] = key;
          serializedHeaders[k + 1] = value;
          k += 2;
        } else {
          String keyString = new String(key, US_ASCII);
          logger.warning("Metadata key=" + keyString + ", value=" + Arrays.toString(value)
              + " contains invalid ASCII characters");
        }
      }
    }
    // Fast path, everything worked out fine.
    if (k == serializedHeaders.length) {
      return serializedHeaders;
    }
    return Arrays.copyOfRange(serializedHeaders, 0, k);
  }

  /**
   * Transform HTTP/2-compliant headers to the raw serialized format which can be deserialized by
   * metadata marshallers. It decodes the Base64-encoded binary headers.  This function modifies
   * the headers in place.  By modifying the input array.
   *
   * @param http2Headers the interleaved keys and values of HTTP/2-compliant headers
   * @return the interleaved keys and values in the raw serialized format
   */
  public static byte[][] toRawSerializedHeaders(byte[][] http2Headers) {
    for (int i = 0; i < http2Headers.length; i += 2) {
      byte[] key = http2Headers[i];
      byte[] value = http2Headers[i + 1];
      http2Headers[i] = key;
      if (endsWith(key, binaryHeaderSuffixBytes)) {
        // Binary header
        http2Headers[i + 1] = BaseEncoding.base64().decode(new String(value, US_ASCII));
      } else {
        // Non-binary header
        // Nothing to do, the value is already in the right place.
      }
    }
    return http2Headers;
  }

  /**
   * Returns {@code true} if {@code subject} ends with {@code suffix}.
   */
  private static boolean endsWith(byte[] subject, byte[] suffix) {
    int start = subject.length - suffix.length;
    if (start < 0) {
      return false;
    }
    for (int i = start; i < subject.length; i++) {
      if (subject[i] != suffix[i - start]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if {@code subject} contains only bytes that are spec-compliant ASCII
   * characters and space.
   */
  private static boolean isSpecCompliantAscii(byte[] subject) {
    for (byte b : subject) {
      if (b < 32 || b > 126) {
        return false;
      }
    }
    return true;
  }

  private TransportFrameUtil() {}
}
