# Chameleon

Chameleon 是一款基于 javassist 动态字节码生成的类型转换工具。

## 解决痛点：
1. Spring 的 BeanUtils 类型转换效率相对不太理想。
2. MapStruct 可以在编译时生成转换类，但是需要使用注解，
   每个类进行转换都需要写注解配置那两个类进行转换。
   
我更希望不需要做任何前期准备，需要的时候自动转换。

后来，1.0.1版本，考虑到如果可以预先加载转换类，效果应该会更好。
于是，我否定了上面痛点的第二点😅，我也实现了通过注解来生成转换类。
与 MapStruct 稍有区别，
在需要转换的类型上添加`@ChameleonTransform`注解，并通过
配置`ChameleonTransformEnvironmentAdaptSelector`选择器
来预加载转换类。见下面的[方式2（通过注解预加载）](方式2（通过注解预加载）)。

## 原理：
1. 在首次转换两个类时，通过 javassist 生成两个类型之间相互转化的字节码类，加载到JVM中，并缓存下来；
2. 根据两个对象的Class，找到缓存中转化两者的动态实现类，调用生成的方法，完成转换。

> 原理跟 MapStruct 相似，生成的转换类中使用Getter/Setter进行赋值，
> MapStruct 和 Chameleon 的效率相当；
> 
> 不同的是 MapStruct 编译时生成转换类，
> Chameleon 运行时根据需要动态生成两者相互转换类；
> 
> Chameleon 与 MapStruct 相比，
> Chameleon的不足大概是，
> 首次动态生成字节码并加载所需要150ms左右；
> Chameleon的优点是，不需要像 MapStruct 一样定义注解就可以直接转换两个类型。

## 支持转换的情景

仅处理 getter/setter/is 函数 

1. 类型相同，直接转换 
   
   1.1 类型为List<?>且泛型类相同，直接转换
   
   1.2 类型为List<?>且泛型类不同，转换泛型类，再赋值
   
   1.3 类型为List<?>且泛型类不同，目标值是String，原值不为空的情况下，将原值 toString 处理
   
2. 类型不同，转换类型，再赋值
   
    2.1 如果目标值是String，原值不为空的情况下，将原值 toString 处理
   

## 使用

### dependency
```xml
<dependency>
   <groupId>cn.muzin</groupId>
   <artifactId>chameleon</artifactId>
   <version>1.0.1</version>
</dependency>
```

### 方式1（惰性加载）：
不预加载转换类，在需要的时候加载转换类

```java
// 1. 根据 Class 进行转换
AStruct aStruct = new AStruct();
// ignore aStruct Code ...
BStruct bStruct = ChameleonUtil.transform(aStruct, BStruct.class);
 
// 第三个参数为true时，子类型不一致，但字段相同，也可以转换
BStruct bStruct = ChameleonUtil.transform(aStruct, BStruct.class, true);

// 2. 值拷贝
BStruct bStruct1 = new BStruct();
ChameleonUtil.transform(aStruct, bStruct1);

// 3. 按照 Class，进行集合的转换
List<AStruct> aStructList = new ArrayList<AStruct>();
List<BStruct> bStructList = ChameleonUtil.transform(aStructList, BStruct.class);
```

### 方式2（通过注解预加载）：
在需要转换的类上面标记`@ChameleonTransform`注解，通过配置`ChameleonTransformEnvironmentAdaptSelector`
选择器，来预加载类型互转的转换类

> 注意：配置完成后，一定要调用`ready`方法！！！

```java
// 配置 注解适配选择器， 扫描指定包下面的所有类（可添加多个包名）
ChameleonUtil.addEnvironmentAdaptSelector(
        new ChameleonTransformEnvironmentAdaptSelector()
        .addBasePackage("cn.muzin.chameleon")
);

// 告诉 Chameleon 已经准备好了，开始配置组建。
ChameleonUtil.ready();

// 开始转换目标对象...
AStruct aStruct = new AStruct();
// ignore aStruct Code ...
BStruct bStruct = ChameleonUtil.transform(aStruct, BStruct.class);
```

### 方式3（自定义预加载）：
实现`EnvironmentAdaptSelector`接口，自定义加载类型转换规则。
根据自己的需要返回类型之间的1对1、1对多关系。


```
//  根据需要返回相应的结构对

StructPair       结构对
   +- StructToOnePair         结构1对1
   +- StructToMultiPair       结构1对多
   
```

示例：

```java

// 实现 EnvironmentAdaptSelector 接口
public class EnvironmentAdaptSelectorImpl implements EnvironmentAdaptSelector {

   public List<StructPair> selector() {
      // your code...
      return new ArrayList<StructPair>();
   }

}

// 通过 Chameleon 或者 ChameleonUtil 添加该适配选择器
ChameleonUtil.addEnvironmentAdaptSelector(
        new EnvironmentAdaptSelectorImpl()
        );

// 告诉 Chameleon 已经准备好了，开始配置组建。
ChameleonUtil.ready();

// 开始转换目标对象...
AStruct aStruct = new AStruct();
// ignore aStruct Code ...
BStruct bStruct = ChameleonUtil.transform(aStruct, BStruct.class);
```