package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.UserDomain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserModulePortFactory {

    private final Map<UserDomain, UserModulePort> adapters;

        public UserModulePortFactory(List<UserModulePort> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(
                        UserModulePort::supportedDomain,
                        adapter -> adapter));
    }

        public UserModulePort getPort(UserDomain domain) {
        UserModulePort adapter = adapters.get(domain);
        if (adapter == null) {
            throw new UnsupportedUserDomainException("Unsupported user domain: " + domain);
        }
        return adapter;
    }

        public boolean supports(UserDomain domain) {
        return adapters.containsKey(domain);
    }

        public Set<UserDomain> getSupportedDomains() {
        return adapters.keySet();
    }

        public static class UnsupportedUserDomainException extends RuntimeException {
        public UnsupportedUserDomainException(String message) {
            super(message);
        }
    }
}
