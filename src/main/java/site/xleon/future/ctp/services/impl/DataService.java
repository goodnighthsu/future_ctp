package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSON;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.models.InstrumentEntity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service("dataService")
public class DataService {
    private static final String DIR = "data";

    @Autowired
    private TradingService tradingService;

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

//        byte[] bytes = Files.readAllBytes(path);
//        T result = JSON.parseObject(bytes, clazz);
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

    @SneakyThrows
    public List<InstrumentEntity> readInstrumentsTradingDay(String tradingDay) {
        Path path = Paths.get(DIR, "instruments_" + tradingDay + ".json");
        return readJson(path, InstrumentEntity.class);
    }

    @SneakyThrows
    public void saveInstrumentsTradingDay(List<InstrumentEntity> params, String tradingDay) {
        Path path = Paths.get(DIR, "instruments_" + tradingDay + ".json");
        saveJson(params, path);
    }
}
