package cn.muzin.chameleon.selector;

import cn.muzin.chameleon.annotation.ChameleonTransform;
import cn.muzin.chameleon.pair.StructPair;
import cn.muzin.chameleon.pair.StructToMultiPair;
import cn.muzin.chameleon.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 加载 标注 有 ChameleonTransform 注解 的 环境适配选择器实现
 *
 * @author sirius
 * @since 2021/10/26
 */
public class ChameleonTransformEnvironmentAdaptSelector implements EnvironmentAdaptSelector {

    private List<String> basePackages;

    public ChameleonTransformEnvironmentAdaptSelector(){ }

    public ChameleonTransformEnvironmentAdaptSelector(List<String> basePackages){
        this.basePackages = basePackages;
    }

    public List<StructPair> selector(){

        List<String> basePackages = getBasePackages();

        List<Class<?>> classes = new ArrayList<>();
        for(String basePackage : basePackages) {
            List<Class<?>> classList = ClassUtils.getClassesWithAnnotationFromPackage(basePackage, ChameleonTransform.class);
            classes.addAll(classList);
        }

        ArrayList<StructPair> structPairs = new ArrayList<>();

        for(Class<?> clazz : classes){
            ChameleonTransform chameleonTransform = clazz.getAnnotation(ChameleonTransform.class);
            Class[] destClasses = chameleonTransform.dest();
            StructToMultiPair structToMultiPair = new StructToMultiPair(clazz);
            for(Class destClass : destClasses) {
                structToMultiPair.addStruct(destClass);
            }
            structPairs.add(structToMultiPair);
        }

        return structPairs;
    }

    public List<String> getBasePackages(){
        if(basePackages == null){
            basePackages = new ArrayList<>();
        }
        return basePackages;
    }

    public ChameleonTransformEnvironmentAdaptSelector setBasePackages(List<String> basePackages){
        this.basePackages = basePackages;
        return this;
    }

    public ChameleonTransformEnvironmentAdaptSelector addBasePackage(String basePackage){
        List<String> basePackages = getBasePackages();
        basePackages.add(basePackage);
        return this;
    }

}
