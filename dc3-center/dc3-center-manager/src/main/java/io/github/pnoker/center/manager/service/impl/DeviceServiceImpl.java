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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.dal.DeviceManager;
import io.github.pnoker.center.manager.entity.bo.*;
import io.github.pnoker.center.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.query.DeviceQuery;
import io.github.pnoker.center.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.center.manager.mapper.DeviceMapper;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.PageUtil;
import io.github.pnoker.common.utils.PoiUtil;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * DeviceService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceServiceImpl implements DeviceService {

    @Resource
    private DeviceBuilder deviceBuilder;

    @Resource
    private DeviceManager deviceManager;
    @Resource
    private DeviceMapper deviceMapper;

    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointAttributeConfigService pointAttributeConfigService;
    @Resource
    private ProfileService profileService;
    @Resource
    private PointService pointService;
    @Resource
    private ProfileBindService profileBindService;
    @Resource
    private MetadataEventPublisher metadataEventPublisher;

    @Override
    public void save(DeviceBO entityBO) {
        checkDuplicate(entityBO, false, true);

        DeviceDO entityDO = deviceBuilder.buildDOByBO(entityBO);
        if (!deviceManager.save(entityDO)) {
            throw new AddException("Failed to create device");
        }

        addProfileBind(entityDO, entityBO.getProfileIds());

        // 通知驱动新增
        DeviceBO deviceBO = selectById(entityDO.getId());
        List<ProfileBO> profileBOList = profileService.selectByDeviceId(entityDO.getId());
        deviceBO.setProfileIds(profileBOList.stream().map(ProfileBO::getId).toList());
        MetadataEvent<DeviceBO> metadataEvent = new MetadataEvent<>(this, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.ADD, deviceBO);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    @Transactional
    public void remove(Long id) {
        DeviceDO entityDO = getDOById(id, true);

        // 删除设备之前需要检查该设备是否存在关联
        if (!profileBindService.removeByDeviceId(id)) {
            throw new DeleteException("Failed to remove profile bind");
        }

        if (!deviceManager.removeById(id)) {
            throw new DeleteException("Failed to remove device");
        }

        // 通知驱动删除设备
        DeviceBO deviceBO = deviceBuilder.buildBOByDO(entityDO);
        MetadataEvent<DeviceBO> metadataEvent = new MetadataEvent<>(this, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.DELETE, deviceBO);
        metadataEventPublisher.publishEvent(metadataEvent);
    }


    @Override
    public void update(DeviceBO entityBO) {
        DeviceDO entityDO = getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        List<Long> newProfileIds = Optional.ofNullable(entityBO.getProfileIds()).orElse(Collections.emptyList());
        List<Long> oldProfileIds = profileBindService.selectProfileIdsByDeviceId(entityBO.getId());

        // 新增的模版
        newProfileIds.removeAll(oldProfileIds);
        addProfileBind(entityDO, newProfileIds);

        // 删除的模版
        Set<Long> delete = new HashSet<>(oldProfileIds);
        delete.removeAll(newProfileIds);
//        delete.forEach(profileId -> profileBindService.removeByDeviceIdAndProfileId(entityBO.getId(), profileId));

        entityDO = deviceBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!deviceManager.updateById(entityDO)) {
            throw new UpdateException("The device update failed");
        }

        DeviceBO deviceBO = selectById(entityBO.getId());
        deviceBO.setProfileIds(CollUtil.isEmpty(newProfileIds) ? oldProfileIds : newProfileIds);
        entityBO.setDeviceName(deviceBO.getDeviceName());
        // 通知驱动更新设备
        MetadataEvent<DeviceBO> metadataEvent = new MetadataEvent<>(this, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE, deviceBO);
        metadataEventPublisher.publishEvent(metadataEvent);
    }


    @Override
    public DeviceBO selectById(Long id) {
        DeviceDO entityDO = getDOById(id, true);
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        entityBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(id));
        return entityBO;
    }

    @Override
    public DeviceBO selectByName(String name, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, name);
        wrapper.eq(DeviceDO::getTenantId, tenantId);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO entityDO = deviceManager.getOne(wrapper);
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        entityBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(entityDO.getId()));
        return entityBO;
    }

    @Override
    public DeviceBO selectByCode(String code, Long tenantId) {
        LambdaQueryChainWrapper<DeviceDO> wrapper = deviceManager.lambdaQuery()
                .eq(DeviceDO::getDeviceCode, code)
                .eq(DeviceDO::getTenantId, tenantId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO entityDO = wrapper.one();
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);
        entityBO.setProfileIds(profileBindService.selectProfileIdsByDeviceId(entityDO.getId()));
        return entityBO;
    }

    @Override
    public List<DeviceBO> selectByDriverId(Long driverId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDriverId, driverId);
        List<DeviceDO> entityDOList = deviceManager.list(wrapper);
        List<DeviceBO> deviceBOList = deviceBuilder.buildBOListByDOList(entityDOList);
        deviceBOList.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return deviceBOList;
    }

    @Override
    public List<Long> selectIdsByDriverId(Long driverId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDriverId, driverId).select(DeviceDO::getId);
        return deviceManager.list(wrapper).stream().map(DeviceDO::getId).toList();
    }

    @Override
    public List<DeviceBO> selectByProfileId(Long profileId) {
        return selectByIds(profileBindService.selectDeviceIdsByProfileId(profileId));
    }

    @Override
    public List<DeviceBO> selectByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<DeviceDO> entityDOList = deviceManager.listByIds(ids);
        List<DeviceBO> deviceBOList = deviceBuilder.buildBOListByDOList(entityDOList);
        deviceBOList.forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return deviceBOList;
    }

    @Override
    public Page<DeviceBO> selectByPage(DeviceQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DeviceDO> entityPageDO = deviceMapper.selectPageWithProfile(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery), entityQuery.getProfileId());
        Page<DeviceBO> entityPageBO = deviceBuilder.buildBOPageByDOPage(entityPageDO);
        entityPageBO.getRecords().forEach(device -> device.setProfileIds(profileBindService.selectProfileIdsByDeviceId(device.getId())));
        return entityPageBO;
    }

    @Override
    @SneakyThrows
    @Transactional
    public void importDevice(DeviceBO entityBO, MultipartFile multipartFile) {
        List<DriverAttributeBO> driverAttributeBOList = driverAttributeService.selectByDriverId(entityBO.getDriverId());
        List<PointAttributeBO> pointAttributeBOList = pointAttributeService.selectByDriverId(entityBO.getDriverId());
        List<PointBO> pointBOList = pointService.selectByProfileIds(entityBO.getProfileIds());

        Workbook workbook = new XSSFWorkbook(multipartFile.getInputStream());
        Sheet mainSheet = workbook.getSheet("设备导入");

        if (!configIsEqual(driverAttributeBOList, pointAttributeBOList, pointBOList, workbook)) {
            throw new ImportException("The import template is formatted incorrectly");
        }

        for (int i = 4; i <= mainSheet.getLastRowNum(); i++) {
            // 导入设备
            DeviceBO importDeviceBO = importDevice(entityBO, mainSheet, i);
            log.info("Importing device: {}, index: {}", importDeviceBO.getDeviceName(), 1);

            // 导入驱动属性配置
            importDriverAttributeConfig(importDeviceBO, driverAttributeBOList, mainSheet, i);

            // 导入位号属性配置
            importPointAttributeConfig(driverAttributeBOList, pointAttributeBOList, pointBOList, mainSheet, i, importDeviceBO);
        }
    }

    @Override
    @SneakyThrows
    public Path generateImportTemplate(DeviceBO entityBO) {
        List<DriverAttributeBO> driverAttributeBOList = driverAttributeService.selectByDriverId(entityBO.getDriverId());
        List<PointAttributeBO> pointAttributeBOList = pointAttributeService.selectByDriverId(entityBO.getDriverId());
        List<PointBO> pointBOList = pointService.selectByProfileIds(entityBO.getProfileIds());

        Workbook workbook = new XSSFWorkbook();
        CellStyle cellStyle = PoiUtil.getCenterCellStyle(workbook);

        // 设置主工作表
        Sheet mainSheet = workbook.createSheet("设备导入");
        mainSheet.setDefaultColumnWidth(25);

        // 设置配置工作表
        configConfigSheet(driverAttributeBOList, pointAttributeBOList, pointBOList, workbook);

        // 设置说明
        Row remarkRow = mainSheet.createRow(0);
        PoiUtil.createCell(remarkRow, 0, "说明: 请从第5行开始添加待导入的设备数据");
        PoiUtil.mergedRegion(mainSheet, 0, 0, 0, 2 + driverAttributeBOList.size() + pointAttributeBOList.size() * pointBOList.size() - 1);

        // 设置设备列
        Row titleRow = mainSheet.createRow(1);
        PoiUtil.createCellWithStyle(titleRow, 0, "设备名称", cellStyle);
        PoiUtil.createCellWithStyle(titleRow, 1, "设备描述", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 0, 0);
        PoiUtil.mergedRegion(mainSheet, 1, 3, 1, 1);

        Row attributeRow = mainSheet.createRow(3);
        // 设置驱动属性配置列
        configAttributeCell(driverAttributeBOList, mainSheet, titleRow, attributeRow, cellStyle);
        // 设置位号属性配置列
        configPointCell(driverAttributeBOList, pointAttributeBOList, pointBOList, mainSheet, titleRow, attributeRow, cellStyle);

        // 生成设备导入模版
        return generateTemplate(workbook);
    }

    private LambdaQueryWrapper<DeviceDO> fuzzyQuery(DeviceQuery entityQuery) {
        QueryWrapper<DeviceDO> wrapper = Wrappers.query();
        wrapper.eq("dd.deleted", 0);
        if (!Objects.isNull(entityQuery)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getDeviceName()), "dd.device_name", entityQuery.getDeviceName());
            wrapper.eq(CharSequenceUtil.isNotEmpty(entityQuery.getDeviceCode()), "dd.device_code", entityQuery.getDeviceCode());
            wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), "dd.driver_id", entityQuery.getDriverId());
            wrapper.eq(!Objects.isNull(entityQuery.getEnableFlag()), "dd.enable_flag", entityQuery.getEnableFlag());
            wrapper.eq("dd.tenant_id", entityQuery.getTenantId());
        }
        return wrapper.lambda();
    }

    /**
     * 导入设备
     *
     * @param deviceBO  Device
     * @param mainSheet Sheet
     * @param rowIndex  Row Index
     * @return DeviceBO
     */
    private DeviceBO importDevice(DeviceBO deviceBO, Sheet mainSheet, int rowIndex) {
        DeviceBO importDeviceBO = getDevice(deviceBO, mainSheet, rowIndex);
        try {
            save(importDeviceBO);
        } catch (Exception e) {
            log.error("导入设备: {}, 错误: {}", deviceBO, rowIndex);
            throw new ServiceException(e.getMessage());
        }
        return importDeviceBO;
    }

    /**
     * @param importDeviceBO        Device
     * @param driverAttributeBOList DriverAttributeBO Array
     * @param mainSheet             Sheet
     * @param rowIndex              Row Index
     */
    private void importDriverAttributeConfig(DeviceBO importDeviceBO, List<DriverAttributeBO> driverAttributeBOList, Sheet mainSheet, int rowIndex) {
        for (int j = 0; j < driverAttributeBOList.size(); j++) {
            DriverAttributeConfigBO importAttributeConfig = getDriverAttributeConfig(importDeviceBO, driverAttributeBOList.get(j), mainSheet, rowIndex, 2 + j);
            driverAttributeConfigService.save(importAttributeConfig);
        }
    }

    private DeviceBO getDevice(DeviceBO deviceBO, Sheet mainSheet, int rowIndex) {
        String deviceName = PoiUtil.getCellStringValue(mainSheet, rowIndex, 0);
        if (CharSequenceUtil.isEmpty(deviceName)) {
            throw new ImportException("The device name in line {} of the import file is empty", rowIndex + 1);
        }

        DeviceBO importDeviceBO = new DeviceBO();
        importDeviceBO.setDeviceName(deviceName);
        importDeviceBO.setDriverId(deviceBO.getDriverId());
        importDeviceBO.setProfileIds(deviceBO.getProfileIds());
        String deviceRemark = PoiUtil.getCellStringValue(mainSheet, rowIndex, 1);
        importDeviceBO.setRemark(deviceRemark);
        importDeviceBO.setTenantId(deviceBO.getTenantId());

        return importDeviceBO;
    }

    private DriverAttributeConfigBO getDriverAttributeConfig(DeviceBO deviceBO, DriverAttributeBO driverAttributeBO, Sheet mainSheet, int rowIndex, int cellIndex) {
        DriverAttributeConfigBO importAttributeConfig = new DriverAttributeConfigBO();
        importAttributeConfig.setDriverAttributeId(driverAttributeBO.getId());
        importAttributeConfig.setDeviceId(deviceBO.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(deviceBO.getTenantId());

        return importAttributeConfig;
    }

    private void importPointAttributeConfig(List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList, Sheet mainSheet, int i, DeviceBO importDeviceBO) {
        for (int j = 0; j < pointBOList.size(); j++) {
            for (int k = 0; k < pointAttributeBOList.size(); k++) {
                PointAttributeConfigBO importAttributeConfig = getPointAttributeConfig(importDeviceBO, pointBOList.get(j), pointAttributeBOList.get(k), mainSheet, i, 2 + driverAttributeBOList.size() + k * pointAttributeBOList.size() + j);
                pointAttributeConfigService.save(importAttributeConfig);
            }
        }
    }

    private PointAttributeConfigBO getPointAttributeConfig(DeviceBO deviceBO, PointBO pointBO, PointAttributeBO pointAttributeBO, Sheet mainSheet, int rowIndex, int cellIndex) {
        PointAttributeConfigBO importAttributeConfig = new PointAttributeConfigBO();
        importAttributeConfig.setPointAttributeId(pointAttributeBO.getId());
        importAttributeConfig.setDeviceId(deviceBO.getId());
        importAttributeConfig.setPointId(pointBO.getId());
        String attributeValue = PoiUtil.getCellStringValue(mainSheet, rowIndex, cellIndex);
        importAttributeConfig.setConfigValue(attributeValue);
        importAttributeConfig.setTenantId(deviceBO.getTenantId());

        return importAttributeConfig;
    }

    /**
     * 判断配置数据是否一致
     *
     * @param driverAttributeBOList 驱动属性Array
     * @param pointAttributeBOList  位号属性Array
     * @param pointBOList           Point 集合
     * @param workbook              Workbook
     */
    private boolean configIsEqual(List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList, Workbook workbook) {
        Sheet configSheet = workbook.getSheet("配置(忽略)");
        String driverAttributesValueNew = JsonUtil.toJsonString(driverAttributeBOList);
        String driverAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 0, 0);
        if (!driverAttributesValueNew.equals(driverAttributesValueOld)) {
            return false;
        }

        String pointAttributesValueNewd = JsonUtil.toJsonString(pointAttributeBOList);
        String pointAttributesValueOld = PoiUtil.getCellStringValue(configSheet, 1, 0);
        if (!pointAttributesValueNewd.equals(pointAttributesValueOld)) {
            return false;
        }

        String pointsValueNew = JsonUtil.toJsonString(pointBOList);
        String pointsValueOld = PoiUtil.getCellStringValue(configSheet, 2, 0);

        return pointsValueNew.equals(pointsValueOld);
    }

    /**
     * 设置驱动属性配置列
     *
     * @param driverAttributeBOList 驱动属性Array
     * @param mainSheet             Main Sheet
     * @param titleRow              Title Row
     * @param attributeRow          Attribute Row
     */
    private void configAttributeCell(List<DriverAttributeBO> driverAttributeBOList, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (driverAttributeBOList.isEmpty()) {
            return;
        }

        PoiUtil.createCellWithStyle(titleRow, 2, "驱动属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 2, 2, 2 + driverAttributeBOList.size() - 1);
        for (int i = 0; i < driverAttributeBOList.size(); i++) {
            PoiUtil.createCellWithStyle(attributeRow, 2 + i, driverAttributeBOList.get(i).getDisplayName(), cellStyle);
        }

    }

    /**
     * 设置配置工作表
     *
     * @param driverAttributeBOList 驱动属性Array
     * @param pointAttributeBOList  位号属性Array
     * @param pointBOList           Point Array
     * @param workbook              Workbook
     */
    private void configConfigSheet(List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList, Workbook workbook) {
        Sheet configSheet = workbook.createSheet("配置(忽略)");
        Row driverAttributesRow = configSheet.createRow(0);
        Row pointAttributesRow = configSheet.createRow(1);
        Row pointsRow = configSheet.createRow(2);
        PoiUtil.createCell(driverAttributesRow, 0, JsonUtil.toJsonString(driverAttributeBOList));
        PoiUtil.createCell(pointAttributesRow, 0, JsonUtil.toJsonString(pointAttributeBOList));
        PoiUtil.createCell(pointsRow, 0, JsonUtil.toJsonString(pointBOList));
    }

    /**
     * 设置位号属性配置列
     *
     * @param driverAttributeBOList 驱动属性Array
     * @param pointAttributeBOList  位号属性Array
     * @param pointBOList           Point  Array
     * @param mainSheet             Main Sheet
     * @param titleRow              Title Row
     * @param attributeRow          Attribute Row
     * @param cellStyle             CellStyle
     */
    private void configPointCell(List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, List<PointBO> pointBOList, Sheet mainSheet, Row titleRow, Row attributeRow, CellStyle cellStyle) {
        if (pointAttributeBOList.isEmpty()) {
            return;
        }

        Row pointRow = mainSheet.createRow(2);
        PoiUtil.createCellWithStyle(titleRow, 2 + driverAttributeBOList.size(), "位号属性配置", cellStyle);
        PoiUtil.mergedRegion(mainSheet, 1, 1, 2 + driverAttributeBOList.size(), 2 + driverAttributeBOList.size() + pointAttributeBOList.size() * pointBOList.size() - 1);
        for (int i = 0; i < pointBOList.size(); i++) {
            PoiUtil.createCellWithStyle(pointRow, 2 + driverAttributeBOList.size() + i * pointAttributeBOList.size(), pointBOList.get(i).getPointName(), cellStyle);
            PoiUtil.mergedRegion(mainSheet, 2, 2, 2 + driverAttributeBOList.size() + i * pointAttributeBOList.size(), 2 + driverAttributeBOList.size() + i * pointAttributeBOList.size() + pointAttributeBOList.size() - 1);
            for (int j = 0; j < pointAttributeBOList.size(); j++) {
                PoiUtil.createCellWithStyle(attributeRow, 2 + driverAttributeBOList.size() + i * pointAttributeBOList.size() + j, pointAttributeBOList.get(j).getDisplayName(), cellStyle);
            }
        }
    }

    /**
     * 生成设备导入模版
     *
     * @param workbook Workbook
     * @return Path
     * @throws IOException
     */
    private Path generateTemplate(Workbook workbook) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        org.springframework.core.io.Resource resource = resolver.getResource("classpath:/");
        Path resourcePath = Paths.get(resource.getURI());
        String fileName = CharSequenceUtil.format("dc3_device_import_template_{}.xlsx", System.currentTimeMillis());
        Path path = resourcePath.resolve(fileName);
        FileOutputStream outputStream = new FileOutputStream(path.toUri().getPath());
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        return path;
    }

    private void addProfileBind(DeviceDO entityDO, List<Long> profileIds) {
        if (CollUtil.isEmpty(profileIds)) {
            return;
        }

        profileIds.forEach(profileId -> {
            try {
                ProfileBindBO entityBO = new ProfileBindBO();
                entityBO.setProfileId(profileId);
                entityBO.setDeviceId(entityDO.getId());
                entityBO.setTenantId(entityDO.getTenantId());
                profileBindService.save(entityBO);

                List<PointBO> pointBOList = pointService.selectByProfileId(profileId);
                // 通知驱动新增位号
                pointBOList.forEach(pointBO -> {
                    MetadataEvent<PointBO> metadataEvent = new MetadataEvent<>(this, MetadataTypeEnum.POINT, MetadataOperateTypeEnum.ADD, pointBO);
                    metadataEventPublisher.publishEvent(metadataEvent);
                });
            } catch (Exception ignored) {
                // nothing to do
            }
        });

    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link DeviceBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(DeviceBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda();
        wrapper.eq(DeviceDO::getDeviceName, entityBO.getDeviceName());
        wrapper.eq(DeviceDO::getDeviceCode, entityBO.getDeviceCode());
        wrapper.eq(DeviceDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DeviceDO one = deviceManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Device has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link DeviceDO}
     */
    private DeviceDO getDOById(Long id, boolean throwException) {
        DeviceDO entityDO = deviceManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Device does not exist");
        }
        return entityDO;
    }

}
