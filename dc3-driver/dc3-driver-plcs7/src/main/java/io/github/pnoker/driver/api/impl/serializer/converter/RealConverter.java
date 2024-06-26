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
package io.github.pnoker.driver.api.impl.serializer.converter;

import io.github.pnoker.driver.api.S7Serializable;
import io.github.pnoker.driver.api.S7Type;

public final class RealConverter implements S7Serializable {

    private static final int OFFSET_POS1 = 0;
    private static final int OFFSET_POS2 = 1;
    private static final int OFFSET_POS3 = 2;
    private static final int OFFSET_POS4 = 3;

    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final int iValue = ((buffer[byteOffset + OFFSET_POS4] & 0xFF))
                | ((buffer[byteOffset + OFFSET_POS3] & 0xFF) << 8) | ((buffer[byteOffset + OFFSET_POS2] & 0xFF) << 16)
                | ((buffer[byteOffset + OFFSET_POS1] & 0xFF) << 24);

        final Float fValue = Float.intBitsToFloat(iValue);

        Object ret = fValue;

        if (targetClass == Double.class) {
            ret = Double.parseDouble(fValue.toString());
        }

        return targetClass.cast(ret);
    }

    @Override
    public S7Type getS7Type() {
        return S7Type.REAL;
    }

    @Override
    public int getSizeInBits() {
        return 0;
    }

    @Override
    public int getSizeInBytes() {
        return 4;
    }

    @Override
    public void insert(final Object javaType, final byte[] buffer, final int byteOffset, final int bitOffset,
                       final int size) {
        final float fValue = Float.parseFloat(javaType.toString());

        final int iValue = Float.floatToIntBits(fValue);

        buffer[byteOffset + OFFSET_POS4] = (byte) ((iValue) & 0xFF);
        buffer[byteOffset + OFFSET_POS3] = (byte) ((iValue >> 8) & 0xFF);
        buffer[byteOffset + OFFSET_POS2] = (byte) ((iValue >> 16) & 0xFF);
        buffer[byteOffset + OFFSET_POS1] = (byte) ((iValue >> 24) & 0xFF);
    }

}
