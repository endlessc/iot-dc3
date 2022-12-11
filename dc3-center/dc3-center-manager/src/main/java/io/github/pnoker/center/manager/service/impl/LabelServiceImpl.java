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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.mapper.LabelMapper;
import io.github.pnoker.center.manager.service.LabelBindService;
import io.github.pnoker.center.manager.service.LabelService;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.api.center.manager.dto.LabelBindDto;
import io.github.pnoker.api.center.manager.dto.LabelDto;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.Label;
import io.github.pnoker.common.model.LabelBind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * LabelService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class LabelServiceImpl implements LabelService {

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private LabelBindService labelBindService;

    /**
     * {@inheritDoc}
     */
    @Override
    public Label add(Label label) {
        selectByName(label.getName(), label.getTenantId());
        if (labelMapper.insert(label) > 0) {
            return labelMapper.selectById(label.getId());
        }
        throw new ServiceException("The label add failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean delete(String id) {
        LabelBindDto labelBindDto = new LabelBindDto();
        labelBindDto.setLabelId(id);
        Page<LabelBind> labelBindPage = labelBindService.list(labelBindDto);
        if (labelBindPage.getTotal() > 0) {
            throw new ServiceException("The label already bound by the entity");
        }
        Label label = selectById(id);
        if (null == label) {
            throw new NotFoundException();
        }
        return labelMapper.deleteById(id) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Label update(Label label) {
        selectById(label.getId());
        label.setUpdateTime(null);
        if (labelMapper.updateById(label) > 0) {
            Label select = labelMapper.selectById(label.getId());
            label.setName(select.getName());
            return select;
        }
        throw new ServiceException("The label update failed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Label selectById(String id) {
        Label label = labelMapper.selectById(id);
        if (null == label) {
            throw new NotFoundException();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Label selectByName(String name, String tenantId) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        queryWrapper.eq(Label::getName, name);
        queryWrapper.eq(Label::getTenantId, tenantId);
        Label label = labelMapper.selectOne(queryWrapper);
        if (null == label) {
            throw new NotFoundException();
        }
        return label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<Label> list(LabelDto labelDto) {
        if (ObjectUtil.isNull(labelDto.getPage())) {
            labelDto.setPage(new Pages());
        }
        return labelMapper.selectPage(labelDto.getPage().convert(), fuzzyQuery(labelDto));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LambdaQueryWrapper<Label> fuzzyQuery(LabelDto labelDto) {
        LambdaQueryWrapper<Label> queryWrapper = Wrappers.<Label>query().lambda();
        if (ObjectUtil.isNotNull(labelDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(labelDto.getName()), Label::getName, labelDto.getName());
            queryWrapper.eq(CharSequenceUtil.isNotBlank(labelDto.getColor()), Label::getColor, labelDto.getColor());
            queryWrapper.eq(CharSequenceUtil.isNotEmpty(labelDto.getTenantId()), Label::getTenantId, labelDto.getTenantId());
        }
        return queryWrapper;
    }

}