package com.pot.member.facade.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Pot
 * @created: 2025/10/17 23:16
 * @description: 创建用户响应类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMemberResponse {
    private Long memberId;
}
