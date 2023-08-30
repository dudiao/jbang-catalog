///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS info.picocli:picocli:4.6.3
//DEPS cn.hutool:hutool-core:5.8.20
//SOURCES cli/MvnClean.java
//SOURCES cli/GitCli.java
//SOURCES cli/BaseCommand.java
//SOURCES NbootVersionProvider.java
//SOURCES Log.java
package dudiao.jbang;

import dudiao.jbang.cli.BaseCommand;
import dudiao.jbang.cli.GitCli;
import dudiao.jbang.cli.MvnClean;
import picocli.CommandLine;

import java.io.IOException;

/**
 * nboot-cli
 *
 * @author songyinyin
 * @since 2022/8/9 15:11
 */
@CommandLine.Command(name = "nboot", mixinStandardHelpOptions = true, versionProvider = NbootVersionProvider.class, description = "常用工具集合",
    subcommands = {MvnClean.class, GitCli.class})
public class nboot extends BaseCommand {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String... args) {
        int exitCode = new CommandLine(new nboot()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer doCall() throws IOException {
        // if the command was invoked without subcommand, show the usage help
        spec.commandLine().usage(System.err);
        return 0;
    }
}
