package site.xleon.future.ctp.controllers;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    /**
     * 当前交易日
     * @return 交易日
     */
    @GetMapping("/tradingDay")
    public Result<String> tradingDay() {
        String tradingDay = this.ctpInfo.getTradingDay();
        return Result.success(tradingDay);
    }

    /**
     * 年度交易日
     */
    @GetMapping("/tradingDays")
    public Result<List<String>> tradingDays(
            @RequestParam Integer year
    ) throws IOException {
        Path path = Paths.get(DataService.TRADING_DAY, year.toString() + ".txt");
        if (!path.toFile().exists()) {
            return Result.success(new ArrayList<>());
        }
        String days = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        return Result.success(Arrays.stream(days.split(",")).collect(Collectors.toList()));
    }

    /**
     * 配置年度交易日
     */
    @PostMapping("/tradingDays")
    public Result<List<String>> updateTradingDays(
            @RequestParam @NonNull Integer year,
            @RequestParam @NonNull String holidays
    ) throws IOException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
        List<String> _holidays = Arrays.stream(holidays.split(",")).collect(Collectors.toList());
        List<String> allDates = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, Month.JANUARY, 1);
        LocalDate endDate = LocalDate.of(year, Month.DECEMBER, 31);

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            log.info("{}", currentDate.format(df));
            if (!_holidays.contains(currentDate.format(df)) &&
                    currentDate.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    currentDate.getDayOfWeek() != DayOfWeek.SUNDAY
            ) {
                allDates.add(currentDate.format(df));
            }
            currentDate = currentDate.plusDays(1);
        }

        Path path = Paths.get(DataService.TRADING_DAY, year.toString() + ".txt");
        dataService.initFile(path);
        FileUtils.writeStringToFile(path.toFile(), String.join(",", allDates), StandardCharsets.UTF_8, false);

        return Result.success(allDates);
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

    /**
     * 市场行情文件删除
     */
    @GetMapping("/delete")
    public Result<String> delete(
            @RequestParam String fileName
    ) throws MyException {

        Path path = Paths.get(DataService.DIR, fileName);
        if (!path.toFile().delete()) {
            throw new MyException(fileName + "delete failure");
        }
        return Result.success(fileName + " deleted");
    }

    @GetMapping("/autoDownload")
    public void autoDownload() throws MyException, IOException {
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
        int _tradingDay = Integer.parseInt(tradingDay);

        List<File> dirs = dataService.listMarkets();
        List<List<TradingEntity>> trades = new ArrayList<>(dirs.size());
        dirs.forEach(item -> {
            trades.add(new ArrayList<>());
        });

        List<CompletableFuture> allFutures = new ArrayList<>();
        // 最多取3天
        int maxTradingSize = Math.min(dirs.size(), 5);
        for (int index = 0; index < maxTradingSize; index ++) {
            int finalIndex = index;
            File dir = dirs.get(index);
            int _dir = 0;
            try {
                 _dir = Integer.parseInt(dir.getName());
            }catch (Exception e) {
                // ignore
            }
            if (_dir > _tradingDay) {
                continue;
            }
            // 倒序获取行情文件 a2307_20230507.csv
            Path path = Paths.get(dir.getPath(), instrument+ "_" + dir.getName() + ".csv");
            if (!path.toFile().exists()) {
                break;
            }
            CompletableFuture<List<TradingEntity>> future = CompletableFuture.supplyAsync(() -> {

                try {
                    List<TradingEntity> period  = dataService.listKLines(instrument, interval, dir.getName());
                    log.info("period: {}-{}", dir.getName(), period.size());
                    trades.set(finalIndex, period);
                    return period;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return new ArrayList<>();
            });
            allFutures.add(future);
        }

        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .join();
        List<TradingEntity> result = new ArrayList<>();
        trades.forEach(item -> {
            result.addAll(0, item);
        });
        return Result.success(result);
    }

    /**
     * 实时行情
     * @return 行情信息
     */
    @GetMapping("/instrument/quotes")
    public Result<List<TradingEntity>> listQuotes() {
        List<TradingEntity> quotes = dataService.getQuoteCurrent().values().stream()
                .sorted(Comparator.comparingLong(TradingEntity::getVolume).reversed())
                .limit(100)
                .collect(Collectors.toList());
        return Result.success(quotes);
    }
}
