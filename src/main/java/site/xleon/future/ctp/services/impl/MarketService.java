package site.xleon.future.ctp.services.impl;

import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thostmduserapi.CThostFtdcReqUserLoginField;
import ctp.thostmduserapi.CThostFtdcUserLogoutField;
import feign.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.models.Result;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.Ctp;
import site.xleon.future.ctp.services.CtpMasterClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Service("marketService")
@Slf4j
public class MarketService {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Autowired
    private DataService dataService;

    @Autowired
    private CtpMasterClient marketClient;

    /**
     * 前置是否连接
     */
    private Boolean isConnected = false;

    /**
     * 前置是否登录
     */
    private Boolean isLogin = false;


    /**
     * 订阅的合约
     */
    private List<String> subscribeInstruments;

    /**
     * 登录
     * @return trading day
     */
    public String login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        if (!getIsConnected()) {
            throw new MyException("ctp 前置未连接");
        }
        if (getIsLogin()) {
            log.info("market已登录: {}", ctpInfo.getTradingDay());
            return ctpInfo.getTradingDay();
        }
        Ctp<String> ctp = new Ctp<>();
        ctp.setId(0);
        String tradingDay = ctp.request(requestId -> {
            CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
            UserConfig user = appConfig.getUser();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            field.setPassword(user.getPassword());
            return mdApi.ReqUserLogin(field, requestId);
        });
        ctpInfo.setTradingDay(tradingDay);
        setIsLogin(true);
        synchronized (CtpInfo.loginLock) {
            CtpInfo.loginLock.notifyAll();
            log.info("行情登录成功通知");
        }
        return tradingDay;
    }

    /**
     * 登出
     * @return userId
     */
    public String logout() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        if (!getIsLogin()) {
            return "logout success";
        }
        Ctp<String> ctp = new Ctp<>();
        ctp.setId(0);
        String userId = ctp.request(requestId -> {
            CThostFtdcUserLogoutField field = new CThostFtdcUserLogoutField();
            UserConfig user = appConfig.getUser();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            return mdApi.ReqUserLogout(field, requestId);
        });
        isLogin = false;
        return userId;
    }



    /**
     * ctp 订阅合约
     */
    public void subscribe(List<String> instruments) {
        log.info("行情订阅开始");
        if (instruments == null || instruments.isEmpty()) {
            log.warn("行情订阅: 没有订阅合约，跳过订阅");
            return;
        }
        // 订阅
        instruments = instruments.stream().distinct().collect(Collectors.toList());
        String[] ids= instruments.toArray(new String[0]);
        if (appConfig.getSchedule().getSubscribe()) {
            int code = mdApi.SubscribeMarketData(ids, ids.length);
            log.info("行情订阅 {} 条, code: {}", ids.length, code);
        } else {
            log.info("行情订阅 {} 条, 订阅关闭", ids.length);
        }

        if (!appConfig.getSchedule().getSaveQuotation()) {
            log.info("行情订阅 {} 条, 行情保存关闭", ids.length);
        }
    }

    /**
     * ctp 取消订阅合约
     */
    public void unsubscribe(List<String> instruments) {
        log.info("instruments unsubscribe start");
        if (instruments.isEmpty()) {
            log.error("instrument unsubscribe failure: no instruments found, unsubscribe skip");
            return;
        }
        // 取消订阅
        String[] ids= instruments.toArray(new String[0]);
        mdApi.UnSubscribeMarketData(ids, ids.length);
        log.info("instruments unsubscribe total {} ", ids.length);
    }

    public void download() throws MyException {
        // 列出所有行情文件
        Result<List<String>> result = marketClient.listTar();

        List<String> fileNames = result.getData();
        log.info("市场行情文件下载开始, 文件数量: {}", fileNames.size());
        long start = System.currentTimeMillis();
        for (String fileName : fileNames) {
            Path backup = Paths.get("backup", fileName);
            if (backup.toFile().exists()) {
                log.info("市场行情文件已存在: {}", fileName);
                continue;
            }
            log.info("市场行情文件下载开始: {}", fileName);
            try (Response response = marketClient.marketFileDownload(fileName)) {
                if (response.status() != 200) {
                    throw new MyException("市场行情文件下载失败: " + fileName + " " + response.body().toString());
                }
                log.info(response.headers().toString());
                String[] contentLengths = response.headers().get("Content-Length").toArray(new String[0]);
                long contentLength = Long.parseLong(contentLengths[0]);

//                rep.reset();
//                rep.setContentType("application/octet-stream;charset=utf-8");
//                rep.addHeader("Content-Disposition", "attachment;filename=" + fileName);
                try (
                        FileOutputStream fos = new FileOutputStream(new File("backup", fileName));
                        BufferedInputStream bis = new BufferedInputStream(response.body().asInputStream())
//                        OutputStream output = rep.getOutputStream();
                ) {
                    byte[] buffer = new byte[8096];
                    int len;
                    long download = 0;
                    long downloadInMinute = 0;
                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMaximumFractionDigits(2);

                    while ((len = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        download += len;
                        downloadInMinute += len;
                        if ((start + 15 * 1000) > System.currentTimeMillis()) {
                            continue;
                        }

                        log.info("{} progress: {} / {}kb/s", fileName,
                                nf.format(download * 100.0 / contentLength) + "%",
                                nf.format(downloadInMinute / 1024 / 60)
                        );
                        downloadInMinute = 0;
                        start = System.currentTimeMillis();
                    }
                } catch (IOException e) {
                    throw new MyException(e.getMessage());
                }
            }
            log.info("市场行情文件下载完成: {}", fileName);
        }
    }
}

