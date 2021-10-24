package cn.muzin.chameleon.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * 变量名称处理工具类
 *
 * @Author sirius
 * @create 2020/9/29 15:11
 */
public class VariableUtils {

    private static Pattern linePattern = Pattern.compile("_(\\w)");

    private static Pattern humpPattern = Pattern.compile("[A-Z]");

    /**
     * 下划线转驼峰
     */
    public static String lineToHump(String str) {
        str = str.toLowerCase();
        Matcher matcher = linePattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 拼接下划线形式的变量
     * @param strs
     * @return
     */
    public static String toLine(String... strs){
        ArrayList<String> strings = new ArrayList<>();
        for(String str : strs){
            strings.add(str);
        }
        return strings.stream().map(strItem->{
            return firstCharToLower(strItem);
        }).collect(Collectors.joining("_"));
    }

    /**
     * 驼峰转下划线
     *
     * 简单写法，效率低于
     *
     * 推荐: {@link #fastHumpToLine(String)}
     *
     */
    public static String humpToLine(String str) {
        String s = str.replaceAll("[A-Z]", "_$0").toLowerCase();
        return s.startsWith("_") ? s.substring(1) : s;
    }

    /**
     * 驼峰转下划线,效率比上面高
     */
    public static String fastHumpToLine(String str) {
        Matcher matcher = humpPattern.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
        }
        matcher.appendTail(sb);
        String s = sb.toString();
        return s.startsWith("_") ? s.substring(1) : s;
    }

    /**
     * 拼接下划线形式的变量
     * @param strs
     * @return
     */
    public static String toHump(String... strs){
        ArrayList<String> strings = new ArrayList<>();
        for(String str : strs){
            strings.add(str);
        }
        return strings.stream().map(strItem->{
            return firstCharToUpper(strItem);
        }).collect(Collectors.joining());
    }

    /**
     * 首字母大写
     * @param str
     * @return
     */
    public static String firstCharToUpper(String str){
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 首字母大写
     * @param str
     * @return
     */
    public static String firstCharToLower(String str){
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }


}
