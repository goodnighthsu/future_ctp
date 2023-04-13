package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.models.InstrumentEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service("dataService")
@Slf4j
public class DataService {
    private static final String DIR = "data";

    /**
     * 当前订阅合约
     */
    private static final Path SUBSCRIBE_PATH = Paths.get(DIR, "subscribe.json");

    /**
     * init file 文件不存在就创建
     * @param path path
     */
    @SneakyThrows
    public void initFile(Path path){
        if (!Files.exists(path) && !Files.isDirectory(path)) {
            FileUtils.createParentDirectories(path.toFile());
            Files.createFile(path);
        }
    }

    private <T> List<T> readJson(Path path, Class<T> clazz) throws IOException {
        if (!Files.exists(path)) {
            return null;
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
     * @param params 合约信息
     * @param tradingDay 交易日
     */
    @SneakyThrows
    public void saveInstrumentsTradingDay(List<InstrumentEntity> params, String tradingDay) {
        Path path = Paths.get(DIR, "instruments_" + tradingDay + ".json");
        saveJson(params, path);
    }

    /**
     * 获取交易日合约市场信息
     * @param tradingDay 交易日
     * @param instrumentId 合约代码
     * @param index 从第几行开始读取
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
}
