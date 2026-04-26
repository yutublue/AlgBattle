package org.kob.backend.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//这三个注解在运行之后就会在target里面对应的pojo/User里面生成对应的文件
@Data//这个主要是包含getId(), getName(), toString()等等的一些操作
@NoArgsConstructor//这个就是一个无参构造, 比如说在这里构造一个没有参数的User就是public User() {}
@AllArgsConstructor//这个就和上面的相反, 就是一个有参构造
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private Integer rating;
    private String photo;
}
