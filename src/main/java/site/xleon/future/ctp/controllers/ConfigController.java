package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.HistoryModel;
import site.xleon.future.ctp.models.Result;
import site.xleon.future.ctp.services.impl.DataService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RefreshScope
@RequestMapping("/config")
public class ConfigController {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private DataService dataService;

    /**
     * 打包非交易日行情文件
     */
    @GetMapping("/tar")
    public Result<String> tar() throws IOException {
        log.info("请求打包非交易日行情文件");
        dataService.compress();
        return Result.success("package command send");
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

}
