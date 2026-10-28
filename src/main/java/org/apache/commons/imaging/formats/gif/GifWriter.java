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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.AbstractBinaryOutputStream;
import org.apache.commons.imaging.common.Allocator;
import org.apache.commons.imaging.mylzw.MyLzwCompressor;
import org.apache.commons.imaging.palette.Palette;
import org.apache.commons.imaging.palette.PaletteFactory;

/**
 * Writes GIF (Graphics Interchange Format) images.
 */
final class GifWriter {

    private static final Logger LOGGER = Logger.getLogger(GifWriter.class.getName());

    private static final int LOCAL_COLOR_TABLE_FLAG_MASK = 1 << 7;
    private static final int INTERLACE_FLAG_MASK = 1 << 6;
    private static final int SORT_FLAG_MASK = 1 << 5;

    private static final boolean LOCAL_COLOR_TABLE_FLAG = true;

    /**
     * Constructs a new GIF writer.
     */
    GifWriter() {
        // Default constructor
    }

    private int computeColorTableScaleLessOne(final int paletteSize) {
        return paletteSize > 128 ? 7
                : paletteSize > 64 ? 6 : paletteSize > 32 ? 5 : paletteSize > 16 ? 4 : paletteSize > 8 ? 3 : paletteSize > 4 ? 2 : paletteSize > 2 ? 1 : 0;
    }

