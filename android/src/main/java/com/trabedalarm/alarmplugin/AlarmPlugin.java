package com.trabedalarm.alarmplugin;

import com.getcapacitor.Logger;

public class AlarmPlugin {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
