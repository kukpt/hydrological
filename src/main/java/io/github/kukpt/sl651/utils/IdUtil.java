package io.github.kukpt.sl651.utils;

import java.util.concurrent.atomic.AtomicInteger;

public final class IdUtil {

    private static final AtomicInteger IDX = new AtomicInteger();

    private IdUtil(){
        //no instance
    }

    public static Integer nextId(){
        return IDX.incrementAndGet();
    }

}