    private Palette createPalette(final BufferedImage src, final int maxColors) throws ImagingException {
        Palette palette = new PaletteFactory().makeExactRgbPaletteSimple(src, maxColors);

        if (palette == null) {
            palette = new PaletteFactory().makeQuantizedRgbPalette(src, maxColors);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("quantizing");
            }
        } else if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("exact palette");
        }

        if (palette == null) {
            throw new ImagingException("Gif: can't write images with more than 256 colors");
        }
        return palette;
    }

    private void writeAsSubBlocks(final byte[] bytes, final OutputStream os) throws IOException {
        int index = 0;

        while (index < bytes.length) {
            final int blockSize = Math.min(bytes.length - index, 255);
            os.write(blockSize);
            os.write(bytes, index, blockSize);
            index += blockSize;
        }
        os.write(0); // last block
    }

    // ALWAYS write GraphicControlExtension
    private void writeGraphicControlExtension(final AbstractBinaryOutputStream bos, final boolean hasAlpha, final Palette palette) throws IOException {
        bos.write(GifConstants.EXTENSION_CODE);
        bos.write((byte) 0xf9);

        bos.write((byte) 4); // block size;
        final int packedFields = hasAlpha ? 1 : 0; // transparency flag
        bos.write((byte) packedFields);
        bos.write((byte) 0); // Delay Time
        bos.write((byte) 0); // Delay Time
        bos.write((byte) (hasAlpha ? palette.length() : 0)); // Transparent Color Index
        bos.write((byte) 0); // terminator
    }

    private void writeHeader(final AbstractBinaryOutputStream bos) throws IOException {
        bos.write(0x47); // G magic numbers
        bos.write(0x49); // I
        bos.write(0x46); // F

        bos.write(0x38); // 8 version magic numbers
        bos.write(0x39); // 9
        bos.write(0x61); // a
    }

    private void writeImageData(final AbstractBinaryOutputStream bos, final BufferedImage src, final Palette palette, final boolean hasAlpha,
            final int colorTableScaleLessOne) throws IOException {
        final int width = src.getWidth();
        final int height = src.getHeight();

        int lzwMinimumCodeSize = colorTableScaleLessOne + 1;
        if (lzwMinimumCodeSize < 2) {
            lzwMinimumCodeSize = 2;
        }

        // TODO: make better choice here.
        bos.write(lzwMinimumCodeSize);

        final MyLzwCompressor compressor = new MyLzwCompressor(lzwMinimumCodeSize, ByteOrder.LITTLE_ENDIAN, false); // GIF Mode

        final byte[] imageData = Allocator.byteArray(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int argb = src.getRGB(x, y);
                final int rgb = 0xffffff & argb;
                final int index;

                if (hasAlpha) {
                    final int alpha = 0xff & argb >> 24;
                    final int alphaThreshold = 255;
                    if (alpha < alphaThreshold) {
                        index = palette.length(); // is transparent
                    } else {
                        index = palette.getPaletteIndex(rgb);
                    }
                } else {
                    index = palette.getPaletteIndex(rgb);
                }

                imageData[y * width + x] = (byte) index;
            }
        }

        final byte[] compressed = compressor.compress(imageData);
        writeAsSubBlocks(compressed, bos);
    }

    /**
     * Writes an image in GIF format.
     *
     * @param src    the image to write.
     * @param os     the destination stream, closed when done.
     * @param params the GIF parameters, may be null.
     * @throws ImagingException if the image has more than 256 colors.
     * @throws IOException      if an I/O error occurs.
     */
    void writeImage(final BufferedImage src, final OutputStream os, GifImagingParameters params) throws ImagingException, IOException {
        if (params == null) {
            params = new GifImagingParameters();
        }

        final String xmpXml = params.getXmpXml();

        final int width = src.getWidth();
        final int height = src.getHeight();

        final boolean hasAlpha = new PaletteFactory().hasTransparency(src);

        final int maxColors = hasAlpha ? 255 : 256;

        final Palette palette = createPalette(src, maxColors);
        final int paletteSize = palette.length() + (hasAlpha ? 1 : 0);

        final int colorTableScaleLessOne = computeColorTableScaleLessOne(paletteSize);
        final int colorTableSizeInFormat = 1 << colorTableScaleLessOne + 1;

        try (AbstractBinaryOutputStream bos = AbstractBinaryOutputStream.littleEndian(os)) {
            writeHeader(bos);
            writeLogicalScreenDescriptor(bos, width, height, colorTableScaleLessOne);
            writeGraphicControlExtension(bos, hasAlpha, palette);
            if (xmpXml != null) {
                writeXmpExtension(bos, xmpXml);
            }
            writeImageDescriptor(bos, width, height, colorTableScaleLessOne);
            writeLocalColorTable(bos, palette, colorTableSizeInFormat);
            writeImageData(bos, src, palette, hasAlpha, colorTableScaleLessOne);
            bos.write(GifConstants.TERMINATOR_BYTE);
        }
        os.close();
    }

    private void writeImageDescriptor(final AbstractBinaryOutputStream bos, final int width, final int height, final int colorTableScaleLessOne)
            throws IOException {
        bos.write(GifConstants.IMAGE_SEPARATOR);
        bos.write2Bytes(0); // Image Left Position
        bos.write2Bytes(0); // Image Top Position
        bos.write2Bytes(width); // Image Width
        bos.write2Bytes(height); // Image Height

        final boolean interlaceFlag = false;
        final boolean sortFlag = false;
        final int sizeOfLocalColorTable = colorTableScaleLessOne;

        final int packedFields;
        if (LOCAL_COLOR_TABLE_FLAG) {
            packedFields = LOCAL_COLOR_TABLE_FLAG_MASK | (interlaceFlag ? INTERLACE_FLAG_MASK : 0) | (sortFlag ? SORT_FLAG_MASK : 0)
                    | 7 & sizeOfLocalColorTable;
        } else {
            packedFields = 0 | (interlaceFlag ? INTERLACE_FLAG_MASK : 0) | (sortFlag ? SORT_FLAG_MASK : 0) | 7 & sizeOfLocalColorTable;
        }
        bos.write(packedFields); // one byte
    }

    private void writeLocalColorTable(final AbstractBinaryOutputStream bos, final Palette palette, final int colorTableSizeInFormat) throws IOException {
        for (int i = 0; i < colorTableSizeInFormat; i++) {
            if (i < palette.length()) {
                final int rgb = palette.getEntry(i);

                final int red = 0xff & rgb >> 16;
                final int green = 0xff & rgb >> 8;
                final int blue = 0xff & rgb >> 0;

                bos.write(red);
                bos.write(green);
                bos.write(blue);
            } else {
                bos.write(0);
                bos.write(0);
                bos.write(0);
            }
        }
    }

    private void writeLogicalScreenDescriptor(final AbstractBinaryOutputStream bos, final int width, final int height, final int colorTableScaleLessOne)
            throws IOException {
        bos.write2Bytes(width);
        bos.write2Bytes(height);

        final byte colorResolution = (byte) colorTableScaleLessOne; // TODO:
        final int packedFields = (7 & colorResolution) * 16;
        bos.write(packedFields); // one byte

        final byte backgroundColorIndex = 0;
        bos.write(backgroundColorIndex);

        final byte pixelAspectRatio = 0;
        bos.write(pixelAspectRatio);
    }

    private void writeXmpExtension(final AbstractBinaryOutputStream bos, final String xmpXml) throws IOException {
        bos.write(GifConstants.EXTENSION_CODE);
        bos.write(GifConstants.APPLICATION_EXTENSION_LABEL);

        bos.write(GifConstants.XMP_APPLICATION_ID_AND_AUTH_CODE.length); // 0x0B
        bos.write(GifConstants.XMP_APPLICATION_ID_AND_AUTH_CODE);

        final byte[] xmpXmlBytes = xmpXml.getBytes(StandardCharsets.UTF_8);
        bos.write(xmpXmlBytes);

        // write "magic trailer"
        for (int magic = 0; magic <= 0xff; magic++) {
            bos.write(0xff - magic);
        }

        bos.write((byte) 0); // terminator
    }
}
