package com.gov.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis-Plus 自动填充处理器。
 * 统一在新增和更新时补齐 `createTime / updateTime`，避免每个控制器手动设置。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /** 新增时同时写入创建时间和更新时间。 */
    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "updateTime", Date.class, now);
    }

    /** 更新时刷新更新时间。 */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
