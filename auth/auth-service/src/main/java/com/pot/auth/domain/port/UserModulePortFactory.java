package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户模块端口工厂（防腐层核心⭐⭐⭐）
 *
 * <p>根据UserDomain动态获取对应的适配器
 * <p>支持多用户域扩展：
 * <ul>
 *   <li>MEMBER → MemberModuleAdapter</li>
 *   <li>ADMIN → AdminModuleAdapter</li>
 *   <li>MERCHANT → MerchantModuleAdapter（未来）</li>
 * </ul>
 *
 * <p>扩展新用户域：
 * <ol>
 *   <li>在UserDomain枚举添加新域</li>
 *   <li>实现UserModulePort接口（如MerchantModuleAdapter）</li>
 *   <li>Spring自动注册，无需修改此类</li>
 * </ol>
 *
 * @author pot
 * @since 1.0.0
 */
@Component
public class UserModulePortFactory {

    private final Map<UserDomain, UserModulePort> adapters;

    /**
     * 构造函数 - Spring自动注入所有UserModulePort实现
     */
    public UserModulePortFactory(List<UserModulePort> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(
                        UserModulePort::supportedDomain,
                        adapter -> adapter
                ));
    }

    /**
     * 获取指定域的适配器
     *
     * @param domain 用户域
     * @return 对应的适配器
     * @throws UnsupportedUserDomainException 如果域不支持
     */
    public UserModulePort getPort(UserDomain domain) {
        UserModulePort adapter = adapters.get(domain);
        if (adapter == null) {
            throw new UnsupportedUserDomainException("不支持的用户域: " + domain);
        }
        return adapter;
    }

    /**
     * 检查是否支持指定域
     */
    public boolean supports(UserDomain domain) {
        return adapters.containsKey(domain);
    }

    /**
     * 获取所有支持的域
     */
    public Set<UserDomain> getSupportedDomains() {
        return adapters.keySet();
    }

    /**
     * 不支持的用户域异常
     */
    public static class UnsupportedUserDomainException extends RuntimeException {
        public UnsupportedUserDomainException(String message) {
            super(message);
        }
    }
}

