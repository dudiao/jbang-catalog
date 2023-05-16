package io.github.dudiao.jbang;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * @author songyinyin
 * @since 2023/5/16 17:36
 */
public abstract class BaseCommand implements Callable<Integer> {

    @Override
    public Integer call() throws IOException {
        return doCall();
    }

    public abstract Integer doCall() throws IOException;
}
