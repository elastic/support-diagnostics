package com.elastic.support.chain;

import com.elastic.support.inspection.InspectionContext;

public interface Command {
    public boolean execute(Context context);
}
