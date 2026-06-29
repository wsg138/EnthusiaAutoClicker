package net.enthusia.autoclicker.server;

import java.util.List;

record ModCheckResult(
    ModCheckStatus status,
    String clientBrand,
    List<String> relevantChannels,
    String detail
) {
}
