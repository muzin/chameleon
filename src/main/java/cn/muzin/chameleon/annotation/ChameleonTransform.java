package cn.muzin.chameleon.annotation;

import java.lang.annotation.*;

/**
 * 使用 ChameleonTransform 标记提前生成类型转换类
 * @author sirius
 * @since 2021/10/26
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
public @interface ChameleonTransform {

    /**
     * 标注当前类型需要对应的其他类型
     *
     * 配置一个对应关系就可以生成两个类型的相互转化，不需要两个类型相互配置
     * @return class
     */
    Class[] dest();

}
