package com.yuqing.magic.log.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yuqing
 * @date 2018-01-14
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class AppTest {

    private static final Logger logger = LoggerFactory.getLogger(AppTest.class);

    @Test
    public void test() {
        logger.debug("Debug Test Log");
        logger.info("Info Test Log");
    }

}
