package com.gov;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.SQLException;

@SpringBootTest
public class GovApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void testDbConnection() throws SQLException {
        System.out.println("============= 单元测试启动 =============");
        System.out.println("获取到的数据库连接为：");
        System.out.println(dataSource.getConnection());
        System.out.println("======================================");

        // 如果上面没报错，说明 Spring 容器和数据库全部正常！
    }
}