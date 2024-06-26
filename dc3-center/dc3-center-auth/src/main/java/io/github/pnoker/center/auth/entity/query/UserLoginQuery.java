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

package io.github.pnoker.center.auth.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * User Query
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    // 查询字段

    /**
     * 登录名称
     */
    private String loginName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户密码ID
     */
    private Long userPasswordId;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}