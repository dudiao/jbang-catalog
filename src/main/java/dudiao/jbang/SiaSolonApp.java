package dudiao.jbang;
///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.noear:solon-parent:2.5.0-M1@pom
//DEPS org.noear:solon.config.yaml
//DEPS org.noear:solon.logging.logback
//DEPS info.picocli:picocli:4.6.3
//DEPS cn.hutool:hutool-core:5.8.20
//DEPS cn.hutool:hutool-poi:5.8.20
//DEPS org.apache.poi:poi-ooxml:5.2.3
//DEPS com.googlecode.aviator:aviator:5.3.3
//DEPS org.projectlombok:lombok:1.18.26

//SOURCES sia/event/*.java
//SOURCES sia/*.java
//SOURCES NbootVersionProvider.java
//SOURCES Log.java
//SOURCES nboot.java
//SOURCES utils/*.java
//FILES ../../../resources/*.yml

import dudiao.jbang.sia.NbootCliService;
import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.annotation.PropertySource;
import org.noear.solon.annotation.SolonMain;
import picocli.CommandLine;

import java.util.List;

/**
 * @author songyinyin
 * @since 2023/8/30 18:36
 */
@SolonMain
@PropertySource(value = {"classpath:application-excel.yml", "classpath:application-keyword.yml"})
public class SiaSolonApp {

    public static void main(String[] args){
        SolonApp solonApp = Solon.start(SiaSolonApp.class, args);

        CommandLine commandLine = new CommandLine(new nboot());
        List<NbootCliService> nbootCliServiceList = solonApp.context().getBeansOfType(NbootCliService.class);
        for (NbootCliService nbootCliService : nbootCliServiceList) {
            commandLine.addSubcommand(nbootCliService);
        }
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
