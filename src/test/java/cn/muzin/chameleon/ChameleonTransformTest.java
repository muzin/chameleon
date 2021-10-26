package cn.muzin.chameleon;

import cn.muzin.chameleon.selector.ChameleonTransformEnvironmentAdaptSelector;
import cn.muzin.chameleon.util.ChameleonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author sirius
 * @since 2021/10/26
 */
public class ChameleonTransformTest {

    public static void main(String[] args) {

        long st = 0;
        long et = 0;

        ChameleonUtil.setTmpDir("/Users/sirius/bucket/project/IdeaProjects/chameleon/target/dclass");

        ChameleonUtil.addEnvironmentAdaptSelector(
                new ChameleonTransformEnvironmentAdaptSelector()
                        .addBasePackage("cn.muzin.chameleon")
        );

        st = System.currentTimeMillis();

        ChameleonUtil.ready();

        et = System.currentTimeMillis();
        System.out.println((et - st) + " ms ready");


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


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

//        List<Long> longs = new ArrayList<Long>();
//        longs.add(123L);
//        longs.add(127L);
//        longs.add(125L);
//
//        aStruct.setSignList(longs);
//        aStruct.setStrList(longs);
//
//        ArrayList<AInnerStruct> aInnerStructs = new ArrayList<>();
//        aInnerStructs.add(aInnerStruct);
//        aInnerStructs.add(aInnerStruct);
//        aStruct.setInnerList(aInnerStructs);


        for(int o = 0; o < 10; o++) {
            st = System.currentTimeMillis();
            for (int i = 0; i < 10000000; i++) {

                BStruct bStruct = ChameleonUtil.transform(aStruct, BStruct.class, true);
//            System.out.println(bStruct);

            }
            et = System.currentTimeMillis();
            System.out.println((et - st) + " ms test");
        }


    }


}
