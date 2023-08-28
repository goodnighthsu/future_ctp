package site.xleon.future.ctp;

import ctp.thostmduserapi.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class CtpApplication {

    static {
        System.loadLibrary("thosttraderapi_se");
        System.loadLibrary("thosttraderapi_wrap");
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("thostmduserapi_wrap");
    }

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(CtpApplication.class, args);
        log.info("application start");
        log.info("library path: {}", System.getProperty("java.library.path"));
        String version = CThostFtdcMdApi.GetApiVersion();
        log.info("version: {}", version);
        // 创建nacos密码
//        log.info("nacos: {}", new BCryptPasswordEncoder().encode("nacos@leonx.site"));
    }
}
