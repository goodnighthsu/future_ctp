package site.xleon.future.ctp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ExchangeEnum {
    SHFE("SHFE", "SHFE"),
    INE("INE", "INE"),
    DCE("DCE", "DCE"),
    CZCE("CZCE", "CZCE"),
    CFFEX("CFFEX", "CFFEX"),
    GFEX("GFEX", "GFEX");

    @Getter
    private final String value;
    @Getter
    private final String label;
}
