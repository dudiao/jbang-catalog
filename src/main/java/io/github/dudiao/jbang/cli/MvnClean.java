package io.github.dudiao.jbang.cli;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.ConsoleTable;
import cn.hutool.core.util.StrUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

@Command(name = "mvnClean", mixinStandardHelpOptions = true, version = "mvnClean 0.1",
        description = "清理Maven相关目录")
public class MvnClean extends BaseCommand {

    @CommandLine.Option(names = {"-s", "--snapshot"}, paramLabel = "<files>", defaultValue = "/Users/songyinyin/.m2/repository/com/q7link/application",
        description = "Maven本地仓库下的某个路径，默认为：/Users/songyinyin/.m2/repository/com/q7link/application")
    private final List<File> snapshotFiles = new ArrayList<>();

    @Override
    public Integer doCall() {
      if (CollectionUtil.isNotEmpty(snapshotFiles)) {
        cleanSnapshot(snapshotFiles);
      }
      return 0;
    }

  /**
   * 清理指定目录下，创建时间30天之外的快照
   *
   * @param files 文件目录集合
   */
  private void cleanSnapshot(List<File> files) {
    deleteFiles(files, itemFile -> {
      String name = itemFile.getName();
      if (StrUtil.isNotBlank(name) && name.endsWith("-SNAPSHOT")) {
        Date date = FileUtil.lastModifiedTime(itemFile);
        return DateUtil.compare(date, DateUtil.offsetDay(new Date(), -30)) < 0;
      }
      return false;
    });
  }

  /**
   * 清理 .lastUpdated 文件
   *
   * @param files 文件目录集合
   */
  private void cleanLastUpdated(List<File> files) {
    deleteFiles(files, itemFile -> {
      if (itemFile.isDirectory()) {
        return false;
      }
      String name = itemFile.getName();
      return StrUtil.isNotBlank(name) && name.endsWith(".lastUpdated");
    });
  }

  /**
   * 递归 删除满足条件的file
   *
   * @param files 文件目录集合
   * @param predicate 是否删除文件
   */
  private void deleteFiles(List<File> files, Predicate<File> predicate) {
    System.out.println("文件的路径：" + files);
    ConsoleTable consoleTable = ConsoleTable.create().addHeader("操作", "文件路径", "文件大小", "是否成功");
    consoleTable.setSBCMode(false);
    long totalSize = 0;
    for (File file : files) {
      if (file.isDirectory()) {
        long clean = clean(file, consoleTable, predicate);
        totalSize = totalSize + clean;
      } else {
        System.out.println("请输入一个文件目录：" + file);
      }
    }
    consoleTable.print();
    System.out.println("总共清除文件大小：" + FileUtil.readableFileSize(totalSize));
  }

  /**
   *
   * @param file
   * @param consoleTable
   * @param predicate 是否删除文件
   * @return
   */
  private long clean(File file, ConsoleTable consoleTable, Predicate<File> predicate) {
    File[] files = file.listFiles();
    long totalSize = 0;
    if (files == null) {
      return totalSize;
    }
    for (File itemFile : files) {
      if (predicate.test(itemFile)) {
        long fileSize = FileUtil.size(itemFile);
        consoleTable.addBody("delete", itemFile.getAbsolutePath(), FileUtil.readableFileSize(fileSize), "ok");
        FileUtil.del(itemFile);
        totalSize = totalSize + fileSize;
        continue;
      }
      if (itemFile.isDirectory()) {
        long clean = clean(itemFile, consoleTable, predicate);
        totalSize = totalSize + clean;
      }
    }
    return totalSize;
  }
}
