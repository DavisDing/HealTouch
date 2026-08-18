package com.healtouch.service;

import com.healtouch.dao.AuditLogDao;
import com.healtouch.dao.Jdbc;
import com.healtouch.model.Permission;
import com.healtouch.model.RoleCode;
import com.healtouch.model.UserSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PermissionService {
    private final DataSource dataSource; private final AuditLogDao audit=new AuditLogDao();
    public PermissionService(DataSource dataSource){this.dataSource=dataSource;}
    public Set<Permission> permissionsFor(RoleCode role){
        if(role==RoleCode.ADMIN)return EnumSet.allOf(Permission.class);
        EnumSet<Permission> result=EnumSet.noneOf(Permission.class);
        String sql="SELECT p.code FROM role_permission rp JOIN role r ON r.id=rp.role_id JOIN permission p ON p.id=rp.permission_id WHERE r.code=?";
        try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement(sql)){ps.setString(1,role.name());try(ResultSet rs=ps.executeQuery()){while(rs.next())result.add(Permission.valueOf(rs.getString(1)));}return result;}catch(Exception e){throw new IllegalStateException("读取角色权限失败",e);}
    }
    public Map<RoleCode,Set<Permission>> all(){Map<RoleCode,Set<Permission>> map=new EnumMap<RoleCode,Set<Permission>>(RoleCode.class);for(RoleCode role:RoleCode.values())map.put(role,permissionsFor(role));return map;}
    public void update(UserSession actor,RoleCode role,Set<Permission> permissions){Authorization.require(actor,Permission.SYSTEM_MANAGE);if(role==null)throw new IllegalArgumentException("请选择角色");if(role==RoleCode.ADMIN)throw new IllegalArgumentException("系统管理员为内置角色，不能修改其权限");final EnumSet<Permission> effective=EnumSet.noneOf(Permission.class); if(permissions!=null) effective.addAll(permissions);Jdbc.inTransaction(dataSource,c->{long roleId=roleId(c,role);try(PreparedStatement del=c.prepareStatement("DELETE FROM role_permission WHERE role_id=?")){del.setLong(1,roleId);del.executeUpdate();}try(PreparedStatement add=c.prepareStatement("INSERT INTO role_permission(role_id,permission_id) SELECT ?,id FROM permission WHERE code=?")){for(Permission p:effective){add.setLong(1,roleId);add.setString(2,p.name());add.addBatch();}add.executeBatch();}audit.record(c,actor.userId,"ROLE_PERMISSION_CHANGED","ROLE",role.name(),null,effective.toString());return null;});}
    private long roleId(Connection c,RoleCode role)throws Exception{try(PreparedStatement ps=c.prepareStatement("SELECT id FROM role WHERE code=?")){ps.setString(1,role.name());try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("角色不存在");return rs.getLong(1);}}}
}
