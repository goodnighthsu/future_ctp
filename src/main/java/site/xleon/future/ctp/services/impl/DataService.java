package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.core.utils.CompressUtils;
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
import java.util.stream.Collectors;

@Service("dataService")
@Slf4j
@Data
public class DataService {
    private static final String DIR = "data";

    @Autowired
    private CtpInfo ctpInfo;

    /**
     * 交易日合约市场信息
     */
    private HashMap<String, TradingEntity> quote = new HashMap<>();

    /**
     * init file 文件不存在就创建
     *
     * @param path path
     */
    @SneakyThrows
    public void initFile(Path path) {
        if (!Files.exists(path) && !Files.isDirectory(path)) {
            FileUtils.createParentDirectories(path.toFile());
            Files.createFile(path);
        }
    }

    private <T> List<T> readJson(Path path, Class<T> clazz) throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }

        String jsonString = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
        return JSON.parseArray(jsonString, clazz);

    }

    private void saveJson(Object params, Path path) throws IOException {
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

    public void compress() {
        Path path = Paths.get(DIR);
        File[] files = path.toFile().listFiles();
        if (files == null) {
            log.info("压缩跳过，没有可压缩文件");
            return;
        }
        Arrays.stream(files).forEach(item -> {
            // 是目录且不是当天交易日目录
            if (item.isDirectory() && !item.getName().equals(ctpInfo.getTradingDay())) {
                log.info("auto compress dir {}", item.getName());
                try {
                    log.info("{} compress start", item.getName());
                    CompressUtils.tar(item.toPath(), Paths.get("data", item.getName() + ".tar.gz"));
                    log.info("{} compress end", item.getName());
                    FileUtils.deleteDirectory(item);
                    log.info("{} deleted", item.getName());
                } catch (IOException e) {
                    log.error("{} compress error: ", item.getName(), e);
                    throw new RuntimeException(e);
                }
            }
        });
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
                return 0;
            }
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
     */
    public List<TradingEntity> listKLines(String instrument, Integer interval, String tradingDay) {
        List<String> quotes = this.readMarket(tradingDay, instrument, 0);
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
        boolean isTradingOpen = true;
        for (String timeLine : timeLines) {
            try {
                long openTime = df.parse(timeLine).getTime();
                long closeTime = openTime + interval * 1000;
                // init k线信息 时间 开盘价格 收盘价 最高 最低 手数
                TradingEntity kLine = new TradingEntity();
                kLine.setOpenPrice(lastTrading.getLastPrice());
                kLine.setHighestPrice(lastTrading.getLastPrice());
                kLine.setLowestPrice(lastTrading.getLastPrice());
                kLine.setClosePrice(lastTrading.getLastPrice());
                kLine.setOpenInterest(lastTrading.getOpenInterest());
                kLine.setTickVolume(0L);
                kLine.setVolume(lastTrading.getVolume());

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
