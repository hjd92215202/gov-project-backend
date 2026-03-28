package com.gov;

import com.gov.common.result.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 职责：提供后端测试体系的最小烟雾测试入口。
 * 为什么存在：确保在不依赖数据库、流程引擎和对象存储的前提下，
 * `mvn test` 至少能够稳定跑通最基础的公共结果结构。
 * 关键输入输出：输入为成功结果构造，输出为统一的 `R` 响应对象。
 * 关联链路：所有控制器返回统一响应包装。
 */
class GovApplicationTests {

    /**
     * 作用：验证统一响应对象的成功分支结构没有被改坏。
     * 为什么这样做：如果 `R` 的基础语义变了，前后端所有接口都会受到影响。
     */
    @Test
    void shouldBuildSuccessResponse() {
        R<String> result = R.ok("ok", "成功");

        assertNotNull(result);
        assertEquals(Integer.valueOf(200), result.getCode());
        assertEquals("成功", result.getMsg());
        assertEquals("ok", result.getData());
    }
}
