package site.xleon.future.ctp.controllers;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.CaseFormat;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import site.xleon.commons.models.Result;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.ApiState;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.utils.Utils;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.models.TradingEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MdService;
import site.xleon.future.ctp.services.mapper.InstrumentMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RefreshScope
@RequestMapping("/market")
@AllArgsConstructor
public class MarketController {
    private final InstrumentMapper instrumentMapper;
    private final MdService mdService;
    private final DataService dataService;

    /**
     * market api state
     * @return api state
     */
    @GetMapping("/state")
    public Result<ApiState> state() {
        return Result.success(mdService.state());
    }


    /**
     * ctp 行情登录
     * @param user 用户
     * @return user id
     */
    @SneakyThrows
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserConfig user)  {
        return Result.success(mdService.login(user));
    }

    /**
     * ctp 行情登出
     * @deprecated ctp 不支持
     * @param user 用户
     * @return user id
     */
//    @PostMapping("/logout")
//    public Result<String> logout(@RequestBody UserConfig user) {
//        return Result.success(mdService.logout(user));
//    }

    /**
     * 注册交易前置
     * @param fronts 交易前置
     * @return 交易前置
     */
    @SneakyThrows
    @PostMapping("/registerFront")
    public Result<String> front(
            @RequestBody List<String> fronts) {
        StateEnum state = MdService.connectFronts(fronts);
        return Result.success(state.getLabel());
    }

    /**
     * 当前交易日
     * @return 交易日
     */
    @GetMapping("/tradingDay")
    public Result<String> tradingDay() {
        return Result.success(MdService.getTradingDay());
    }

    /**
     * 合约订阅
     * @param instruments instruments
     * @return instruments
     */
    @PostMapping("/subscribe")
    public Result<List<String>> subscribe(
            @RequestBody List<String> instruments) {
        return Result.success(mdService.subscribe(instruments));
    }

    /**
     * 年度交易日
     * @param year 年度
     * @return 年度交易日
     * @throws IOException exception
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
        List<String> aHolidays = Arrays.stream(holidays.split(",")).collect(Collectors.toList());
        List<String> allDates = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, Month.JANUARY, 1);
        LocalDate endDate = LocalDate.of(year, Month.DECEMBER, 31);

        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            log.info("{}", currentDate.format(df));
            if (!aHolidays.contains(currentDate.format(df)) &&
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
            @RequestParam(required = false) String tradingDay,
            @RequestParam(defaultValue = "0") Integer index
    ) {
        if (tradingDay == null || tradingDay.isEmpty()) {
            tradingDay = MdService.getTradingDay();
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
       mdService.download();
    }

    /**
     * 返回分页合约
     *
     * @return 合约
     */
    @GetMapping("/instruments")
    public Result<Page<InstrumentEntity>> instruments(
            @RequestParam(required = false) Integer idMin,
            @RequestParam(required = false) Integer idMax,
            @RequestParam(required = false) String instrumentName,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize,
            @RequestParam(required = false) String sorter,
            @RequestParam(required = false) String order
    ) {
        Page<InstrumentEntity> paging = Utils.page(page, pageSize);
        QueryWrapper<InstrumentEntity> query = new QueryWrapper<>();

        if (idMin != null) {
            query.ge("id", idMin);
        }
        if (idMax != null) {
            query.le("id", idMax);
        }
        if (instrumentName != null) {
            query.like("instrument_name", "%" + instrumentName + "%");
        }

        if (sorter == null) {
            query.select().orderByDesc("id");
        } else {
            String mySorter = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, sorter);
            if (order != null) {
                if (order.equals("ascend")) {
                    query.select().orderByAsc(mySorter);
                } else if (order.equals("descend")) {
                    query.select().orderByDesc(mySorter);
                }
            } else {
                query.select().orderByDesc("id");
            }
        }

        Page<InstrumentEntity> instruments = instrumentMapper.selectPage(paging, query);

        return Result.success(instruments);
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
        List<TradingEntity> tradings = dataService.readMarketFund(MdService.getTradingDay());
        return Result.success(tradings);
    }

    @GetMapping("/instrument/schedule")
    public Result<List<String>> schedule(String instrument, Integer interval) {
        TradingEntity trading = TradingEntity.createByInstrument(instrument);
        String tradingDay = MdService.getTradingDay();
        List<String> quotes = dataService.readMarket(tradingDay, instrument, 0);


        return Result.success(trading.getSchedule());
    }

    @GetMapping("/instrument/period")
    public Result<List<TradingEntity>> listPeriodByInterval(
            @RequestParam String instrument,
            @RequestParam String tradingDay,
            @RequestParam Integer interval) {
        int aTradingDay = Integer.parseInt(tradingDay);

        List<File> dirs = dataService.listMarkets();
        List<List<TradingEntity>> trades = new ArrayList<>(dirs.size());
        dirs.forEach(item -> trades.add(new ArrayList<>()));

        List<CompletableFuture<List<TradingEntity>>> allFutures = new ArrayList<>();
        // 最多取3天
        int maxTradingSize = Math.min(dirs.size(), 5);
        for (int index = 0; index < maxTradingSize; index ++) {
            int finalIndex = index;
            File dir = dirs.get(index);
            int aDir = 0;
            try {
                aDir = Integer.parseInt(dir.getName());
            }catch (Exception e) {
                // ignore
            }
            if (aDir > aTradingDay) {
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
        trades.forEach(item -> result.addAll(0, item));
        return Result.success(result);
    }

    /**
     * 保存合约k线(5s 30s 1m 5m 15m 1h, 日线),到kline/instrument文件夹
     * 创建时会先删除上次创建的所有数据
     */
    @GetMapping("/instrument/kLine/create")
    public Result<String> createKLine() throws ParseException, IOException, MyException {
        // 删除原来kline目录下所有文件，避免重复创建合约的日线数据
        Path kLinePath = Paths.get(DataService.KLINE_DIR);
        try {
            FileUtils.deleteDirectory(kLinePath.toFile());
        } catch (Exception e) {
            throw new MyException("kline文件夹删除失败");
        }
//        扫描data文件夹，创建合约的k线数据
        Path dataPath = Paths.get(DataService.DIR);
        for (File subDir: Objects.requireNonNull(dataPath.toFile().listFiles())) {
            if (subDir.isFile()) {
                continue;
            }
            for (File file: Objects.requireNonNull(subDir.listFiles())) {
                // 正则表达式: 1-2个字母开头，后跟4位数字（YYMM），最后以.csv结尾
                String regex = "^([a-zA-Z]{1,2}\\d{4})_(\\d{8})\\.csv$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.find()) {
                    String instrument = matcher.group(1);
                    String tradingDay = matcher.group(2);
                    log.info("instrument: {}", instrument);
                    log.info("trading day: {}",tradingDay);
                    dataService.createKLine(instrument, tradingDay);
                }
            }
        }
        return Result.success("success");
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

    /**
     * 行情振幅
     */
    @GetMapping("/amplitude")
    public void amplitude(
            @RequestParam String tradingDay,
            @RequestParam String instrumentId,
            @RequestParam Integer index,
            @RequestParam Float range
    ) {
        List<String> quotes = dataService.readMarket(tradingDay, instrumentId, index);

        String line2 = quotes.get(2);
        float openPrice = Float.parseFloat(line2.split(",")[10]);
        log.info("open price: {}", openPrice);
        // 基准价格
        float basePrice = openPrice;
        float aLastPrice = openPrice;
        // 方向
        int lastDir = 0;
        float offset = 0f;
        int i =  index;
        float min = Float.MAX_VALUE;
        float max = 0f;

        List<Float> prices = quotes.stream()
                .map(item -> {
                    List<String> lines = Arrays.asList(item.split(","));
                    return Float.parseFloat(lines.get(6));
                })
                .collect(Collectors.toList());

        List<String> records = new ArrayList<>();

        float nodePrice = openPrice;
        // 振幅精度range, 累计的振幅小于这个不记录
        for (float price: prices
             ) {
            i++;
            // 本次振幅
            float tickOffset = price - aLastPrice;
            offset += tickOffset;
            // 振幅小于要求的精度，不记录
            if (Math.abs(tickOffset) < range ) {
                continue;
            }
            float offsetP = tickOffset / openPrice * 100;

            // tick 是涨 1 跌 -1 平0
            int tickDir = 0;
            if (tickOffset > 0) {
                tickDir = 1;
            }else if (tickOffset < 0) {
                tickDir = -1;
            }

            // 和上次方向不一致， 记录振幅
            if (lastDir != tickDir) {
                lastDir = tickDir;
                float aOffset =  aLastPrice - basePrice;
                String record = String.format("%s-%s-%s", basePrice, aLastPrice, aOffset);
                records.add(record);
                basePrice = aLastPrice;
            }

//            min = Math.min(price, min);
//            max = Math.max(price, max);
            aLastPrice = price;
        }

        log.info("prices size: {}", prices.size());
        log.info("max: {}", max);
        log.info("min: {}", min);
        log.info("records size:  {}", records.size());
        log.info("records:  {}", records);
    }
}
