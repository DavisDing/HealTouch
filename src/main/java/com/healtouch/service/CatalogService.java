package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.Permission;
import com.healtouch.model.TreatmentProject;
import com.healtouch.model.UserSession;
import com.healtouch.util.Checks;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CatalogService {
    public static class Category { public long id; public String name; public String code; public Long parentId; public boolean active; @Override public String toString(){return name;} }
    private final DataSource dataSource; private final AuditLogDao audit=new AuditLogDao();
    public CatalogService(DataSource dataSource){this.dataSource=dataSource;}
    public List<Category> categories(boolean enabledOnly){List<Category> result=new ArrayList<Category>();String sql="SELECT id,name,code,parent_id,active FROM project_category "+(enabledOnly?"WHERE active=1 ":"")+"ORDER BY sort_order,id";try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next()){Category v=new Category();v.id=rs.getLong(1);v.name=rs.getString(2);v.code=rs.getString(3);long parent=rs.getLong(4);v.parentId=rs.wasNull()?null:parent;v.active=rs.getBoolean(5);result.add(v);}return result;}catch(SQLException e){throw new IllegalStateException("查询分类失败",e);}}
    public List<TreatmentProject> activeProjects(){return projects(true);}
    public List<TreatmentProject> projects(boolean enabledOnly){List<TreatmentProject> result=new ArrayList<TreatmentProject>();String sql="SELECT p.*,c.name category_name FROM treatment_project p JOIN project_category c ON c.id=p.category_id "+(enabledOnly?"WHERE p.active=1 AND c.active=1 ":"")+"ORDER BY c.sort_order,p.name";try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement(sql);ResultSet rs=ps.executeQuery()){while(rs.next())result.add(map(rs));return result;}catch(SQLException e){throw new IllegalStateException("查询项目失败",e);}}
    public long addCategory(UserSession actor,String name,String code,Long parentId,int sort){Authorization.require(actor,Permission.SYSTEM_MANAGE);Checks.required(name,"分类名称");Checks.required(code,"分类编码");return Jdbc.inTransaction(dataSource,c->{try(PreparedStatement ps=c.prepareStatement("INSERT INTO project_category(name,code,parent_id,sort_order) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,name.trim());ps.setString(2,code.trim());if(parentId==null)ps.setNull(3,Types.INTEGER);else ps.setLong(3,parentId);ps.setInt(4,sort);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){rs.next();long id=rs.getLong(1);audit.record(c,actor.userId,"CATEGORY_CREATED","CATEGORY",String.valueOf(id),null,name);return id;}}});}
    public long addProject(UserSession actor,TreatmentProject p){Authorization.require(actor,Permission.SYSTEM_MANAGE);validate(p);return Jdbc.inTransaction(dataSource,c->{try(PreparedStatement ps=c.prepareStatement("INSERT INTO treatment_project(name,code,category_id,price_cents,duration_minutes,description,applicable_population,precautions,active) VALUES(?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,p.name.trim());ps.setString(2,p.code);ps.setLong(3,p.categoryId);ps.setLong(4,p.priceCents);if(p.durationMinutes==null)ps.setNull(5,Types.INTEGER);else ps.setInt(5,p.durationMinutes);ps.setString(6,p.description);ps.setString(7,null);ps.setString(8,null);ps.setBoolean(9,p.active);ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){rs.next();long id=rs.getLong(1);audit.record(c,actor.userId,"PROJECT_CREATED","PROJECT",String.valueOf(id),null,p.name);return id;}}});}
    public void setProjectActive(UserSession actor,long projectId,boolean active){Authorization.require(actor,Permission.SYSTEM_MANAGE);Jdbc.inTransaction(dataSource,c->{try(PreparedStatement ps=c.prepareStatement("UPDATE treatment_project SET active=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")){ps.setBoolean(1,active);ps.setLong(2,projectId);if(ps.executeUpdate()!=1)throw new IllegalArgumentException("治疗项目不存在");}audit.record(c,actor.userId,"PROJECT_STATUS_CHANGED","PROJECT",String.valueOf(projectId),null,String.valueOf(active));return null;});}
    private void validate(TreatmentProject p){if(p==null)throw new IllegalArgumentException("项目不能为空");Checks.required(p.name,"项目名称");if(p.categoryId<=0||p.priceCents<0)throw new IllegalArgumentException("项目分类或价格不正确");}
    private TreatmentProject map(ResultSet rs)throws SQLException{TreatmentProject p=new TreatmentProject();p.id=rs.getLong("id");p.name=rs.getString("name");p.code=rs.getString("code");p.categoryId=rs.getLong("category_id");p.categoryName=rs.getString("category_name");p.priceCents=rs.getLong("price_cents");int mins=rs.getInt("duration_minutes");p.durationMinutes=rs.wasNull()?null:mins;p.description=rs.getString("description");p.active=rs.getBoolean("active");return p;}
}
