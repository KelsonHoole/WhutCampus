package cn.hukecn.whutcampus;

/**
 * Created by Administrator on 2016/5/12.
 */
public class UserAccount {
    private String username;
    private String password;
    private boolean status;

    public UserAccount(String username,String password,boolean status){
        this.username = username;
        this.password = password;
        this.status = status;
    }

    public String getUsername(){
        return username;
    }
    public String getPassword(){
        return password;
    }
    public boolean getStatus(){
        return status;
    }
}
