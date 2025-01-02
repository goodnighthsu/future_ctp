package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import site.xleon.commons.cql.CommonParam;
import site.xleon.future.ctp.config.MyCommonRelation;
import site.xleon.future.ctp.models.TradingEntity;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service("dataService")
@Slf4j
@Data
public class DataService {
    /**
     * 从ctp下载行情文件的保存目录
     */
    public static final String DIR = "data";

    /**
     * 从主服务器下载的行情文件的保存目录
     */
    public static final String BACK_UP = "backup";

    /**
     * 保存行情文件历史记录目录
     * 从主服务器下载的数据
     */
    public static final String HISTORY_DIR = "history";

    /**
     * 年交易日
     */
    public static final String TRADING_DAY = "tradingDay";

    /**
     * 交易日合约市场信息
     */
    private HashMap<String, TradingEntity> quoteCurrent = new HashMap<>();

    private final SqlSessionFactory sqlSessionFactory;
    private final MyCommonRelation myCommonRelation;

    public <T> Page<T> commons(String jsonString) throws site.xleon.commons.cql.MyException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        CommonParam param = JSON.parseObject(jsonString, CommonParam.class);
        return param.query(sqlSessionFactory, myCommonRelation);
    }

    /**
     * init file 文件不存在就创建
     *
     * @param path path
     */
    public void initFile(Path path) throws IOException {
        if (!Files.exists(path) && !Files.isDirectory(path)) {
            FileUtils.createParentDirectories(path.toFile());
            Files.createFile(path);
        }
    }

    public <T> List<T> readJson(Path path, Class<T> clazz) throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        String jsonString = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        return JSON.parseArray(jsonString, clazz);

    }

    public void saveJson(Object params, Path path) throws IOException {
        initFile(path);
        Files.write(path, JSON.toJSONBytes(params));
    }

    /**
     * 获取交易日合约市场信息
     *
     * @param tradingDay   交易日
     * @param instrumentId 合约代码
     * @param index        从第几行开始读取
     */
    public List<String> readMarket(String tradingDay, String instrumentId, int index) {
        try {
            Path path = Paths.get(DIR, tradingDay, instrumentId + "_" + tradingDay + ".csv");
            List<String> lines = Files.readAllLines(path);
            if (index == 0) {
                return lines;
            }
            return lines.subList(index, lines.size());
        } catch (NoSuchFileException e) {
            log.trace("文件不存在: {}", e.getMessage());
        } catch (IOException e) {
            log.error("读取文件失败: ", e);
        }

        return new ArrayList<>();
    }

    /**
     * 读取市场合约资金
     * @param tradingDay 交易日
     * @return tradings
     */
    public List<TradingEntity> readMarketFund(String tradingDay) {
        List<TradingEntity> list = new ArrayList<>();
        try {
            Path path = Paths.get(DIR, tradingDay);
            File[] files = path.toFile().listFiles();
            if (files == null) {
                return list;
            }

            for (File file :
                    files) {
                // 获取最后一行
                try (ReversedLinesFileReader reader = new ReversedLinesFileReader(file, Charset.defaultCharset())){
                    String line = reader.readLine();
                    if (line != null) {
                        TradingEntity trading = TradingEntity.createByString(line);
                        list.add(trading);
                    }
                }catch (Exception e) {
                    log.error("read market fund error", e);
                }
            }
        }catch (Exception e) {
            log.error("read market fund error", e);
        }

        return list;
    }

    /**
     * 打包非交易日行情文件
     * @apiNote 打包data文件夹下非交易日目录，打包成功后删除交易日文件夹
     * @throws IOException exception
     */
    public void compress() throws IOException {
        Path path = Paths.get(DIR);
        File[] files = path.toFile().listFiles();
        if (files == null) {
            log.info("压缩跳过，没有可压缩文件");
            return;
        }

        for (File item :
                files) {
            // 是目录且不是当天交易日目录
//            if (item.isDirectory() && !item.getName().equals(ctpInfo.getTradingDay())) {
//                log.info("auto compress dir {}", item.getName());
//                log.info("{} compress start", item.getName());
//                CompressUtils.tar(item.toPath(), Paths.get("data", item.getName() + ".tar.gz"));
//                log.info("{} compress end", item.getName());
//                FileUtils.deleteDirectory(item);
//                log.info("{} deleted", item.getName());
//            }
        }
    }

    /**
     * 返回市场文件夹
     */
    public List<File> listMarkets() {
        Path path = Paths.get(DIR);
        File[] sub  = path.toFile().listFiles();
        if (sub == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(sub)
                .sorted((File file1, File file2) -> {
            try {
                Integer name1 = Integer.parseInt(file1.getName());
                Integer name2 = Integer.parseInt(file2.getName());
                return name2 - name1;
            } catch (Exception e) {
                log.error("市场文件夹异常, 存在非日期的文件夹: ", e);
            }
            return 0;
        }).collect(Collectors.toList());
    }

    /**
     * 返回tar.gz文件列表
     * @return tar.gz文件列表
     */
    public String[] listTar() {
        Path path = Paths.get(DIR);
        return path.toFile().list((dir, name) -> name.endsWith(".tar.gz"));
    }

    /**
     * 合约交易日k线
     * 当天交易，实际交易时间缺失
     */
    public List<TradingEntity> listKLines(String instrument, Integer interval, String tradingDay) throws ParseException {
        List<String> quotes = this.readMarket(tradingDay, instrument, 0);
        TradingEntity trading = TradingEntity.createByInstrument(instrument);
        // 交易时间段，不包含收盘
        List<String> timeLines = trading.getTimeLinesByInterval(interval, false);

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat dfFull = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        // k线信息 时间， 开盘， 最高，收盘，最低
        List<TradingEntity> periods = new ArrayList<>();

        TradingEntity lastKLine = new TradingEntity();
        lastKLine.setVolume(0L);
        int last = 0;
        // 交易日开盘
        boolean isTradingOpen = true;
        // 收盘成交量
        Long closeVolume = 0L;
        log.info("instrument/tradingday/line: {}/{}/{}/{}", instrument,tradingDay, quotes.size(), timeLines.size());
        for (String timeLine : timeLines) {
            try {
                long openTime = df.parse(timeLine).getTime();
                // 跨日调整
                if (openTime < 60*60*20*1000) {
                    openTime += 60*60*24*1000;
                }
                long closeTime = openTime + interval*1000;
                // init k线信息 时间 开盘价格 收盘价 最高 最低 手数
                // 开盘价 = 上个k线的收盘价
                TradingEntity kLine = new TradingEntity();
                kLine.setOpenPrice(lastKLine.getClosePrice());
                kLine.setHighestPrice(lastKLine.getClosePrice());
                kLine.setLowestPrice(lastKLine.getClosePrice());
                kLine.setClosePrice(lastKLine.getClosePrice());
                kLine.setOpenInterest(lastKLine.getOpenInterest());
                kLine.setTickVolume(0L);
                // 开盘成交量是上个
                kLine.setVolume(closeVolume);
                periods.add(kLine);

                // 时间段端内数据
                for (int i = last; i < quotes.size(); i++) {
                    TradingEntity quote = TradingEntity.createByString(quotes.get(i));
                    // 跳过脏数据，没有开盘价格
                    if (quote.getOpenPrice() == null || quote.getOpenPrice().compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }
                    if (!quote.getActionTimeDate().isPresent()) {
                        continue;
                    }

                    // 没有交易量，跳过
                    long lastTickVolume = quote.getVolume()-lastKLine.getVolume();
                    if (lastTickVolume <=0) {
                        continue;
                    }

                    // 交易时间 实际发生日 + 时间
                    kLine.setTradingActionTime(dfFull.parse(quote.getActionDay() + " " + timeLine));

                    // 超过收盘时间，跳出
                    long actionTime = quote.getActionTimeDate().get().getTime();
                    // 跨日调整
                    if (actionTime < 60*60*20*1000) {
                        actionTime += 60*60*24*1000;
                    }
                    if (actionTime >= closeTime) {
                        last = Math.max(i - 1, 0);
                        break;
                    }

                    closeVolume = quote.getVolume();
                    // 开盘
                    if (actionTime == openTime) {
                        kLine.setOpenPrice(quote.getLastPrice());
                        kLine.setHighestPrice(quote.getLastPrice());
                        kLine.setLowestPrice(quote.getLastPrice());
                        kLine.setVolume(quote.getVolume());
                        // 设置上个K线的收盘价、成交、持仓
                        lastKLine.setClosePrice(quote.getLastPrice());
                        lastKLine.setTickVolume(lastTickVolume);
                        lastKLine.setOpenInterest(quote.getOpenInterest());
                    }

                    //  交易日开盘，会有早于8:55 -9:00撮合时间区内的数据，开盘时间使用openPrice
                    if (isTradingOpen) {
                        kLine.setOpenPrice((quote.getOpenPrice()));
                        kLine.setHighestPrice((quote.getOpenPrice()));
                        kLine.setLowestPrice((quote.getOpenPrice()));
                        isTradingOpen = false;
                    }

                    // 收盘价
                    kLine.setClosePrice(quote.getLastPrice());
                    // 收盘成交量 = 当前成交 - 开盘成交
                    kLine.setTickVolume(quote.getVolume() - kLine.getVolume());
                    // 持仓
                    kLine.setOpenInterest(quote.getOpenInterest());
                    // 最高
                    if (kLine.getHighestPrice() != null && quote.getLastPrice().doubleValue() > kLine.getHighestPrice().doubleValue()) {
                        kLine.setHighestPrice(quote.getLastPrice());
                    }
                    // 最低
                    if (kLine.getLowestPrice() != null && quote.getLastPrice().doubleValue() < kLine.getLowestPrice().doubleValue()) {
                        kLine.setLowestPrice(quote.getLastPrice());
                    }
                }

                lastKLine = kLine;

                // TODO: 当天交易，实际交易时间缺失
                /**
                 * eg: 交易日是8/14周一, 实际交易日可能是 8/11的21：00 到 8/12的2:00 到 8/14的9：00-15：00
                 * 现在使用的是收到行情里的apction day,  如果是当日交易就没有尚未发生交易的日期
                 * tradingActionTime为空
                 */
                if (kLine.getTradingActionTime() == null) {
                    periods.remove(kLine);
                }
            } catch (Exception e) {
                log.error("error: {}", e.getMessage());
            }
        }

        return periods;
    }
}
