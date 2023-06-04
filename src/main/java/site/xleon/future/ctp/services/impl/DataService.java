package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.core.utils.CompressUtils;
import site.xleon.future.ctp.models.InstrumentEntity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service("dataService")
@Slf4j
public class DataService {
    private static final String DIR = "data";

    @Autowired
    private CtpInfo ctpInfo;

    /**
     * 当前订阅合约
     */
    private static final Path SUBSCRIBE_PATH = Paths.get(DIR, "subscribe.json");

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
     * 获取当前订阅合约
     */
    @SneakyThrows
    public List<String> readSubscribe() {
        return readJson(SUBSCRIBE_PATH, String.class);
    }

    /**
     * 保存订阅合约
     *
     * @param params instruments
     */
    @SneakyThrows
    public void saveSubscribe(List<String> params) {
        saveJson(params, SUBSCRIBE_PATH);
    }

    /**
     * 获取交易日合约信息
     *
     * @param tradingDay 交易日
     * @return 合约信息
     */
    @SneakyThrows
    public List<InstrumentEntity> readInstrumentsTradingDay(String tradingDay) {
        Path path = Paths.get(DIR, "instruments_" + tradingDay + ".json");
        return readJson(path, InstrumentEntity.class);
    }

    /**
     * 保存交易日合约信息
     *
     * @param params     合约信息
     * @param tradingDay 交易日
     */
    @SneakyThrows
    public void saveInstrumentsTradingDay(List<InstrumentEntity> params, String tradingDay) {
        Path path = Paths.get(DIR, "instruments_" + tradingDay + ".json");
        saveJson(params, path);
    }

    /**
     * 合约详情
     */
    public Optional<InstrumentEntity> readeInstrumentDetail(String instrumentId, String tradingDay) {
        return readInstrumentsTradingDay(tradingDay).stream().filter(instrumentEntity -> instrumentEntity.getInstrumentID().equals(instrumentId)).findFirst();

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
