package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSON;
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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service("dataService")
@Slf4j
public class DataService {
    private static final String DIR = "data";

    @Autowired
    private CtpInfo ctpInfo;

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
     * 返回tar.gz文件列表
     * @return tar.gz文件列表
     */
    public String[] listTar() {
        Path path = Paths.get(DIR);
        return path.toFile().list((dir, name) -> name.endsWith(".tar.gz"));
    }

}
