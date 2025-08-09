package com.pot.im.service.utils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.pot.common.utils.CodeGenerator;

/**
 * @author: Pot
 * @created: 2025/8/9 23:01
 * @description: 代码生成器
 */
public class CodeGeneratorUtils {
    public static void main(String[] args) {

        CodeGenerator.create()
                .mysql("localhost", 3306, "im")
                .auth("root", "000802")
                .projectPath("/Users/yecao/Project/Pot/Zing/zing/im/im-service")
                .basePackage("com.pot.im.service")  // 直接使用完整包名
                .author("Pot")
                .idType(IdType.AUTO)
                .tablePrefix("im_")
                .enableLombok(true)
                .generate();
    }
}
