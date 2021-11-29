package cn.muzin.chameleon;

import java.util.List;

/**
 * @author sirius
 * @since 2021/10/22
 */
public interface Environment {

    Chameleon getChameleon();

    void setChameleon(Chameleon chameleon);

    Class getSourceClass();

    Class getDestClass();

    void setSourceClass(Class sourceClass);

    void setDestClass(Class sourceClass);

    default void transform(Object source, Object dest, boolean adaptationStructureMismatch){
        transform(source, dest, adaptationStructureMismatch, false);
    }

    /**
     * 不同类型转换函数
     *
     * <p><strong>adaptationStructureMismatch</strong> 用于对类型中的嵌套类型不一致,但嵌套类型中字段一致的情况进行适配。 </p>
     *
     * <p>Examples:</p>
     *
     * <pre>
     * public class AStruct {
     *      private String a;
     *      private Integer b;
     *      private AInnerStruct innerStruct;
     *      private List&lt;AInnerStruct&gt; innerList;
     *  }
     *
     *  public class BStruct {
     *      private String a;
     *      private Integer b;
     *      private BInnerStruct innerStruct;
     *      private List&lt;BInnerStruct&gt; innerList;
     *  }
     *
     *  public class AInnerStruct {
     *      private String a;
     *  }
     *
     *  public class BInnerStruct {
     *      private String a;
     *  }
     * </pre>
     * <p>adaptationStructureMismatch 为 true 时，</p>
     * <p>可以适配上面的 AStruct.AInnerStruct 到 BStruct.BInnerStruct 的转换。</p>
     * <p>同时，如果是 List&lt;AStruct.AInnerStruct&gt;、List&lt;BStruct.BInnerStruct&gt; 也可以进行转换。</p>
     *
     * @param source 源类型对象
     * @param dest 目标类型对象
     * @param adaptationStructureMismatch 适配结构不匹配的情况
     * @param skipNull 跳过空值
     */
    void transform(Object source, Object dest, boolean adaptationStructureMismatch, boolean skipNull);

}
