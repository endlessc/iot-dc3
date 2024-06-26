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

package io.github.pnoker.center.manager.grpc.builder;

import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcDriverAttributeConfigDTO;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

/**
 * GrpcDriverAttributeConfig Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcDriverAttributeConfigBuilder {

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DriverAttributeConfigBO
     * @return GrpcDriverAttributeConfigDTO
     */
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "configValueBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcDriverAttributeConfigDTO buildGrpcDTOByBO(DriverAttributeConfigBO entityBO);

    @AfterMapping
    default void afterProcess(DriverAttributeConfigBO entityBO, @MappingTarget GrpcDriverAttributeConfigDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.NULL_INT));
    }
}
