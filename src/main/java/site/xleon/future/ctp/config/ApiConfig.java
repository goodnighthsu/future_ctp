package site.xleon.future.ctp.config;

import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfig {
    @Bean
    public CThostFtdcTraderApi traderApi() {
        return CThostFtdcTraderApi.CreateFtdcTraderApi("trader");
    }

    @Bean
    public CThostFtdcMdApi mdApi() {return CThostFtdcMdApi.CreateFtdcMdApi("market", false, false);}
}
