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

package io.github.pnoker.api.center.manager.fallback;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.DriverAttributeClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.service.ManagerServiceConstant;
import io.github.pnoker.api.center.manager.dto.DriverAttributeDto;
import io.github.pnoker.common.model.DriverAttribute;
import io.github.pnoker.common.utils.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DriverAttributeClient
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverAttributeClientFallback implements FallbackFactory<DriverAttributeClient> {

    @Override
    public DriverAttributeClient create(Throwable throwable) {
        String message = ExceptionUtil.getNotAvailableServiceMessage(ManagerServiceConstant.SERVICE_NAME, throwable.getMessage());
        log.error("Fallback:{}", message);

        return new DriverAttributeClient() {

            /**
             * {@inheritDoc}
             */
            @Override
            public R<DriverAttribute> add(DriverAttribute driverAttribute) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Boolean> delete(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<DriverAttribute> update(DriverAttribute driverAttribute) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<DriverAttribute> selectById(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<List<DriverAttribute>> selectByDriverId(String id) {
                return R.fail(message);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public R<Page<DriverAttribute>> list(DriverAttributeDto driverAttributeDto) {
                return R.fail(message);
            }

        };
    }
}