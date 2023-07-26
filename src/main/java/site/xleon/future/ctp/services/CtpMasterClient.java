package site.xleon.future.ctp.services;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import site.xleon.future.ctp.models.Result;

import java.util.List;

/**
 * 调用位于公网的ctp主服务
 * 提供行情文件下载服务
 */
@FeignClient(name = "ctp" , url="http://124.221.2.131:8800")
public interface CtpMasterClient {
    @GetMapping(value = "/market/tar")
    Result<List<String>> listTar();

    /**
     * 下载行情文件
     * @param fileName 文件名
     * @return 文件流
     */
    @GetMapping(value = "/market/download")
    Response marketFileDownload(@RequestParam String fileName);

    /**
     * 删除行情文件
     * @param fileName 文件名
     * @return Result
     */
    @GetMapping(value = "/market/delete")
    Result<String> marketFileDelete(@RequestParam String fileName);
}
