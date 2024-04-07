package site.xleon.future.ctp.services.impl;

import ctp.thostmduserapi.*;
import feign.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.core.utils.CompressUtils;
import site.xleon.future.ctp.models.ApiState;
import site.xleon.future.ctp.models.Result;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.Ctp;
import site.xleon.future.ctp.services.CtpMasterClient;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Service("marketService")
@Slf4j
public class MdService {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private DataService dataService;

    @Autowired
    private CtpMasterClient marketClient;

    private static String tradingDay;
    public static String getTradingDay() {
        MdService.tradingDay = mdApi.GetTradingDay();
        return MdService.tradingDay;
    }
    public static void setTradingDay(String value) {
        tradingDay = value;
    }

    /**
     * 登录用户
     */
    private static UserConfig user;

    public static final Object loginLock = new Object();
    /**
     * 用户是否登录
     */
    private static volatile StateEnum loginState = StateEnum.DISCONNECT;
    public static StateEnum getLoginState() {
        synchronized (loginLock){
            return loginState;
        }
    }

    /**
     * 更新用户的登录状态
     * @param value 登录状态
     */
    public static void notifyLogin(StateEnum value) {
        synchronized (MdService.loginLock) {
            loginState = value;
            MdService.loginLock.notifyAll();
        }
    }

    private static CThostFtdcMdSpi mdSpi;
    /**
     * 行情api
     */
    private static CThostFtdcMdApi mdApi;
    static {
        mdApi = CThostFtdcMdApi.CreateFtdcMdApi("flow" + File.separator);
        mdSpi = new MdSpiImpl();
        mdApi.RegisterSpi(mdSpi);
        mdApi.Init();
    }

    private static final Object connectLock = new Object();
    /**
     * 前置连接状态
     */
    private static StateEnum connectState = StateEnum.DISCONNECT;
    public static StateEnum getConnectState() {
        return connectState;
    }

    /**
     * 行情前置地址
     */
    private static List<String> fronts = new ArrayList<>();
    public static List<String> getFronts() {
        return fronts;
    }
    /**
     * 重置前置连接
     * @param fronts 前置地址数组 eg: ["tcp://180.169.75.18:61213"]
     * @return 连接状态
     */
    public static StateEnum setFronts(List<String> fronts) throws InterruptedException, MyException {
        MdService.fronts = fronts;
        connectState = StateEnum.DISCONNECT;
        loginState = StateEnum.DISCONNECT;
        if (mdApi != null) {
            mdApi.Release();
        }

        mdApi = CThostFtdcMdApi.CreateFtdcMdApi("flow" + File.separator);
        for (String front: fronts) {
            mdApi.RegisterFront(front);
        }
        mdSpi = new MdSpiImpl();
        mdApi.RegisterSpi(mdSpi);
        mdApi.Init();

        synchronized (connectLock) {
            while (StateEnum.DISCONNECT == connectState) {
                connectLock.wait(6000);
                // 超时退出
                if (StateEnum.DISCONNECT == connectState) {
                    throw new MyException(StateEnum.TIMEOUT.getLabel());
                }
            }
        }

        return connectState;
    }

    /**
     * 设置前置已连接
     */
    public static synchronized void notifyConnected(StateEnum value) {
        synchronized (connectLock) {
            connectState = value;
            connectLock.notifyAll();
        }
    }

    /**
     * 订阅的合约
     */
    private List<String> subscribeInstruments;

    /**
     * 登录
     * @return trading day
     */
    public String login(UserConfig user) throws MyException {
        if (StateEnum.SUCCESS == MdService.getLoginState()) {
            throw new MyException("用户已登录，请勿重复登录");
        }
        Ctp<String> ctp = new Ctp<>();
        ctp.setId(0);
        try {
            ctp.request(requestId -> {
                CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
                field.setBrokerID(user.getBrokerId());
                field.setUserID(user.getUserId());
                field.setPassword(user.getPassword());
                return mdApi.ReqUserLogin(field, requestId);
            });
        }catch (Exception e) {
            log.info("login: ", e);
        }

        String aTradingDay = mdApi.GetTradingDay();
        MdService.setTradingDay(aTradingDay);
        return aTradingDay;
    }

    /**
     * 退出登录
     * @deprecated (ctp 不支持)
     * @param user 用户
     * @return 用户id
     */
    public String logout(UserConfig user) {
        Ctp<String> ctp = new Ctp<>();
        return ctp.request(id -> {
            CThostFtdcUserLogoutField field = new CThostFtdcUserLogoutField();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            return mdApi.ReqUserLogout(field, id);
        });
    }

    /**
     * api state
     * @return state
     */
    public ApiState state() {
        ApiState state = new ApiState();
        state.setFronts(MdService.fronts);
        state.setFrontState(MdService.getConnectState());
        state.setUser(MdService.user);
        state.setLoginState(MdService.loginState);
        return state;
    }

