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
package io.github.pnoker.driver.api.annotation;

import io.github.pnoker.driver.api.S7Type;

import java.lang.annotation.*;

/**
 * Defines an Offset in a DB
 *
 * @author Thomas Rudin
 */
@Target(value = {ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface S7Variable {
    /**
     * The size of the array
     *
     * @return Size
     */
    int arraySize() default 1;

    /**
     * The bit offset, if any
     *
     * @return Offset
     */
    int bitOffset() default 0;

    /**
     * The Byte Offset
     *
     * @return Offset
     */
    int byteOffset();

    /**
     * The specified size (for String)
     *
     * @return Size
     */
    int size() default 0;

    /**
     * The corresponding S7 Type
     *
     * @return S7Type
     */
    S7Type type();

}
