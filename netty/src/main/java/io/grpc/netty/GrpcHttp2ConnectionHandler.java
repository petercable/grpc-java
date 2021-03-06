/*
 * Copyright 2016, gRPC Authors All rights reserved.
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

package io.grpc.netty;

import io.grpc.Attributes;
import io.grpc.Internal;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Settings;

/**
 * gRPC wrapper for {@link Http2ConnectionHandler}.
 */
@Internal
public abstract class GrpcHttp2ConnectionHandler extends Http2ConnectionHandler {
  public GrpcHttp2ConnectionHandler(Http2ConnectionDecoder decoder,
      Http2ConnectionEncoder encoder,
      Http2Settings initialSettings) {
    super(decoder, encoder, initialSettings);
  }

  /**
   * Triggered on protocol negotiation completion.
   *
   * <p>It must me called after negotiation is completed but before given handler is added to the
   * channel.
   *
   * @param attrs arbitrary attributes passed after protocol negotiation (eg. SSLSession).
   */
  public void handleProtocolNegotiationCompleted(Attributes attrs) {
  }
}
