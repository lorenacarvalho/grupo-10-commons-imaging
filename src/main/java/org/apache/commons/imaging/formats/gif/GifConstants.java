/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.imaging.formats.gif;

/**
 * GIF format constants shared by {@link GifImageParser} and {@link GifWriter}.
 */
final class GifConstants {

    /** Introduces an extension block. */
    static final int EXTENSION_CODE = 0x21;

    /** Introduces an image descriptor. */
    static final int IMAGE_SEPARATOR = 0x2C;

    /** Marks the end of the GIF data stream. */
    static final int TERMINATOR_BYTE = 0x3b;

    /** Application extension label. */
    static final int APPLICATION_EXTENSION_LABEL = 0xff;

    /** Application identifier and authentication code of the XMP application extension. */
    static final byte[] XMP_APPLICATION_ID_AND_AUTH_CODE = { 0x58, // X
            0x4D, // M
            0x50, // P
            0x20, //
            0x44, // D
            0x61, // a
            0x74, // t
            0x61, // a
            0x58, // X
            0x4D, // M
            0x50, // P
    };

    private GifConstants() {
    }
}
