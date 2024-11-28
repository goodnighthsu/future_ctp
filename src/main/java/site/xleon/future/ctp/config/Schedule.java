package site.xleon.future.ctp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.CtpMasterClient;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MdService;
import site.xleon.future.ctp.services.impl.TradeService;
import java.io.IOException;

@Configuration
@EnableAsync
@Slf4j
public class Schedule {
    @Autowired
    private AppConfig config;
    @Autowired
    private TradeService tradeService;
    @Autowired
    private MdService mdService;
    @Autowired
    private DataService dataService;

    @Autowired
    private CtpMasterClient marketClient;

    /**
     * 交易自动登录
     */
//    @Async
//    @Scheduled(cron = "0 50 8,20 * * MON-FRI")
//    public void autoTradeLogin() {
//        try {
//            log.info("交易自动登录");
//            String userId = tradeService.login(config.getUser());
//            log.info("交易自动登录成功, 用户: {}", userId);
//        } catch (Exception e) {
//            log.error("交易自动登录失败: {}", e.getMessage());
//            try {
//                Thread.sleep(60 * 1000 * 5);
//            } catch (InterruptedException interruptedException) {
//                interruptedException.printStackTrace();
//            }
//            autoTradeLogin();
//        }
//    }
//
//    /**
//     * 行情自动登录
//     */
//    @Async
//    @Scheduled(cron = "0 55 8,20 * * MON-FRI")
//    public void autoMarketLogin() {
//        try {
//            log.info("行情自动登录");
//            mdService.login();
//            log.info("行情自动登录成功, 交易日: {}", ctpInfo.getTradingDay());
//        } catch (Exception e) {
//            log.error("行情自动登录失败: ", e);
//            try {
//                Thread.sleep(6000);
//            } catch (InterruptedException interruptedException) {
//                interruptedException.printStackTrace();
//            }
//            autoMarketLogin();
//        }
//    }
//
//    /**
//     * 自动压缩
//     * 压缩非交易日的行情文件到data目录
//     */
//    @Async
//    @Scheduled(cron = "0 0 5 * * ?")
//    public void autoCompress () throws IOException {
//        if (!config.getSchedule().getMarketDataAutoCompress()) {
//            log.info("自动压缩跳过");
//            return;
//        }
//        log.info("自动压缩");
//        dataService.compress();
//        log.info("自动压缩完成");
//    }
//
//    /**
//     * 下载备份的市场行情文件
//     * @apiNote 从主服务器下载压缩的行情文件到backup文件夹，并解压缩到history文件夹，
//     * 解压成功后删除主服务上的压缩文件
//     * @throws MyException
//     * @throws IOException
//     */
//    @Async
//    @Scheduled(cron = "0 0 7 * * ?")
//    public void autoDownload () throws MyException, IOException {
//        if (!config.getSchedule().getDownloadCtpData()) {
//            log.info("行情文件下载跳过");
//            return;
//        }
//       mdService.download();
//    }
}