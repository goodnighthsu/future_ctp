package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.Result;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.utils.CompressUtils;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RefreshScope
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private MarketService marketService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private DataService dataService;

    /**
     * ctp 登录
     *
     * @return trading day
     */
    @GetMapping("/login")
    public Result<String> login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        String tradingDay = marketService.login();
        return Result.success(tradingDay);
    }

    @GetMapping("/logout")
    public Result<String> logout() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        String result = marketService.logout();
        return Result.success(result);
    }

    @GetMapping("/tradingDay")
    public Result<String> tradingDay() {
        String tradingDay = this.ctpInfo.getTradingDay();
        return Result.success(tradingDay);
    }

    /**
     * 订阅合约
     *
     * @param params 合约id
     * @return 订阅的合约
     */
    @PutMapping("/subscribe")
    public Result<List<String>> subscribe(@RequestBody List<String> params) {
        List<String> subscribes = marketService.getSubscribeInstruments();
        subscribes.addAll(params);
        marketService.subscribe(subscribes);
        marketService.setSubscribeInstruments(subscribes);
        return Result.success(params);
    }

    /**
     * 取消订阅
     *
     * @param params 合约id
     * @return 取消订阅的合约
     */
    @PutMapping("/unsubscribe")
    public Result<List<String>> unsubscribe(@RequestBody List<String> params) {
        marketService.unsubscribe(params);
        // ctpInfo 移除订阅信息
        List<String> subscribes = marketService.getSubscribeInstruments();
        subscribes.removeAll(params);
        marketService.setSubscribeInstruments(subscribes);
        return Result.success(params);
    }

    /**
     * 获取指定合约的行情
     *
     * @param instrument 合约id
     * @param tradingDay 交易日
     * @return 行情
     */
    @GetMapping("/query")
    public Result<List<String>> query(
            @RequestParam @NonNull String instrument,
            @RequestParam @Nullable String tradingDay,
            @RequestParam(defaultValue = "0") Integer index
    ) {
        if (tradingDay == null || tradingDay.isEmpty()) {
            tradingDay = ctpInfo.getTradingDay();
        }
        return Result.success(dataService.readMarket(tradingDay, instrument, index));
    }

    /**
     * 订阅全市场合约
     *
     * @return 订阅的合约数量
     */
    @GetMapping("/subscribeAll")
    public Result<Integer> subscribeAll() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        List<InstrumentEntity> all = tradeService.listInstruments(null);
        marketService.subscribe(all.stream().map(InstrumentEntity::getInstrumentID).collect(Collectors.toList()));
        return Result.success(all.size());
    }

    /**
     * 合约详情
     *
     * @param instrument 合约id
     * @param tradingDay 交易日
     * @return 合约详情
     * @throws MyException exception
     */
    @GetMapping("/instrument/info")
    public Result<InstrumentEntity> info(@RequestParam @NonNull String instrument,
                                         @RequestParam @NonNull String tradingDay) throws MyException {
        InstrumentEntity result = dataService.readeInstrumentDetail(instrument, tradingDay)
                .orElseThrow(() -> new MyException("instrument not found"));
        return Result.success(result);
    }

    @GetMapping("/compress")
    public Result<String> compress(@RequestParam @NonNull String dir) throws IOException {
        CompressUtils.tar(Paths.get("data", dir), Paths.get("data", dir + ".tar.gz"));
        return Result.success("success");
    }

    @GetMapping("/compress/all")
    public Result<String> compressAll() {
        dataService.compress();
        return Result.success("success");
    }

    /**
     * 行情压缩列表
     */
    @GetMapping("/tar")
    public Result<String[]> listTar() {
        return Result.success(dataService.listTar());
    }

    /**
     * 行情下载
     */
    @GetMapping("/download")
    public Result<String> download(@RequestParam String fileName,
                                   HttpServletResponse response
    ) {
        File file = new File("data", fileName);
        if (!file.exists()) {
            return Result.fail("file not found");
        }

        response.setContentType("application/octet-stream;charset=utf-8");
        response.setContentLengthLong(file.length());
        response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
        try (
                BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()));
                OutputStream output = response.getOutputStream()
        ) {
            byte[] buffer = new byte[8096];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            return Result.success("download success");
        } catch (Exception e) {
            return Result.fail("download error: " + e.getMessage());
        }
    }

    @GetMapping("/autoDownload")
    public void autoDownload() throws MyException {
       marketService.download();
    }

    @Autowired
    private AppConfig appConfig;
    @GetMapping("/config")
    public Result<AppConfig> config() {
        return Result.success(appConfig);
    }
}
