package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.HistoryModel;
import site.xleon.future.ctp.models.Result;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RefreshScope
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private DataService dataService;

    @Autowired
    private MarketService marketService;

    /**
     * 打包非交易日行情文件
     */
    @GetMapping("/tar")
    public Result<String> tar() throws IOException {
        log.info("请求打包非交易日行情文件");
        dataService.compress();
        return Result.success("package command send");
    }

    @GetMapping("/download/history")
    public Result<String> downloadHistory() throws MyException, IOException {
        marketService.download();
        return Result.success("download history success");
    }

    /**
     * 行情历史文件状态
     */
    @GetMapping("/history")
    public Result<List<HistoryModel>>state() throws IOException, MyException {
        File[] files = appConfig.getHistoryPath().toFile().listFiles();
        List<HistoryModel> histories = new ArrayList<>();
        if (files == null) {
            return Result.success(histories);
        }

        for (File file :
                files) {
            if (!file.isDirectory()) {
                continue;
            }

            FileUtils.sizeOf(file);
            HistoryModel history = new HistoryModel();
            history.setTradingDay(file.getName());
            history.setSize(FileUtils.sizeOf(file));
            if (file.listFiles() != null) {
                history.setCount(file.listFiles().length);
            }
            histories.add(history);
        }

        return Result.success(histories);
    }
}
