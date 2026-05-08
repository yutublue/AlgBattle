package org.kob.botrunningsystem.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CodeSecurityScanner {

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        RULES.add(new Rule("System\\.(exit|gc)", "禁止调用 System.exit / System.gc"));
        RULES.add(new Rule("Runtime\\.getRuntime", "禁止获取 Runtime 实例"));
        RULES.add(new Rule("ProcessBuilder", "禁止使用 ProcessBuilder"));
        RULES.add(new Rule("\\.exec\\s*\\(", "禁止调用 exec 执行系统命令"));
        RULES.add(new Rule("java\\.io\\.(File|RandomAccessFile)", "禁止访问文件系统"));
        RULES.add(new Rule("FileWriter|FileReader|FileInputStream|FileOutputStream", "禁止读写文件"));
        RULES.add(new Rule("java\\.net\\.", "禁止网络访问"));
        RULES.add(new Rule("java\\.lang\\.reflect", "禁止使用反射"));
        RULES.add(new Rule("ClassLoader", "禁止操作类加载器"));
        RULES.add(new Rule("new\\s+\\w*\\s*Thread\\s*\\(", "禁止创建线程"));
    }

    /**
     * 扫描用户代码，返回被拦截的规则描述；null 表示通过
     */
    public static String scan(String code) {
        if (code == null || code.isEmpty()) {
            return "Bot 代码为空";
        }
        // 去掉字符串字面量和注释后再扫描，降低误报
        String cleaned = code
                .replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"", "\"\"")
                .replaceAll("//[^\n]*", "")
                .replaceAll("/\\*[\\s\\S]*?\\*/", "");

        for (Rule rule : RULES) {
            if (rule.pattern.matcher(cleaned).find()) {
                return rule.message;
            }
        }
        return null;
    }

    public static List<String> allRules() {
        List<String> msgs = new ArrayList<>();
        for (Rule rule : RULES) {
            msgs.add(rule.message);
        }
        return msgs;
    }

    private static class Rule {
        final Pattern pattern;
        final String message;

        Rule(String regex, String message) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.message = message;
        }
    }
}
