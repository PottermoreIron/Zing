package com.pot.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtConfig")
class JwtConfigTest {

    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private Resource resource;

    @InjectMocks
    private JwtConfig jwtConfig;

    @Test
    @DisplayName("优先从内联公钥加载")
    void jwtPublicKey_loadsInlinePublicKey() throws Exception {
        KeyPair keyPair = generateKeyPair();
        given(jwtProperties.getPublicKey()).willReturn(toPem(keyPair.getPublic()));

        PublicKey publicKey = jwtConfig.jwtPublicKey();

        assertThat(publicKey.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    @Test
    @DisplayName("从资源流加载公钥，兼容打包后的 classpath 资源")
    void jwtPublicKey_loadsFromResourceStream() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String pem = toPem(keyPair.getPublic());

        given(jwtProperties.getPublicKey()).willReturn(null);
        given(jwtProperties.getPublicKeyLocation()).willReturn("classpath:keys/jwt_public_key.pem");
        given(resourceLoader.getResource("classpath:keys/jwt_public_key.pem")).willReturn(resource);
        given(resource.exists()).willReturn(true);
        given(resource.getInputStream()).willReturn(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));

        PublicKey publicKey = jwtConfig.jwtPublicKey();

        assertThat(publicKey.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    @Test
    @DisplayName("资源不存在时抛出明确配置异常")
    void jwtPublicKey_missingResource_throwsConfigurationException() {
        given(jwtProperties.getPublicKey()).willReturn(null);
        given(jwtProperties.getPublicKeyLocation()).willReturn("classpath:missing.pem");
        given(resourceLoader.getResource("classpath:missing.pem")).willReturn(resource);
        given(resource.exists()).willReturn(false);

        assertThatThrownBy(() -> jwtConfig.jwtPublicKey())
                .isInstanceOf(GatewayConfigurationException.class)
                .hasMessageContaining("无法加载JWT公钥");
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private String toPem(PublicKey publicKey) {
        return "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(publicKey.getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }
}