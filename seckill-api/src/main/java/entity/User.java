package entity;

import lombok.Data;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

/**
 * User Entity
 */
@Data
@Table(name = "seckill_user")
public class User implements Serializable {

    @Id
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "status")
    private Integer status;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "update_time")
    private Date updateTime;

    /**
     * Check if user is active
     */
    public boolean isActive() {
        return status != null && status == 1;
    }
}
