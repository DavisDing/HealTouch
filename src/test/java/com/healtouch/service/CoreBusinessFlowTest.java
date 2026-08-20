package com.healtouch.service;

import static org.junit.Assert.*;

import com.healtouch.config.Database;
import com.healtouch.model.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CoreBusinessFlowTest {
  private Database database;
  private AuthService auth;
  private UserSession admin;
  private PatientService patients;
  private CatalogService catalog;
  private UserService users;
  private BillingService billing;
  private DepositService deposits;

  @Before
  public void setUp() throws Exception {
    File dbFile = Files.createTempDirectory("healtouch-test-").resolve("healtouch.db").toFile();
    database = new Database(dbFile);
    auth = new AuthService(database.dataSource());
    auth.initializeAdministrator("TestAdmin@123");
    admin = auth.login("admin", "TestAdmin@123");
    patients = new PatientService(database.dataSource());
    catalog = new CatalogService(database.dataSource());
    users = new UserService(database.dataSource());
    billing = new BillingService(database.dataSource());
    deposits = new DepositService(database.dataSource());
  }

  @After
  public void tearDown() {
    if (database != null) database.close();
  }

  @Test
  public void fullPaymentMustMatchAndDepositConsumptionIsTraceable() {
    Patient patient = new Patient();
    patient.patientType = PatientType.ADULT;
    patient.name = "张三";
    patient.gender = Gender.MALE;
    patient.idType = "身份证";
    patient.idNumber = "110101199001010011";
    patient.birthDate = LocalDate.of(1990, 1, 1);
    patient.phone = "13800000000";
    long patientId = patients.create(admin, patient);
    long projectId =
        catalog.addProject(admin, project("颈肩推拿", catalog.categories(true).get(0).id, 10000));
    long therapistId =
        users.create(
            admin,
            "治疗师甲",
            "therapist-a",
            "13800000001",
            "Therapist@123",
            EnumSet.of(RoleCode.THERAPIST));
    deposits.recharge(admin, patientId, 4000, PaymentMethod.CASH, "测试充值");
    long billId =
        billing.createPendingBill(
            admin,
            patientId,
            LocalDate.now(),
            therapistId,
            "测试",
            Arrays.asList(new TreatmentLine(projectId, "错误名称", 1, 1)));
    try {
      billing.checkout(admin, billId, Arrays.asList(new PaymentLine(PaymentMethod.CASH, 9999)));
      fail("应拒绝部分支付");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("实收总额"));
    }
    billing.checkout(
        admin,
        billId,
        Arrays.asList(
            new PaymentLine(PaymentMethod.DEPOSIT, 4000),
            new PaymentLine(PaymentMethod.CASH, 6000)));
    assertEquals(0, deposits.balance(admin, patientId));
    BillSummary bill = billing.list("", null).get(0);
    assertEquals(BillStatus.PAID, bill.status);
    assertEquals(10000, bill.paidCents);
    assertEquals(0, bill.refundedCents);
    assertEquals(patientId, bill.patientId);
    assertEquals(TransactionType.CONSUMPTION, deposits.history(admin, patientId).get(0).type);
    assertEquals(-4000, deposits.history(admin, patientId).get(0).amountCents);
  }

  @Test
  public void initialAdministratorPasswordIsNotHardCoded() {
    try {
      auth.login("admin", "Admin@123");
      fail("不应接受历史默认密码");
    } catch (SecurityException expected) {
      assertEquals("账号或密码错误", expected.getMessage());
    }
  }

  @Test
  public void patientSearchSupportsPaginationBeyondOneHundredRecords() {
    for (int index = 0; index < 101; index++) {
      Patient patient = new Patient();
      patient.patientType = PatientType.ADULT;
      patient.name = "分页患者" + index;
      patient.gender = Gender.UNKNOWN;
      patient.idType = "测试证件";
      patient.idNumber = "PAGE-" + index;
      patient.birthDate = LocalDate.of(1990, 1, 1);
      patient.phone = String.format("139%08d", index);
      patients.create(admin, patient);
    }

    PageResult<Patient> first = patients.searchPage(admin, "分页患者", 1, 50);
    PageResult<Patient> third = patients.searchPage(admin, "分页患者", 3, 50);

    assertEquals(101, first.getTotalItems());
    assertEquals(3, first.getTotalPages());
    assertEquals(50, first.getItems().size());
    assertEquals(1, third.getItems().size());
    assertTrue(third.hasPreviousPage());
    assertFalse(third.hasNextPage());
  }

  @Test
  public void patientTypeIsDerivedFromBirthDateUsingEighteenthBirthday() {
    Patient adult = new Patient();
    adult.patientType = PatientType.CHILD; // The supplied type must not override the date-derived value.
    adult.name = "刚满十八岁";
    adult.gender = Gender.FEMALE;
    adult.idType = "身份证";
    adult.idNumber = "AGE-18";
    adult.birthDate = LocalDate.now().minusYears(18);
    adult.phone = "13700000000";
    long adultId = patients.create(admin, adult);
    assertEquals(PatientType.ADULT, patients.get(admin, adultId).patientType);

    Patient minor = new Patient();
    minor.patientType = PatientType.ADULT;
    minor.name = "尚未成年";
    minor.gender = Gender.MALE;
    minor.idType = "病历卡";
    minor.idNumber = "AGE-17";
    minor.birthDate = LocalDate.now().minusYears(18).plusDays(1);
    minor.phone = "13700000001";
    minor.guardianName = "家长";
    minor.guardianRelationship = "父亲";
    minor.guardianPhone = "13700000002";
    long minorId = patients.create(admin, minor);
    assertEquals(PatientType.CHILD, patients.get(admin, minorId).patientType);
  }

  @Test
  public void childPatientRequiresGuardian() {
    Patient child = new Patient();
    child.patientType = PatientType.CHILD;
    child.name = "小明";
    child.gender = Gender.MALE;
    child.idType = "出生证明";
    child.idNumber = "B-001";
    child.birthDate = LocalDate.of(2024, 1, 1);
    child.phone = "13900000000";
    try {
      patients.create(admin, child);
      fail("小儿缺少监护人信息时应被拒绝");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("监护人"));
    }
  }

  private TreatmentProject project(String name, long categoryId, long price) {
    TreatmentProject p = new TreatmentProject();
    p.name = name;
    p.categoryId = categoryId;
    p.priceCents = price;
    p.active = true;
    return p;
  }
}