    /**
     * ctp 订阅合约
     */
    public List<String> subscribe(List<String> instruments) {
        log.info("行情订阅开始");
        if (instruments == null || instruments.isEmpty()) {
            log.warn("行情订阅: 没有订阅合约，跳过订阅");
            return instruments;
        }
        // 订阅
        instruments = instruments.stream().distinct().collect(Collectors.toList());
        String[] ids= instruments.toArray(new String[0]);
        if (appConfig.getSchedule().getSubscribe()) {
            int code = mdApi.SubscribeMarketData(ids, ids.length);
            log.info("行情订阅 {} 条, code: {}", ids.length, code);
        } else {
            log.info("行情订阅 {} 条, 订阅关闭", ids.length);
        }

        if (!appConfig.getSchedule().getSaveQuotation()) {
            log.info("行情订阅 {} 条, 行情保存关闭", ids.length);
        }

        return instruments;
    }

    /**
     * ctp 取消订阅合约
     */
    public void unsubscribe(List<String> instruments) {
        log.info("instruments unsubscribe start");
        if (instruments.isEmpty()) {
            log.error("instrument unsubscribe failure: no instruments found, unsubscribe skip");
            return;
        }
        // 取消订阅
        String[] ids= instruments.toArray(new String[0]);
        mdApi.UnSubscribeMarketData(ids, ids.length);
        log.info("instruments unsubscribe total {} ", ids.length);
    }

    /**
     * 下载备份的市场行情文件
     * @apiNote 从主服务器下载压缩的行情文件到backup文件夹，并解压缩到history文件夹，
     * 解压成功后删除主服务上的压缩文件
     * @throws MyException exception
     * @throws IOException exception
     */
    public void download() throws MyException, IOException {
        // 列出所有行情文件
        Result<List<String>> result = marketClient.listTar();

        List<String> fileNames = result.getData();
        log.info("市场行情文件下载开始, 文件数量: {}", fileNames.size());

        for (String fileName : fileNames) {
            Path backupPath = Paths.get(DataService.BACK_UP, fileName);
            if (backupPath.toFile().exists()) {
                log.info("市场行情文件已存在: {}", fileName);
                continue;
            }

            // 下载
            downloadFile(fileName, backupPath);

            // 解压
            Path targetPath = Paths.get(DataService.HISTORY_DIR);
            CompressUtils.uncompress(backupPath, targetPath);

            // 删除主服务器文件
            marketClient.marketFileDelete(fileName);
        }
    }

    /**
     * 下载文件
     * @param fileName 下载源文件
     * @param targetPath 下载目的文件
     * @throws MyException exception
     */
    private void downloadFile(String fileName, Path targetPath) throws MyException {
        if (!targetPath.getParent().toFile().exists()) {
            boolean isSuccess = targetPath.getParent().toFile().mkdirs();
            if (!isSuccess) {
                throw new MyException("创建下载文件夹失败: " + targetPath);
            }
        }
        log.info("文件下载开始: {}", fileName);
        long contentLength;
        try (Response response = marketClient.marketFileDownload(fileName)) {
            if (response.status() != 200) {
                throw new MyException("市场行情文件下载失败: " + fileName + " " + response.body().toString());
            }
            String[] contentLengths = response.headers().get("Content-Length").toArray(new String[0]);
            contentLength = Long.parseLong(contentLengths[0]);

            // 下载进度时间起点， 每隔15s打印进度
            long start = System.currentTimeMillis();
            try (
                    FileOutputStream fos = new FileOutputStream(targetPath.toFile());
                    BufferedInputStream bis = new BufferedInputStream(response.body().asInputStream())
            ) {
                // 文件下载
                byte[] buffer = new byte[8096];
                int len;
                long download = 0;
                long downloadInMinute = 0;
                NumberFormat nf = NumberFormat.getNumberInstance();
                nf.setMaximumFractionDigits(2);

                while ((len = bis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    download += len;
                    downloadInMinute += len;
                    if ((start + 15 * 1000) > System.currentTimeMillis()) {
                        continue;
                    }

                    log.info("{} progress: {} / {}kb/s", fileName,
                            nf.format(download * 100.0 / contentLength) + "%",
                            nf.format(downloadInMinute / 1024 / 60)
                    );
                    downloadInMinute = 0;
                    // 重置打印时间
                    start = System.currentTimeMillis();
                }

            } catch (IOException e) {
                throw new MyException(e.getMessage());
            }
        }

        if (contentLength != targetPath.toFile().length()){
            // 下载失败, 删除下载的文件
            if (targetPath.toFile().delete()) {
                log.error("文件{}删除失败", targetPath);
            }

            throw new MyException("文件" + fileName + "下载失败, 下载文件长度不匹配");
        }

        log.info("文件下载完成: {}", fileName);
    }
}

