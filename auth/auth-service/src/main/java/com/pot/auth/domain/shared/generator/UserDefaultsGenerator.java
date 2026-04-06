package com.pot.auth.domain.shared.generator;

import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class UserDefaultsGenerator {

        private static final String SPECIAL_CHARS = "!@#$%^&*";

        private static final String ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

        private static final String ALPHABETIC_CHARS = "abcdefghijklmnopqrstuvwxyz";

        private static final String NUMERIC_CHARS = "0123456789";

        private static final SecureRandom RANDOM = new SecureRandom();

    private final String avatarUrl;
    private final String nicknamePrefix;
    private final int passwordLength;
    private final boolean includeUppercase;
    private final boolean includeLowercase;
    private final boolean includeDigits;
    private final boolean includeSpecial;

    public UserDefaultsGenerator(
            String avatarUrl,
            String nicknamePrefix,
            int passwordLength,
            boolean includeUppercase,
            boolean includeLowercase,
            boolean includeDigits,
            boolean includeSpecial) {
        this.avatarUrl = avatarUrl;
        this.nicknamePrefix = nicknamePrefix;
        this.passwordLength = passwordLength;
        this.includeUppercase = includeUppercase;
        this.includeLowercase = includeLowercase;
        this.includeDigits = includeDigits;
        this.includeSpecial = includeSpecial;

        if (passwordLength < 8) {
            throw new IllegalArgumentException("默认密码长度不能小于8位");
        }
        if (!includeUppercase && !includeLowercase && !includeDigits && !includeSpecial) {
            throw new IllegalArgumentException("默认密码规则至少需要启用一种字符类型");
        }
    }

        public String generateNicknameFromPhone(String phone) {
        long timestamp = System.currentTimeMillis();
        String random = randomAlphanumeric(4).toLowerCase();
        String nickname = nicknamePrefix + timestamp + "_" + random;

        log.debug("[用户默认值生成] 基于手机号生成昵称: phone={}, nickname={}", phone, nickname);
        return nickname;
    }

        public String generateNicknameFromEmail(String email) {
        String prefix = email.substring(0, email.indexOf("@"));
        String random = randomAlphanumeric(4).toLowerCase();
        String nickname = prefix + "_" + random;

        log.debug("[用户默认值生成] 基于邮箱生成昵称: email={}, nickname={}", email, nickname);
        return nickname;
    }

        public String generateNickname() {
        long timestamp = System.currentTimeMillis();
        String random = randomAlphanumeric(6).toLowerCase();
        String nickname = nicknamePrefix + timestamp + "_" + random;

        log.debug("[用户默认值生成] 生成通用昵称: nickname={}", nickname);
        return nickname;
    }

        public String generateRandomPassword() {
        List<Character> passwordChars = new ArrayList<>();
        StringBuilder candidatePool = new StringBuilder();

        if (includeUppercase) {
            passwordChars.add(randomAlphabetic(1).toUpperCase().charAt(0));
            candidatePool.append(ALPHABETIC_CHARS.toUpperCase());
        }
        if (includeLowercase) {
            passwordChars.add(randomAlphabetic(1).toLowerCase().charAt(0));
            candidatePool.append(ALPHABETIC_CHARS);
        }
        if (includeDigits) {
            passwordChars.add(randomNumeric(1).charAt(0));
            candidatePool.append(NUMERIC_CHARS);
        }
        if (includeSpecial) {
            passwordChars.add(SPECIAL_CHARS.charAt(ThreadLocalRandom.current().nextInt(SPECIAL_CHARS.length())));
            candidatePool.append(SPECIAL_CHARS);
        }

        while (passwordChars.size() < passwordLength) {
            passwordChars.add(candidatePool.charAt(RANDOM.nextInt(candidatePool.length())));
        }

        String password = shuffleCharacters(passwordChars);

        log.debug("[用户默认值生成] 生成随机密码: length={}", password.length());
        return password;
    }

        public String getDefaultAvatarUrl() {
        return avatarUrl;
    }

        private String shuffleCharacters(List<Character> input) {
        Collections.shuffle(input, RANDOM);

        StringBuilder result = new StringBuilder();
        for (char c : input) {
            result.append(c);
        }
        return result.toString();
    }

        private String randomAlphanumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC_CHARS.charAt(RANDOM.nextInt(ALPHANUMERIC_CHARS.length())));
        }
        return sb.toString();
    }

        private String randomAlphabetic(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABETIC_CHARS.charAt(RANDOM.nextInt(ALPHABETIC_CHARS.length())));
        }
        return sb.toString();
    }

        private String randomNumeric(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(NUMERIC_CHARS.charAt(RANDOM.nextInt(NUMERIC_CHARS.length())));
        }
        return sb.toString();
    }
}
