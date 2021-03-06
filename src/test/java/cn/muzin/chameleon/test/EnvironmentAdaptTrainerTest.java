package cn.muzin.chameleon.test;

import cn.muzin.chameleon.Chameleon;
import cn.muzin.chameleon.Environment;
import cn.muzin.chameleon.entity.AInnerStruct;
import cn.muzin.chameleon.entity.AStruct;
import cn.muzin.chameleon.entity.BStruct;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @suthor sirius
 * @since 2021/10/23
 */
public class EnvironmentAdaptTrainerTest {

    @Test
    public void trainerTest(){

        Chameleon chameleon = new Chameleon();
        chameleon.setTmpdir("/Users/sirius/bucket/project/IdeaProjects/chameleon/target/dclass");
        chameleon.setPackagePrefix("cn.muzin.chameleon.transform");
//
//        EnvironmentAdaptTrainer environmentAdaptTrainer = new EnvironmentAdaptTrainer(chameleon);
//
//        Map<Class, Environment> environmentMap = environmentAdaptTrainer.train(AStruct.class, BStruct.class);
//
//        Map<Class, Environment> environmentMap1 = environmentAdaptTrainer.train(AStruct.class, BStruct.class);

        long st = 0;
        long et = 0;

        st = System.currentTimeMillis();


        chameleon.readapt(AStruct.class, BStruct.class);

        et = System.currentTimeMillis();
        System.out.println((et - st) + " ms readapt");


        Environment environment = chameleon.getEnvironment(AStruct.class, BStruct.class);

        System.out.println(environment);


        st = System.currentTimeMillis();

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


        for(int i = 0; i < 1; i++) {

            BStruct bStruct = chameleon.transform(aStruct, BStruct.class, true);
            System.out.println(bStruct);

        }

        et = System.currentTimeMillis();
        System.out.println((et - st) + " ms test");




        // ??????
        // 100000       ignoreStructMismatch       adaptationStructMismatch
        //                  load 147-160ms            load 150-165ms
        //                  run 25-35ms               run 46-58ms

        // ??????
        // 1000000      ignoreStructMismatch       adaptationStructMismatch
        //                  load 141-178ms            load 145-162ms
        //                  run 81-98ms               run 125-152ms

        // ??????
        // 10000000     ignoreStructMismatch       adaptationStructMismatch
        //                  load 141-178ms            load 145-162ms
        //                  run 436-484ms               run 812-940ms

        // ???
        // 100000000    ignoreStructMismatch       adaptationStructMismatch
        //                  load ???-???ms ??????170ms     load ???-???ms   ??????170ms
        //                  run ???-???ms  ??????3.6-4.2s  run ???-???ms    ??????6.2-7s


        //?????? list ?????? ??????

    }

    @Ignore
    @Test
    public void trainerCollectionTest(){

        Chameleon chameleon = new Chameleon();
        chameleon.setTmpdir("/Users/sirius/bucket/project/IdeaProjects/chameleon/target/dclass");
        chameleon.setPackagePrefix("cn.muzin.chameleon.transform");
//
//        EnvironmentAdaptTrainer environmentAdaptTrainer = new EnvironmentAdaptTrainer(chameleon);
//
//        Map<Class, Environment> environmentMap = environmentAdaptTrainer.train(AStruct.class, BStruct.class);
//
//        Map<Class, Environment> environmentMap1 = environmentAdaptTrainer.train(AStruct.class, BStruct.class);

        long st = 0;
        long et = 0;

        st = System.currentTimeMillis();


        chameleon.readapt(AStruct.class, BStruct.class);

        et = System.currentTimeMillis();
        System.out.println((et - st) + " ms readapt");


        Environment environment = chameleon.getEnvironment(AStruct.class, BStruct.class);

        System.out.println(environment);



        int total = 10000000;

        int bulk = 100000;

        ArrayList<ArrayList<AStruct>> aStructsList = new ArrayList<>();

        for(int o = 0; o < (total%bulk > 0 ? (total/bulk+1) : total/bulk ); o++) {
            ArrayList<AStruct> aStructs = new ArrayList<>();
            for (int i = 0; i < bulk; i++) {
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
                aStructs.add(aStruct);
            }
            aStructsList.add(aStructs);
        }

        st = System.currentTimeMillis();

        for(ArrayList aStructs : aStructsList){
            List<BStruct> bStructs = chameleon.transform(aStructs, BStruct.class, true);
            System.out.println("");
        }

        et = System.currentTimeMillis();
        System.out.println((et - st) + " ms test");




        // ??????
        // 100000       ignoreStructMismatch       adaptationStructMismatch
        //                  load 147-160ms            load 150-165ms
        //                  run 25-35ms               run 46-58ms

        // ??????
        // 1000000      ignoreStructMismatch       adaptationStructMismatch
        //                  load 141-178ms            load 145-162ms
        //                  run 81-98ms               run 125-152ms

        // ??????
        // 10000000     ignoreStructMismatch       adaptationStructMismatch
        //                  load 141-178ms            load 145-162ms
        //                  run 436-484ms               run 812-940ms

        // ???
        // 100000000    ignoreStructMismatch       adaptationStructMismatch
        //                  load ???-???ms ??????170ms     load ???-???ms   ??????170ms
        //                  run ???-???ms  ??????3.6-4.2s  run ???-???ms    ??????6.2-7s


        //?????? list ?????? ??????

    }
}
