package cn.muzin.chameleon.util;

import cn.muzin.chameleon.Chameleon;
import cn.muzin.chameleon.selector.EnvironmentAdaptSelector;

import java.util.Collection;
import java.util.List;

/**
 * @author sirius
 * @since 2021/10/22
 */
public class ChameleonUtil {

    private static Chameleon chameleon = new Chameleon();

    public static Chameleon getChameleon(){
        return chameleon;
    }

    public static void setTmpDir(String tmpdir){
        chameleon.setTmpdir(tmpdir);
    }

    public static String getTmpDir(){
        return chameleon.getTmpdir();
    }

    public static void setPackagePrefix(String packagePrefix){
        chameleon.setPackagePrefix(packagePrefix);
    }

    public static String getPackagePrefix(){
        return chameleon.getPackagePrefix();
    }

    public static void addEnvironmentAdaptSelector(EnvironmentAdaptSelector selector){
        chameleon.addEnvironmentAdaptSelector(selector);
    }

    public static void ready(){
        chameleon.ready();
    }

    public static <T, R> void transform(T source, R dest){
        chameleon.transform(source, dest);
    }

    public static <T, R> void transform(T source, R dest, Boolean adaptationStructureMismatch){
        chameleon.transform(source, dest, adaptationStructureMismatch);
    }

    public static <T, R> R transform(T source, Class<R> destClass){
        return chameleon.transform(source, destClass, false);
    }

    public static <T, R> R transform(T source, Class<R> destClass, Boolean adaptationStructureMismatch){
        return chameleon.transform(source, destClass, adaptationStructureMismatch);
    }

    public static <T, R> List<R> transform(Collection<T> source, Class<R> destClass){
        return chameleon.transform(source, destClass, false);
    }

    public static <T, R> List<R> transform(Collection<T> source, Class<R> destClass, Boolean adaptationStructureMismatch){
        return chameleon.transform(source, destClass, adaptationStructureMismatch);
    }

}
