/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pnoker.driver.api;

import io.github.pnoker.driver.api.impl.serializer.converter.*;

/**
 * Type of the Address
 *
 * @author Thomas Rudin Libnodave:
 * <a href="http://libnodave.sourceforge.net/">libnodave.sourceforge.net</a>
 */
public enum S7Type {
    /**
     * Boolean type
     */
    BOOL(BitConverter.class, 0, 1),

    /**
     * Byte type
     */
    BYTE(ByteConverter.class, 1, 0),

    /**
     * A INT-type
     */
    INT(ShortConverter.class, 2, 0),

    /**
     * A DINT-type (same as DWORD-type)
     */
    DINT(LongConverter.class, 4, 0),

    /**
     * A Word-type (same as int-type)
     */
    WORD(IntegerConverter.class, 2, 0),
    /**
     * Double word
     */
    DWORD(LongConverter.class, 4, 0),

    /**
     * Real-type, corresponds to float or double
     */
    REAL(RealConverter.class, 4, 0),

    /**
     * String type, size must be specified manually
     */
    STRING(StringConverter.class, 2, 0),

    /**
     * Simple Date with 2 bytes in length
     */
    DATE(DateConverter.class, 2, 0),

    /**
     * Time-type, 4 bytes in length, number of millis
     */
    TIME(TimeConverter.class, 4, 0),

    /**
     * Full Date and time format with precision in milliseconds
     */
    DATE_AND_TIME(DateAndTimeConverter.class, 8, 0),

    /**
     * Structure type
     */
    STRUCT(StructConverter.class, 0, 0);

    private final int byteSize;
    private final int bitSize;

    private final Class<? extends S7Serializable> serializer;

    /**
     * Enum Constructor
     *
     * @param serializer S7Serializable
     * @param byteSize   A Byte Size
     * @param bitSize    A bit Size
     */
    S7Type(final Class<? extends S7Serializable> serializer, final int byteSize, final int bitSize) {
        this.serializer = serializer;
        this.bitSize = bitSize;
        this.byteSize = byteSize;
    }

    public int getBitSize() {
        return this.bitSize;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    public Class<? extends S7Serializable> getSerializer() {
        return this.serializer;
    }
}
