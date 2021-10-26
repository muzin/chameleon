package cn.muzin.chameleon;

import cn.muzin.chameleon.annotation.ChameleonTransform;

/**
 * @author sirius
 * @since 2021/10/22
 */
@ChameleonTransform(dest = { BInnerStruct.class })
public class AInnerStruct {

    private String ppp;

    public String getPpp() {
        return ppp;
    }

    public void setPpp(String ppp) {
        this.ppp = ppp;
    }
}
