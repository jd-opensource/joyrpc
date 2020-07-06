参数校验
==
参数校验功能是基于 JSR303 实现的，用户只需标识 JSR303 标准的验证 annotation。
Provider和Consumer都可以独立开启参数校验功能。两者配置无关联性。
支持接口级和方法级的配置。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

### 1.参数bean配置jsr303的annotation

  ```java
  public class ValidationBean implements Serializable {
   
      private static final long serialVersionUID = -200698852458623917L;
   
      @MyAnnotation // 自定义annotation
      private String name;
   
      @Pattern(regexp = "^\\s*\\w+(?:\\.{0,1}[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$")
      private String email;
   
      @Min(18) // 最小值
      @Max(100) // 最大值
      private int age;
      
      public ValidationBean() {
      }
   
      public String getName() {
          return name;
      }
   
      public void setName(String name) {
          this.name = name;
      }
      public int getAge() {
          return age;
      }
   
      public void setAge(int age) {
          this.age = age;
      }
   
      public String getEmail() {
          return email;
      }
   
      public void setEmail(String email) {
          this.email = email;
      }
  }
  ```
###  2.自定义annotation

  ```java
  @Constraint(validatedBy = {MyAnnotationValidator.class}) //指定校验类
  @Documented
  @Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyAnnotation {
   
      String message() default "不正确的状态 , 应该是 'created', 'paid', shipped', closed'其中之一";
   
      Class<?>[] groups() default {};
   
      Class<? extends Payload>[] payload() default {};
  }
  ```
### 3.自定义annotation校验类

  ```java
  public class MyAnnotationValidator implements ConstraintValidator<MyAnnotation, String> {
   
      private final String[] ALL_STATUS = {"created", "paid", "shipped", "closed"};
   
      public void initialize(MyAnnotation status) {
      }
   
      public boolean isValid(String value, ConstraintValidatorContext context) {
          if (Arrays.asList(ALL_STATUS).contains(value))
              return true;
          return false;
      }
  }
  ```
### 4.定义测试接口

  ```java
  import javax.validation.Valid;
   
  public interface DemoService {
      ValidationBean validation(@Valid ValidationBean obj);
  }
  ```
### 5. Consumer调用测试

validation参数值设置为true

  ```
     public class DemoServiceTest {
              
        public void main() throws Exception { 
          ConsumerConfig<DemoService> consumerConfig = initConsumer();
          consumerConfig.setValidation(true);
          CompletableFuture<Void> future = new CompletableFuture<>();
          DemoService service = consumerConfig.refer(future);
          future.get();
           
          try {
              ValidationBean bean = new ValidationBean();
              bean.setName("joyrpc-demo");
              bean.setAge(1);
              bean.setEmail("lafaa.com");
              ValidationBean serverBean = service.validation(bean);
              logger.info(serverBean.getName());
          } catch (Exception e) {
              logger.error("exception:", e);
          }
          Thread.currentThread().join(); 
        }
     }
  ```

