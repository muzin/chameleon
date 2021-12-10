package cn.muzin.chameleon.trainer.code;

import cn.muzin.chameleon.Chameleon;
import cn.muzin.chameleon.Environment;
import cn.muzin.chameleon.trainer.TrainerConstant;
import cn.muzin.chameleon.util.ClassUtils;
import cn.muzin.chameleon.util.VariableUtils;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @author sirius
 * @since 2021/12/7
 */
public class Map2EntityCodeImpl {
    private volatile String packagePrefix = "";

    private volatile String tmpDir = null;


    public Map2EntityCodeImpl(String packagePrefix){
        this.packagePrefix = packagePrefix;
    }

    public String getPackagePrefix() {
        return packagePrefix;
    }

    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }

    public String getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(String tmpDir) {
        this.tmpDir = tmpDir;
    }

    private ClassPool getClassPool(){
        ClassPool pool = ClassPool.getDefault();
        return pool;
    }

    public Class<Environment> generateEnvironmentImpl(Class tClass, Class rClass)
            throws NotFoundException, CannotCompileException, IOException {
        String tClassSimpleName = tClass.getSimpleName();
        String rClassSimpleName = rClass.getSimpleName();

        String environmentImplClassName =
                packagePrefix + "." + tClassSimpleName + "To" + rClassSimpleName + "EnvironmentImpl";

        ClassPool pool = getClassPool();

        // 创建一个空类
        CtClass cc = pool.makeClass(environmentImplClassName);

        // 实现 environment 接口
        cc.addInterface(pool.get(TrainerConstant.ENVIRONMENT_CLASS_NAME));

        // 添加 EnvironmentImpl 无参的构造函数
        CtConstructor cons = new CtConstructor(new CtClass[]{}, cc);
        cons.setBody("{}");
        cc.addConstructor(cons);

        // 新增一个字段 private Chameleon chameleon;
        CtField chameleonParam = new CtField(pool.get(TrainerConstant.CHAMELEON_CLASS_NAME), "chameleon", cc);
        chameleonParam.setModifiers(Modifier.PRIVATE);
        cc.addField(chameleonParam);
        // 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setChameleon", chameleonParam));
        cc.addMethod(CtNewMethod.getter("getChameleon", chameleonParam));

        // 新增一个字段 private Class sourceClass;
        CtField sourceClassParam = new CtField(pool.get(TrainerConstant.CLASS_NAME), "sourceClass", cc);
        sourceClassParam.setModifiers(Modifier.PRIVATE);
        cc.addField(sourceClassParam);
        // 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setSourceClass", sourceClassParam));
        cc.addMethod(CtNewMethod.getter("getSourceClass", sourceClassParam));

        // 新增一个字段 private Class destClass;
        CtField destClassParam = new CtField(pool.get(TrainerConstant.CLASS_NAME), "destClass", cc);
        destClassParam.setModifiers(Modifier.PRIVATE);
        cc.addField(destClassParam);
        // 生成 getter、setter 方法
        cc.addMethod(CtNewMethod.setter("setDestClass", destClassParam));
        cc.addMethod(CtNewMethod.getter("getDestClass", destClassParam));


        // 创建一个名为transform方法
        String transformCtMethodName = "transform";
        CtClass[] transformCtMethodParams = new CtClass[]{
                pool.get(TrainerConstant.OBJECT_NAME),
                pool.get(TrainerConstant.OBJECT_NAME),
                CtClass.booleanType};

        CtMethod transformCtMethod = new CtMethod(CtClass.voidType,
                transformCtMethodName,
                transformCtMethodParams,
                cc);
        transformCtMethod.setModifiers(Modifier.PUBLIC);

        String transformMethodBody = generateTransformMethodBody();
        transformCtMethod.setBody(transformMethodBody);

        cc.addMethod(transformCtMethod);

        String transform2CtMethodName = "transform";
        CtClass[] transform2CtMethodParams = new CtClass[]{
                pool.get(TrainerConstant.OBJECT_NAME),
                pool.get(TrainerConstant.OBJECT_NAME),
                CtClass.booleanType,
                CtClass.booleanType,
        };

        CtMethod transform2CtMethod = new CtMethod(CtClass.voidType,
                transform2CtMethodName,
                transform2CtMethodParams,
                cc);
        transform2CtMethod.setModifiers(Modifier.PUBLIC);

        String transform2MethodBody = generateTransform2MethodBody(tClass, rClass, transform2CtMethod);
        transform2CtMethod.setBody(transform2MethodBody);

        cc.addMethod(transform2CtMethod);

        // 这里会将这个创建的类对象编译为.class文件
        String tmpDir = getTmpDir();
        cc.writeFile(tmpDir);

        // 获取 class
        Class<Environment> environmentClass = null;
        try {
            environmentClass = (Class<Environment>) cc.toClass();
        }catch(Exception e){
            environmentImplClassName += "$" + new Object().hashCode();
            if(cc.isFrozen()){
                cc.defrost();
            }
            cc.setName(environmentImplClassName);
            environmentClass = (Class<Environment>) cc.toClass();
        }

        if(cc.isFrozen()){
            cc.defrost();
        }

        // 释放 classPool 中的该类
        cc.detach();

        return environmentClass;
    }


    private String generateTransformMethodBody() {
        return "$0.transform($1, $2, $3, " + Chameleon.DEFAULT_SKIP_NULL + ");\n";
    }

    private String generateTransform2MethodBody(Class sourceClass, Class destClass, CtMethod ctMethod) {

        String str = "";
        str += "if($4){";
        str += generateTransform2MethodCodes(sourceClass, destClass, ctMethod, true);
        str += "}else{";
        str += generateTransform2MethodCodes(sourceClass, destClass, ctMethod);
        str += "}";

        return "{\n" + str + "\n}";
    }

    private String generateTransform2MethodCodes(Class sourceClass, Class destClass, CtMethod ctMethod) {
        return generateTransform2MethodCodes(sourceClass, destClass, ctMethod, false);
    }


    /**
     * 生成 transform 方法体
     * @param sourceClass
     * @param destClass
     * @param ctMethod
     * @param genCheckSkipNull 生成 检查是否需要跳过空值的代码
     * @return
     */
    private String generateTransform2MethodCodes(Class sourceClass, Class destClass, CtMethod ctMethod, boolean genCheckSkipNull) {

        String sourceClassSimpleName = sourceClass.getSimpleName();
        String destClassSimpleName = destClass.getSimpleName();
        String sourceClassName = sourceClass.getName();
        String destClassName = destClass.getName();

        ClassPool pool = getClassPool();

        StringBuilder stringBuilder = new StringBuilder();

        String sourceVariableName = "source";
        String destVariableName = "dest";


        // 给 形参 命名
        stringBuilder.append(sourceClassName + " " + sourceVariableName + " = (" + sourceClassName + ") $1;\n");
        stringBuilder.append(destClassName + " " + destVariableName + " = (" + destClassName + ") $2;\n");

        List<Field> destClassFields = ClassReaderUtil.getAllFields(destClass);
        Method[] destClassMethods = ClassReaderUtil.filterPublicMethods(destClass.getMethods());

        Map<String, Field> destClassFieldMap = ClassReaderUtil.fieldsToFieldMap(destClassFields);

        for(Field destClassField : destClassFields){
            String destClassFieldName = destClassField.getName();
            String destClassFieldClassName = destClassField.getType().getName();
            Field sourceClassField = destClassField;
            String sourceClassFieldName = sourceClassField.getName();

            Method writeMethod = ClassReaderUtil.getWriteMethod(destClassField, destClass, destClassMethods);

            if(writeMethod == null){ continue; }

            Class<?> writeMethodParameterType = writeMethod != null
                    ? writeMethod.getParameterTypes()[0]
                    : null;
            Class<?> readMethodReturnType = writeMethodParameterType;

            String writeMethodName = writeMethod.getName();
            String readMethodName = writeMethodName;

            // 如果是集合
            boolean assignableFromReturnTypeList = readMethodReturnType.isAssignableFrom(List.class);
            if(assignableFromReturnTypeList) {
                boolean assignableFromParamTypeList = writeMethodParameterType.isAssignableFrom(List.class);
                if(!assignableFromParamTypeList){ continue; }

                Class writeMethodGenericParameterType = ClassReaderUtil.getGenericClassOfListByGenericParameterType(writeMethod);
                Class readMethodGenericReturnType = writeMethodGenericParameterType;

                String readMethodGenericReturnTypeName = readMethodGenericReturnType.getName();
                String writeMethodGenericParameterTypeName = writeMethodGenericParameterType.getName();


                // 两个泛型类型不同

                String chameleonVariableName = VariableUtils.firstCharToLower(TrainerConstant.CHAMELEON_CLASS_SIMPLE_NAME);
                String readMethodReturnTypeName = readMethodReturnType.getName();
                String writeMethodParameterTypeName = writeMethodParameterType.getName();

                if(writeMethodGenericParameterTypeName.startsWith("java.")){
                    // 如果 目标泛型类型 为 String， 原目标进行 toString
                    if(writeMethodGenericParameterType == String.class){

                        // 检查 是否 是空值 start
                        if(genCheckSkipNull) {
                            stringBuilder.append("if(" + sourceVariableName + ".get(\"" + sourceClassFieldName + "\") != null){");
                        }
                        // 检查 是否 是空值 end

                        // collection toString Field Convert
                        //
                        //
                        // Examples:
                        // Collection<Type> readFieldCollection = source.getReadField();
                        // if(readFieldCollection != null && adaptationStructureMismatch) {
                        //      List<Type> newWriteFieldCollection = new ArrayList<Type>();
                        //      for(Type item : readFieldCollection){
                        //          newWriteFieldCollection.add(item != null ? item.toString() : null);
                        //      }
                        //      dest.setWriteField(newWriteFieldCollection);
                        // }
                        stringBuilder.append(TrainerConstant.LIST_CLASS_NAME + " "
                                + sourceVariableName + sourceClassFieldName + "Collection = (" + TrainerConstant.LIST_CLASS_NAME + ") "
                                + sourceVariableName + ".get(\"" + sourceClassFieldName + "\");\n");
                        stringBuilder.append("if(" + sourceVariableName + sourceClassFieldName + "Collection != null && $3){\n");
                        stringBuilder.append("\t" + TrainerConstant.LIST_CLASS_NAME + " new" + destVariableName + destClassFieldName + "Collection = "
                                + "new " + TrainerConstant.ARRAYLIST_CLASS_NAME + "();\n");
                        stringBuilder.append("int " + sourceVariableName + sourceClassFieldName + "CollectionSize"
                                + " = " + sourceVariableName + sourceClassFieldName + "Collection.size();");
                        stringBuilder.append("\tfor(int i = 0; i < " + sourceVariableName + sourceClassFieldName + "CollectionSize; i++){ \n");
                        stringBuilder.append("\t\t" + TrainerConstant.OBJECT_NAME + " item = " + sourceVariableName + sourceClassFieldName + "Collection.get(i);");
                        stringBuilder.append("\t\tnew" + destVariableName + destClassFieldName + "Collection.add(item != null ? item.toString() : null);\n");
                        stringBuilder.append("\t}\n");
                        stringBuilder.append("\t" + destVariableName + "." + writeMethodName
                                + "(new" + destVariableName + destClassFieldName + "Collection);\n");
                        stringBuilder.append("}\n");

                        // 检查 是否 是空值 start
                        if(genCheckSkipNull) {
                            stringBuilder.append("}");
                        }
                        // 检查 是否 是空值 end
                    }else {
                        // 如果 目标泛型类型 为 其他 java.lang 包下面的类
                        simpleAssignValueConvertForTransformMethodBody(stringBuilder,
                                destVariableName, writeMethodName, destClassFieldClassName,
                                sourceVariableName, sourceClassFieldName,
                                genCheckSkipNull);
                    }
                }else{

                    // 检查 是否 是空值 start
                    if(genCheckSkipNull) {
                        stringBuilder.append("if(" + sourceVariableName + ".get(\"" + sourceClassFieldName + "\") != null){");
                    }
                    // 检查 是否 是空值 end

                    //
                    // Examples:
                    // Collection<Type> readFieldCollection = source.getReadField();
                    // if(readFieldCollection != null && adaptationStructureMismatch) {
                    //      List<WriteField> newWriteFieldCollection = this.chameleon.transform(
                    //              readField,
                    //              WriteField.class,
                    //              adaptationStructureMismatch
                    //              );
                    //      dest.setWriteField(newWriteField);
                    // }
                    stringBuilder.append(TrainerConstant.LIST_CLASS_NAME + " "
                            + sourceVariableName + sourceClassFieldName + "Collection = (" + TrainerConstant.LIST_CLASS_NAME + ")"
                            + sourceVariableName + ".get(\"" + sourceClassFieldName + "\");\n");
                    stringBuilder.append("if(" + sourceVariableName + sourceClassFieldName + "Collection != null && $3){\n");
                    stringBuilder.append("\t" + TrainerConstant.LIST_CLASS_NAME
                            + " new" + destVariableName + destClassFieldName + "Collection = ("
                            + writeMethodParameterTypeName + ") "
                            + "$0." + chameleonVariableName + ".transform("
                            + sourceVariableName + sourceClassFieldName + "Collection, "
                            + writeMethodGenericParameterTypeName + ".class, "
                            + "$3, $4);\n");
                    stringBuilder.append("\t" + destVariableName + "." + writeMethodName
                            + "(new" + destVariableName + destClassFieldName + "Collection);\n");
                    stringBuilder.append("}\n");

                    // 检查 是否 是空值 start
                    if(genCheckSkipNull) {
                        stringBuilder.append("}");
                    }
                    // 检查 是否 是空值 end

                }

            }else{

                // 类型不同

                // 如果 类型 不同，且 type 为 null 跳过
                if(writeMethodParameterType == null
                        || readMethodReturnType == null){
                    continue;
                }

                String chameleonVariableName = VariableUtils.firstCharToLower(TrainerConstant.CHAMELEON_CLASS_SIMPLE_NAME);
                String readMethodReturnTypeName = readMethodReturnType.getName();
                String writeMethodParameterTypeName = writeMethodParameterType.getName();

                if(writeMethodParameterTypeName.startsWith("java.")) {
                    // 如果 WriteField 是 字符串，将 ReadField转换为 toString
                    if(writeMethodParameterTypeName.startsWith("java.lang.String")){
                        // toString Field Convert
                        toStringAssignValueConvertForTransformMethodBody(stringBuilder,
                                destVariableName, writeMethodName,
                                sourceVariableName, sourceClassFieldName);
                    }else{
                        // 如果 目标泛型类型 为 其他 java.lang 包下面的类
                        simpleAssignValueConvertForTransformMethodBody(stringBuilder,
                                destVariableName, writeMethodName, destClassFieldClassName,
                                sourceVariableName, sourceClassFieldName,
                                genCheckSkipNull);
                    }
                }else{

                    // 检查 是否 是空值 start
                    if(genCheckSkipNull) {
                        stringBuilder.append("if(" + sourceVariableName + ".get(\"" + sourceClassFieldName + "\") != null){");
                    }
                    // 检查 是否 是空值 end

                    // Examples:
                    // Type readField = source.getReadField();
                    // if(readField != null && adaptationStructureMismatch) {
                    //      WriteField newWriteField =
                    //              (WriteField) chameleon.transform(readField, WriteField.class, adaptationStructureMismatch);
                    //      dest.setWriteField(newWriteField);
                    // }
                    stringBuilder.append(TrainerConstant.OBJECT_NAME + " "
                            + sourceVariableName + sourceClassFieldName + " = "
                            + sourceVariableName + ".get(\"" + sourceClassFieldName + "\");\n");
                    stringBuilder.append("if(" + sourceVariableName + sourceClassFieldName + " != null && $3){\n");
                    stringBuilder.append("\t" + writeMethodParameterTypeName
                            + " new" + destVariableName + destClassFieldName + " = ("
                            + writeMethodParameterTypeName + ") "
                            + "$0." + chameleonVariableName + ".transform("
                            + sourceVariableName + sourceClassFieldName + ", "
                            + writeMethodParameterTypeName + ".class, "
                            + "$3, $4);\n");
                    stringBuilder.append("\t" + destVariableName + "." + writeMethodName
                            + "(new" + destVariableName + destClassFieldName + ");\n");
                    stringBuilder.append("}\n");

                    // 检查 是否 是空值 start
                    if(genCheckSkipNull) {
                        stringBuilder.append("}");
                    }
                    // 检查 是否 是空值 end
                }

            }

        }

        String str = stringBuilder.toString();

        return "{\n" + str + "\n}";
    }


    /**
     * 简单赋值 代码
     *
     * Examples:
     *  dest.setWriteField(source.getReadField());
     *
     */
    private void simpleAssignValueConvertForTransformMethodBody(StringBuilder stringBuilder,
                                                                String destVariableName,
                                                                String destMethodName,
                                                                String destFieldClassName,
                                                                String sourceVariableName,
                                                                String sourceClassFieldName){
        simpleAssignValueConvertForTransformMethodBody(stringBuilder,
                destVariableName,
                destMethodName,
                destFieldClassName,
                sourceVariableName,
                sourceClassFieldName,
                false);
    }

    private void simpleAssignValueConvertForTransformMethodBody(StringBuilder stringBuilder,
                                                                String destVariableName,
                                                                String destMethodName,
                                                                String destFieldClassName,
                                                                String sourceVariableName,
                                                                String sourceClassFieldName,
                                                                boolean genCheckSkipNull){
        // Examples: dest.setWriteField(source.getReadField());
        if(genCheckSkipNull) {
            stringBuilder.append("if(" + sourceVariableName + ".get(\"" + sourceClassFieldName + "\") != null){");
        }

        stringBuilder.append("if(" + sourceVariableName + ".get(\"" + sourceClassFieldName + "\") instanceof " + destFieldClassName + "){");
        stringBuilder.append(destVariableName + "." + destMethodName + "((" + destFieldClassName + ") "
                + sourceVariableName + ".get(\"" + sourceClassFieldName + "\"));\n");
        stringBuilder.append("}");

        if(genCheckSkipNull){
            stringBuilder.append("}");
        }
    }

    /**
     * 原结果toString后进行赋值
     *
     * Examples:
     *  dest.setWriteField(source.getReadField() != null ? source.getReadField().toString() : null);
     *
     */
    private void toStringAssignValueConvertForTransformMethodBody(StringBuilder stringBuilder,
                                                                  String destVariableName,
                                                                  String destMethodName,
                                                                  String sourceVariableName,
                                                                  String sourceFieldName){

        // Examples: dest.setWriteField(source.getReadField() != null ? source.getReadField().toString() : null);
        stringBuilder.append(destVariableName + "." + destMethodName + "("
                + sourceVariableName + ".get(\"" + sourceFieldName + "\") != null ? "
                + sourceVariableName + ".get(\"" + sourceFieldName + "\").toString() : null);\n");
    }

}
