package pro.ax.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.ax.platform.service.BillingService;

/** Раз в час снимает истёкшие платные тарифы. */
@Component
public class PlanExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(PlanExpiryJob.class);

    private final BillingService billingService;

    public PlanExpiryJob(BillingService billingService) {
        this.billingService = billingService;
    }

    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 60_000L)
    public void expirePlans() {
        int expired = billingService.expirePromotions();
        if (expired > 0) {
            log.info("Тарифы сняты у {} компаний", expired);
        }
    }
}
