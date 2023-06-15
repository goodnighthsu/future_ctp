package site.xleon.future.ctp.controllers;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.models.Result;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.utils.Utils;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.models.TradingEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.mapper.InstrumentMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private InstrumentMapper instrumentMapper;

    @Autowired
    private MarketService marketService;

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
     * 合约详情
     *
     * @param instrument 合约id
     * @return 合约详情
     * @throws MyException exception
     */
    @GetMapping("/instrument/info")
    public Result<InstrumentEntity> info(@RequestParam @NonNull String instrument) throws MyException {
        QueryWrapper<InstrumentEntity> query = new QueryWrapper<>();
        query.select("*").eq("instrument_i_d", instrument);
        InstrumentEntity result = instrumentMapper.selectOne(query);
        return Result.success(result);
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

    /**
     * 返回分页合约
     *
     * @return 合约
     */
    @GetMapping("/instruments")
    public Result<List<InstrumentEntity>> instruments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        Page<InstrumentEntity> paging = Utils.page(page, pageSize);
        QueryWrapper<InstrumentEntity> query = new QueryWrapper<>();

        if (keyword != null && keyword.length() > 0 ) {
            query.eq("id", keyword);
        }
        query.select().orderByDesc("id");
        Page<InstrumentEntity> instruments = instrumentMapper.selectPage(paging, query);

        return Result.page(instruments);
    }

    /**
     * 返回所有合约
     */
    @GetMapping("/instruments/all")
    public Result<List<String>> allInstruments() {
        QueryWrapper<InstrumentEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("*").orderByDesc("id");
        List<InstrumentEntity> list = instrumentMapper.selectList(queryWrapper);
        return Result.success(
                list.stream().map(InstrumentEntity::getInstrumentID).collect(Collectors.toList())
        );
    }

    /**
     * 返回市场资金
     */
    @GetMapping("/funds")
    public Result<List<TradingEntity>> funds() {
        List<TradingEntity> tradings = dataService.readMarketFund(ctpInfo.getTradingDay());
        return Result.success(tradings);
    }

    @GetMapping("/instrument/schedule")
    public Result<List<String>> schedule(String instrument, Integer interval) {
        TradingEntity trading = TradingEntity.createByInstrument(instrument);
        String tradingDay = ctpInfo.getTradingDay();
        List<String> quotes = dataService.readMarket(tradingDay, instrument, 0);


        return Result.success(trading.getSchedule());
    }

    @GetMapping("/instrument/period")
    public Result<List<TradingEntity>> listPeriodByInterval(
            @RequestParam String instrument,
            @RequestParam String tradingDay,
            @RequestParam Integer interval) {
        List<String> quotes = dataService.readMarket(tradingDay, instrument, 0);
        TradingEntity trading = TradingEntity.createByInstrument(instrument);
        List<String> timeLines = trading.getTimeLinesByInterval(interval);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat df1 = new SimpleDateFormat("HH:mm:ss.SSS");
        SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        // k线信息 时间， 开盘， 最高，收盘，最低
        List<TradingEntity> periods = new ArrayList<>();

        TradingEntity lastTrading = new TradingEntity();
        lastTrading.setOpenPrice(BigDecimal.ZERO);
        lastTrading.setClosePrice(BigDecimal.ZERO);
        lastTrading.setHighestPrice(BigDecimal.ZERO);
        lastTrading.setLowestPrice(BigDecimal.valueOf(Double.MAX_VALUE));
        lastTrading.setVolume(0L);
        int last = 0;
        for(int n=0; n < timeLines.size(); n++) {
            try {
                String time = timeLines.get(n);
                long openTime = df.parse(time).getTime();
                long closeTime = openTime + interval * 1000;
                TradingEntity item = new TradingEntity();
                item.setOpenPrice(lastTrading.getClosePrice());
//                item.setClosePrice(lastTrading.getClosePrice());
                item.setHighestPrice(lastTrading.getHighestPrice());
                item.setLowestPrice(lastTrading.getLowestPrice());
                item.setVolume(lastTrading.getVolume());

                for (int i = last; i < quotes.size(); i++) {
                    TradingEntity quote = TradingEntity.createByString(quotes.get(i));
                    long actionTime = df1.parse(quote.getActionTime()).getTime();
                    if (actionTime >= openTime && actionTime < closeTime) {
                        item.setTradingActionTime(df2.parse(quote.getActionDay() + " " + time));
                        if (quote.getLastPrice().doubleValue() > item.getHighestPrice().doubleValue()) {
                            item.setHighestPrice(quote.getLastPrice());
                        }

                        if (quote.getLastPrice().doubleValue() < item.getLowestPrice().doubleValue()) {
                            item.setLowestPrice(quote.getLastPrice());
                        }

                        item.setVolume(quote.getVolume());
                        item.setTickVolume(quote.getVolume() - lastTrading.getVolume());
                        item.setClosePrice(quote.getLastPrice());
                    }

                    if (actionTime >= closeTime) {
                        last = i;
                        break;
                    }
                }

                periods.add(item);
                lastTrading = item;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return Result.success(periods);
    }
}
