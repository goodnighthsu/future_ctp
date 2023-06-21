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
import site.xleon.future.ctp.models.TradingEntity;
import site.xleon.future.ctp.services.Ctp;
import site.xleon.future.ctp.services.CtpMasterClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        String[] ids= instruments.toArray(new String[instruments.size()]);
        if (appConfig.getSchedule().getSubscribe()) {
            int code = mdApi.SubscribeMarketData(ids, ids.length);
            log.info("行情订阅 {} 条, code: {}", ids.length, code);
        } else {
            log.info("行情订阅 {} 条, 订阅关闭", ids.length);
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
                        BufferedInputStream bis = new BufferedInputStream(response.body().asInputStream());
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

    /**
     * 合约交易日k线
     */
    public List<TradingEntity> listKLines(String instrument, Integer interval, String tradingDay) {
        List<String> quotes = dataService.readMarket(tradingDay, instrument, 0);
        TradingEntity trading = TradingEntity.createByInstrument(instrument);
        // 交易时间段，不包含收盘
        List<String> timeLines = trading.getTimeLinesByInterval(interval, false);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat dfFull = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        // k线信息 时间， 开盘， 最高，收盘，最低
        List<TradingEntity> periods = new ArrayList<>();

        TradingEntity lastTrading = new TradingEntity();
        lastTrading.setVolume(0L);
        int last = 0;
        // 交易日开盘
        Boolean isTradingOpen = true;
        for (String timeLine : timeLines) {
            try {
                long openTime = df.parse(timeLine).getTime();
                long closeTime = openTime + interval * 1000;
                // k线信息 时间 开盘价格 收盘价 最高 最低 手数 使劲按
                TradingEntity kLine = new TradingEntity();

                // 开盘
                boolean isOpen = true;
                // 时间段端内数据
                for (int i = last; i < quotes.size(); i++) {
                    TradingEntity quote = TradingEntity.createByString(quotes.get(i));
                    // 跳过脏数据，没有开盘价格
                    if (quote.getOpenPrice().compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    if (!quote.getActionTimeDate().isPresent()) {
                        continue;
                    }

                    // 交易时间 实际发生日 + 时间
                    kLine.setTradingActionTime(dfFull.parse(quote.getActionDay() + " " + timeLine));
                    // 超过收盘时间，跳出
                    long actionTime = quote.getActionTimeDate().get().getTime();
                    if (actionTime >= closeTime) {
                        last = Math.max(i - 1, 0);
                        break;
                    }

                    if (actionTime >= openTime) {
                        // 第一个有效数据价格作为开盘价， 并且设置上个K线的收盘价
                        if (isOpen) {
                            kLine.setOpenPrice(quote.getLastPrice());
                            kLine.setHighestPrice(quote.getLastPrice());
                            kLine.setLowestPrice(quote.getLastPrice());
                            lastTrading.setClosePrice(quote.getLastPrice());

                            // 交易量
                            kLine.setVolume(quote.getVolume());
                            lastTrading.setTickVolume(quote.getVolume()-lastTrading.getVolume());
                            // 持仓
                            lastTrading.setOpenInterest(quote.getOpenInterest());
                            isOpen = false;
                        }
                        //  交易日开盘，会有早于8:55 -9:00撮合时间区内的数据，开盘时间使用openPrice
                        if (isTradingOpen) {
                            kLine.setOpenPrice((quote.getOpenPrice()));
                            kLine.setVolume(0L);
                            isTradingOpen = false;
                        }
                        kLine.setLastPrice(quote.getLastPrice());
                        kLine.setClosePrice(quote.getLastPrice());
                        // 交易量
                        kLine.setTickVolume(quote.getVolume() - kLine.getVolume());
                        // 持仓
                        kLine.setOpenInterest(quote.getOpenInterest());

                        if (quote.getLastPrice().doubleValue() > kLine.getHighestPrice().doubleValue()) {
                            kLine.setHighestPrice(quote.getLastPrice());
                        }

                        if (kLine.getLowestPrice() == null || quote.getLastPrice().doubleValue() < kLine.getLowestPrice().doubleValue()) {
                            kLine.setLowestPrice(quote.getLastPrice());
                        }
                    }
                }

                if (kLine.getVolume() == null) {
                    kLine.setVolume(0L);
                }

                periods.add(kLine);
                lastTrading = kLine;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return periods;
    }
}

