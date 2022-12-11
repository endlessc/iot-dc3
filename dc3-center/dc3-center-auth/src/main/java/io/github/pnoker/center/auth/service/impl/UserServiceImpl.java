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

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.mapper.UserMapper;
import io.github.pnoker.center.auth.service.UserService;
import io.github.pnoker.common.annotation.Logs;
import io.github.pnoker.common.bean.common.Pages;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.api.center.auth.dto.UserDto;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.model.User;
import io.github.pnoker.common.utils.DecodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    @Logs("Add user")
    @Transactional
    public User add(User user) {
        // todo 不通过，会返回密码数据
        // 判断用户是否存在
        User selectByName = selectByName(user.getName(), false);
        if (ObjectUtil.isNotNull(selectByName)) {
            throw new DuplicateException("The user already exists with username: {}", user.getName());
        }

        // 判断 phone 是否存在，如果有 phone 不为空，检查该 phone 是否被占用
        if (CharSequenceUtil.isNotBlank(user.getPhone())) {
            User selectByPhone = selectByPhone(user.getPhone(), false);
            if (ObjectUtil.isNotNull(selectByPhone)) {
                throw new DuplicateException("The user already exists with phone: {}", user.getPhone());
            }
        }

        // 判断 email 是否存在，如果有 email 不为空，检查该 email 是否被占用
        if (CharSequenceUtil.isNotBlank(user.getEmail())) {
            User selectByEmail = selectByEmail(user.getEmail(), false);
            if (ObjectUtil.isNotNull(selectByEmail)) {
                throw new DuplicateException("The user already exists with email: {}", user.getEmail());
            }
        }

        // 插入 user 数据，并返回插入后的 user
        if (userMapper.insert(user.setPassword(DecodeUtil.md5(user.getPassword()))) > 0) {
            return userMapper.selectById(user.getId());
        }

        throw new AddException("The user add failed: {}", user.toString());
    }

    @Override
    @Transactional
    public Boolean delete(String id) {
        User user = selectById(id);
        if (null == user) {
            throw new NotFoundException();
        }
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public User update(User user) {
        User byId = selectById(user.getId());
        // todo 需要调整，用户名、手机号、邮箱保持租户内唯一即可，没有必要全局唯一
        // 判断 phone 是否修改
        if (CharSequenceUtil.isNotBlank(user.getPhone())) {
            if (!user.getPhone().equals(byId.getPhone())) {
                User byPhone = selectByPhone(user.getPhone(), false);
                if (ObjectUtil.isNotNull(byPhone)) {
                    throw new DuplicateException("The user already exists with phone {}", user.getPhone());
                }
            }
        } else {
            user.setPhone(null);
        }

        // 判断 email 是否修改
        if (CharSequenceUtil.isNotBlank(user.getEmail())) {
            if (!user.getEmail().equals(byId.getEmail())) {
                User byEmail = selectByEmail(user.getEmail(), false);
                if (ObjectUtil.isNotNull(byEmail)) {
                    throw new DuplicateException("The user already exists with email {}", user.getEmail());
                }
            }
        } else {
            user.setEmail(null);
        }

        user.setName(null).setUpdateTime(null);
        if (userMapper.updateById(user) > 0) {
            User select = userMapper.selectById(user.getId());
            user.setName(select.getName());
            return select;
        }
        throw new ServiceException("The user update failed");
    }

    @Override
    public User selectById(String id) {
        return userMapper.selectById(id);
    }

    @Override
    public User selectByName(String name, boolean isEx) {
        if (CharSequenceUtil.isEmpty(name)) {
            if (isEx) {
                throw new EmptyException("The name is empty");
            }
            return null;
        }

        return selectByKey(User::getName, name, isEx);
    }

    @Override
    public User selectByPhone(String phone, boolean isEx) {
        if (CharSequenceUtil.isEmpty(phone)) {
            if (isEx) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(User::getPhone, phone, isEx);
    }

    @Override
    public User selectByEmail(String email, boolean isEx) {
        if (CharSequenceUtil.isEmpty(email)) {
            if (isEx) {
                throw new EmptyException("The phone is empty");
            }
            return null;
        }

        return selectByKey(User::getEmail, email, isEx);
    }

    @Override
    public Page<User> list(UserDto userDto) {
        if (ObjectUtil.isNull(userDto.getPage())) {
            userDto.setPage(new Pages());
        }
        return userMapper.selectPage(userDto.getPage().convert(), fuzzyQuery(userDto));
    }

    @Override
    public boolean checkUserValid(String name) {
        User user = selectByName(name, false);
        if (ObjectUtil.isNotNull(user)) {
            return user.getEnable();
        }

        user = selectByPhone(name, false);
        if (ObjectUtil.isNotNull(user)) {
            return user.getEnable();
        }

        user = selectByEmail(name, false);
        if (ObjectUtil.isNotNull(user)) {
            return user.getEnable();
        }

        return false;
    }

    @Override
    public boolean restPassword(String id) {
        User user = selectById(id);
        if (ObjectUtil.isNotNull(user)) {
            user.setPassword(DecodeUtil.md5(AlgorithmConstant.DEFAULT_PASSWORD));
            return null != update(user);
        }
        return false;
    }

    @Override
    public LambdaQueryWrapper<User> fuzzyQuery(UserDto userDto) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        if (ObjectUtil.isNotNull(userDto)) {
            queryWrapper.like(CharSequenceUtil.isNotBlank(userDto.getName()), User::getName, userDto.getName());
        }
        return queryWrapper;
    }

    private User selectByKey(SFunction<User, ?> key, String value, boolean isEx) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>query().lambda();
        queryWrapper.eq(key, value);
        User user = userMapper.selectOne(queryWrapper);
        if (ObjectUtil.isNull(user)) {
            if (isEx) {
                throw new NotFoundException();
            }
            return null;
        }
        return user;
    }

}