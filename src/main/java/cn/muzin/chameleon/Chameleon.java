package cn.muzin.chameleon;

import cn.muzin.chameleon.exception.ChameleonTransformException;
import cn.muzin.chameleon.pair.StructPair;
import cn.muzin.chameleon.pair.StructToMultiPair;
import cn.muzin.chameleon.pair.StructToOnePair;
import cn.muzin.chameleon.selector.EnvironmentAdaptSelector;
import cn.muzin.chameleon.trainer.EnvironmentAdaptTrainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Chameleon
 *
 * <p>Chameleon 用于实现两个不同类型之间的转换。</p>
 * <p>在转换之前，需要学习训练两个模型间的相互转换。</p>
 *
 * <p>原理：</p>
 * <p>通过 javassist 生成两个类型之间相互转化的字节码类，加载到JVM中，进行缓存；</p>
 * <p>在转换的时候，根据两个对象的Class，找到转化两者的动态实现类，调用生成的方法，完成转换。</p>
 *
 * @author sirius
 * @since 2021/10/22
 */
public class Chameleon {

    private static final String SYSTEM_TMP_DIR = System.getProperty("java.io.tmpdir");

    private static final String DEFAULT_TRANSFORM_PACKAGE_PREFIX = "cn.muzin.chameleon.transform";

    private volatile Map<Class, Map<Class, Environment>> environments = new ConcurrentHashMap<>();

    private volatile EnvironmentAdaptTrainer environmentAdaptTrainer = null;

    private volatile String tmpdir = SYSTEM_TMP_DIR;

    private volatile String packagePrefix = DEFAULT_TRANSFORM_PACKAGE_PREFIX;

    /**
     * 记录 预加载 类转换的选择器，如果有 按照选择器的规则进行预定义转换类
     */
    private volatile List<EnvironmentAdaptSelector> selectors = new ArrayList<>();

    public Chameleon(){
        environmentAdaptTrainer = new EnvironmentAdaptTrainer(this);
    }

    public void readapt(Class tClass, Class rClass){
        if(tClass == rClass){
            return;
        }

        if(!environments.containsKey(tClass)) {
            synchronized (environments) {
                if(!environments.containsKey(tClass)) {
                    environments.put(tClass, new ConcurrentHashMap<>());
                }
            }
        }

        Map<Class, Environment> environmentMap = environments.get(tClass);

        synchronized (environmentMap) {
            Map<Class, Environment> classEnvironmentMap = environmentAdaptTrainer.train(tClass, rClass);
            Environment tClassToRClassEnvironment = classEnvironmentMap.get(tClass);
            Environment rClassToTClassEnvironment = classEnvironmentMap.get(rClass);
            adaptEnvironment(tClass, rClass, tClassToRClassEnvironment);
            adaptEnvironment(rClass, tClass, rClassToTClassEnvironment);
        }
    }

    /**
     * 适应两个类型的相互转换
     *
     * 如果已经存在两个类型类型的相互转换将跳过，
     * 如果类被修改过，需要调用`readapt`进行重新适应
     *
     * @param tClass 源Class
     * @param rClass 目标Class
     */
    public void adapt(Class tClass, Class rClass){
        if(existsEnvironment(tClass, rClass)){
            return;
        }
        readapt(tClass, rClass);
    }

    private <T, R> void adaptEnvironment(Class<T> tClass, Class<R> rClass, Environment environment){
        Map<Class, Environment> tClassEnvironmentMap = null;
        if(!environments.containsKey(tClass)){
            tClassEnvironmentMap = new ConcurrentHashMap<>();
            environments.put(tClass, tClassEnvironmentMap);
        }else{
            tClassEnvironmentMap = environments.get(tClass);
        }
        tClassEnvironmentMap.put(rClass, environment);
    }

    private <T, R> Boolean existsEnvironment(Class<T> tClass, Class<R> rClass){
        if(environments.containsKey(tClass)){
            Map<Class, Environment> tClassEnvironmentMap = environments.get(tClass);
            if(tClassEnvironmentMap == null){
                return false;
            }
            return tClassEnvironmentMap.containsKey(rClass);
        }else{
            return false;
        }
    }

    public void setTmpdir(String tmpdir){
        this.tmpdir = tmpdir;
        environmentAdaptTrainer.setTmpDir(tmpdir);
    }

    public String getTmpdir(){
        return this.tmpdir;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
        environmentAdaptTrainer.setPackagePrefix(packagePrefix);
    }

    public Environment getEnvironment(Class<?> sourceClass, Class<?> destClass){
        Map<Class, Environment> environmentMap = this.environments.get(sourceClass);
        if(environmentMap == null){
            return null;
        }
        return environmentMap.get(destClass);
    }

