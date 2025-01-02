package site.xleon.future.ctp.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.web.bind.annotation.*;
import site.xleon.commons.models.Result;
import site.xleon.future.ctp.config.MyCommonRelation;
import site.xleon.commons.cql.CommonParam;

@Slf4j
@RestController
@RequestMapping("/common")
@AllArgsConstructor
public class CommonController<T> {
    private final SqlSessionFactory sqlSessionFactory;
    private final MyCommonRelation myCommonRelation;

    /**
     * 通用的查询接口
     * @param query 查询条件，是CommonParam的json string
     * @return result
     */
    @GetMapping("")
    public Result<Page<T>> list(
            @RequestParam String query
    ) throws InstantiationException, IllegalAccessException, JsonProcessingException, ClassNotFoundException, site.xleon.commons.cql.MyException {
        ObjectMapper mapper = new ObjectMapper();
        CommonParam param = mapper.readValue(query, CommonParam.class);
        Page<T> result = param.query(sqlSessionFactory, myCommonRelation);
        return Result.success(result);
    }

    /**
     * 通过post方法, 调用通用的查询接口
     * 仅用于开发阶段, 方便postman调试
     * @param param param
     * @return result
     */
    @PostMapping("")
    public Result<Page<T>> list(
            @RequestBody CommonParam param
    ) throws InstantiationException, IllegalAccessException, ClassNotFoundException, site.xleon.commons.cql.MyException {
        Page<T> result = param.query(sqlSessionFactory, myCommonRelation);
        return Result.success(result);
    }
}
