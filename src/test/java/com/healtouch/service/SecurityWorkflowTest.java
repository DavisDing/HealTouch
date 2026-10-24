package com.healtouch.service;

import static org.junit.Assert.*;
import com.healtouch.config.Database;
import com.healtouch.model.*;
import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class SecurityWorkflowTest {
  @Rule public TemporaryFolder folder=new TemporaryFolder();
  private Database db;
  private AuthService auth;
  private UserSession admin;
  private UserService users;
  private BillingService billing;
  private PatientService patients;
  private CatalogService catalog;
  private RefundService refunds;
  @Before public void setup() throws Exception {
    db=new Database(new File(folder.getRoot(),"healtouch.db"));
    auth=new AuthService(db.dataSource());auth.initializeAdministrator("AdminTest@123");admin=auth.login("admin","AdminTest@123");
    users=new UserService(db.dataSource());billing=new BillingService(db.dataSource());patients=new PatientService(db.dataSource());
    catalog=new CatalogService(db.dataSource());refunds=new RefundService(db.dataSource());
  }
  @After public void close(){if(db!=null)db.close();Authorization.configure(null);}
  private UserSession user(String login,RoleCode role) {
    users.create(admin,login,login,"13900000001","UserTest@123",EnumSet.of(role));return auth.login(login,"UserTest@123");
  }
  private long bill() {
    Patient patient=new Patient();patient.name="测试患者";patient.gender=Gender.UNKNOWN;patient.idType="测试证件";patient.idNumber="ID1";
    patient.birthDate=LocalDate.of(1990,1,1);patient.phone="13800000001";
    long patientId=patients.create(admin,patient);
    TreatmentProject project=new TreatmentProject();project.name="项目";project.categoryId=catalog.categories(admin,true).get(0).id;project.priceCents=10000;project.active=true;
    long projectId=catalog.addProject(admin,project);UserSession therapist=user("therapist",RoleCode.THERAPIST);
    return billing.createPendingBill(admin,patientId,LocalDate.now(),therapist.userId,"",Arrays.asList(new TreatmentLine(projectId,"项目",10000,1)));
  }
  private void sql(String sql) throws Exception {try(Connection c=db.dataSource().getConnection();Statement st=c.createStatement()){st.execute(sql);}}
  private long number(String sql) throws Exception {try(Connection c=db.dataSource().getConnection();Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)){rs.next();return rs.getLong(1);}}
  private String text(String sql) throws Exception {try(Connection c=db.dataSource().getConnection();Statement st=c.createStatement();ResultSet rs=st.executeQuery(sql)){rs.next();return rs.getString(1);}}

  @Test public void disabledAdminCannotChargeOrManage() {
    UserSession other=user("otherAdmin",RoleCode.ADMIN);users.setActive(other,admin.userId,false);
    assertThrows(Authorization.SessionExpiredException.class,()->Authorization.require(admin,Permission.BILL_CHARGE));
    assertThrows(Authorization.SessionExpiredException.class,()->users.listAll(admin));
  }
  @Test public void roleChangeAndPasswordChangeRevokeOldSessions() throws Exception {
    UserSession reception=user("front",RoleCode.RECEPTION);
    sql("DELETE FROM user_role WHERE user_id="+reception.userId);
    assertThrows(Authorization.SessionExpiredException.class,()->Authorization.require(reception,Permission.PATIENT_VIEW));
    UserSession secondLogin=auth.login("admin","AdminTest@123");
    auth.changePassword(admin,"AdminTest@123","Different@123");
    assertThrows(Authorization.SessionExpiredException.class,()->users.listAll(secondLogin));
    assertThrows(Authorization.SessionExpiredException.class,()->users.listAll(admin));
  }
  @Test public void idleClockExpiresWithoutBeingExtendedByRefresh() {
    AtomicLong clock=new AtomicLong(0);
    UserSession timed=new UserSession(admin.userId,admin.userCode,admin.name,admin.roles,false,admin.credentialVersion,clock::get);
    Authorization.refresh(timed);clock.set(UserSession.IDLE_TIMEOUT_NANOS-1);Authorization.refresh(timed);
    clock.incrementAndGet();assertThrows(Authorization.SessionExpiredException.class,()->Authorization.refresh(timed));
    timed.touch();assertFalse(timed.isValid());
  }
  @Test public void reservedPermissionsCannotBeGranted() {
    PermissionService permissions=new PermissionService(db.dataSource());
    assertThrows(IllegalArgumentException.class,()->permissions.update(admin,RoleCode.RECEPTION,EnumSet.of(Permission.SYSTEM_MANAGE)));
    assertThrows(IllegalArgumentException.class,()->permissions.update(admin,RoleCode.FINANCE,EnumSet.of(Permission.DISCOUNT_APPLY)));
    assertThrows(SQLException.class,()->sql("INSERT INTO role_permission SELECT r.id,p.id FROM role r,permission p WHERE r.code='RECEPTION' AND p.code='SYSTEM_MANAGE'"));
  }
  @Test public void therapistCannotReadFinancialAndUserLists() {
    UserSession therapist=user("therapist",RoleCode.THERAPIST);
    assertThrows(SecurityException.class,()->billing.list(therapist,"",null));
    assertThrows(SecurityException.class,()->refunds.list(therapist));
    assertThrows(SecurityException.class,()->users.listAll(therapist));
    assertThrows(SecurityException.class,()->catalog.categories(therapist,false));
    assertFalse(catalog.categories(therapist,true).isEmpty());
    assertThrows(SecurityException.class,()->new DashboardService(db.dataSource()).report(therapist));
  }
  @Test public void listIsReadOnlyAndSystemExpiryIsAuditedOnce() throws Exception {
    long id=bill();sql("UPDATE bill SET created_at=datetime('now','-31 minutes') WHERE id="+id);
    assertEquals(BillStatus.PENDING_PAYMENT,billing.list(admin,"",null).get(0).status);
    assertEquals(1,billing.voidExpiredPending());assertEquals(0,billing.voidExpiredPending());
    assertEquals(BillStatus.VOIDED,billing.list(admin,"",null).get(0).status);
    assertEquals(1,number("SELECT COUNT(*) FROM audit_log WHERE action='BILL_EXPIRED_SYSTEM' AND operator_id IS NULL"));
  }
  @Test public void expiredCheckoutCannotWritePayments() throws Exception {
    long id=bill();sql("UPDATE bill SET created_at=datetime('now','-31 minutes') WHERE id="+id);
    assertThrows(IllegalStateException.class,()->billing.checkout(admin,id,Arrays.asList(new PaymentLine(PaymentMethod.CASH,10000))));
    assertEquals(0,number("SELECT COUNT(*) FROM payment"));assertEquals("VOIDED",text("SELECT status FROM bill WHERE id="+id));
  }
  @Test public void discountNeedsApprovalAndCannotMakeAZeroBill() throws Exception {
    long id=bill();UserSession front=user("front",RoleCode.RECEPTION);
    assertThrows(IllegalArgumentException.class,()->billing.applyDiscount(admin,id,10000,"全免"));
    assertThrows(SecurityException.class,()->billing.applyDiscount(front,id,100,"未经授权"));
    billing.requestDiscount(front,id,1000,"申请优惠");
    assertThrows(IllegalStateException.class,()->billing.checkout(front,id,Arrays.asList(new PaymentLine(PaymentMethod.CASH,10000))));
    billing.applyDiscount(admin,id,1000,"管理员同意");billing.checkout(front,id,Arrays.asList(new PaymentLine(PaymentMethod.CASH,9000)));
    assertEquals(9000,number("SELECT paid_cents FROM bill WHERE id="+id));
  }
  @Test public void duplicateRefundMustBeRevokedAndApprovalPreservesApplicantRemark() throws Exception {
    long id=bill();billing.checkout(admin,id,Arrays.asList(new PaymentLine(PaymentMethod.CASH,10000)));
    long first=refunds.request(admin,id,5000,"独立申请原因","申请人备注");
    assertThrows(IllegalStateException.class,()->refunds.request(admin,id,1000,"重复申请",null));
    UserSession other=user("otherAdmin",RoleCode.ADMIN);refunds.approve(other,first,true,"审批人意见");
    assertEquals("申请人备注",text("SELECT remark FROM refund_request WHERE id="+first));
    assertEquals("独立申请原因",text("SELECT reason FROM refund_request WHERE id="+first));
    assertEquals("审批人意见",text("SELECT approval_remark FROM refund_request WHERE id="+first));
    refunds.revoke(admin,first,"需重新申请");assertEquals("REVOKED",text("SELECT status FROM refund_request WHERE id="+first));
    assertTrue(refunds.request(admin,id,1000,"重新申请",null)>first);
  }
  @Test public void tinyProportionalRefundSkipsZeroRowsAndExecutes() throws Exception {
    long id=bill();billing.checkout(admin,id,Arrays.asList(new PaymentLine(PaymentMethod.CASH,1),new PaymentLine(PaymentMethod.WECHAT,9999)));
    long request=refunds.request(admin,id,1,"分摊边界",null);UserSession other=user("otherAdmin",RoleCode.ADMIN);
    refunds.approve(other,request,true,"通过");refunds.execute(admin,request);
    assertEquals(1,number("SELECT refunded_cents FROM bill WHERE id="+id));
    assertEquals(0,number("SELECT COUNT(*) FROM refund_payment WHERE amount_cents<=0"));
  }
  @Test public void lockExpiryStartsNewConsecutiveFailureWindow() throws Exception {
    sql("UPDATE app_user SET failed_login_count=5,locked_until='"+LocalDateTime.now().minusMinutes(1)+"' WHERE id="+admin.userId);
    assertThrows(SecurityException.class,()->auth.login("admin","Wrong@123"));
    assertEquals(1,number("SELECT failed_login_count FROM app_user WHERE id="+admin.userId));
    auth.login("admin","AdminTest@123");assertEquals(0,number("SELECT failed_login_count FROM app_user WHERE id="+admin.userId));
  }
  @Test public void auditRowsAreAppendOnlyAndLocalOriginIsRecorded() throws Exception {
    assertEquals("127.0.0.1",text("SELECT ip_address FROM audit_log ORDER BY id DESC LIMIT 1"));
    assertThrows(SQLException.class,()->sql("UPDATE audit_log SET action='forged'"));
    assertThrows(SQLException.class,()->sql("DELETE FROM audit_log"));
  }
  @Test public void suspiciousPatientMustBeExplicitlyConfirmed() {
    bill();Patient p=new Patient();p.name="测试患者";p.gender=Gender.UNKNOWN;p.idType="测试证件";p.idNumber="ID2";p.birthDate=LocalDate.of(1980,1,1);p.phone="13800000002";
    PatientService.PossibleDuplicateException error=assertThrows(PatientService.PossibleDuplicateException.class,()->patients.create(admin,p));
    Set<Long> confirmed=new HashSet<Long>();for(Patient match:error.matches)confirmed.add(match.id);
    assertTrue(patients.create(admin,p,confirmed)>0);
  }
  @Test public void databaseReplacementRollsBackOnRecordingFailure() throws Exception {
    File candidate=new File(folder.getRoot(),"candidate.db");
    try(Connection c=db.dataSource().getConnection();PreparedStatement ps=c.prepareStatement("VACUUM INTO ?")){ps.setString(1,candidate.getAbsolutePath());ps.execute();}
    assertThrows(Exception.class,()->db.replace(candidate,()->{throw new IllegalStateException("注入审计失败");}));
    assertEquals(1,number("SELECT COUNT(*) FROM app_user"));assertTrue(Authorization.allowed(admin,Permission.SYSTEM_MANAGE));
  }
  @Test public void secondProcessCannotEnterRecoveryAgainstAnOpenDatabase() {
    assertThrows(IllegalStateException.class,()->new Database(db.file(),true));
  }
  @Test public void everyConnectionEnforcesForeignKeysAndBusyTimeout() throws Exception {
    assertEquals(1,number("PRAGMA foreign_keys"));assertEquals(10000,number("PRAGMA busy_timeout"));
  }
}
