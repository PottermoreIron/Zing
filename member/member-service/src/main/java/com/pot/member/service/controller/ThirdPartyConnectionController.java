package com.pot.member.service.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 第三方平台连接表 前端控制器
 * </p>
 *
 * @author pot
 * @since 2025-04-06
 */
@RestController
@RequestMapping("/thirdPartyConnection")
@Tag(
        name = "第三方平台连接相关接口",
        description = "提供第三方平台连接的管理和查询功能"
)
public class ThirdPartyConnectionController {

}
