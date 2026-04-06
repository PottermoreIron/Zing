package com.pot.auth.domain.port;

import com.pot.auth.domain.shared.valueobject.UserDomain;
import com.pot.auth.domain.shared.valueobject.UserId;

import java.util.Set;

public interface PermissionQueryPort {

        Set<String> getCachedPermissions(UserId userId, UserDomain userDomain);
}