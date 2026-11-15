package com.nearkirana.backend.sms;

import com.nearkirana.backend.util.MobileNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DEV / DEMO ONLY: "sends" the OTP by printing it in the console (or Render logs).
 *
 * @Profile({"dev", "demo"}) means Spring creates this bean ONLY when the "dev"
 * or "demo" profile is active. In real production neither is active, so this
 * class does not exist, and
 * the app will not even start until a real SMS provider is configured.
 */
@Component
@Profile({"dev", "demo"})
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void sendOtp(String mobileNumber, String otp) {
        log.warn("[DEV SMS - NOT SENT] OTP for {} is {} (visible only in the dev profile)",
                MobileNumbers.mask(mobileNumber), otp);
    }
}
