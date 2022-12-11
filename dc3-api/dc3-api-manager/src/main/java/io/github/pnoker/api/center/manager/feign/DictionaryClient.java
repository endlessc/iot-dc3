/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.fallback.DictionaryClientFallback;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.bean.common.Dictionary;
import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.api.center.manager.dto.DictionaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 字典 FeignClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@FeignClient(path = ManagerServiceConstant.DICTIONARY_URL_PREFIX, name = ManagerServiceConstant.SERVICE_NAME, fallbackFactory = DictionaryClientFallback.class)
public interface DictionaryClient {

    /**
     * 查询驱动 Dictionary
     *
     * @param dictionaryDto DictionaryDto
     * @param tenantId      租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/driver")
    R<Page<Dictionary>> driverDictionary(@RequestBody(required = false) DictionaryDto dictionaryDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 查询模板 Dictionary
     *
     * @param dictionaryDto DictionaryDto
     * @param tenantId      租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/profile")
    R<Page<Dictionary>> profileDictionary(@RequestBody(required = false) DictionaryDto dictionaryDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 查询设备 Dictionary
     *
     * @param dictionaryDto DictionaryDto
     * @param tenantId      租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/device")
    R<Page<Dictionary>> deviceDictionary(@RequestBody(required = false) DictionaryDto dictionaryDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

    /**
     * 查询位号 Dictionary
     *
     * @param dictionaryDto DictionaryDto
     * @param tenantId      租户ID
     * @return Page Of Dictionary
     */
    @PostMapping("/point")
    R<Page<Dictionary>> pointDictionary(@RequestBody(required = false) DictionaryDto dictionaryDto, @RequestHeader(value = RequestConstant.Header.X_AUTH_TENANT_ID, defaultValue = "-1") String tenantId);

}