    /**
     * 添加 环境适应 选择器
     * @param selector 环境适应选择器
     */
    public void addEnvironmentAdaptSelector(EnvironmentAdaptSelector selector){
        this.selectors.add(selector);
    }

    /**
     * 添加 环境适应 选择器
     * @return EnvironmentAdaptSelector 集合
     */
    public List<EnvironmentAdaptSelector> getEnvironmentAdaptSelectors(){
        return this.selectors;
    }

    /**
     * 配置了 环境适应 选择器 等信息后，
     * 需要调用 ready 方法进行加载。
     */
    public void ready(){
        readyEnvironmentAdaptSelectors();
    }

    private void readyEnvironmentAdaptSelectors(){
        List<EnvironmentAdaptSelector> environmentAdaptSelectors = this.getEnvironmentAdaptSelectors();
        if(environmentAdaptSelectors != null && environmentAdaptSelectors.size() > 0){
            Map<Class, StructToMultiPair> structToMultiPairMap = new HashMap<>();
            for(EnvironmentAdaptSelector environmentAdaptSelector : environmentAdaptSelectors){
                List<StructPair> structPairs = environmentAdaptSelector.selector();
                for(StructPair structPair : structPairs){
                    Class mainStruct = structPair.getMainStruct();
                    StructToMultiPair tmpPair = null;
                    if(!structToMultiPairMap.containsKey(mainStruct)){
                        tmpPair = new StructToMultiPair(mainStruct);
                        structToMultiPairMap.put(mainStruct, tmpPair);
                    }else{
                        tmpPair = structToMultiPairMap.get(mainStruct);
                    }

                    if(structPairs instanceof StructToOnePair){
                        StructToOnePair tmpStructToOnePair = (StructToOnePair) structPair;
                        Class tmpStruct = tmpStructToOnePair.getStruct();
                        if(tmpStruct != null) {
                            tmpPair.addStruct(tmpStruct);
                        }
                    }else if(structPair instanceof StructToMultiPair){
                        StructToMultiPair tmpStructToMultiPair = (StructToMultiPair) structPair;
                        Set<Class> tmpStructSet = tmpStructToMultiPair.getStructSet();
                        if(tmpStructSet != null) {
                            tmpPair.addStructs(tmpStructSet);
                        }
                    }
                }
            }

            Collection<StructToMultiPair> structToMultiPairs = structToMultiPairMap.values();

            CountDownLatch countDownLatch = new CountDownLatch(structToMultiPairs.size());
            structToMultiPairs.parallelStream().forEach(structToMultiPair->{
                Class mainStruct = structToMultiPair.getMainStruct();
                Set<Class> structSet = structToMultiPair.getStructSet();

                for(Class structClass : structSet){
                    this.adapt(mainStruct, structClass);
                }

                countDownLatch.countDown();

            });

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public <T, R> void transform(T source, R dest){
        transform(source, dest, false);
    }

    public <T, R> void transform(T source, R dest, boolean adaptationStructureMismatch){
        if(source == null || dest == null){ return; }

        Class<?> sourceClass = source.getClass();
        Class<?> destClass = dest.getClass();

        Boolean exists = existsEnvironment(sourceClass, destClass);
        if(!exists) {
            adapt(sourceClass, destClass);
        }

        Environment environment = getEnvironment(sourceClass, destClass);
        if(environment == null){
            throw new ChameleonTransformException("Environment of structure conversion not found");
        }

        environment.transform(source, dest, adaptationStructureMismatch);
    }


    public <T, R> List<R> transform(Collection<T> source, Class<R> destClass){
        return transform(source, destClass, false);
    }

    public <T, R> List<R> transform(Collection<T> source, Class<R> destClass, boolean adaptationStructureMismatch){
        ArrayList<R> list = new ArrayList<>();

        if(source == null
                || (source != null && source.size() == 0)
                || destClass == null){
            return list;
        }

        Iterator<T> tmpIterator = source.iterator();
        T first = tmpIterator.next();

        Class<?> sourceGenericClass = first.getClass();

        Boolean exists = existsEnvironment(sourceGenericClass, destClass);
        if(!exists) {
            adapt(sourceGenericClass, destClass);
        }

        Environment environment = getEnvironment(sourceGenericClass, destClass);
        if(environment == null){
            throw new ChameleonTransformException("Environment of structure conversion not found");
        }

        for(T item : source) {
            R destInstance = null;
            try {
                destInstance = destClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
                break;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                break;
            }
            environment.transform(item, destInstance, adaptationStructureMismatch);
            list.add(destInstance);
        }
        return list;
    }


    public <T, R> R transform(T source, Class<R> destClass){
        return transform(source, destClass, false);
    }

    public <T, R> R transform(T source, Class<R> destClass, boolean adaptationStructureMismatch){
        try {
            R destInstance = destClass.newInstance();
            transform(source, destInstance, adaptationStructureMismatch);
            return destInstance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
