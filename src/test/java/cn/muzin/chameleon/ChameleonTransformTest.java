package cn.muzin.chameleon;

import cn.muzin.chameleon.selector.ChameleonTransformEnvironmentAdaptSelector;
import cn.muzin.chameleon.util.ChameleonUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sirius
 * @since 2021/10/26
 */
public class ChameleonTransformTest {

    public static void main(String[] args) {

        long st = 0;
        long et = 0;

        // 设置 class 文件存储位置，不设置时，默认在临时目录下，便于debug时，查看class的情况。
        ChameleonUtil.setTmpDir("/Users/sirius/bucket/project/IdeaProjects/chameleon/target/dclass");

        // 配置 注解适配选择器
        ChameleonUtil.addEnvironmentAdaptSelector(
                new ChameleonTransformEnvironmentAdaptSelector()
                        .addBasePackage("cn.muzin.chameleon")
        );

        st = System.currentTimeMillis();

        // 开始根据注解 预加载 转换类
        ChameleonUtil.ready();

        et = System.currentTimeMillis();
        System.out.println((et - st) + " ms ready");


        AStruct aStruct = new AStruct();
        AInnerStruct aInnerStruct = new AInnerStruct();
        aInnerStruct.setPpp("asdf");
        aStruct.setAge("23");
        aStruct.setName("23");
        aStruct.setHeight("23");
        aStruct.setIdcard("23");
        aStruct.setPhoto("23");
        aStruct.setSchool("23");
        aStruct.setWeight("23");
        aStruct.setNamec("23");
        aStruct.setInner(aInnerStruct);
        aStruct.setTtt(123);

        List<Long> longs = new ArrayList<Long>();
        longs.add(123L);
        longs.add(127L);
        longs.add(125L);

        aStruct.setSignList(longs);
        aStruct.setStrList(longs);

        ArrayList<AInnerStruct> aInnerStructs = new ArrayList<>();
        aInnerStructs.add(aInnerStruct);
        aInnerStructs.add(aInnerStruct);
        aStruct.setInnerList(aInnerStructs);

        aStruct.setDecimal(new BigDecimal("3.1415926579"));

        BStruct bStruct1 = ChameleonUtil.transform(aStruct, BStruct.class, true);

        Map mapStruct = ChameleonUtil.transform(aStruct, Map.class, true);

        AStruct a2Struct = ChameleonUtil.transform(mapStruct, AStruct.class, true);

        for(int o = 0; o < 1; o++) {
            st = System.currentTimeMillis();
            for (int i = 0; i < 1; i++) {
                BStruct bStruct = ChameleonUtil.transform(aStruct, BStruct.class, true);
                System.out.println(bStruct);
            }
            et = System.currentTimeMillis();
            System.out.println((et - st) + " ms transform");
        }

    }

}
