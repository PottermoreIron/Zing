package com.pot.user.service.controller;

import com.pot.common.R;
import com.pot.user.service.service.wechat.WechatMpService;
import com.pot.user.service.utils.RandomStringGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

/**
 * @author: Pot
 * @created: 2025/4/12 23:24
 * @description: 微信controller refer to: <a href="https://github.com/binarywang/weixin-java-mp-demo/tree/master">...</a>
 */
@RestController
@RequestMapping("/api/wechat/{appid}")
@RequiredArgsConstructor
@Slf4j
public class WechatController {
    private final WxMpService wxService;
    private final WxMpMessageRouter messageRouter;
    private final WechatMpService wechatMpService;
    @Value("${wechat.mp.callbackUrl}")
    private String callbackUrl;


    /**
     * WeChat server verification endpoint
     */
    @GetMapping(produces = "text/plain;charset=utf-8")
    public String verifyServer(@PathVariable String appid,
                               @RequestParam(required = false) String signature,
                               @RequestParam(required = false) String timestamp,
                               @RequestParam(required = false) String nonce,
                               @RequestParam(required = false) String echostr) {
        log.info("Received WeChat server verification: signature=[{}], timestamp=[{}], nonce=[{}], echostr=[{}]",
                signature, timestamp, nonce, echostr);

        validateParameters(signature, timestamp, nonce, echostr);
        switchoverAppId(appid);

        return wxService.checkSignature(timestamp, nonce, signature) ? echostr : "Invalid request";
    }

    /**
     * Generate QR code URL for WeChat public account
     */
    @GetMapping("/get/qrCodeUrl")
    public R<String> getQrCodeUrl(@PathVariable String appid) {
        log.info("Generating QR code URL for public account");
        switchoverAppId(appid);

        try {
            String randomString = RandomStringGenerator.generateRandomString(20);
            String ticket = wxService.getQrcodeService().qrCodeCreateTmpTicket(randomString, 10 * 60).getTicket();
            String qrCodeUrl = wxService.getQrcodeService().qrCodePictureUrl(ticket);
            return R.success(qrCodeUrl);
        } catch (Exception e) {
            log.error("Failed to generate QR code URL", e);
            return R.fail("Failed to generate QR code");
        }
    }

    /**
     * Handle WeChat message events
     */
    @PostMapping(produces = "application/xml; charset=UTF-8")
    public String handleMessage(@PathVariable String appid,
                                @RequestBody String requestBody,
                                @RequestParam String signature,
                                @RequestParam String timestamp,
                                @RequestParam String nonce,
                                @RequestParam String openid,
                                @RequestParam(name = "encrypt_type", required = false) String encType,
                                @RequestParam(name = "msg_signature", required = false) String msgSignature) {
        log.info("Received WeChat request: openid=[{}], signature=[{}], encType=[{}], timestamp=[{}], nonce=[{}]",
                openid, signature, encType, timestamp, nonce);
        log.debug("Request body: {}", requestBody);

        switchoverAppId(appid);
        validateSignature(timestamp, nonce, signature);

        return processMessage(requestBody, encType, timestamp, nonce, msgSignature);
    }

    /**
     * Get OAuth2 authorization URL
     */
    @GetMapping("/get/authorizationUrl")
    public R<String> getAuthorizationUrl(@PathVariable String appid) {
        log.info("appid: {}", appid);
        switchoverAppId(appid);

        String redirectUrl = callbackUrl + "/api/wechat/" + appid + "/weChatUserInfo";
        String authUrl = wxService.getOAuth2Service()
                .buildAuthorizationUrl(redirectUrl, "snsapi_userinfo", "STATE");

        return R.success(authUrl);
    }

    /**
     * Handle WeChat OAuth2 callback and user info retrieval
     */
    @GetMapping("/get/weChatUserInfo")
    public R<String> handleOAuthCallback(@PathVariable String appid, @RequestParam String code) {
        log.info("Received WeChat OAuth2 callback: appid=[{}], code=[{}]", appid, code);
        switchoverAppId(appid);

        try {
            WxOAuth2AccessToken accessToken = wxService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxService.getOAuth2Service().getUserInfo(accessToken, null);

            wechatMpService.authorize(userInfo);
            log.info("User info updated successfully");

            return R.success("Authorization successful");
        } catch (Exception e) {
            log.error("Authorization failed", e);
            return R.fail("Authorization failed");
        }
    }

    // Helper methods for cleaner code organization
    private void validateParameters(String... params) {
        if (Stream.of(params).anyMatch(StringUtils::isBlank)) {
            throw new IllegalArgumentException("Invalid request parameters");
        }
    }

    private void switchoverAppId(String appid) {
        if (!wxService.switchover(appid)) {
            throw new IllegalArgumentException(
                    String.format("Configuration not found for appid=[%s]", appid));
        }
    }

    private void validateSignature(String timestamp, String nonce, String signature) {
        if (!wxService.checkSignature(timestamp, nonce, signature)) {
            throw new IllegalArgumentException("Invalid signature, possible forged request");
        }
    }

    private String processMessage(String requestBody, String encType, String timestamp,
                                  String nonce, String msgSignature) {
        WxMpXmlMessage inMessage;

        if (encType == null) {
            // Plaintext message
            inMessage = WxMpXmlMessage.fromXml(requestBody);
        } else if ("aes".equalsIgnoreCase(encType)) {
            // AES encrypted message
            inMessage = WxMpXmlMessage.fromEncryptedXml(
                    requestBody, wxService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            log.debug("Decrypted message content: {}", inMessage);
        } else {
            throw new IllegalArgumentException("Unsupported encryption type: " + encType);
        }

        WxMpXmlOutMessage outMessage = routeMessage(inMessage);
        if (outMessage == null) {
            return "";
        }

        String responseXml = encType == null ?
                outMessage.toXml() :
                outMessage.toEncryptedXml(wxService.getWxMpConfigStorage());

        log.debug("Response message: {}", responseXml);
        return responseXml;
    }

    private WxMpXmlOutMessage routeMessage(WxMpXmlMessage message) {
        try {
            return messageRouter.route(message);
        } catch (Exception e) {
            log.error("Error routing message", e);
            return null;
        }
    }
}
