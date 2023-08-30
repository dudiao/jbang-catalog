package dudiao.jbang.cli;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import dudiao.jbang.Log;
import picocli.CommandLine;

import java.io.File;
import java.util.Arrays;

/**
 * 在指定目录下，递归查找所有 git 仓库，执行 git 命令
 *
 * @author songyinyin
 * @since 2023/8/28 18:51
 */
@CommandLine.Command(name = "git", description = "在指定目录下，递归执行 git 命令")
public class GitCli extends BaseCommand {

    @CommandLine.Parameters(index = "0..*", description = "git config parameters")
    private String[] clis;

    @CommandLine.Option(names = {"-f", "--file"}, paramLabel = "<files>", defaultValue = "/Users/songyinyin/77hub-project")
    private File rootFile;

    @Override
    public Integer doCall() {
        File[] files = rootFile.listFiles();
        if (files == null) {
            return 0;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                executeGitConfigCli(file);
            }
        }


        return 0;
    }

    private void executeGitConfigCli(File file) {
        if (!file.isDirectory()) {
            return;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return;
        }
        if (Arrays.asList(file.list()).contains(".git")) {
            String[] gitClis = ArrayUtil.addAll(new String[]{"git"}, clis);
            Log.info("{} => {}", file.getPath(), Arrays.toString(gitClis));
            RuntimeUtil.exec(null, file, gitClis);
        } else {
            for (File itemFile : files) {
                executeGitConfigCli(itemFile);
            }
        }
    }
}
