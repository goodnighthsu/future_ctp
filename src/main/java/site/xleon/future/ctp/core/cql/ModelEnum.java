package site.xleon.future.ctp.core.cql;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.CaseFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ModelEnum {
    INSTRUMENT("instrument", "InstrumentMapper", "InstrumentEntity");

    @EnumValue
    @JsonValue
    private final String value;
    public final String getValue() {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, value);
    }

    private final String mapper;
    public final String getMapper() {
        return "site.xleon.future.ctp.services.mapper." + this.mapper;
    }

    private final String model;
    public final String getModel() {
        return "site.xleon.future.ctp.models." + this.model;
    }
}
