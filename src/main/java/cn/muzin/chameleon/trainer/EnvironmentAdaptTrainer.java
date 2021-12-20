package cn.muzin.chameleon.trainer;

import cn.muzin.chameleon.Chameleon;
import cn.muzin.chameleon.Environment;
import cn.muzin.chameleon.exception.ChameleonTrainException;
import cn.muzin.chameleon.trainer.code.ClassReaderUtil;
import cn.muzin.chameleon.trainer.code.Entity2EntityCodeImpl;
import cn.muzin.chameleon.trainer.code.Entity2MapCodeImpl;
import cn.muzin.chameleon.trainer.code.Map2EntityCodeImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sirius
 * @since 2021/10/22
 */
public class EnvironmentAdaptTrainer {

    private volatile String tmpDir = null;

    private volatile String packagePrefix = "";

    private Chameleon chameleon;

    private volatile Entity2EntityCodeImpl entity2EntityCodeImpl;
    private volatile Entity2MapCodeImpl entity2MapCodeImpl;
    private volatile Map2EntityCodeImpl map2EntityCodeImpl;

    public EnvironmentAdaptTrainer(Chameleon chameleon){
        this.chameleon = chameleon;
        this.packagePrefix = chameleon.getPackagePrefix();
        this.setTmpDir(chameleon.getTmpdir());
    }

    /**
     * 训练 类型转换 环境
     * @param tClass 源Class
     * @param rClass 目标Class
     * @return Environment集合
     */
    public Map<Class, Environment> train(Class tClass, Class rClass){
        HashMap<Class, Environment> map = new HashMap<>();
        Environment tClassToRClassEnvironment = mockEnvironment(tClass, rClass);
        Environment rClassToTClassEnvironment = mockEnvironment(rClass, tClass);
        map.put(tClass, tClassToRClassEnvironment);
        map.put(rClass, rClassToTClassEnvironment);
        return map;
    }



    /**
     * 模拟类型转换环境
     * @param tClass
     * @param rClass
     * @return
     */
    private Environment mockEnvironment(Class tClass, Class rClass){
        try {
            Class<Environment> environmentClass = null;


            if((Map.class.isAssignableFrom(tClass) || ClassReaderUtil.isExtends(tClass, Map.class))
                    && (!Map.class.isAssignableFrom(rClass) && !ClassReaderUtil.isExtends(rClass, Map.class))){
                // 如果 前者 是 Map，后者 是 Entity

                environmentClass = getMap2EntityCodeImpl().generateEnvironmentImpl(tClass, rClass);

            }else if((Map.class.isAssignableFrom(rClass) || ClassReaderUtil.isExtends(rClass, Map.class))
                    && (!Map.class.isAssignableFrom(tClass) && !ClassReaderUtil.isExtends(tClass, Map.class))){
                // 如果 前者 是 Entity, 后者 是 Map

                environmentClass = getEntity2MapCodeImpl().generateEnvironmentImpl(tClass, rClass);

            }else{

                environmentClass = getEntity2EntityCodeImpl().generateEnvironmentImpl(tClass, rClass);

            }

            Environment environment = environmentClass.newInstance();
            environment.setSourceClass(tClass);
            environment.setDestClass(rClass);
            environment.setChameleon(this.chameleon);
            return environment;
        }catch(Exception e){
            throw new ChameleonTrainException(e);
        }
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
        if(getEntity2EntityCodeImpl() != null){
            getEntity2EntityCodeImpl().setTmpDir(tmpDir);
        }
        if(getEntity2MapCodeImpl() != null){
            getEntity2MapCodeImpl().setTmpDir(tmpDir);
        }
        if(getMap2EntityCodeImpl() != null){
            getMap2EntityCodeImpl().setTmpDir(tmpDir);
        }
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    private Entity2EntityCodeImpl getEntity2EntityCodeImpl(){
        if(entity2EntityCodeImpl == null){
            entity2EntityCodeImpl = new Entity2EntityCodeImpl(this.packagePrefix);
        }
        return entity2EntityCodeImpl;
    }

    private Entity2MapCodeImpl getEntity2MapCodeImpl(){
        if(entity2MapCodeImpl == null){
            entity2MapCodeImpl = new Entity2MapCodeImpl(this.packagePrefix);
        }
        return entity2MapCodeImpl;
    }

    private Map2EntityCodeImpl getMap2EntityCodeImpl(){
        if(map2EntityCodeImpl == null){
            map2EntityCodeImpl = new Map2EntityCodeImpl(this.packagePrefix);
        }
        return map2EntityCodeImpl;
    }
}
