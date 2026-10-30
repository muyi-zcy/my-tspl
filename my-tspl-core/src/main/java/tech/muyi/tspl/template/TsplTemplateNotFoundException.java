package tech.muyi.tspl.template;

import tech.muyi.tspl.TsplException;

/**
 * 当 {@link TsplTemplateSource} 无法解析 location 时抛出。
 */
public class TsplTemplateNotFoundException extends TsplException {

    private final String location;

    public TsplTemplateNotFoundException(String location) {
        super("TSPL template not found: " + location);
        this.location = location;
    }

    public TsplTemplateNotFoundException(String location, Throwable cause) {
        super("TSPL template not found: " + location, cause);
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
