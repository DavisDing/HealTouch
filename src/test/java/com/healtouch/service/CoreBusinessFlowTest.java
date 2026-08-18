package com.healtouch.service;

import com.healtouch.config.Database;
import com.healtouch.model.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class CoreBusinessFlowTest {
    private Database database;
    private AuthService auth;
    private UserSession admin;
    private PatientService patients;
    private CatalogService catalog;
    private UserService users;
    private BillingService billing;
    private DepositService deposits;

    @Before public void setUp() throws Exception {
        File dbFile = Files.createTempDirectory("healtouch-test-").resolve("healtouch.db").toFile();
        database = new Database(dbFile);
        auth = new AuthService(database.dataSource()); auth.ensureInitialAdministrator();
        admin = auth.login("admin", "Admin@123");
        patients = new PatientService(database.dataSource()); catalog = new CatalogService(database.dataSource());
        users = new UserService(database.dataSource()); billing = new BillingService(database.dataSource()); deposits = new DepositService(database.dataSource());
    }
    @After public void tearDown() { if (database != null) database.close(); }

    @Test public void fullPaymentMustMatchAndDepositConsumptionIsTraceable() {
        Patient patient = new Patient(); patient.patientType=PatientType.ADULT; patient.name="张三";patient.gender=Gender.MALE;
        patient.idType="身份证";patient.idNumber="110101199001010011";patient.birthDate=LocalDate.of(1990,1,1);patient.phone="13800000000";
        long patientId=patients.create(admin,patient);
        long projectId=catalog.addProject(admin, project("颈肩推拿",catalog.categories(true).get(0).id,10000));
        long therapistId=users.create(admin,"治疗师甲","therapist-a","13800000001","Therapist@123", EnumSet.of(RoleCode.THERAPIST));
        deposits.recharge(admin,patientId,4000,PaymentMethod.CASH,"测试充值");
        long billId=billing.createPendingBill(admin,patientId,LocalDate.now(),therapistId,"测试",Arrays.asList(new TreatmentLine(projectId,"错误名称",1,1)));
        try { billing.checkout(admin,billId,Arrays.asList(new PaymentLine(PaymentMethod.CASH,9999))); fail("应拒绝部分支付"); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("实收总额")); }
        billing.checkout(admin,billId,Arrays.asList(new PaymentLine(PaymentMethod.DEPOSIT,4000),new PaymentLine(PaymentMethod.CASH,6000)));
        assertEquals(0,deposits.balance(admin,patientId));
        BillSummary bill=billing.list("",null).get(0);
        assertEquals(BillStatus.PAID,bill.status);assertEquals(10000,bill.paidCents);assertEquals(0,bill.refundedCents);
    }
    @Test public void childPatientRequiresGuardian() {
        Patient child=new Patient();child.patientType=PatientType.CHILD;child.name="小明";child.gender=Gender.MALE;child.idType="出生证明";child.idNumber="B-001";child.birthDate=LocalDate.of(2024,1,1);child.phone="13900000000";
        try { patients.create(admin,child);fail("小儿缺少监护人信息时应被拒绝"); } catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("监护人")); }
    }
    private TreatmentProject project(String name,long categoryId,long price){TreatmentProject p=new TreatmentProject();p.name=name;p.categoryId=categoryId;p.priceCents=price;p.active=true;return p;}
}
