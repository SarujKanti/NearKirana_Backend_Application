package com.nearkirana.backend.sms;

import com.nearkirana.backend.util.MobileNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DEVELOPMENT ONLY: "sends" the OTP by printing it in the console.
 *
 * @Profile("dev") means Spring creates this bean ONLY when the "dev" profile
 * is active. In production (no "dev" profile) this class does not exist, and
 * the app will not even start until a real SMS provider is configured.
 */
@Component
@Profile("dev")
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void sendOtp(String mobileNumber, String otp) {
        log.warn("[DEV SMS - NOT SENT] OTP for {} is {} (visible only in the dev profile)",
                MobileNumbers.mask(mobileNumber), otp);
    }
}
