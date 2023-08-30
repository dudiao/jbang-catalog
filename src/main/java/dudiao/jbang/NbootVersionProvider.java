package dudiao.jbang;

import picocli.CommandLine;

/**
 * 项目版本
 *
 * @author songyinyin
 * @since 2022/8/9 18:07
 */
public class NbootVersionProvider implements CommandLine.IVersionProvider {

    private static final String appVersion = "0.0.1";


    @Override
    public String[] getVersion() throws Exception {
        String nbootCliVersion = String.format(":: Nboot-cli  :: v(%s)", appVersion);
        return new String[]{nbootCliVersion};
    }
}
