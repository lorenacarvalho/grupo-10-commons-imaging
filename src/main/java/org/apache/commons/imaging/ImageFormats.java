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
package org.apache.commons.imaging;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Enumerates known image formats.
 *
 * <p>
 * Each constant holds everything that identifies the format: its name, its file name extensions and, when the format has one, the "magic numbers" (the first
 * bytes of the data) used to recognize it.
 * </p>
 */
public enum ImageFormats implements ImageFormat {

    /** Unknown image format. */
    UNKNOWN("bin"),

    /** BMP (Windows Bitmap) image format. */
    BMP(magic(MagicNumber.of(0x42, 0x4d)), "bmp", "dib"),

    /** DCX (ZSoft Multi-page Paintbrush) image format. */
    DCX(magic(MagicNumber.of(0xB1, 0x68)), "dcx"),

    /** GIF (Graphics Interchange Format) image format. */
    GIF(magic(MagicNumber.of(0x47, 0x49)), "gif"),

    /** ICNS (Apple Icon Image) format. */
    ICNS(magic(MagicNumber.of(0x69, 0x63)), "icns"),

    /** ICO (Windows Icon) image format. */
    ICO("ico"),

    /** JBIG2 image format. */
    JBIG2(magic(MagicNumber.of(0x97, 0x4A, 0x42, 0x32)), "jbig2"),

    /** JPEG (Joint Photographic Experts Group) image format. */
    JPEG(magic(MagicNumber.of(0xff, 0xd8)), "jpg", "jpeg"),

    /** PAM (Portable Arbitrary Map) image format. */
    PAM(magic(MagicNumber.of(0x50, 0x37)), "pam"),

    /** PSD (Adobe Photoshop Document) image format. */
    PSD(magic(MagicNumber.of(0x38, 0x42)), "psd"),

    /** PBM (Portable Bitmap) image format. */
    PBM(magic(MagicNumber.of(0x50, 0x31), MagicNumber.of(0x50, 0x34)), "pbm"),

    /** PGM (Portable Graymap) image format. */
    PGM(magic(MagicNumber.of(0x50, 0x32), MagicNumber.of(0x50, 0x35)), "pgm"),

    /** PNM (Portable Any Map) image format. */
    PNM("pnm"),

    /** PPM (Portable Pixmap) image format. */
    PPM(magic(MagicNumber.of(0x50, 0x33), MagicNumber.of(0x50, 0x36)), "ppm"),

    /** PCX (PC Paintbrush) image format. */
    PCX("pcx", "pcc"),

    /** PNG (Portable Network Graphics) image format. */
    PNG(magic(MagicNumber.of(0x89, 0x50)), "png"),

    /** RGBE (Radiance HDR) image format. */
    RGBE(magic(MagicNumber.of(0x23, 0x3F)), "hdr", "pic"),

    /** TGA (Truevision TGA/TARGA) image format. */
    TGA("tga"),

    /** TIFF (Tagged Image File Format). */
    TIFF(magic(MagicNumber.of(0x4D, 0x4D), MagicNumber.of(0x49, 0x49)), "tif", "tiff"),

    /** WBMP (Wireless Bitmap) image format. */
    WBMP("wbmp"),

    /** WebP image format (a RIFF container: "RIFF", 4 bytes with the file size, "WEBP"). */
    WEBP(magic(MagicNumber.of(0x52, 0x49, 0x46, 0x46, MagicNumber.ANY, MagicNumber.ANY, MagicNumber.ANY, MagicNumber.ANY, 0x57, 0x45, 0x42, 0x50)), "webp"),

    /** XBM (X11 Bitmap) image format. */
    XBM("xbm"),

    /** XPM (X11 Pixmap) image format. */
    XPM("xpm");

    /**
     * A sequence of bytes that identifies a format when found at the beginning of the data.
     */
    private static final class MagicNumber {

        /** Wildcard that matches any byte value. */
        private static final int ANY = -1;

        static MagicNumber of(final int... pattern) {
            return new MagicNumber(pattern);
        }

        private final int[] pattern;

        private MagicNumber(final int[] pattern) {
            this.pattern = pattern.clone();
        }

        int length() {
            return pattern.length;
        }

        boolean matches(final byte[] header, final int headerLength) {
            if (headerLength < pattern.length) {
                return false;
            }
            for (int i = 0; i < pattern.length; i++) {
                if (pattern[i] != ANY && pattern[i] != (header[i] & 0xff)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static MagicNumber[] magic(final MagicNumber... magicNumbers) {
        return magicNumbers;
    }

    /**
     * Finds the format whose magic numbers match the beginning of the given data.
     *
     * @param header       the first bytes of the data.
     * @param headerLength how many bytes of {@code header} are valid.
     * @return the matching format, or empty if none matches.
     */
    static Optional<ImageFormats> findByMagicNumbers(final byte[] header, final int headerLength) {
        return Stream.of(values()).filter(format -> format.hasMagicNumberMatching(header, headerLength)).findFirst();
    }

    /**
     * Finds the format that has the extension of the given file name.
     *
     * @param fileName the file name, may be null.
     * @return the format, or {@link #UNKNOWN} if the name is empty or has no known extension.
     */
    static ImageFormats findByFileName(final String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return UNKNOWN;
        }
        final String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return Stream.of(values()).filter(format -> Stream.of(format.extensions).anyMatch(fileExtension::equalsIgnoreCase)).findFirst().orElse(UNKNOWN);
    }

    /**
     * Gets how many bytes from the beginning of the data are needed to check the magic numbers of every format.
     *
     * @return the number of bytes.
     */
    static int maxMagicNumberLength() {
        return Stream.of(values()).flatMap(format -> Stream.of(format.magicNumbers)).mapToInt(MagicNumber::length).max().orElse(0);
    }

    private final MagicNumber[] magicNumbers;

    private final String[] extensions;

    ImageFormats(final MagicNumber[] magicNumbers, final String... extensions) {
        this.magicNumbers = Objects.requireNonNull(magicNumbers, "magicNumbers");
        this.extensions = Objects.requireNonNull(extensions, "extensions");
    }

    ImageFormats(final String... extensions) {
        this(new MagicNumber[0], extensions);
    }

    @Override
    public String getDefaultExtension() {
        return extensions[0];
    }

    @Override
    public String[] getExtensions() {
        return this.extensions.clone();
    }

    @Override
    public String getName() {
        return name();
    }

    private boolean hasMagicNumberMatching(final byte[] header, final int headerLength) {
        return Stream.of(magicNumbers).anyMatch(magicNumber -> magicNumber.matches(header, headerLength));
    }
}
