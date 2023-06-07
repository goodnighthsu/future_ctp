package site.xleon.future.ctp.services.mapper;

import site.xleon.future.ctp.models.TradingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface TradingMapper extends BaseMapper<TradingEntity> {

    /**
     * 表是否存在
     * @param database 数据库名
     * @param tableName 表名
     * @return 表数量
     */
    @Select("select count(*) from information_schema.TABLES where table_schema = #{database} and table_name = #{tableName}")
    int existTable(@Param("database") String database, @Param("tableName") String tableName);

    /**
     * 动态创建期权合约表
     * @param tableName trading_{合约名}
     *  期货
     * 上期/能源所	小写+4个数字（rb1909代表rb品种，19年9月份到期）
     * 中金所	    大写+4个数字
     * 郑商所	    大写+3个数字(TA001代表TA品种，20年01月份到期)
     * 大商所	    小写+4个数字
     *
     * 期权
     * 上期所	    小写+四个数字+C（或者P）+行权价，如cu1912C43000
     * 大商所	    小写+四个数字+ -C-（或者-P-）+ 行权价，如c2001-C-1800
     * 郑商所	    大写+三个数字+C（或者P）+行权价，如CF001C11200
     * 中金所	    大写+四个数字+ -C-（或者-P-）+ 行权价，如IO1908-C-2100
     *
     * 组合合约
     * 郑商所跨期	    SPD 第一腿&第二腿 例如: SPD TA009&TA011
     * 郑商所跨品种   IPS 第一腿&第二腿 例如: IPS CF009&CY009
     * 大商所跨期     SP 第一腿&第二腿 例如: SP m2009&m2101
     * 大商所跨品种	SPC 第一腿&第二腿 例如: SPC a2009&m2009
     */
    @Update("create table `${tableName}` (" +
            " `id` int not null auto_increment," +
            " `recv_time` datetime(3) null default null," +
            " `trading_action_time` datetime(3) null default null," +
            " `instrument_id` varchar(31) character set utf8mb4 collate utf8mb4_0900_ai_ci null default null," +
            " `trading_day` varchar(9) character set utf8mb4 collate utf8mb4_0900_ai_ci null default null," +
            " `action_day` varchar(9) character set utf8mb4 collate utf8mb4_0900_ai_ci null default null," +
            " `update_time` varchar(9) character set utf8mb4 collate utf8mb4_0900_ai_ci null default null," +
            " `update_milli_sec` int(0) null default null," +
            " `exchange_id` varchar(9) character set utf8mb4 collate utf8mb4_0900_ai_ci null default null," +
            " `exchange_inst_id` varchar(31) character set utf8mb4 collate utf8mb4_0900_ai_ci null default null," +
            " `last_price` decimal(28, 2) null default null," +
            " `pre_settlement_price` decimal(28, 2) null default null," +
            " `pre_close_price` decimal(28, 2) null default null," +
            " `pre_open_interest` bigint(0) null default null," +
            " `open_price` decimal(28, 2) default null," +
            " `highest_price` decimal(28, 2) null default null," +
            " `lowest_price` decimal(28, 2) default null," +
            " `volume` bigint(0) null default null," +
            " `turnover` decimal(28, 2) null default null," +
            " `open_interest` bigint(0) null default null," +
            " `close_price` decimal(65, 2) null default null," +
            " `settlement_price` decimal(28, 2) null default null," +
            " `upper_limit_price` decimal(28, 2) null default null," +
            " `lower_limit_price` decimal(28, 2) null default null," +
            " `pre_delta` int(0) null default null," +
            " `curr_delta` int(0) null default null," +
            " `bid_price1` decimal(28, 2) null default null," +
            " `bid_volume1` int(0) null default null," +
            " `ask_price1` decimal(28, 2) null default null," +
            " `ask_volume1` int(0) null default null," +
            " `bid_price2` decimal(28, 2) null default null," +
            " `bid_volume2` int(0) null default null," +
            " `ask_price2` decimal(28, 2) null default null," +
            " `ask_volume2` int(0) null default null," +
            " `bid_price3` decimal(28, 2) null default null," +
            " `bid_volume3` int(0) null default null," +
            " `ask_price3` decimal(28, 2) null default null," +
            " `ask_volume3` int(0) null default null," +
            " `bid_price4` decimal(28, 2) null default null," +
            " `bid_volume4` int(0) null default null," +
            " `ask_price4` decimal(28, 2) null default null," +
            " `ask_volume4` int(0) null default null," +
            " `bid_price5` decimal(28, 2) null default null," +
            " `bid_volume5` int(0) null default null," +
            " `ask_price5` decimal(28, 2) null default null," +
            " `ask_volume5` int(0) null default null," +
            " `average_price` decimal(28, 2) null default null," +
            " `pre_tick_power` int(0) null default null," +
            " `pre_tick_volume` int(0) null default null," +
            " `pre_tick_avg_price` int(0) null default null," +
            " primary key (`id`) using btree," +
            " index `index_tradingDay`(`trading_day`, `trading_action_time`) using btree," +
            " index `index_tradingActionTime`(`trading_action_time`, `trading_day`) using btree" +
            ") engine = InnoDB auto_increment = 1 character set = utf8mb4 collate = utf8mb4_0900_ai_ci row_format = dynamic; "
    )
    void createTable(@Param("tableName") String tableName);

    @Select("SELECT id FROM ${tableName} WHERE trading_action_time = #{tradingActionTime}")
    List<TradingEntity> listByActionTime(String tableName, Date tradingActionTime);


}
